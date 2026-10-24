"""Best-effort retirement of explicitly enrolled updater files, never a disk sweeper.

Only install-time output of a verified installer or explicitly named download output
can be enrolled. Legacy directories are not adopted by scanning. Receipts/journals
are local integrity records, not a new publisher trust anchor or a hostile-user sandbox.
"""
from contextlib import contextmanager, ExitStack
from pathlib import Path, PurePosixPath
import argparse, ctypes, hashlib, json, msvcrt, os, re, time, uuid
from ctypes import wintypes
from install_ops import no_reparse, atomic_json, verify_install
from studio_update import UpdateRejected, exclusive

DAY=86400
ROLLBACK_SECONDS=7*DAY
FAILED_SECONDS=30*DAY
MAX_RECEIPTS=1000

def read_json(path, limit=4*1024**2):
    path=no_reparse(path)
    if path.stat().st_size>limit:raise UpdateRejected('Oversized cleanup record')
    return json.loads(path.read_text(encoding='utf-8-sig'))

def relative(name):
    if not isinstance(name,str) or len(name)>1024:raise UpdateRejected('Invalid owned filename')
    p=PurePosixPath(name)
    if (not name or p.as_posix()!=name or p.is_absolute() or any(x in ('.','..') for x in p.parts)
        or '\\' in name or ':' in name or any(x.endswith((' ','.')) or re.match(r'^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)',x,re.I) for x in p.parts)):
        raise UpdateRejected('Unsafe owned filename')
    return p.parts

def file_identity(path):
    path=no_reparse(path)
    with path.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    return dict(path=path.name,bytes=path.stat().st_size,sha256=digest)

def identity(expected):
    value={k:expected[k] for k in ('product','platform','channel','version','sourceSha')}
    if (value['product']!='ccb-studio-suite' or value['platform']!='windows-x86_64'
        or value['channel'] not in ('internal','preview','stable') or type(value['version']) is not int or value['version']<1
        or not re.fullmatch('[0-9a-f]{40}',value['sourceSha'])):raise UpdateRejected('Invalid cleanup identity')
    return value

def write_receipt(cache, install_root, area, base, files, expected, proof, now=None):
    cache=no_reparse(cache);install_root=no_reparse(install_root);base=no_reparse(base)
    anchor=install_root if area=='program' else cache/'downloads'
    if base.parent!=anchor or not base.is_dir():raise UpdateRejected('Owned directory is outside its exact parent')
    folder=cache/'cleanup/owned';no_reparse(folder);folder.mkdir(parents=True,exist_ok=True)
    path=folder/(uuid.uuid4().hex+'.json')
    atomic_json(path,dict(format=1,installRoot=str(install_root),cache=str(cache),area=area,base=base.name,
        expected=identity(expected),files=files,proof=proof,createdAt=time.time() if now is None else now))
    return path

def enroll_program(cache, install_root, directory, expected, installer_sha256, journal, now=None):
    """Called while the authenticated installer handle is still held, after it exits.
    Even on failure only matching manifest members are enrolled; partial/unknown bytes
    remain untouched. No future scan of an arbitrary legacy manifest grants ownership.
    """
    directory=no_reparse(directory);manifest=directory/'INSTALL_MANIFEST.json'
    if not manifest.is_file():return None
    value=read_json(manifest);expected=identity(expected)
    if any(value.get(k)!=v for k,v in expected.items()) or value.get('format')!=1:return None
    if not re.fullmatch('[0-9a-f]{64}',installer_sha256):raise UpdateRejected('Missing authenticated installer digest')
    entries=value.get('files')
    if not isinstance(entries,list) or not 1<=len(entries)<=20000:return None
    files=[];seen=set()
    for item in entries:
        parts=relative(item['path']);folded=item['path'].casefold()
        if folded in seen:raise UpdateRejected('Duplicate owned filename')
        seen.add(folded);p=no_reparse(directory.joinpath(*parts))
        if not p.is_file():continue
        actual=file_identity(p)
        if actual['bytes']==item.get('bytes') and actual['sha256']==item.get('sha256'):
            files.append(dict(actual,path=item['path']))
    for name in ('INSTALL_MANIFEST.json','INSTALL_COMPLETE','Uninstall.exe'):
        p=no_reparse(directory/name)
        if p.is_file() and name.casefold() not in seen:files.append(file_identity(p))
    return write_receipt(cache,install_root,'program',directory,files,expected,
        dict(kind='VERIFIED_INSTALLER_OUTPUT',installerSha256=installer_sha256,
             manifestSha256=file_identity(manifest)['sha256'],installJournal=Path(journal).name),now)

