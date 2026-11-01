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

def read_entrypoint(root):
    path=no_reparse(root/'launch-current.json')
    if not path.exists():return None
    if path.stat().st_size>16384:raise UpdateRejected('Oversized default entrypoint')
    value=json.loads(path.read_text(encoding='utf-8'))
    if value.get('format')!=1 or type(value.get('version')) is not int:raise UpdateRejected('Invalid default entrypoint')
    for key in ('installed','data','runtime'):
        if not isinstance(value.get(key),str) or not Path(value[key]).is_absolute():raise UpdateRejected('Invalid default entrypoint root')
        no_reparse(Path(value[key]))
    return value


class EntrypointRecoveryRequired(UpdateRejected):
    pass

class UpdateCancelled(UpdateRejected):
    pass

def create_launcher(final,data,runtime,expected_current=None,expected_absent=False,migration_source=None,repair_manifest_sha256=None,publication_admission=None):
    # Same byte-range lock as the native app's entrypoint registration. A delayed
    # older writer must recheck the descriptor under this lock before committing.
    import msvcrt
    lock=no_reparse(final.parent/'entrypoints.lock')
    with lock.open('a+b') as stream:
        if stream.seek(0,2)==0:stream.write(b'0');stream.flush()
        stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
        try:return _create_launcher_locked(final,data,runtime,expected_current,expected_absent,migration_source,repair_manifest_sha256,publication_admission)
        finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)

def _create_launcher_locked(final,data,runtime,expected_current=None,expected_absent=False,migration_source=None,repair_manifest_sha256=None,publication_admission=None):
    """Windows Shell Link preserves Unicode paths without batch parsing/codepage rules."""
    final=no_reparse(final);data=no_reparse(data);runtime=no_reparse(runtime)
    current=read_entrypoint(final.parent)
    if expected_absent and current is not None or expected_current is not None and current!=expected_current:
        raise UpdateRejected('Default entrypoint changed during update')
    if repair_manifest_sha256 is not None and (current is None or expected_current is None or expected_absent or migration_source is not None):
        raise UpdateRejected('Repair publication requires the exact existing entrypoint')
    config_path=no_reparse(final/'update-config.json')
    config=json.loads(config_path.read_text(encoding='utf-8')) if config_path.exists() else None
    if current is not None:
        if config is None or type(config.get('version')) is not int or config['version']<current['version']:
            raise UpdateRejected('Default entrypoint cannot move to an older program')
        same_data=no_reparse(Path(current['data']))==data
        migration=(migration_source is not None and expected_current is not None
            and no_reparse(Path(current['data']))==no_reparse(migration_source))
        if (not same_data and not migration) or no_reparse(Path(current['runtime']))!=runtime:
            raise UpdateRejected('Secondary workspace cannot replace the default entrypoint')
        if config['version']==current['version'] and (no_reparse(Path(current['installed']))!=final or not same_data):
            if repair_manifest_sha256 is None or not same_data or expected_current is None:
                raise UpdateRejected('Same-version entrypoint identity changed')
        if repair_manifest_sha256 is not None:
            # Only the standalone authenticated-package repair path supplies this
            # reference. Normal updates cannot relax the same-version CAS rule.
            from install_ops import verify_install
            if not re.fullmatch('[0-9a-f]{64}',repair_manifest_sha256):
                raise UpdateRejected('Invalid repair package reference')
            proof=verify_install(final,{k:config[k] for k in ['product','platform','channel','version','sourceSha']})
            if proof['manifestSha256']!=repair_manifest_sha256:
                raise UpdateRejected('Repair output differs from package reference')
    launcher=final.parent/'Launch-CCB-Studio.lnk'
    bundled=final/'updater/CCBStudioLauncher.exe'
    if bundled.is_file():
        stable=no_reparse(final.parent/'CCBStudioLauncher.exe')
        if config is None:raise UpdateRejected('Missing installed program configuration')
        descriptor=dict(format=1,version=config['version'],installed=str(final),data=str(data),runtime=str(runtime),exeSha256=hashlib.sha256((final/'CCBStudioSuitePreview.exe').read_bytes()).hexdigest())
        if current is not None and 'previousRoots' in current:
            from residue_cleanup import relocation_projection
            relocation_projection(final.parent,current) # Validate before propagating local move history.
            descriptor['previousRoots']=current['previousRoots']
        publish_entrypoints(final.parent,bundled,launcher,descriptor,current,publication_admission)
        return launcher
    # Supported update packages always contain the native launcher. The legacy
    # direct-link path cannot provide descriptor-based publication semantics.
    if publication_admission is not None:raise UpdateRejected('Native launcher required for update publication')
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

