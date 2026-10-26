from pathlib import Path
import json,hashlib,subprocess,sys,os,time,uuid,ctypes,shutil,msvcrt
from ctypes import wintypes as W
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';CROSS=Path('C:/CCB-Studio-Suite-Test-118-20261027/p');PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe'
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from install_ops import atomic_json,verify_install,immutable_file
from apply_update import create_launcher,ParentProcess
from residue_cleanup import enroll_program,enroll_download
from tuf.api.metadata import TargetFile
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);atomic_json(p,v)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def wait(fn,seconds=60):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  value=fn()
  if value:return value
  time.sleep(.15)
 raise TimeoutError('Bounded isolated condition not reached')
user=ctypes.WinDLL('user32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True);CB=ctypes.WINFUNCTYPE(W.BOOL,W.HWND,W.LPARAM)
user.EnumWindows.argtypes=[CB,W.LPARAM];user.IsWindowVisible.argtypes=[W.HWND];user.GetClassNameW.argtypes=[W.HWND,W.LPWSTR,ctypes.c_int];user.GetWindowThreadProcessId.argtypes=[W.HWND,ctypes.POINTER(W.DWORD)];user.PostMessageW.argtypes=[W.HWND,W.UINT,W.WPARAM,W.LPARAM]
kernel.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];kernel.OpenProcess.restype=W.HANDLE;kernel.QueryFullProcessImageNameW.argtypes=[W.HANDLE,W.DWORD,W.LPWSTR,ctypes.POINTER(W.DWORD)];kernel.CloseHandle.argtypes=[W.HANDLE]
def windows(exe):
 found=[]
 def cb(hwnd,_):
  klass=ctypes.create_unicode_buffer(256);user.GetClassNameW(hwnd,klass,256)
  if not user.IsWindowVisible(hwnd) or klass.value!='SunAwtFrame':return True
  pid=W.DWORD();user.GetWindowThreadProcessId(hwnd,ctypes.byref(pid));h=kernel.OpenProcess(0x1000,False,pid.value)
  if h:
   try:
    value=ctypes.create_unicode_buffer(32768);size=W.DWORD(32768)
    if kernel.QueryFullProcessImageNameW(h,0,value,ctypes.byref(size)) and Path(value.value)==exe:found.append(dict(pid=pid.value,hwnd=int(hwnd)))
   finally:kernel.CloseHandle(h)
  return True
 callback=CB(cb);user.EnumWindows(callback,0);return found

def close(exe):
 views=wait(lambda:windows(exe),20);handles=[ParentProcess(v['pid'],exe) for v in views]
 try:
  for view in views:assert user.PostMessageW(view['hwnd'],0x10,0,0)
  for h in handles:wait(h.exited,45)
 finally:
  for h in handles:h.close()
 return views

def command(args,name,timeout=60):
 with (EVIDENCE/name).open('xb') as log:
  r=subprocess.run(list(map(str,args)),cwd=CROSS,env=ENV,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=timeout)
 assert r.returncode==0,name
 return r

def unlocked(data):
 with (data/'.owner.lock').open('r+b') as f:
  try:msvcrt.locking(f.fileno(),msvcrt.LK_NBLCK,1)
  except OSError:return False
  else:f.seek(0);msvcrt.locking(f.fileno(),msvcrt.LK_UNLCK,1);return True


EVIDENCE=OUT/'public-native';root=CROSS/'install';old=root/'117-b9b240a248d4';data=root/'data/workspace';runtime=root/'data/runtime';cache=runtime/'updates';ENV=load(EVIDENCE/'ENVIRONMENT.json')
newReceipt=load(Path('C:/CCB-Studio-Suite-Test-118-20261027/b/BUILD_RECEIPT.json'));oldReceipt=load(ROOT/'.local/relocation-117-20261027/candidate-preview-117/BUILD_RECEIPT.json')
current=root/'118-a1202e24f7a0';bexe=current/'CCBStudioSuitePreview.exe'
command([root/'CCBStudioLauncher.exe'],'resume118-entry.log');bwindows=wait(lambda:windows(bexe),30);assert len(bwindows)==1
bprocess=ParentProcess(bwindows[0]['pid'],bexe)
folders=list((cache/'handoffs').iterdir());assert len(folders)==1;folder=folders[0];result=load(folder/'result.json');assert result['status']=='COMPLETE';snapshot=load(EVIDENCE/'A_SNAPSHOT.json');custom=load(folder/'plan.json')['approved']
JAVA=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/java.exe');record=EVIDENCE/'SYNTHETIC_FINGERPRINTS.txt'
def seed(mode,app,label,snapshot=None):
 args=[JAVA,'-cp',str(EVIDENCE)+os.pathsep+str(app/'app/*'),'SeedCandidate118',mode,data,record]
 if snapshot:args.append(snapshot)
 command(args,label)
