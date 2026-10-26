"""Windows same-handle verification/deletion for explicitly inventoried disposable files.
No recursive deletion, glob ownership inference, trust metadata or user-data discovery.
"""
from contextlib import contextmanager, ExitStack
from pathlib import Path, PurePosixPath
from ctypes import wintypes
import ctypes, hashlib, msvcrt, os, re

def io_path(path):
    """Use extended Win32 paths for IO; keep normal absolute paths in identities."""
    value=os.path.abspath(path)
    if os.name=='nt' and not value.startswith('\\\\?\\'):
        value='\\\\?\\UNC\\'+value[2:] if value.startswith('\\\\') else '\\\\?\\'+value
    return Path(value)
def relative(name):
    if not isinstance(name,str) or len(name)>1024:raise ValueError('Invalid owned filename')
    p=PurePosixPath(name)
    if (not name or p.as_posix()!=name or p.is_absolute() or any(x in ('.','..') for x in p.parts)
        or '\\' in name or ':' in name or any(x.endswith((' ','.')) or re.match(r'^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)',x,re.I) for x in p.parts)):
        raise ValueError('Unsafe owned filename')
    return p.parts

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
    handle=k.CreateFileW(str(io_path(path)),0x80000000|(0x10000 if delete else 0),1,None,3,0x02200000,None)
    if handle==ctypes.c_void_p(-1).value:raise ctypes.WinError(ctypes.get_last_error())
    try:
        info=FileInfo()
        if not k.GetFileInformationByHandle(handle,ctypes.byref(info)):raise ctypes.WinError(ctypes.get_last_error())
        if info.attributes&0x400:raise ValueError('Reparse handle refused')
        yield k,handle,info
    finally:k.CloseHandle(handle)

@contextmanager
def held_parents(path):
    with ExitStack() as stack:
        for p in reversed(Path(path).parents):stack.enter_context(held(p))
        yield

def dispose(k,handle):
    # jpackage ships its launcher read-only. Ignore that attribute only when
    # disposing the already verified owned HANDLE; never clear attributes by path.
    # Older Windows without FileDispositionInfoEx falls back to safe deferral.
    flags=wintypes.DWORD(1|16) # DELETE | IGNORE_READONLY_ATTRIBUTE
    if k.SetFileInformationByHandle(handle,21,ctypes.byref(flags),ctypes.sizeof(flags)):return
    flag=wintypes.BOOL(True)
    if not k.SetFileInformationByHandle(handle,4,ctypes.byref(flag),ctypes.sizeof(flag)):raise ctypes.WinError(ctypes.get_last_error())

def delete_matching(path,item):
    with held_parents(path),held(path,True) as (k,handle,info):
        if info.attributes&0x10:raise ValueError('Owned file became a directory')
        # Duplicate a readable descriptor so the exact verified HANDLE is also deleted.
        import _winapi
        copy=_winapi.DuplicateHandle(_winapi.GetCurrentProcess(),handle,_winapi.GetCurrentProcess(),0,False,2)
        fd=msvcrt.open_osfhandle(copy,os.O_RDONLY|os.O_BINARY)
        with os.fdopen(fd,'rb') as stream:
            if (info.sizeHigh<<32|info.sizeLow)!=item['bytes'] or hashlib.file_digest(stream,'sha256').hexdigest()!=item['sha256']:
                return 'RETAIN_CHANGED'
        dispose(k,handle)
    return 'REMOVED'

def prune_empty(base,files,extra_dirs=()):
    dirs={base}|{base.joinpath(*relative(d)) for d in extra_dirs}
    for item in files:
        p=base.joinpath(*relative(item['path'])).parent
        while p!=base:dirs.add(p);p=p.parent
    results=[]
    for directory in sorted(dirs,key=lambda p:len(p.parts),reverse=True):
        if not io_path(directory).exists():continue
        try:
            with held_parents(directory),held(directory,True) as (k,handle,info):
                if not info.attributes&0x10:raise ValueError('Owned directory changed type')
                dispose(k,handle) # Windows rejects nonempty directories; never recursive.
            results.append(dict(path=directory.relative_to(base).as_posix(),status='REMOVED'))
        except OSError as error:
            code=getattr(error,'winerror',None)
            # Nonempty can mean unknown/user content: retain it, without endlessly
            # retrying or walking it. Busy/ACL failures of owned directories must
            # remain visible even when every inventoried file is already absent.
            if code in (2,3):continue
            results.append(dict(path=directory.relative_to(base).as_posix(),
                status='RETAIN_NONEMPTY' if code==145 else 'RETRY_DIRECTORY',winerror=code))
    return results