def publish_entrypoints(root,bundled,launcher,descriptor,current,admission=None):
    """Caller holds entrypoints.lock. Prepare first; descriptor replace commits.
    Failed precommit writes are restored only if still equal to our own bytes.
    Recovery material stays local; neither a newer descriptor nor unknown bytes
    are ever rolled back. A power loss requires journal inspection, not guessing.
    """
    stable=no_reparse(root/'CCBStudioLauncher.exe');launcher=no_reparse(launcher)
    descriptor_path=no_reparse(root/'launch-current.json')
    transaction=no_reparse(root/('.entrypoint-'+uuid.uuid4().hex));transaction.mkdir()
    journal=transaction/'journal.json'
    record=dict(format=1,status='PREPARING',commitPoint='launch-current.json',previous=current,desired=descriptor)
    atomic_json(journal,record)
    targets=[stable,launcher];before={};desired={};written=[];attempted=False
    try:
        for target in targets:
            before[target]=target.read_bytes() if target.exists() else None
            if before[target] is not None:(transaction/(target.name+'.before')).write_bytes(before[target])
        exe=transaction/'launcher.prepared.exe';exe.write_bytes(bundled.read_bytes())
        link=transaction/'launcher.prepared.lnk'
        result=subprocess.run([str(bundled),'--prepare-shortcut',str(link),str(stable),str(launcher)],creationflags=subprocess.CREATE_NO_WINDOW)
        if result.returncode or not link.is_file():raise UpdateRejected('Could not prepare stable application shortcut')
        desired={stable:exe.read_bytes(),launcher:link.read_bytes()}
        record.update(status='PREPARED',files={p.name:dict(beforeSha256=hashlib.sha256(before[p]).hexdigest() if before[p] is not None else None,
            desiredSha256=hashlib.sha256(desired[p]).hexdigest()) for p in targets});atomic_json(journal,record)
        if read_entrypoint(root)!=current:raise UpdateRejected('Default entrypoint changed during preparation')
        if admission is not None:
            admitted=admission() # Accepted CANCEL/FAIL/deadline wins before this point.
            if admitted is not None:
                record['admission']=admitted
                atomic_json(no_reparse(Path(admitted['folder'])/'PUBLICATION_ADMITTED.json'),
                    dict(format=1,nonce=admitted['nonce'],transaction=str(transaction)))
        record['status']='PUBLISHING';atomic_json(journal,record)
        for source,target in [(exe,stable),(link,launcher)]:
            written.append(target) # Includes a replace that takes effect then raises.
            os.replace(source,target)
        attempted=True
        atomic_json(descriptor_path,descriptor) # Single semantic commit point.
    except Exception:
        try:observed=read_entrypoint(root)
        except Exception:observed=object() # Unreadable is not permission to restore.
        if attempted and observed==descriptor:
            record['status']='COMMITTED_RECEIPT_RECOVERED'
        else:
            restored=True
            # All cooperating writers use entrypoints.lock. Still use a byte CAS
            # so external changes are retained, never overwritten by recovery.
            if observed!=current:restored=False
            else:
                for target in reversed(written):
                    try:
                        actual=target.read_bytes() if target.exists() else None
                        if actual==before[target]:continue
                        if actual!=desired[target]:restored=False;continue
                        if before[target] is None:target.unlink()
                        else:
                            restore=transaction/(target.name+'.restore');restore.write_bytes(before[target]);os.replace(restore,target)
                    except Exception:restored=False
            record['status']='FAILED_RESTORED' if restored else 'ENTRYPOINT_RECOVERY_REQUIRED'
            try:atomic_json(journal,record)
            except OSError:
                if not restored:raise EntrypointRecoveryRequired('Entrypoint publication incomplete; retain transaction '+str(transaction))
                raise
            if not restored:raise EntrypointRecoveryRequired('Entrypoint publication incomplete; retain journal '+str(journal))
            raise
    else:record['status']='COMMITTED'
    # Failure of a supplementary receipt after descriptor commit is not an
    # unpublished failure. The descriptor is the authoritative committed state.
    try:atomic_json(journal,record)
    except OSError:pass

