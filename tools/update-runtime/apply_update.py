"""Owned-app handoff: verified target -> wait for A -> fresh install -> B receipt.

No old-binary/data rollback, forced writer termination, public trust changes or keys.
"""
import argparse,base64,ctypes,json,os,re,subprocess,time,uuid,hashlib
from ctypes import wintypes
from pathlib import Path
from studio_update import Installed,UpdateRejected,exclusive,inspect,create_updater,authorize_download
from install_ops import atomic_json,no_reparse,install_candidate

def data_destination(plan,source,identity,custom):
    """Bind a fresh copy destination to the confirmed, authenticated data transition."""
    output=custom.get('data_version',identity.reader)
    if plan.get('sourceDataVersion',identity.reader)!=identity.reader or plan.get('targetDataVersion',identity.reader)!=output:
        raise UpdateRejected('Data transition changed after confirmation')
    destination=no_reparse(Path(plan.get('destinationDataRoot',str(source))))
    if output==identity.reader:
        if destination!=source:raise UpdateRejected('Unexpected data relocation')
    else:
        expected=source.with_name(source.name+'-format'+str(output)+'-'+plan['nonce'][:12])
        if destination!=expected or destination.exists():raise UpdateRejected('Fresh confirmed migration destination required')
    return destination

def create_launcher(final,data,runtime):
    # Same byte-range lock as the native app's entrypoint registration. A delayed
    # older writer must recheck the descriptor under this lock before committing.
    import msvcrt
    lock=no_reparse(final.parent/'entrypoints.lock')
    with lock.open('a+b') as stream:
        if stream.seek(0,2)==0:stream.write(b'0');stream.flush()
        stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
        try:return _create_launcher_locked(final,data,runtime)
        finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)

def _create_launcher_locked(final,data,runtime):
    """Windows Shell Link preserves Unicode paths without batch parsing/codepage rules."""
    launcher=final.parent/'Launch-CCB-Studio.lnk'
    bundled=final/'updater/CCBStudioLauncher.exe'
    if bundled.is_file():
        stable=no_reparse(final.parent/'CCBStudioLauncher.exe')
        body=bundled.read_bytes()
        if not stable.exists() or stable.read_bytes()!=body:
            temporary=stable.with_suffix('.'+uuid.uuid4().hex+'.tmp')
            temporary.write_bytes(body);os.replace(temporary,stable)
        config=json.loads((final/'update-config.json').read_text(encoding='utf-8'))
        atomic_json(no_reparse(final.parent/'launch-current.json'),dict(format=1,version=config['version'],installed=str(final),data=str(data),runtime=str(runtime),exeSha256=hashlib.sha256((final/'CCBStudioSuitePreview.exe').read_bytes()).hexdigest()))
        result=subprocess.run([str(stable),'--shortcut',str(launcher)],creationflags=subprocess.CREATE_NO_WINDOW)
        if result.returncode:raise UpdateRejected('Could not create stable application shortcut')
        return launcher
    temporary=launcher.with_name(launcher.stem+'.'+uuid.uuid4().hex+'.tmp.lnk')
    def literal(value):
        value=str(value)
        if any(c in value for c in '\r\n\0'):raise UpdateRejected('Unsupported shortcut path')
        return "'"+value.replace("'","''")+"'"
    args=subprocess.list2cmdline(['--studio-root',str(data),'--runtime-root',str(runtime)])
    script="$ErrorActionPreference='Stop';$shell=New-Object -ComObject WScript.Shell;$link=$shell.CreateShortcut("+literal(temporary)+");$link.TargetPath="+literal(final/'CCBStudioSuitePreview.exe')+";$link.Arguments="+literal(args)+";$link.WorkingDirectory="+literal(final)+";$link.Save()"
    encoded=base64.b64encode(script.encode('utf-16-le')).decode('ascii')
    result=subprocess.run(['powershell.exe','-NoProfile','-NonInteractive','-EncodedCommand',encoded],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if result.returncode or not temporary.is_file():raise UpdateRejected('Could not create verified-version shortcut')
    os.replace(temporary,launcher)
    return launcher

class ParentProcess:
    def __init__(self,pid,expected_image):
        self.kernel=ctypes.WinDLL('kernel32',use_last_error=True)
        self.kernel.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];self.kernel.OpenProcess.restype=wintypes.HANDLE
        self.kernel.CloseHandle.argtypes=[wintypes.HANDLE]
        self.kernel.WaitForSingleObject.argtypes=[wintypes.HANDLE,wintypes.DWORD];self.kernel.WaitForSingleObject.restype=wintypes.DWORD
        self.kernel.QueryFullProcessImageNameW.argtypes=[wintypes.HANDLE,wintypes.DWORD,wintypes.LPWSTR,ctypes.POINTER(wintypes.DWORD)]
        self.handle=self.kernel.OpenProcess(0x100000|0x1000,False,pid)
        if not self.handle:raise ctypes.WinError(ctypes.get_last_error())
        try:
            buffer=ctypes.create_unicode_buffer(32768);length=wintypes.DWORD(len(buffer))
            if not self.kernel.QueryFullProcessImageNameW(self.handle,0,buffer,ctypes.byref(length)):raise ctypes.WinError(ctypes.get_last_error())
            if Path(buffer.value).resolve()!=expected_image.resolve():raise UpdateRejected('Parent executable identity mismatch')
        except BaseException:self.close();raise
    def exited(self):
        result=self.kernel.WaitForSingleObject(self.handle,0)
        if result not in [0,258]:raise ctypes.WinError(ctypes.get_last_error())
        return result==0
    def close(self):
        if self.handle:self.kernel.CloseHandle(self.handle);self.handle=None