def move_program_receipt(path, final):
    if path is not None:
        value=read_json(path);value['base']=Path(final).name;atomic_json(path,value)

def enroll_download(cache, install_root, directory, expected, installer_sha256, verified, now=None):
    directory=no_reparse(directory)
    files=[file_identity(directory/name) for name in ('studio-installer.exe','VERIFIED.json') if (directory/name).is_file()]
    return write_receipt(cache,install_root,'download',directory,files,expected,
        dict(kind='TUF_VERIFIED_DOWNLOAD' if verified else 'OWNED_DOWNLOAD_FAILURE',installerSha256=installer_sha256),now)

class FileInfo(ctypes.Structure):
    _fields_=[('attributes',wintypes.DWORD),('created',wintypes.FILETIME),('accessed',wintypes.FILETIME),('written',wintypes.FILETIME),
              ('volume',wintypes.DWORD),('sizeHigh',wintypes.DWORD),('sizeLow',wintypes.DWORD),('links',wintypes.DWORD),('indexHigh',wintypes.DWORD),('indexLow',wintypes.DWORD)]

def kernel_api():
    k=ctypes.WinDLL('kernel32',use_last_error=True)
    k.CreateFileW.argtypes=[wintypes.LPCWSTR,wintypes.DWORD,wintypes.DWORD,ctypes.c_void_p,wintypes.DWORD,wintypes.DWORD,wintypes.HANDLE];k.CreateFileW.restype=wintypes.HANDLE
    k.CloseHandle.argtypes=[wintypes.HANDLE]
    k.GetFileInformationByHandle.argtypes=[wintypes.HANDLE,ctypes.POINTER(FileInfo)]
    k.SetFileInformationByHandle.argtypes=[wintypes.HANDLE,ctypes.c_int,ctypes.c_void_p,wintypes.DWORD]
    return k

@contextmanager
def held(path, delete=False):
    k=kernel_api()
    # Open the reparse point itself; deny concurrent write/delete/rename, including
    # replacing a parent junction between validation and disposition of the file.
    handle=k.CreateFileW(str(path),0x80000000|(0x10000 if delete else 0),1,None,3,0x02200000,None)
    if handle==ctypes.c_void_p(-1).value:raise ctypes.WinError(ctypes.get_last_error())
    try:
        info=FileInfo()
        if not k.GetFileInformationByHandle(handle,ctypes.byref(info)):raise ctypes.WinError(ctypes.get_last_error())
        if info.attributes&0x400:raise UpdateRejected('Reparse handle refused')
        yield k,handle,info
    finally:k.CloseHandle(handle)

@contextmanager
def held_parents(path):
    with ExitStack() as stack:
        for p in reversed(Path(path).parents):stack.enter_context(held(p))
        yield

def dispose(k,handle):
    flag=wintypes.BOOL(True)
    if not k.SetFileInformationByHandle(handle,4,ctypes.byref(flag),ctypes.sizeof(flag)):raise ctypes.WinError(ctypes.get_last_error())

def delete_matching(path,item):
    with held_parents(path),held(path,True) as (k,handle,info):
        if info.attributes&0x10:raise UpdateRejected('Owned file became a directory')
        # Duplicate a readable descriptor so the exact verified HANDLE is also deleted.
        import _winapi
        copy=_winapi.DuplicateHandle(_winapi.GetCurrentProcess(),handle,_winapi.GetCurrentProcess(),0,False,2)
        fd=msvcrt.open_osfhandle(copy,os.O_RDONLY|os.O_BINARY)
        with os.fdopen(fd,'rb') as stream:
            if (info.sizeHigh<<32|info.sizeLow)!=item['bytes'] or hashlib.file_digest(stream,'sha256').hexdigest()!=item['sha256']:
                return 'RETAIN_CHANGED'
        dispose(k,handle)
    return 'REMOVED'

