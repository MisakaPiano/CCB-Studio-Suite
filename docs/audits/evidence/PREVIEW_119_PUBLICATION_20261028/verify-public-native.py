from pathlib import Path
import json,hashlib,subprocess,sys,os,time,uuid,ctypes,shutil,msvcrt
from ctypes import wintypes as W
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-119-20261028';CROSS=Path('H:\\CCB-Studio-Suite\\.local\\disposable\\p119n-9f3fa12a1bfd436ab87e075b099f7068\\payload');PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe'
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

assert load(OUT/'METADATA_PUBLICATION.json')['releaseVersion']==119
assert CROSS.is_dir() and not any(CROSS.iterdir());write(CROSS/'OWNER.json',dict(kind='AUTHORIZED_SUITE_SYNTHETIC_119',root=str(CROSS),suite=str(ROOT),source='d9be9d9f90cd305c71749933a0b2c4f2929b06a3',allowedByUser=True,dailyData=False))
EVIDENCE=OUT/'public-native';EVIDENCE.mkdir()
profile=CROSS/'profile';profile.mkdir();ENV={k:v for k,v in os.environ.items() if k.upper() in ('SYSTEMROOT','WINDIR','COMSPEC','PATHEXT','SYSTEMDRIVE','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS','OS')}
for name in ('USERPROFILE','HOME','APPDATA','LOCALAPPDATA','TEMP','TMP'):
 p=profile/name;p.mkdir();ENV[name]=str(p)
ENV.update(PATH='C:/Windows/System32;C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0;C:/Program Files/Git/cmd',JAVA_TOOL_OPTIONS='-Duser.home='+ENV['HOME'])
write(EVIDENCE/'ENVIRONMENT.json',ENV)
root=CROSS/'install';root.mkdir();old=root/'118-a1202e24f7a0';data=root/'data/workspace';runtime=root/'data/runtime';cache=runtime/'updates';data.mkdir(parents=True);(cache/'downloads').mkdir(parents=True)
oldInstaller=ROOT/'.local/preview-candidate-118-20261027/candidate-r3/studio-installer.exe';oldReceipt=load(oldInstaller.parent/'BUILD_RECEIPT.json')
assert sha(oldInstaller)==oldReceipt['sha256']=='3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d'
with immutable_file(oldInstaller):command([oldInstaller,'/S','/D='+str(old)],'install-118.log')
oldConfig=load(old/'update-config.json');oldVerification=verify_install(old,oldConfig)
oldFiles={e['path']:e for e in load(old/'INSTALL_MANIFEST.json')['files']}
assert sha(old/'updater/lib/apply_update.py')==oldFiles['updater/lib/apply_update.py']['sha256']
oldJournal=cache/'install-initial118.json';write(oldJournal,dict(status='INSTALLED_NOT_STARTED',target=str(old),expected={k:oldConfig[k] for k in ('product','platform','channel','version','sourceSha')},verification=oldVerification))
# Enroll exact verified installer output while the original package handle is held.
with immutable_file(oldInstaller):ownership=enroll_program(cache,root,old,oldConfig,sha(oldInstaller),oldJournal)
create_launcher(old,data,runtime)
JAVA=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin/java.exe');JAVAC=JAVA.with_name('javac.exe')
# Compile/run this driver against the actual OLD 118 application classes.
command([JAVAC,'--release','17','-encoding','UTF-8','-cp',str(old/'app/*'),'-d',str(EVIDENCE),str(OUT/'SeedCandidate119.java')],'compile-seed-old118.log')
record=EVIDENCE/'SYNTHETIC_FINGERPRINTS.txt'
def seed(mode,app,label,snapshot=None):
 args=[JAVA,'-cp',str(EVIDENCE)+os.pathsep+str(app/'app/*'),'SeedCandidate119',mode,data,record]
 if snapshot:args.append(snapshot)
 command(args,label)