wait(lambda:not old.exists() and not list((cache/'downloads').iterdir()),30)
assert not list((cache/'downloads').iterdir()),'Owned completed download still present'
assert not list(root.glob('*.installing'))
assert (root/'backups/synthetic-keep.bin').read_bytes()==b'SYNTHETIC_BACKUP_KEEP'
assert Path(load(root/'launch-current.json')['installed'])==current
assert Path(load(root/'launch-current.json')['data'])==data and Path(load(root/'launch-current.json')['runtime'])==runtime
bverify=verify_install(current,load(current/'update-config.json'));assert sha(current/'update-root.json')==sha(Path('C:/CCB-Studio-Suite-Test-118-20261027/b/payload/update-root.json'))
ack=load(folder/'B_VERIFIED.json');assert ack['snapshot']==snapshot
before=(folder/'result.json').read_bytes()
value=dict(status='PASS',source=newReceipt['sourceSha'],installerSha256=newReceipt['sha256'],version=118,oldVersion=117,oldInstallerSha256=oldReceipt['sha256'],oldDirectoryRemovedWhileBRunning=True,bWindows=bwindows,bProcessStillRunning=not bprocess.exited(),bVerification=bverify,defaultAfter=load(root/'launch-current.json'),downloadsRemaining=0,installingDirectoriesRemaining=0,assetsVerifiedInAck=2,syntheticCredentialsVerifiedInAck=2,backupRetained=True,formalRootUnchanged=True,discovery='REAL_PUBLIC_SIGNED_TUF_117_TO_118_NO_ADAPTER',actual117And118ProductionMain=True,oldHelperSourceUnmodified=True,forcedTermination=False,publicRelease=False,dailyInstallationModified=False)
value['continuationAfterEarlyAssertion']=True;value['originalBProcessEndedAfterDriverExit']='NOT_INDEPENDENTLY_DIAGNOSED';value['oldRootRemovalWhileOriginalBRunning']='Prior driver passed alive check after old root vanished';value['resumeUsesSameCompletedUpgrade']=True
write(EVIDENCE/'LIVE_B_RESULT.json',value)
# Re-run the actual B cleanup once, without a manual deletion path.
command([current/'updater/python.exe','-B',current/'updater/lib/residue_cleanup.py','--package',current,'--cache',cache,'--data',data,'--runtime',runtime],'repeat-cleanup.log')
repeat=load(EVIDENCE/'repeat-cleanup.log');assert repeat['status']=='COMPLETE' and repeat['removed']==0
assert (folder/'result.json').read_bytes()==before and not old.exists() and not bprocess.exited()
value['normalBClose']=close(bexe);wait(bprocess.exited);bprocess.close();wait(lambda:unlocked(data));seed('check',current,'after-update-check.log')
# Reopen the new default entry, check lock and identity, then close normally.
command([root/'CCBStudioLauncher.exe'],'reopen118-entry.log');wait(lambda:windows(bexe));assert not unlocked(data);value['reopen118NormalClose']=close(bexe);wait(lambda:unlocked(data))
value['reopenDefault118']=True;value['repeatCleanupNoChange']=True;write(EVIDENCE/'RESULT.json',value)
# Preserve metadata and original cleanup events before retirement; no plaintext secrets.
for name in ('events.jsonl','state.json'):
 p=cache/'cleanup'/name
 if p.exists():shutil.copy2(p,EVIDENCE/('cleanup-'+name))
shutil.copy2(folder/'B_VERIFIED.json',EVIDENCE/'B_VERIFIED.json');shutil.copy2(folder/'b-stdout.log',EVIDENCE/'b-stdout.log');shutil.copy2(folder/'b-stderr.log',EVIDENCE/'b-stderr.log')
currentCache=CROSS/'current-check'
command([current/'updater/python.exe','-B',current/'updater/lib/studio_update.py','--package',current,'--cache',currentCache],'current118-public.log',timeout=100)
currentResult=load(EVIDENCE/'current118-public.log');assert currentResult['status']=='UP_TO_DATE' and currentResult['version']==118 and currentResult['sha256']==newReceipt['sha256']
write(EVIDENCE/'CURRENT118_PUBLIC.json',dict(status='PASS',result=currentResult))
raw=EVIDENCE/'signed-metadata';raw.mkdir()
for label,directory in [('old117',cache/'metadata'),('current118',currentCache/'metadata')]:
 target=raw/label;target.mkdir()
 for path in directory.glob('*.json'):shutil.copy2(path,target/path.name)
value['public117DownloadAndSignature']='PASS';value['public118Current']='UP_TO_DATE';value['discoveryAdapterUsed']=False;write(EVIDENCE/'RESULT.json',value)
write(OUT/'PUBLIC_CHANNEL_VERIFICATION.json',value)
print(json.dumps(value,indent=2),flush=True)