def prune_empty(base,files):
    dirs={base}
    for item in files:
        p=base.joinpath(*relative(item['path'])).parent
        while p!=base:dirs.add(p);p=p.parent
    for directory in sorted(dirs,key=lambda p:len(p.parts),reverse=True):
        if not directory.exists():continue
        try:
            with held_parents(directory),held(directory,True) as (k,handle,info):
                if not info.attributes&0x10:raise UpdateRejected('Owned directory changed type')
                dispose(k,handle) # Windows rejects nonempty directories; never recursive.
        except OSError:pass

def protected_paths(install_root, data, runtime):
    protected=[no_reparse(data),no_reparse(runtime)]
    for name in ('data','plugins','backups','source','.git'):protected.append(install_root/name)
    record=install_root/'ACTIVE_INSTALLATION.json'
    if record.exists():
        value=read_json(record,65536)
        if value.get('doNotCleanReferencedDirectories') is not False:
            for key in ('program','workspace','runtime','retainedBackupWorkspace','retainedBackupRuntime'):
                if key in value:
                    if not isinstance(value[key],str) or not Path(value[key]).is_absolute():raise UpdateRejected('Invalid protected installation record')
                    protected.append(no_reparse(value[key]))
    return protected

def overlaps(path, protected):return any(path==p or path.is_relative_to(p) or p.is_relative_to(path) for p in protected)

def event(cache,kind,**fields):
    folder=no_reparse(cache/'cleanup');folder.mkdir(parents=True,exist_ok=True)
    log=no_reparse(folder/'events.jsonl')
    with log.open('a',encoding='utf-8') as stream:
        stream.write(json.dumps(dict(at=time.time(),event=kind,**fields),ensure_ascii=True)+'\n');stream.flush();os.fsync(stream.fileno())

def completed_updates(cache,install_root):
    """Keep handoff proof in place. Return bounded identity projections, never log snapshots."""
    completed=[];blocked=set()
    folders=cache/'handoffs'
    if not folders.exists():return completed,blocked
    paths=sorted(folders.iterdir())
    if len(paths)>MAX_RECEIPTS:raise UpdateRejected('Handoff inspection bound reached; retained')
    for folder in paths:
        no_reparse(folder)
        if not folder.is_dir():continue
        plan_path=folder/'plan.json';result_path=folder/'result.json'
        if not plan_path.exists():continue
        plan=read_json(plan_path);result=read_json(result_path) if result_path.exists() else {}
        if plan.get('format')!=1 or not re.fullmatch('[0-9a-f]{64}',plan.get('nonce','')):raise UpdateRejected('Invalid handoff identity')
        if Path(plan.get('cache','')).absolute()!=cache:raise UpdateRejected('Cleanup handoff cache mismatch')
        old=no_reparse(plan['installed']);new=no_reparse(result['installed']) if result.get('installed') else None
        if old.parent!=install_root or (new is not None and new.parent!=install_root):continue
        if result.get('status')!='COMPLETE' or result.get('nonce')!=plan.get('nonce') or result.get('verification')!='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH':
            if result.get('status')!='FAILED_RETAINED':
                blocked.add(old.name)
                if new:blocked.add(new.name)
                approved=plan.get('approved',{})
                if 'version' in approved and 'sourceSha' in approved:blocked.add(str(approved['version'])+'-'+approved['sourceSha'][:12])
            continue
        approved=plan['approved'];journal=no_reparse(result['installJournal'])
        if journal.parent!=cache:raise UpdateRejected('Cleanup journal outside cache')
        installed=read_json(journal)
        if Path(installed.get('target','')).absolute()!=new or installed.get('expected',{}).get('sourceSha')!=approved.get('sourceSha') or installed.get('expected',{}).get('version')!=approved.get('version'):
            raise UpdateRejected('Cleanup completion identity mismatch')
        completed.append(dict(old=old.name,new=new.name,approved=approved,journal=journal,installation=installed,nonce=plan['nonce']))
    return completed,blocked