def recovery_body(path,limit):
    """Bounded read of the exact non-reparse handle, never a journal-selected file.
    Existing ownership helpers hold parents and deny write/rename during read.
    """
    import msvcrt,_winapi
    from owned_files import held,held_parents
    with held_parents(path),held(path) as (kernel,handle,info):
        if info.attributes&0x10 or (info.sizeHigh<<32|info.sizeLow)>limit:
            raise ValueError('Invalid or oversized recovery proof')
        copy=_winapi.DuplicateHandle(_winapi.GetCurrentProcess(),handle,_winapi.GetCurrentProcess(),0,False,2)
        fd=msvcrt.open_osfhandle(copy,os.O_RDONLY|os.O_BINARY)
        with os.fdopen(fd,'rb') as stream:body=stream.read(limit+1)
        if len(body)>limit:raise ValueError('Oversized recovery proof')
        return body

def recovery_entries_match(root,files,field):
    limits={'CCBStudioLauncher.exe':16*1024**2,'Launch-CCB-Studio.lnk':1024**2}
    if type(files) is not dict or set(files)!=set(limits):return False
    for name,limit in limits.items():
        item=files[name]
        if type(item) is not dict or set(item)!={'beforeSha256','desiredSha256'}:return False
        for key in ('beforeSha256','desiredSha256'):
            value=item[key]
            if value is None and key=='beforeSha256':continue
            if type(value) is not str or not re.fullmatch('[0-9a-f]{64}',value):return False
        expected=item[field];path=root/name
        if expected is None:
            # lstat also sees a broken symlink: it is not an absent old entry.
            from owned_files import held_parents
            with held_parents(path):
                try:os.lstat(path)
                except FileNotFoundError:continue
            return False
        if hashlib.sha256(recovery_body(path,limit)).hexdigest()!=expected:return False
    return True

