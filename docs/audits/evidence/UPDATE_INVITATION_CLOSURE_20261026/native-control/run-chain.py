from pathlib import Path
import ctypes,datetime,hashlib,json,os,shutil,subprocess,sys,threading,time
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/update-chain-20261026/retry-b'
plan=json.loads((OUT/'PLAN.json').read_text(encoding='utf-8'));LAB=Path(plan['lab'])
close=OUT/'close-native-lab.py';text=close.read_text(encoding='utf-8')
text=text.replace('base=Path(__file__).resolve().parents[2]',"base=Path('H:/CCB-Studio-Suite')")
text=text.replace("lab=Path(json.loads((base/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])",'lab=Path('+repr(str(LAB))+')')
assert 'native-lab.json' not in text
close.write_text(text,encoding='utf-8')
PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe'
APP=ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'
RUNTIME=ROOT/'.local/candidate-review-20261026/updater-runtime'
BUILD=ROOT/'.local/windows-preview/evidence/20261026T100534386552Z-token-candidate-app-20261026'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,value):(OUT/name).write_text(json.dumps(value,indent=2),encoding='utf-8')
class Memory(ctypes.Structure):
 _fields_=[('length',ctypes.c_ulong),('load',ctypes.c_ulong),('totalPhys',ctypes.c_ulonglong),('availPhys',ctypes.c_ulonglong),('totalPage',ctypes.c_ulonglong),('availPage',ctypes.c_ulonglong),('totalVirtual',ctypes.c_ulonglong),('availVirtual',ctypes.c_ulonglong),('availExtended',ctypes.c_ulonglong)]
def memory():
 value=Memory();value.length=ctypes.sizeof(value);assert ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(value));return dict(availableBytes=value.availPhys,commitHeadroomBytes=value.availPage)
payload_bytes=sum(p.stat().st_size for p in (ROOT/'.local/token-candidate-20261026/isolated-install').rglob('*') if p.is_file())
installer_bytes=(ROOT/'.local/token-candidate-20261026/candidate/studio-installer.exe').stat().st_size
budget=4*payload_bytes+3*installer_bytes+256*1024**2
pre=dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),**memory(),hFree=shutil.disk_usage(ROOT).free,cFree=shutil.disk_usage('C:/').free,hBudget=budget,physicalBudget=896*1024**2,explanation='No Gradle. Sequential native A and B, each existing -Xmx512m; 256MiB native/Python plus 128MiB margin. Four payload copies and three installer-size buffers plus256MiB disk safety. Estimate, not measured peak.')
write('RESOURCE_ADMISSION.json',pre)
if pre['hFree']<budget or pre['availableBytes']<pre['physicalBudget'] or pre['commitHeadroomBytes']<1536*1024**2:raise RuntimeError('Resource gate not satisfied; no server/package/native process started')
profile=LAB/'profile';profile.mkdir();
for child in ('Local','Roaming','Temp'): (profile/child).mkdir()
env={k:v for k,v in os.environ.items() if k.upper() in ('SYSTEMROOT','WINDIR','COMSPEC','PATHEXT','SYSTEMDRIVE','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS','OS')}
env.update(PATH=str(Path(env.get('SystemRoot',env.get('SYSTEMROOT','C:/Windows')))/'System32')+';C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0;C:/Program Files/Git/cmd',USERPROFILE=str(profile),HOME=str(profile),APPDATA=str(profile/'Roaming'),LOCALAPPDATA=str(profile/'Local'),TEMP=str(profile/'Temp'),TMP=str(profile/'Temp'),JAVA_TOOL_OPTIONS='-Duser.home='+str(profile))
write('ENVIRONMENT_SCOPE.json',dict(names=sorted(env),profile=str(profile),inheritedPrivateConfiguration=False))
stop=threading.Event();samples=[]
def monitor():
 while not stop.is_set():
  samples.append(dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),**memory(),hFree=shutil.disk_usage(ROOT).free))
  stop.wait(1)
thread=threading.Thread(target=monitor,daemon=True);thread.start()
server=None;status='FAILED';failure=None;start=time.monotonic()
try:
 with (OUT/'server-process.log').open('wb') as log:
  server=subprocess.Popen([str(PY),'-B',str(ROOT/'tools/update-runtime/internal_repository.py'),'--folder',str(LAB/'repository')],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
 deadline=time.monotonic()+15
 while not (LAB/'repository/server.json').exists():
  if server.poll() is not None:raise RuntimeError('Internal server exited')
  if time.monotonic()>deadline:raise TimeoutError('Internal server ready')
  time.sleep(.1)
 port=json.loads((LAB/'repository/server.json').read_text())['port']
 command=[str(PY),'-B',str(ROOT/'tools/Build-InternalInstaller.py'),'--app',str(APP),'--runtime',str(RUNTIME),'--bootstrap',str(LAB/'repository/root.json'),'--destination',str(LAB/'candidate'),'--version','2','--metadata-url',f'http://127.0.0.1:{port}/metadata/','--targets-url',f'http://127.0.0.1:{port}/targets/','--build-evidence',str(BUILD)]
 write('PACKAGE_COMMAND.json',dict(arguments=command,source=plan['source'],appBuild='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0',public=False,formalTrustChanged=False))
 with (OUT/'package-process.log').open('wb') as log:completed=subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
 write('PACKAGE_EXIT.json',dict(exitCode=completed.returncode))
 if completed.returncode:raise RuntimeError('Package failed, inspect raw log')
 with (OUT/'driver-process.log').open('wb') as log:completed=subprocess.run([str(PY),'-B',str(OUT/'driver.py')],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=480)
 if completed.returncode:raise RuntimeError('Native driver failed, inspect raw log')
 native=json.loads((OUT/'native/NATIVE_UPDATE_SUMMARY.json').read_text())
 assert native['status']=='PASS'
 status='PASS'
except BaseException as error:
 failure=dict(kind=type(error).__name__,detail=str(error));raise
finally:
 if (LAB/'repository').exists():(LAB/'repository/STOP').write_text('Stop test server normally',encoding='ascii')
 if server is not None:
  try:server.wait(timeout=10)
  except subprocess.TimeoutExpired:failure=dict(kind='SERVER_DID_NOT_STOP',detail='No force kill attempted');status='FAILED'
 stop.set();thread.join(timeout=3)
 with (OUT/'memory-samples.jsonl').open('w',encoding='utf-8') as stream:
  for sample in samples:stream.write(json.dumps(sample)+'\n')
 write('RUN_RESULT.json',dict(status=status,failure=failure,seconds=time.monotonic()-start,source=plan['source'],serverExitCode=server.returncode if server else None,samples=len(samples),minAvailableBytes=min((s['availableBytes'] for s in samples),default=None),minHFree=min((s['hFree'] for s in samples),default=None),dailyInstallationAccessed=False,privateCredentialsRead=False,realProviderCalls=0,formalTrustChanged=False,published=False,independentReview='NOT REQUESTED'))
print((OUT/'RUN_RESULT.json').read_text(),flush=True)