def perform(plan_path):
    plan_path=no_reparse(plan_path)
    with exclusive(plan_path.parent):return _perform(plan_path)

def _perform(plan_path):
    plan_path=no_reparse(plan_path)
    if plan_path.stat().st_size>4*1024**2:raise UpdateRejected('Oversized handoff')
    plan=json.loads(plan_path.read_text(encoding='utf-8'));folder=plan_path.parent;result_path=folder/'result.json'
    if plan.get('format')!=1 or not re.fullmatch('[0-9a-f]{64}',plan.get('nonce','')):raise UpdateRejected('Unsupported handoff')
    installed=no_reparse(Path(plan['installed']));cache=no_reparse(Path(plan['cache']));data=no_reparse(Path(plan['dataRoot']));runtime=no_reparse(Path(plan['runtimeRoot']))
    if not plan_path.is_relative_to(cache/'handoffs'):raise UpdateRejected('Handoff outside cache')
    if not (data/'.owner.lock').exists():raise UpdateRejected('Existing workspace ownership marker required')
    parent=None;record={'status':'PREPARING','nonce':plan['nonce'],'dataModifiedByInstaller':False}
    def update(status,**values):
        record.update(status=status,**values)
        with (folder/'events.jsonl').open('a',encoding='utf-8') as stream:
            stream.write(json.dumps(dict(timeUtc=time.time(),**record))+'\n');stream.flush();os.fsync(stream.fileno())
        atomic_json(result_path,record)
    if result_path.exists():
        previous=json.loads(result_path.read_text(encoding='utf-8'))
        if previous.get('nonce')!=plan['nonce']:raise UpdateRejected('Handoff receipt identity mismatch')
        if previous.get('status')=='COMPLETE':return 0
        if previous.get('status')=='FAILED_RETAINED':return 1
        update('FAILED_RETAINED',reason='Interrupted handoff retained; start a new checked update from the existing version',previousStatus=previous.get('status'))
        return 1
    try:
        parent=ParentProcess(plan['parentPid'],installed/'CCBStudioSuitePreview.exe')
        identity=Installed.load(installed)
        with exclusive(cache):
            updater,observed=create_updater(identity,cache/'metadata',cache/'downloads')
            target,custom=inspect(updater,identity)
            authorize_download(observed,target,identity)
            approved=plan['approved']
            if target.hashes['sha256']!=approved['sha256'] or custom['version']!=approved['version'] or custom['sourceSha']!=approved['sourceSha']:
                raise UpdateRejected('Release changed after confirmation; confirmation must be repeated')
            destination=data_destination(plan,data,identity,custom)
            if approved.get('dataVersion',identity.reader)!=custom.get('data_version',identity.reader):raise UpdateRejected('Output data format changed after confirmation')
            installer=no_reparse(Path(approved['installer']))
            update('READY_TO_DRAIN')
            deadline=time.monotonic()+120
            while not parent.exited():
                if (folder/'CANCEL').exists():raise UpdateRejected('Handoff cancelled before installation')
                if time.monotonic()>deadline:raise UpdateRejected('Parent drain timed out; data lock was not forced')
                time.sleep(.1)
            parent.close();parent=None
            if (folder/'CANCEL').exists():raise UpdateRejected('Handoff cancelled before installation')
            update('INSTALLING')
            final,journal,install_record=install_candidate(installer,target,custom,installed,cache,data)
            update('STARTING_B',installed=str(final),installJournal=str(journal))
            # B receives only the controlled handoff path and the same explicit roots.
            args=[str(final/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime),'--update-handoff',str(plan_path)]
            with (folder/'b-stdout.log').open('wb') as stdout,(folder/'b-stderr.log').open('wb') as stderr:
                child=subprocess.Popen(args,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
            deadline=time.monotonic()+120;ack=folder/'B_VERIFIED.json'
            while not ack.exists():
                if (folder/'B_FAILED.json').exists():raise UpdateRejected('B program/data verification failed; old version and data retained')
                if child.poll() is not None:raise UpdateRejected('B exited before verification; old version and data retained')
                if time.monotonic()>deadline:raise UpdateRejected('B verification timed out; no process killed or data rolled back')
                time.sleep(.1)
            if ack.stat().st_size>4*1024**2:raise UpdateRejected('Oversized B acknowledgement')
            verified=json.loads(ack.read_text(encoding='utf-8'))
            if verified.get('nonce')!=plan['nonce'] or verified.get('version')!=custom['version'] or verified.get('sourceSha')!=custom['sourceSha'] or verified.get('snapshot')!=plan['snapshot']:
                raise UpdateRejected('B data/credential verification mismatch')
            if Path(verified.get('dataRoot',str(data))).absolute()!=destination or verified.get('dataVersion',identity.reader)!=custom.get('data_version',identity.reader):
                raise UpdateRejected('B data destination/format mismatch')
            # A stable launcher is committed only after B verifies the actual workspace.
            launcher=create_launcher(final,destination,runtime)
            update('COMPLETE',verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH',launcher=str(launcher),dataRoot=str(destination),sourceDataRoot=str(data))
    except Exception as error:
        update('FAILED_RETAINED',reason=str(error) if isinstance(error,UpdateRejected) else type(error).__name__)
        return 1
    finally:
        if parent:parent.close()
    return 0

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--handoff',required=True,type=Path);a=p.parse_args()
    raise SystemExit(perform(a.handoff))