def run(package,cache,data,runtime,startup_token=None,now=None,after_delete=lambda _:None):
    now=time.time() if now is None else now
    package=no_reparse(package);cache=no_reparse(cache);data=no_reparse(data);runtime=no_reparse(runtime);root=package.parent
    if cache!=runtime/'updates' or overlaps(cache,[package]) or overlaps(root,[data]) and data==root:
        raise UpdateRejected('Cleanup roots overlap')
    descriptor=read_json(root/'launch-current.json',16384)
    if any(Path(descriptor.get(k,'')).absolute()!=v for k,v in [('installed',package),('data',data),('runtime',runtime)]):
        return dict(status='RETAINED_SECONDARY_WORKSPACE',removed=0)
    protections=protected_paths(root,data,runtime)
    with exclusive(cache):
        completed,blocked=completed_updates(cache,root)
        current=[c for c in completed if c['new']==package.name]
        if not current:return dict(status='RETAINED_NO_COMPLETE_HANDOFF',removed=0)
        folder=no_reparse(cache/'cleanup');folder.mkdir(exist_ok=True)
        state_path=folder/'state.json';state=read_json(state_path) if state_path.exists() else dict(format=1,current=package.name,starts=[],resources={})
        if state.get('format')!=1:raise UpdateRejected('Unsupported cleanup state')
        if state.get('current')!=package.name:state.update(current=package.name,starts=[])
        if startup_token is not None:
            if not re.fullmatch('[0-9a-f-]{36}',startup_token):raise UpdateRejected('Invalid startup token')
            state['starts']=(state.get('starts',[])+[startup_token] if startup_token not in state.get('starts',[]) else state['starts'])[-2:]
        owned=folder/'owned';owned.mkdir(exist_ok=True);no_reparse(owned)
        paths=sorted(owned.glob('*.json'))
        if len(paths)>MAX_RECEIPTS:raise UpdateRejected('Cleanup receipt bound reached; retained')
        # First upgrade into this feature can use an older helper. Adopt ONLY the running
        # new package, tied to that helper's pinned manifest digest, never its predecessor.
        if not any(read_json(p).get('area')=='program' and read_json(p).get('base')==package.name for p in paths):
            c=current[-1];expected=c['installation']['expected'];check=verify_install(package,expected)
            if check['manifestSha256']!=c['installation'].get('verification',{}).get('manifestSha256'):raise UpdateRejected('Current manifest differs from verified install')
            receipt=enroll_program(cache,root,package,expected,c['approved']['sha256'],c['journal'],now)
            if receipt:paths.append(receipt)
        atomic_json(state_path,state)
        removed=0;deferred=0
        for path in paths[:MAX_RECEIPTS]:
            receipt=read_json(path)
            if receipt.get('format')!=1 or receipt.get('installRoot')!=str(root) or receipt.get('cache')!=str(cache):raise UpdateRejected('Cleanup receipt root mismatch')
            expected=identity(receipt['expected']);area=receipt['area'];name=receipt['base'];parts=relative(name)
            if len(parts)!=1 or area not in ('program','download'):raise UpdateRejected('Invalid cleanup area')
            version_name=str(expected['version'])+'-'+expected['sourceSha'][:12]
            if area=='program' and not (name==version_name or re.fullmatch(re.escape(version_name)+r'\.[0-9a-f]{32}\.installing',name)):raise UpdateRejected('Invalid owned program basename')
            if area=='download' and not re.fullmatch(r'[0-9a-f-]{36}(?:\.incomplete)?',name):raise UpdateRejected('Invalid owned download basename')
            base=no_reparse((root if area=='program' else cache/'downloads')/name)
            files=receipt['files'];seen=set()
            if not isinstance(files,list) or not 0<=len(files)<=20003:raise UpdateRejected('Invalid cleanup file inventory')
            for item in files:
                relative(item['path']);folded=item['path'].casefold()
                if folded in seen or type(item.get('bytes')) is not int or not 0<=item['bytes']<=1024**3 or not re.fullmatch('[0-9a-f]{64}',item.get('sha256','')):raise UpdateRejected('Invalid cleanup file identity')
                seen.add(folded)
                if area=='download' and item['path'] not in ('studio-installer.exe','VERIFIED.json'):raise UpdateRejected('Unowned download member')
            if area=='program' and (base==package or any(name==b or name.startswith(b+'.') for b in blocked) or overlaps(base,protections)):continue
            proof=receipt['proof']
            if proof.get('kind') not in ('VERIFIED_INSTALLER_OUTPUT','TUF_VERIFIED_DOWNLOAD','OWNED_DOWNLOAD_FAILURE'):raise UpdateRejected('Unsupported ownership proof')
            if not re.fullmatch('[0-9a-f]{64}',proof.get('installerSha256','')):raise UpdateRejected('Missing source proof')
            matching=[c for c in completed if c['approved'].get('sourceSha')==expected['sourceSha'] and c['approved'].get('version')==expected['version']]
            if area=='download' and not matching:
                # A failed owned download can retire only after another successful update.
                if now-receipt['createdAt']<FAILED_SECONDS:continue
            if area=='program' and not any(c['old']==version_name or name.endswith('.installing') for c in completed):continue
            resource=state['resources'].setdefault(path.stem,dict(retiredAt=now))
            failed=name.endswith('.installing') or proof.get('kind')=='OWNED_DOWNLOAD_FAILURE'
            if failed and now-receipt['createdAt']<FAILED_SECONDS:continue
            if area=='program' and (len(state['starts'])<2 or now-resource['retiredAt']<ROLLBACK_SECONDS):continue
            atomic_json(state_path,state) # Persist retirement before the first deletion.
            if not base.exists():continue
            if area=='program':
                # Keep the whole fallback intact while its executable is in use, rather
                # than removing its unlocked JARs around a running older process.
                executable=base/'CCBStudioSuitePreview.exe'
                if executable.exists():
                    try:
                        with held_parents(executable),held(executable,True):pass
                    except OSError as error:
                        deferred+=1;event(cache,'RESOURCE_BUSY',receipt=path.name,reason=type(error).__name__);continue
            statuses={};count=0
            for item in files:
                target=no_reparse(base.joinpath(*relative(item['path'])))
                if not target.exists():continue
                if overlaps(target,protections) and area=='program':continue
                # User/plugin/backup areas stay protected even if a future package shipped defaults.
                if area=='program' and any(p.casefold() in ('plugins','backups','userdata','secrets','.git','source') for p in relative(item['path'])):continue
                try:status=delete_matching(target,item)
                except (OSError,UpdateRejected) as error:status='RETRY_'+type(error).__name__;deferred+=1
                statuses[status]=statuses.get(status,0)+1
                if status=='REMOVED':removed+=1;count+=1;after_delete(target)
            prune_empty(base,files)
            event(cache,'RESOURCE_PASS',receipt=path.name,area=area,base=name,counts=statuses,retentionSeconds=FAILED_SECONDS if failed else ROLLBACK_SECONDS if area=='program' else 0)
        atomic_json(state_path,state)
        event(cache,'PASS_COMPLETE',removed=removed,deferred=deferred)
        return dict(status='DEFERRED' if deferred else 'COMPLETE',removed=removed,deferred=deferred)

def main():
    p=argparse.ArgumentParser();p.add_argument('--package',required=True,type=Path);p.add_argument('--cache',required=True,type=Path);p.add_argument('--data',required=True,type=Path);p.add_argument('--runtime',required=True,type=Path);p.add_argument('--startup-token')
    a=p.parse_args()
    try:value=run(a.package,a.cache,a.data,a.runtime,a.startup_token)
    except Exception as error:
        value=dict(status='DEFERRED',reason=type(error).__name__)
        try:event(a.cache,'PASS_DEFERRED',reason=type(error).__name__)
        except Exception:pass
    print(json.dumps(value));return 0 # Cleanup never rewrites update success/failure.

if __name__=='__main__':raise SystemExit(main())
