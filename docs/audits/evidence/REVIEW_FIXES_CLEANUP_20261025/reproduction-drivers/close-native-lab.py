import ctypes,json,time
from pathlib import Path
from ctypes import wintypes
base=Path(__file__).resolve().parents[2]
lab=Path(json.loads((base/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])
source=json.loads((lab/'candidate/BUILD_RECEIPT.json').read_text())['sourceSha']
expected=lab/('programs/2-'+source[:12])/'CCBStudioSuitePreview.exe'
user=ctypes.WinDLL('user32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
kernel.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];kernel.OpenProcess.restype=wintypes.HANDLE;kernel.CloseHandle.argtypes=[wintypes.HANDLE]
kernel.QueryFullProcessImageNameW.argtypes=[wintypes.HANDLE,wintypes.DWORD,wintypes.LPWSTR,ctypes.POINTER(wintypes.DWORD)]
user.GetWindowThreadProcessId.argtypes=[wintypes.HWND,ctypes.POINTER(wintypes.DWORD)];user.PostMessageW.argtypes=[wintypes.HWND,wintypes.UINT,wintypes.WPARAM,wintypes.LPARAM]
closed=[];callback_type=ctypes.WINFUNCTYPE(wintypes.BOOL,wintypes.HWND,wintypes.LPARAM)
user.IsWindowVisible.argtypes=[wintypes.HWND];user.GetClassNameW.argtypes=[wintypes.HWND,wintypes.LPWSTR,ctypes.c_int]
def callback(hwnd,_):
 klass=ctypes.create_unicode_buffer(256);user.GetClassNameW(hwnd,klass,len(klass))
 if not user.IsWindowVisible(hwnd) or klass.value!='SunAwtFrame':return True
 pid=wintypes.DWORD();user.GetWindowThreadProcessId(hwnd,ctypes.byref(pid));h=kernel.OpenProcess(0x1000,False,pid.value)
 if not h:return True
 try:
  text=ctypes.create_unicode_buffer(32768);length=wintypes.DWORD(len(text))
  if kernel.QueryFullProcessImageNameW(h,0,text,ctypes.byref(length)) and Path(text.value)==expected:
   closed.append(dict(pid=pid.value,hwnd=int(hwnd),posted=bool(user.PostMessageW(hwnd,0x10,0,0))))
 finally:kernel.CloseHandle(h)
 return True
cb=callback_type(callback);user.EnumWindows.argtypes=[callback_type,wintypes.LPARAM];user.EnumWindows(cb,0)
(lab/'NORMAL_CLOSE_EXACT_TEST_IMAGE.json').write_text(json.dumps(closed,indent=2));print(json.dumps(closed))
kernel.WaitForSingleObject.argtypes=[wintypes.HANDLE,wintypes.DWORD];kernel.WaitForSingleObject.restype=wintypes.DWORD
exits=[]
for pid in {x['pid'] for x in closed}:
 h=kernel.OpenProcess(0x100000|0x1000,False,pid)
 if not h:exits.append(dict(pid=pid,alreadyExited=True));continue
 try:exits.append(dict(pid=pid,waitResult=kernel.WaitForSingleObject(h,30000)))
 finally:kernel.CloseHandle(h)
(lab/'NORMAL_CLOSE_EXIT.json').write_text(json.dumps(exits,indent=2));print(json.dumps(exits))