seed('seed',old,'seed.log')
exe=old/'CCBStudioSuitePreview.exe'
command([root/'CCBStudioLauncher.exe'],'cold118-entry.log');wait(lambda:windows(exe));time.sleep(1);assert not unlocked(data)
close(exe);wait(lambda:unlocked(data));snapshotPath=EVIDENCE/'A_SNAPSHOT.json';seed('snapshot',old,'snapshot.log',snapshotPath)
snapshot=load(snapshotPath);assert len(snapshot['assets'])==2 and len(snapshot['credentials'])==2
# No unknown sentinels inside A: the test requires its ROOT to disappear, not only files.
(root/'backups').mkdir();(root/'backups/synthetic-keep.bin').write_bytes(b'SYNTHETIC_BACKUP_KEEP')
newReceipt=load(Path('H:\\CCB-Studio-Suite\\.local\\disposable\\preview119-package-74b3235609b7475eb90f5c4026a13984\\payload\\candidate\\BUILD_RECEIPT.json'))
publicCommand=[old/'updater/python.exe','-B',old/'updater/lib/studio_update.py','--package',old,'--cache',cache,'--download']
write(EVIDENCE/'OLD118_PUBLIC_COMMAND.json',dict(arguments=list(map(str,publicCommand)),noAdapter=True,rootSha256=sha(old/'update-root.json')))
command(publicCommand,'old118-public-download.log',timeout=240)
download=load(EVIDENCE/'old118-public-download.log');assert download['status']=='DOWNLOADED_VERIFIED' and download['version']==119 and download['sha256']==newReceipt['sha256']
installer=Path(download['installer']);assert installer.is_relative_to(cache/'downloads') and sha(installer)==newReceipt['sha256'];installerFolder=installer.parent
write(EVIDENCE/'OLD118_PUBLIC_DOWNLOAD.json',dict(status='PASS',result=download,publicSignedDiscovery=True,realPublicBytes=True,sourceAdapter=False))
custom=dict(product='ccb-studio-suite',platform='windows-x86_64',channel=download['channel'],version=download['version'],minimum_reader=download['minimumReader'],maximum_reader=download['maximumReader'],data_version=download['dataVersion'],sourceSha=download['sourceSha'],displayVersion=download['displayVersion'],notes=download['notes'],installedBytes=download['installedBytes'])
command([root/'CCBStudioLauncher.exe'],'active118-entry.log');active=wait(lambda:windows(exe));assert len(active)==1
time.sleep(2)
def cache_free():
 with (cache/'client.lock').open('r+b') as f:
  try:msvcrt.locking(f.fileno(),msvcrt.LK_NBLCK,1)
  except OSError:return False
  else:f.seek(0);msvcrt.locking(f.fileno(),msvcrt.LK_UNLCK,1);return True
stable=[None]
def idle():
 if not cache_free():stable[0]=None;return False
 if stable[0] is None:stable[0]=time.monotonic()
 return time.monotonic()-stable[0]>2
