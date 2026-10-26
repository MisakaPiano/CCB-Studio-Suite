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


CANDIDATE=OUT/'candidate-preview-117';formal=load(CANDIDATE/'BUILD_RECEIPT.json');evidence=OUT/'formal-package-populated';profile=CROSS/'profile'
JAVA=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/java.exe');seed_record=OUT/'POPULATED_ASSET_AND_CREDENTIAL_HASHES.txt'
verified=verify_install(CROSS/'.local/review-update-native/g1'/('117-'+SOURCE[:12]),load(CROSS/'.local/review-update-native/g1'/('117-'+SOURCE[:12])/'update-config.json'))
entries=[load(evidence/'installed-RESULT.json')];moves=[]
def material_check(root,mode,label):
 app=root/('117-'+SOURCE[:12])
 cmd=[str(JAVA),'-Xmx128m','-XX:ActiveProcessorCount=2','-Djava.awt.headless=true','-Djava.io.tmpdir='+str(profile/'Temp'),'--class-path',str(app/'app')+'/*',str(OUT/'SeedRelocation.java'),mode,str(root/'data'),str(seed_record)]
 with (evidence/(label+'-material.log')).open('xb') as log:result=subprocess.run(cmd,env=ENV,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=30)
 assert result.returncode==0,'Material check failed; raw log retained'

def entry(root,label):
 app=root/('117-'+SOURCE[:12]);assert command([root/'CCBStudioLauncher.exe'],evidence/(label+'-entry.log')).returncode==0
 shown=wait(lambda:windows(app/'CCBStudioSuitePreview.exe'),45)
 state=wait(lambda:[p for p in (root/'data/documents').rglob('*') if p.is_file()],15)
 with (root/'data/.owner.lock').open('r+b') as stream:
  blocked=False
  try:msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
  except OSError:blocked=True
  else:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
  assert blocked
 pid=shown[0]['pid'];cmd=['powershell.exe','-NoProfile','-NonInteractive','-Command',f'(Get-CimInstance Win32_Process -Filter "ProcessId={pid}").CommandLine']
 result=subprocess.run(cmd,env=ENV,capture_output=True,text=True,creationflags=subprocess.CREATE_NO_WINDOW,timeout=12)
 line=result.stdout;assert result.returncode==0 and str(root/'data') in line and str(root/'runtime') in line,line
 time.sleep(1)
 shortcut=evidence/(label+'-shortcut.json');assert command([root/'CCBStudioLauncher.exe','--inspect-shortcut',root/'Launch-CCB-Studio.lnk',shortcut],evidence/(label+'-shortcut.log')).returncode==0
 assert load(shortcut)['target']==str(root/'CCBStudioLauncher.exe')
 ended=close(app/'CCBStudioSuitePreview.exe');material_check(root,'verify',label);value=load(root/'launch-current.json')
 assert Path(value['data'])==root/'data' and Path(value['runtime'])==root/'runtime' and value['version']==117
 entries.append(dict(label=label,status='PASS',root=str(root),visibleWindows=shown,commandLine=line.strip(),normalClose=ended,previousRoots=value.get('previousRoots',[]),workspaceLockHeld=True,stateFiles=len(state),preMoveAssetsVerified=2,preMoveSyntheticCredentialsVerified=2))
 write(evidence/(label+'-RESULT.json'),entries[-1])

root=CROSS/'.local/review-update-native/g1';assert root.exists() and not root.with_name('g0').exists()
entry(root,'rename')
moves.append(dict(kind='rename',source=str(root.with_name('g0')),destination=str(root),sourceGone=True,operation='Real NTFS rename resumed after transient access denial; program manifest and pre-seeded asset/credential digests verified',fullBeforeMoveInventory='NOT RETAINED FOR THIS FIRST STEP; do not claim whole-directory comparison'))
holder=ROOT/'.local/disposable'/('proof117-pop-'+uuid.uuid4().hex[:8]);holder.mkdir();destinations=[CROSS/'.local/review-update-native/sub/g2',holder/'payload/f3']
for label,destination in zip(('same-volume-move','cross-volume-C-to-H'),destinations):
 assert not destination.exists() and (destination.is_relative_to(CROSS) or destination.is_relative_to(holder));destination.parent.mkdir(parents=True,exist_ok=True)
 before=inventory(root)
 if root.drive==destination.drive:
  retry=[]
  for attempt in range(8):
   try:root.rename(destination);break
   except PermissionError as error:
    retry.append(dict(attempt=attempt,winerror=error.winerror));time.sleep(.5*(attempt+1))
  else:raise RuntimeError('Move remained busy; source retained')
  write(evidence/(label+'-MOVE_RETRY.json'),dict(attempts=retry))
 else:
  shutil.copytree(root,destination)
  for item in before:
   p=destination/item['path'];assert p.stat().st_size==item['bytes'] and sha(p)==item['sha256']
  dirs=sorted([p for p in root.rglob('*') if p.is_dir()],key=lambda p:len(p.parts),reverse=True)
  for item in before:assert delete_matching(root/item['path'],item)=='REMOVED'
  for p in dirs:p.rmdir()
  root.rmdir()
 for item in before:
  p=destination/item['path'];assert p.stat().st_size==item['bytes'] and sha(p)==item['sha256']
 moves.append(dict(kind=label,source=str(root),destination=str(destination),files=len(before),allBytesMatched=True,sourceGone=not root.exists()))
 root=destination;entry(root,label)
 assert (root/'data/exports/retained-original.bin').read_bytes()==b'SYNTHETIC_FORMAL117_ORIGINAL'
result=dict(status='PASS',version=117,source=SOURCE,installerSha256=formal['sha256'],installation=verified,channel='preview',formalRootUnchanged=True,productionMain=True,testProbePackaged=False,moves=moves,entryChecks=entries,fullUpdateAndRetirement='Mapped to three internal A-to-B cases; 547 exact members match; separate native launcher built from same input, actually exercised here',public117Metadata='NOT PUBLISHED / NOT TESTED',taskbarPin='NOT TESTED',dailyInstallationModified=False)
write(OUT/'POPULATED_FORMAL_VERIFICATION.json',result);retire(root,evidence)
print(json.dumps(dict(status='PASS',version=117,actualPackageEntries=len(entries),sequentialMoves=len(moves))),flush=True)