def interrupted_publication_status(folder,root,nonce):
    """Reconcile only status after interruption; never mutate entrypoints.
    An admitted but incomplete transaction is not a FAILED_RETAINED promise.
    """
    try:
        root=no_reparse(root);folder=no_reparse(folder)
        try:body=recovery_body(folder/'PUBLICATION_ADMITTED.json',16384)
        except FileNotFoundError:return None
        value=json.loads(body.decode('utf-8'))
        if type(value) is not dict or type(value.get('format')) is not int or value['format']!=1 or value.get('nonce')!=nonce:
            return 'ENTRYPOINT_RECOVERY_REQUIRED'
        if type(value.get('transaction')) is not str or not Path(value['transaction']).is_absolute():return 'ENTRYPOINT_RECOVERY_REQUIRED'
        transaction=no_reparse(Path(value['transaction']))
        if transaction.parent!=root or not re.fullmatch(r'\.entrypoint-[0-9a-f]{32}',transaction.name):return 'ENTRYPOINT_RECOVERY_REQUIRED'
        journal=json.loads(recovery_body(transaction/'journal.json',65536).decode('utf-8'))
        if type(journal) is not dict or type(journal.get('format')) is not int or journal['format']!=1 or journal.get('commitPoint')!='launch-current.json':
            return 'ENTRYPOINT_RECOVERY_REQUIRED'
        if journal.get('admission')!=dict(nonce=nonce,folder=str(folder)):return 'ENTRYPOINT_RECOVERY_REQUIRED'
        try:current=json.loads(recovery_body(root/'launch-current.json',16384).decode('utf-8'))
        except FileNotFoundError:current=None
        if current is not None:
            if type(current) is not dict or type(current.get('format')) is not int or current['format']!=1 or type(current.get('version')) is not int:
                return 'ENTRYPOINT_RECOVERY_REQUIRED'
            if any(type(current.get(k)) is not str or not Path(current[k]).is_absolute() for k in ('installed','data','runtime')):
                return 'ENTRYPOINT_RECOVERY_REQUIRED'
            for key in ('installed','data','runtime'):no_reparse(Path(current[key]))
        if current==journal['desired'] and journal['status'] in ('PUBLISHING','COMMITTED','COMMITTED_RECEIPT_RECOVERED'):
            if current is not None and recovery_entries_match(root,journal.get('files'),'desiredSha256'):
                return 'COMPLETE_RECEIPT_RECOVERED'
        if current==journal['previous'] and journal['status']=='FAILED_RESTORED':
            if recovery_entries_match(root,journal.get('files'),'beforeSha256'):return 'FAILED_RETAINED'
    except (OSError,ValueError,KeyError,TypeError):pass
    return 'ENTRYPOINT_RECOVERY_REQUIRED'

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

def start_status_window(installed,folder):
    config=json.loads((installed/'update-config.json').read_text(encoding='utf-8'))
    if config.get('updateStatusUi')!=1:return None
    executable=no_reparse(installed/'updater/CCBStudioLauncher.exe')
    return subprocess.Popen([str(executable),'--update-status',str(folder)],cwd=installed,creationflags=subprocess.CREATE_NO_WINDOW)

def perform(plan_path):
    plan_path=no_reparse(plan_path)
    with exclusive(plan_path.parent):return _perform(plan_path)

def start_b(final,data,runtime,plan_path):
    folder=plan_path.parent
    args=[str(final/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime),'--update-handoff',str(plan_path)]
    with (folder/'b-stdout.log').open('wb') as stdout,(folder/'b-stderr.log').open('wb') as stderr:
        return subprocess.Popen(args,cwd=final,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)


def start_cleanup(final,cache,data,runtime):
    cleanup=final/'updater/lib/residue_cleanup.py'
    if not cleanup.is_file():return
    log=no_reparse(cache/'cleanup');log.mkdir(exist_ok=True)
    with (log/'post-update-process.log').open('ab') as output:
        return subprocess.Popen([str(final/'updater/pythonw.exe'),'-B',str(cleanup),'--package',str(final),'--cache',str(cache),'--data',str(data),'--runtime',str(runtime)],cwd=final,stdout=output,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)


def verification_open(folder,deadline,clock=time.monotonic):
    if (folder/'B_FAILED.json').exists():raise UpdateRejected('B program/data verification failed; old version and data retained')
    if (folder/'CANCEL').exists():raise UpdateCancelled('Verification wait cancelled; program/data retained; no process killed')
    if clock()>=deadline:raise UpdateRejected('B verification deadline reached; verified installation retained for checked retry')