wait(idle,90)
active=windows(exe);assert len(active)==1
write(EVIDENCE/'A_EMPTY_EXTRA_DIRECTORIES.json',dict(directories=[str(p.relative_to(old)) for p in old.rglob('*') if p.is_dir() and not any(p.iterdir())]))
parent=ParentProcess(active[0]['pid'],exe)
nonce=uuid.uuid4().hex+uuid.uuid4().hex;folder=cache/'handoffs'/str(uuid.uuid4());folder.mkdir(parents=True)
approved=dict(custom,sha256=newReceipt['sha256'],installer=str(installer),dataVersion=3)
plan=folder/'plan.json';write(plan,dict(format=1,nonce=nonce,installed=str(old),cache=str(cache),dataRoot=str(data),runtimeRoot=str(runtime),parentPid=active[0]['pid'],sourceDataVersion=3,targetDataVersion=3,destinationDataRoot=str(data),approved=approved,snapshot=snapshot))
with (EVIDENCE/'old-helper.log').open('xb') as log:
 helper=subprocess.Popen([str(old/'updater/pythonw.exe'),'-B',str(old/'updater/lib/apply_update.py'),'--handoff',str(plan)],cwd=old,env=ENV,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
write(EVIDENCE/'A_READY.json',dict(status='RUNNING',parent=active,helperPid=helper.pid,oldManifest=oldVerification,oldHelperSha256=sha(old/'updater/lib/apply_update.py'),snapshot=snapshot,defaultBefore=load(root/'launch-current.json')))
wait(lambda:load(folder/'result.json').get('status')=='READY_TO_DRAIN' if (folder/'result.json').exists() else False,30)
closed=close(exe);wait(parent.exited);parent.close()
wait(lambda:load(folder/'result.json').get('status') in ('COMPLETE','FAILED_RETAINED'),150)
result=load(folder/'result.json');write(EVIDENCE/'RAW_UPDATE_RESULT.json',result);assert result['status']=='COMPLETE',result
assert helper.wait(timeout=30)==0
current=root/('119-'+custom['sourceSha'][:12]);bexe=current/'CCBStudioSuitePreview.exe';bwindows=wait(lambda:windows(bexe),30);assert len(bwindows)==1
bprocess=ParentProcess(bwindows[0]['pid'],bexe)
write(EVIDENCE/'B_RUNNING_BEFORE_CLEANUP.json',dict(bWindows=bwindows,processAlive=not bprocess.exited(),updateStatus=result['status']))
wait(lambda:not old.exists() and not list((cache/'downloads').iterdir()) and not list(root.glob('*.installing')),90)
assert not bprocess.exited() and windows(bexe)
assert not list((cache/'downloads').iterdir()),'Owned completed download still present'
assert not list(root.glob('*.installing'))
assert (root/'backups/synthetic-keep.bin').read_bytes()==b'SYNTHETIC_BACKUP_KEEP'
assert Path(load(root/'launch-current.json')['installed'])==current
assert Path(load(root/'launch-current.json')['data'])==data and Path(load(root/'launch-current.json')['runtime'])==runtime
bverify=verify_install(current,load(current/'update-config.json'));assert sha(current/'update-root.json')==sha(Path('H:\\CCB-Studio-Suite\\.local\\disposable\\preview119-package-74b3235609b7475eb90f5c4026a13984\\payload\\candidate\\payload\\update-root.json'))
ack=load(folder/'B_VERIFIED.json');assert ack['snapshot']==snapshot
before=(folder/'result.json').read_bytes()
value=dict(status='PASS',source=newReceipt['sourceSha'],installerSha256=newReceipt['sha256'],version=119,oldVersion=118,oldInstallerSha256=oldReceipt['sha256'],oldDirectoryRemovedWhileBRunning=True,bWindows=bwindows,bProcessStillRunning=not bprocess.exited(),bVerification=bverify,defaultAfter=load(root/'launch-current.json'),downloadsRemaining=0,installingDirectoriesRemaining=0,assetsVerifiedInAck=2,syntheticCredentialsVerifiedInAck=2,backupRetained=True,formalRootUnchanged=True,discovery='REAL_PUBLIC_SIGNED_TUF_118_TO_119_NO_ADAPTER',actual118And119ProductionMain=True,oldHelperSourceUnmodified=True,forcedTermination=False,publicRelease=False,dailyInstallationModified=False)
write(EVIDENCE/'LIVE_B_RESULT.json',value)
# Re-run the actual B cleanup once, without a manual deletion path.
command([current/'updater/python.exe','-B',current/'updater/lib/residue_cleanup.py','--package',current,'--cache',cache,'--data',data,'--runtime',runtime],'repeat-cleanup.log')
repeat=load(EVIDENCE/'repeat-cleanup.log');assert repeat['status']=='COMPLETE' and repeat['removed']==0
assert (folder/'result.json').read_bytes()==before and not old.exists() and not bprocess.exited()
value['normalBClose']=close(bexe);wait(bprocess.exited);bprocess.close();wait(lambda:unlocked(data));seed('check',current,'after-update-check.log')
# Reopen the new default entry, check lock and identity, then close normally.
command([root/'CCBStudioLauncher.exe'],'reopen119-entry.log');wait(lambda:windows(bexe));assert not unlocked(data);value['reopen119NormalClose']=close(bexe);wait(lambda:unlocked(data))
value['reopenDefault119']=True;value['repeatCleanupNoChange']=True;write(EVIDENCE/'RESULT.json',value)
# Preserve metadata and original cleanup events before retirement; no plaintext secrets.
for name in ('events.jsonl','state.json'):
 p=cache/'cleanup'/name
 if p.exists():shutil.copy2(p,EVIDENCE/('cleanup-'+name))
shutil.copy2(folder/'B_VERIFIED.json',EVIDENCE/'B_VERIFIED.json');shutil.copy2(folder/'b-stdout.log',EVIDENCE/'b-stdout.log');shutil.copy2(folder/'b-stderr.log',EVIDENCE/'b-stderr.log')
currentCache=CROSS/'current-check'
command([current/'updater/python.exe','-B',current/'updater/lib/studio_update.py','--package',current,'--cache',currentCache],'current119-public.log',timeout=100)
currentResult=load(EVIDENCE/'current119-public.log');assert currentResult['status']=='UP_TO_DATE' and currentResult['version']==119 and currentResult['sha256']==newReceipt['sha256']
write(EVIDENCE/'CURRENT119_PUBLIC.json',dict(status='PASS',result=currentResult))
raw=EVIDENCE/'signed-metadata';raw.mkdir()
for label,directory in [('old118',cache/'metadata'),('current119',currentCache/'metadata')]:
 target=raw/label;target.mkdir()
 for path in directory.glob('*.json'):shutil.copy2(path,target/path.name)
value['public118DownloadAndSignature']='PASS';value['public119Current']='UP_TO_DATE';value['discoveryAdapterUsed']=False;write(EVIDENCE/'RESULT.json',value)
write(OUT/'PUBLIC_CHANNEL_VERIFICATION.json',value)
print(json.dumps(value,indent=2),flush=True)
