from pathlib import Path
import ctypes,hashlib,json,os,shutil,subprocess,sys,time,uuid,msvcrt
from ctypes import wintypes as W
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';CROSS=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027')
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from apply_update import create_launcher
from install_ops import atomic_json,verify_install
from residue_cleanup import enroll_program,run as cleanup
from owned_files import delete_matching,prune_empty
BUILD=OUT/'candidate-v3';REPO=OUT/'repository-v2';ENV=json.loads((OUT/'SYNTHETIC_ENVIRONMENT_2.json').read_text());receipt=json.loads((BUILD/'BUILD_RECEIPT.json').read_text());SOURCE=receipt['sourceSha']
user=ctypes.WinDLL('user32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
CB=ctypes.WINFUNCTYPE(W.BOOL,W.HWND,W.LPARAM)
user.EnumWindows.argtypes=[CB,W.LPARAM];user.IsWindowVisible.argtypes=[W.HWND];user.GetClassNameW.argtypes=[W.HWND,W.LPWSTR,ctypes.c_int];user.GetWindowThreadProcessId.argtypes=[W.HWND,ctypes.POINTER(W.DWORD)];user.PostMessageW.argtypes=[W.HWND,W.UINT,W.WPARAM,W.LPARAM]
kernel.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];kernel.OpenProcess.restype=W.HANDLE;kernel.CloseHandle.argtypes=[W.HANDLE];kernel.QueryFullProcessImageNameW.argtypes=[W.HANDLE,W.DWORD,W.LPWSTR,ctypes.POINTER(W.DWORD)];kernel.WaitForSingleObject.argtypes=[W.HANDLE,W.DWORD];kernel.WaitForSingleObject.restype=W.DWORD
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);atomic_json(p,v)
def wait(check,seconds=40):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  value=check()
  if value:return value
  time.sleep(.15)
 raise TimeoutError('Condition not reached')
def windows(exe):
 found=[]
 def callback(hwnd,_):
  klass=ctypes.create_unicode_buffer(256);user.GetClassNameW(hwnd,klass,256)
  if not user.IsWindowVisible(hwnd) or klass.value!='SunAwtFrame':return True
  pid=W.DWORD();user.GetWindowThreadProcessId(hwnd,ctypes.byref(pid));h=kernel.OpenProcess(0x1000,False,pid.value)
  if not h:return True
  try:
   text=ctypes.create_unicode_buffer(32768);size=W.DWORD(32768)
   if kernel.QueryFullProcessImageNameW(h,0,text,ctypes.byref(size)) and Path(text.value)==exe:found.append(dict(pid=pid.value,window=int(hwnd),image=text.value))
  finally:kernel.CloseHandle(h)
  return True
 cb=CB(callback);user.EnumWindows(cb,0);return found
def close(exe):
 seen=wait(lambda:windows(exe),20);handles=[]
 try:
  for w in seen:
   h=kernel.OpenProcess(0x100000|0x1000,False,w['pid']);handles.append(h)
   assert user.PostMessageW(w['window'],0x10,0,0)
  for h in handles:assert h and kernel.WaitForSingleObject(h,45000)==0,'Normal close did not exit; no forced kill'
 finally:
  for h in handles:
   if h:kernel.CloseHandle(h)
 return seen
def command(args,log):
 with log.open('xb') as stream:return subprocess.run(list(map(str,args)),env=ENV,cwd=log.parent,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=35)
def native_entry(root,app,evidence,label):
 p=root/'runtime/NATIVE_ENTRY_OBSERVATION.json'
 if p.exists():p.unlink() # Exact synthetic adapter output; avoid reading a stale observation.
 assert command([root/'CCBStudioLauncher.exe'],evidence/(label+'-entry.log')).returncode==0
 view=wait(lambda:windows(app/'CCBStudioSuitePreview.exe'),45)
 obs=wait(lambda:load(p) if p.exists() else None,10)
 assert Path(obs['data'])==root/'data' and Path(obs['runtime'])==root/'runtime'
 assert Path(obs['command'])==app/'CCBStudioSuitePreview.exe'
 assert obs['pid'] in [x['pid'] for x in view]
 # Production open must actually hold its workspace ownership lock while visible.
 state=wait(lambda:[p for p in (root/'data/documents').rglob('*') if p.is_file()],10)
 with (root/'data/.owner.lock').open('r+b') as stream:
  blocked=False
  try:msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
  except OSError:blocked=True
  else:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
  assert blocked,'Production workspace lock not held'
 wait(lambda:load(root/'launch-current.json').get('installed')==str(app),10)
 time.sleep(1)
 link=evidence/(label+'-ROOT_SHORTCUT.json');assert command([root/'CCBStudioLauncher.exe','--inspect-shortcut',root/'Launch-CCB-Studio.lnk',link],evidence/(label+'-root-shortcut.log')).returncode==0
 assert load(link)['target']==str(root/'CCBStudioLauncher.exe')
 ended=close(app/'CCBStudioSuitePreview.exe')
 write(evidence/(label+'-NATIVE_ENTRY.json'),dict(status='PASS',observation=obs,visibleWindows=view,normalClose=ended,workspaceStateFiles=[str(x.relative_to(root)) for x in state]))
def inventory(root):
 result=[]
 for p in root.rglob('*'):
  assert not p.is_symlink() and not p.is_junction()
  if p.is_file():result.append(dict(path=p.relative_to(root).as_posix(),bytes=p.stat().st_size,sha256=sha(p)))
 return result