def read_b_ack(folder,plan,identity,custom,destination):
    ack=no_reparse(folder/'B_VERIFIED.json')
    if ack.stat().st_size>4*1024**2:raise UpdateRejected('Oversized B acknowledgement')
    verified=json.loads(ack.read_text(encoding='utf-8'))
    fields={'nonce':str,'version':int,'sourceSha':str,'snapshot':dict,'dataRoot':str,'dataVersion':int}
    if not isinstance(verified,dict) or any(type(verified.get(k)) is not kind for k,kind in fields.items()):
        raise UpdateRejected('Incomplete or invalid B acknowledgement fields')
    # Existing B protocol has no envelope format field; dataVersion explicitly
    # carries the reader format. Never synthesize it from the expected reader.
    if 'format' in verified and (type(verified['format']) is not int or verified['format']!=1):
        raise UpdateRejected('Unsupported B acknowledgement format')
    if verified['nonce']!=plan['nonce'] or verified['version']!=custom['version'] or verified['sourceSha']!=custom['sourceSha'] or verified['snapshot']!=plan['snapshot']:
        raise UpdateRejected('B data/credential verification mismatch')
    root=Path(verified['dataRoot'])
    if not root.is_absolute() or any(c in verified['dataRoot'] for c in '\r\n\0') or no_reparse(root)!=destination or verified['dataVersion']!=custom.get('data_version',identity.reader):
        raise UpdateRejected('B data destination/format mismatch')
    return verified

def admit_publication(folder,proof,plan,identity,custom,destination,update,clock=time.monotonic):
    # Same one-byte gate used by native Cancel. Lock order: entrypoints -> gate.
    # The atomic ADMITTED marker linearizes acceptance: later cancellation is
    # reported too late and never kills B or rolls back a committed descriptor.
    import msvcrt
    with no_reparse(folder/'publication.lock').open('a+b') as stream:
        if stream.seek(0,2)==0:stream.write(b'0');stream.flush()
        stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
        try:
            verification_open(folder,proof['deadline'],clock)
            if read_b_ack(folder,plan,identity,custom,destination)!=proof['verified']:
                raise UpdateRejected('B acknowledgement changed before publication')
            verification_open(folder,proof['deadline'],clock)
            atomic_json(no_reparse(folder/'PUBLICATION_ADMITTED.json'),dict(format=1,nonce=plan['nonce']))
            update('COMMITTING',cancelAccepted=False)
            return dict(nonce=plan['nonce'],folder=str(folder))
        finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)

def wait_for_b(folder,child,plan,identity,custom,destination,update,timeout=1800,clock=time.monotonic,sleep=time.sleep,proof=None):
    """Bounded, observable verification wait; a live verifier is not failed at120s.
    Existing/late receipts never bypass the nonce, program, root and data proof.
    """
    deadline=clock()+timeout;started=clock();next_status=started
    ack=folder/'B_VERIFIED.json'
    while True:
        verification_open(folder,deadline,clock)
        if ack.exists():break
        exited=child.poll() is not None
        if ack.exists():break
        if exited:raise UpdateRejected('B exited before verification; old version and data retained')
        now=clock()
        if now>=next_status:
            update('VERIFYING_B',verificationElapsedSeconds=max(0,int(now-started)),verificationDeadlineSeconds=timeout)
            next_status=now+15
        sleep(.1)
    verified=read_b_ack(folder,plan,identity,custom,destination)
    verification_open(folder,deadline,clock)
    if proof is not None:proof.update(deadline=deadline,verified=verified)
    return verified

