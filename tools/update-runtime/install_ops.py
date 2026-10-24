"""Fresh-version installation operations; original installation and data are never replaced.

The caller must pass a TargetFile obtained through the installed TUF trust root and
hold the update-cache lock. This module cannot select or authenticate a release.
"""
from contextlib import contextmanager
import ctypes,hashlib,json,msvcrt,os,re,shutil,subprocess,uuid
from pathlib import Path,PurePosixPath
from ctypes import wintypes
from studio_update import UpdateRejected

def no_reparse(path):
    path=Path(os.path.abspath(path))
    for part in [path,*path.parents]:
        if part.exists() and (part.is_symlink() or part.is_junction()):raise UpdateRejected('Reparse point in installation path')
    return path

def atomic_json(path,value):
    path=Path(path);stage=path.with_name(path.name+'.'+uuid.uuid4().hex+'.tmp')
    with stage.open('x',encoding='utf-8') as stream:
        json.dump(value,stream,indent=2);stream.flush();os.fsync(stream.fileno())
    os.replace(stage,path)

@contextmanager
def immutable_file(path):
    """Prevent mutation/deletion between hash verification and installer execution."""
    kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    kernel.CreateFileW.argtypes=[wintypes.LPCWSTR,wintypes.DWORD,wintypes.DWORD,ctypes.c_void_p,wintypes.DWORD,wintypes.DWORD,wintypes.HANDLE]
    kernel.CreateFileW.restype=wintypes.HANDLE
    handle=kernel.CreateFileW(str(path),0x80000000,1,None,3,0x80,None) # READ, SHARE_READ, OPEN_EXISTING
    if handle==ctypes.c_void_p(-1).value:raise ctypes.WinError(ctypes.get_last_error())
    fd=msvcrt.open_osfhandle(handle,os.O_RDONLY|os.O_BINARY)
    with os.fdopen(fd,'rb') as stream:yield stream

def verify_install(directory,expected):
    directory=no_reparse(directory);manifest=directory/'INSTALL_MANIFEST.json'
    if not manifest.is_file() or manifest.stat().st_size>4*1024**2:raise UpdateRejected('Installation manifest missing or oversized')
    value=json.loads(manifest.read_text(encoding='utf-8'))
    for name in ['product','platform','channel','version','sourceSha']:
        if value.get(name)!=expected[name]:raise UpdateRejected('Installed '+name+' mismatch')
    if value.get('format')!=1 or not isinstance(value.get('files'),list) or not 1<=len(value['files'])<=20000:raise UpdateRejected('Unsupported installation manifest')
    seen=set();total=0
    for item in value['files']:
        name=item['path'];p=PurePosixPath(name)
        if (not name or p.as_posix()!=name or p.is_absolute() or '..' in p.parts or '.' in p.parts or '\\' in name or ':' in name
            or any(x.endswith((' ','.')) or re.match(r'^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)',x,re.I) for x in p.parts)):
            raise UpdateRejected('Unsafe installed filename')
        folded=name.casefold()
        if folded in seen:raise UpdateRejected('Duplicate installed filename')
        seen.add(folded);file=no_reparse(directory.joinpath(*p.parts))
        if not file.is_relative_to(directory) or not file.is_file():raise UpdateRejected('Installed file missing')
        if type(item.get('bytes')) is not int or not 0<=item['bytes']<=1024**3:raise UpdateRejected('Invalid installed size')
        total+=item['bytes']
        if total>4*1024**3:raise UpdateRejected('Installation too large')
        if file.stat().st_size!=item['bytes']:raise UpdateRejected('Installed file size mismatch')
        with file.open('rb') as stream:actual=hashlib.file_digest(stream,'sha256').hexdigest()
        if actual!=item['sha256']:raise UpdateRejected('Installed file hash mismatch')
    allowed=seen|{'install_manifest.json','install_complete','uninstall.exe'}
    for p in directory.rglob('*'):
        no_reparse(p)
        if p.is_file() and p.relative_to(directory).as_posix().casefold() not in allowed:raise UpdateRejected('Unexpected installed file')
    if not (directory/'INSTALL_COMPLETE').is_file() or not (directory/'CCBStudioSuitePreview.exe').is_file():raise UpdateRejected('Installer has no completion/launcher')
    return {'files':len(seen),'bytes':total,'manifestSha256':hashlib.sha256(manifest.read_bytes()).hexdigest()}

def install_candidate(installer,target,custom,installed,cache,data_root,run=None,disk_usage=shutil.disk_usage):
    installed=no_reparse(installed);cache=no_reparse(cache);data_root=no_reparse(data_root)
    installer=no_reparse(installer)
    if not installer.is_relative_to(cache/'downloads'):raise UpdateRejected('Installer is outside owned download cache')
    expected={k:custom[k] for k in ['product','platform','channel','version','sourceSha']}
    name=str(expected['version'])+'-'+expected['sourceSha'][:12]
    final=installed.parent/name;stage=installed.parent/(name+'.'+uuid.uuid4().hex+'.installing')
    if final.exists():raise UpdateRejected('Target version directory already exists; inspect retained installation')
    for p in [final,stage]:
        if data_root==p or data_root.is_relative_to(p) or p.is_relative_to(data_root) or p==installed:raise UpdateRejected('Program/data directories overlap')
    if disk_usage(installed.parent).free<custom['installedBytes']+64*1024**2:raise UpdateRejected('Insufficient installation space')
    journal=cache/('install-'+uuid.uuid4().hex+'.json')
    record={'status':'VERIFYING','target':str(final),'stage':str(stage),'expected':expected,'dataModified':False}
    atomic_json(journal,record)
    try:
        with immutable_file(installer) as stream:
            target.verify_length_and_hashes(stream)
            record['status']='INSTALLING';atomic_json(journal,record)
            if run is None:
                code=subprocess.run([str(installer),'/S','/D='+str(stage)],creationflags=subprocess.CREATE_NO_WINDOW).returncode
            else:code=run(installer,stage)
            # Record only output owned by this authenticated execution. Cleanup failure
            # is diagnostic and must not change installation success/failure.
            ownership=None
            try:
                from residue_cleanup import enroll_program
                stream.seek(0);installer_sha256=hashlib.file_digest(stream,'sha256').hexdigest()
                ownership=enroll_program(cache,installed.parent,stage,expected,installer_sha256,journal)
                if ownership:record['cleanupOwnership']=str(ownership);atomic_json(journal,record)
            except Exception as cleanup_error:
                record['cleanupDeferred']=type(cleanup_error).__name__;atomic_json(journal,record)
            if code!=0:raise UpdateRejected('Installer failed with exit code '+str(code))
        record['verification']=verify_install(stage,expected)
        if record['verification']['bytes']>custom['installedBytes']:raise UpdateRejected('Installed size exceeds authenticated budget')
        os.rename(stage,final)
        try:
            from residue_cleanup import move_program_receipt
            move_program_receipt(ownership,final)
        except Exception as cleanup_error:record['cleanupDeferred']=type(cleanup_error).__name__
        record['status']='INSTALLED_NOT_STARTED';atomic_json(journal,record)
        return final,journal,record
    except Exception as error:
        record.update(status='FAILED_RETAINED',reason=type(error).__name__)
        atomic_json(journal,record)
        raise