def retire(root,evidence):
 # This exact fresh test root was created by this driver, never adopted by enumeration.
 files=inventory(root);directories=sorted([p for p in root.rglob('*') if p.is_dir()],key=lambda p:len(p.parts),reverse=True);write(evidence/'FINAL_FIXTURE_MANIFEST.json',dict(root=str(root),files=files,directories=[str(p.relative_to(root)) for p in directories]))
 removed=0
 for entry in files:
  assert delete_matching(root/entry['path'],entry)=='REMOVED';removed+=1
 prune_empty(root,files)
 for p in directories:
  if p.exists():p.rmdir()
 assert not root.exists() or not any(root.iterdir())
 if root.exists():root.rmdir()
 write(evidence/'FIXTURE_RETIREMENT.json',dict(status='COMPLETE',files=removed,bytes=sum(x['bytes'] for x in files),evidenceRetained=True))


results=[load(OUT/'native-r3'/c/'RESULT.json') for c in ('same-volume-rename','same-volume-move')]
case='cross-volume-H-to-C';evidence=OUT/'native-r3'/case
move=load(evidence/'REAL_MOVE.json');initial=Path(move['source']);root=Path(move['destination']);assert not initial.exists()
a=root/('1-'+SOURCE[:12]);protected=root/('111-'+'d'*12);protected_bytes=(root/'ACTIVE_INSTALLATION.json').read_bytes()
probe=ROOT/'modules/studio-desktop/build/libs/studio-desktop-review-update-probe.jar'
assert load(evidence/'after-NATIVE_ENTRY.json')['status']=='PASS'
def finish():
  write(evidence/'A_INPUT_MAP.json',dict(kind='TEST_ONLY_A_APP_IMAGE',source=SOURCE,main='RelocationNativeA delegates production GUI / ReviewUpdateNativeA',manifest=sha(a/'INSTALL_MANIFEST.json'),probe=sha(probe),basePayload=receipt['sha256'],notPublic116Binary=True))
  with (evidence/'native-a.log').open('xb') as log:process=subprocess.Popen([str(a/'CCBStudioSuitePreview.exe'),str(root)],cwd=a,env=ENV,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
  ready=wait(lambda:load(root/'A_HANDOFF_READY.json') if (root/'A_HANDOFF_READY.json').exists() else (_ for _ in ()).throw(RuntimeError(str(load(root/'A_FAILED.json')))) if (root/'A_FAILED.json').exists() else None,140)
  assert process.wait(timeout=30)==0
  plan_path=Path(ready['plan']);plan=load(plan_path)
  result=wait(lambda:(lambda v:v if v.get('status')=='COMPLETE' else (_ for _ in ()).throw(RuntimeError(str(v))) if v.get('status')=='FAILED_RETAINED' else None)(load(plan_path.parent/'result.json')) if (plan_path.parent/'result.json').exists() else None,150)
  b=Path(result['installed']);ack=load(plan_path.parent/'B_VERIFIED.json')
  assert ack['snapshot']==ready['snapshot']==plan['snapshot'] and len(ack['snapshot']['credentials'])==2 and len(ack['snapshot']['assets'])>=3
  assert Path(result['dataRoot'])==root/'data' and Path(result['sourceDataRoot'])==root/'data'
  wait(lambda:not (a/'CCBStudioSuitePreview.exe').exists() and not list((root/'runtime/updates/downloads').rglob('studio-installer.exe')),120)
  assert (a/'UNKNOWN_USER_SENTINEL.txt').read_bytes()==b'unknown must remain'
  assert (root/'ACTIVE_INSTALLATION.json').read_bytes()==protected_bytes and (root/protected.name/'protected.txt').is_file()
  assert (root/'data/exports/relocation-original.bin').read_bytes()==b'SYNTHETIC_RELOCATION_ORIGINAL_UNCHANGED'
  closed=close(b/'CCBStudioSuitePreview.exe')
  again=cleanup(b,root/'runtime/updates',root/'data',root/'runtime');assert again['status']=='COMPLETE' and again['removed']==0
  assert str(initial) in load(root/'launch-current.json')['previousRoots']
  write(evidence/'RAW_UPDATE_RESULT.json',dict(plan=plan,result=result,b=ack,a=ready))
  results.append(dict(case=case,status='PASS',source=SOURCE,realFilesystemMove=True,nativeBeforeAfter='PRODUCTION_GUI_VISIBLE_AND_NORMAL_CLOSE',relocatedDataAndRuntime=True,nativeAtoB='REAL_PARENT_DRAIN_NSIS_B_COMPLETE',syntheticCredentials=2,assets=len(ack['snapshot']['assets']),ownedPredecessorRemoved=True,downloadRemoved=True,unknownAndProtectedRetained=True,repeatedCleanupRemoved=again['removed'],externalShortcut='RECREATED_AND_TARGET_VERIFIED; NOT AUTOMATIC_SHELL_TRACKING',taskbarPin='NOT TESTED',nativeBClose=closed))
  write(evidence/'RESULT.json',results[-1]);status='PASS';retire(root,evidence)

finish()
write(OUT/'NATIVE_CASES_RESULT_FINAL.json',dict(status='PASS',uniqueRealCases=len(results),cases=results,notUserAcceptance=True,formalTrustChanged=False,dailyInstallationModified=False))
print(json.dumps(dict(status='PASS',uniqueRealCases=len(results))),flush=True)