def _perform(plan_path):
    plan_path=no_reparse(plan_path)
    if plan_path.stat().st_size>4*1024**2:raise UpdateRejected('Oversized handoff')
    plan=json.loads(plan_path.read_text(encoding='utf-8'));folder=plan_path.parent;result_path=folder/'result.json'
    if plan.get('format')!=1 or not re.fullmatch('[0-9a-f]{64}',plan.get('nonce','')):raise UpdateRejected('Unsupported handoff')
    installed=no_reparse(Path(plan['installed']));cache=no_reparse(Path(plan['cache']));data=no_reparse(Path(plan['dataRoot']));runtime=no_reparse(Path(plan['runtimeRoot']))
    if not plan_path.is_relative_to(cache/'handoffs'):raise UpdateRejected('Handoff outside cache')
    if not (data/'.owner.lock').exists():raise UpdateRejected('Existing workspace ownership marker required')
    parent=None;published=False;record={'status':'PREPARING','nonce':plan['nonce'],'dataModifiedByInstaller':False}
    def update(status,**values):
        record.update(status=status,**values)
        with (folder/'events.jsonl').open('a',encoding='utf-8') as stream:
            stream.write(json.dumps(dict(timeUtc=time.time(),**record))+'\n');stream.flush();os.fsync(stream.fileno())
        atomic_json(result_path,record)
    if result_path.exists():
        previous=json.loads(result_path.read_text(encoding='utf-8'))
        if previous.get('nonce')!=plan['nonce']:raise UpdateRejected('Handoff receipt identity mismatch')
        if previous.get('status') in ('COMPLETE','COMPLETE_RECEIPT_INCOMPLETE','COMPLETE_RECEIPT_RECOVERED'):return 0
        if previous.get('status') in ('FAILED_RETAINED','CANCELLED_RETAINED','ENTRYPOINT_RECOVERY_REQUIRED'):return 1
        recovered=interrupted_publication_status(folder,installed.parent,plan['nonce'])
        if recovered is not None:
            update(recovered,verification='PREVIOUS_PUBLICATION_STATUS_RECONCILED_NO_ENTRYPOINT_CHANGE')
            return 0 if recovered=='COMPLETE_RECEIPT_RECOVERED' else 1
        update('FAILED_RETAINED',reason='Interrupted handoff retained; start a new checked update from the existing version',previousStatus=previous.get('status'))
        return 1
    try:
        parent=ParentProcess(plan['parentPid'],installed/'CCBStudioSuitePreview.exe')
        identity=Installed.load(installed)
        try:start_status_window(installed,folder)
        except Exception:pass # Status UI failure cannot alter update trust/data outcome.
        entrypoint_before=read_entrypoint(installed.parent)
        if entrypoint_before is not None and (entrypoint_before['version']!=identity.version or any(no_reparse(Path(entrypoint_before[k]))!=v for k,v in [('installed',installed),('data',data),('runtime',runtime)])):
            raise UpdateRejected('Handoff is not the active default workspace')
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
                if (folder/'CANCEL').exists():raise UpdateCancelled('Handoff cancelled before installation')
                if time.monotonic()>deadline:raise UpdateRejected('Parent drain timed out; data lock was not forced')
                time.sleep(.1)
            parent.close();parent=None
            if (folder/'CANCEL').exists():raise UpdateCancelled('Handoff cancelled before installation')
            update('INSTALLING')
            final,journal,install_record=install_candidate(installer,target,custom,installed,cache,data)
            update('STARTING_B',installed=str(final),installJournal=str(journal))
            # B receives only the controlled handoff path and the same explicit roots.
            child=start_b(final,data,runtime,plan_path)
            proof={}
            wait_for_b(folder,child,plan,identity,custom,destination,update,proof=proof)
            # A stable launcher is committed only after B verifies the actual workspace.
            launcher=create_launcher(final,destination,runtime,expected_current=entrypoint_before,
                expected_absent=entrypoint_before is None,migration_source=data if destination!=data else None,
                publication_admission=lambda:admit_publication(folder,proof,plan,identity,custom,destination,update))
            published=True
            update('COMPLETE',verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH',launcher=str(launcher),dataRoot=str(destination),sourceDataRoot=str(data))
    except Exception as error:
        status=('COMPLETE_RECEIPT_INCOMPLETE' if published else 'ENTRYPOINT_RECOVERY_REQUIRED' if isinstance(error,EntrypointRecoveryRequired)
            else 'CANCELLED_RETAINED' if isinstance(error,UpdateCancelled) else 'FAILED_RETAINED')
        update(status,reason=str(error) if isinstance(error,UpdateRejected) else type(error).__name__)
        return 0 if published else 1
    finally:
        if parent:parent.close()
    # This is outside the update transaction: cleanup cannot turn COMPLETE into FAILED.
    # Use B's runtime so A's program can be retired after this helper exits.
    try:
        start_cleanup(final,cache,destination,runtime)
    except Exception:pass # B startup and later ordinary launches retry from retained receipts.
    return 0

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--handoff',required=True,type=Path);a=p.parse_args()
    raise SystemExit(perform(a.handoff))
