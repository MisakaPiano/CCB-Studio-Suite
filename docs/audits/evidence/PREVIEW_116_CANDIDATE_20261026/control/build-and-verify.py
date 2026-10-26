from pathlib import Path
import ctypes,datetime,hashlib,json,os,shutil,subprocess,sys,threading,time,zipfile
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-candidate-116-20261026';plan=json.loads((OUT/'PLAN.json').read_text());budget=json.loads((OUT/'BUDGET.json').read_text());PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,v):
 with (OUT/name).open('x',encoding='utf-8') as stream:json.dump(v,stream,indent=2);stream.write('\n')
class Memory(ctypes.Structure):
 _fields_=[('length',ctypes.c_ulong),('load',ctypes.c_ulong),('totalPhys',ctypes.c_ulonglong),('availPhys',ctypes.c_ulonglong),('totalPage',ctypes.c_ulonglong),('availPage',ctypes.c_ulonglong),('totalVirtual',ctypes.c_ulonglong),('availVirtual',ctypes.c_ulonglong),('availExtended',ctypes.c_ulonglong)]
def memory():
 m=Memory();m.length=ctypes.sizeof(m);assert ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(m));return dict(availableBytes=m.availPhys,commitHeadroomBytes=m.availPage)
pre=dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),**memory(),hFree=shutil.disk_usage(ROOT).free,cFree=shutil.disk_usage('C:/').free)
write('RESOURCE_ADMISSION.json',pre)
if pre['hFree']<budget['hRequiredAdditionalBytes'] or pre['cFree']<budget['cBudgetBytes'] or pre['availableBytes']<budget['physicalEstimateBytes'] or pre['commitHeadroomBytes']<512*1024**2:raise RuntimeError('Resource gate did not admit packaging')
assert not (OUT/'candidate').exists()
# Both ends of the supported app-image move are exact, non-reparse Suite paths.
app=Path(plan['app']);destination=OUT/'candidate'
assert app==ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'
for p in (app,destination):
 assert p.is_relative_to(ROOT)
 for a in (p,*p.parents):assert not a.is_symlink() and not (a.exists() and a.is_junction())
for f in json.loads((OUT/'APP_IMAGE_BEFORE.json').read_text())['files']:
 p=app/f['path'];assert p.stat().st_size==f['bytes'] and sha(p)==f['sha256']
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as artifacts
fixture=artifacts.create('pi');install=fixture/'payload/installation';profile=fixture/'payload/profile';profile.mkdir()
for child in ('Local','Roaming','Temp'):(profile/child).mkdir()
write('INSTALL_FIXTURE_OWNER.json',dict(root=str(fixture),install=str(install),origin='New explicit disposable root; no historical adoption'))
env={k:v for k,v in os.environ.items() if k.upper() in ('SYSTEMROOT','WINDIR','COMSPEC','PATHEXT','SYSTEMDRIVE','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS','OS')}
env.update(PATH='C:/Windows/System32;C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0;C:/Program Files/Git/cmd',USERPROFILE=str(profile),HOME=str(profile),APPDATA=str(profile/'Roaming'),LOCALAPPDATA=str(profile/'Local'),TEMP=str(profile/'Temp'),TMP=str(profile/'Temp'))
write('ENVIRONMENT_SCOPE.json',dict(names=sorted(env),syntheticProfile=str(profile),userPrivateEnvironmentInherited=False))
stop=threading.Event();samples=[]
def monitor():
 while not stop.is_set():samples.append(dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),**memory(),hFree=shutil.disk_usage(ROOT).free));stop.wait(1)
thread=threading.Thread(target=monitor,daemon=True);thread.start();status='FAILED';error=None;start=time.monotonic()
try:
 command=[str(PY),'-B',str(ROOT/'tools/Build-InternalInstaller.py'),'--app',str(app),'--runtime',plan['runtime'],'--bootstrap',str(OUT/'preview-update-root.json'),'--destination',str(destination),'--version','116','--metadata-url','https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/long-term-metadata/preview/windows-x86_64/metadata/','--targets-url','https://github.com/MisakaPiano/CCB-Studio-Suite/releases/download/','--build-evidence',plan['buildEvidence'],'--release-profile',str(OUT/'release-profile.json'),'--consume-app-image']
 write('PACKAGE_COMMAND.json',dict(arguments=command,source=plan['source'],formalKeysRead=False,publish=False))
 with (OUT/'package.log').open('xb') as log:r=subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
 write('PACKAGE_EXIT.json',dict(exitCode=r.returncode))
 assert r.returncode==0,'Inspect retained package.log'
 receipt=json.loads((destination/'BUILD_RECEIPT.json').read_text());exe=destination/'studio-installer.exe';assert sha(exe)==receipt['sha256']
 nsi=(destination/'installer.nsi').read_text(encoding='utf-8-sig')
 assert not any(s.casefold() in nsi.casefold() for s in ('WriteReg','CreateShortCut','ExecWait','ExecShell','SetShellVarContext'))
 manifest=json.loads((destination/'payload/INSTALL_MANIFEST.json').read_text());needed=sum(x['bytes'] for x in manifest['files'])+128*1024**2
 if shutil.disk_usage(ROOT).free<needed:raise RuntimeError('Insufficient free space for isolated install; candidate retained')
 command=[str(exe),'/S','/D='+str(install)];write('INSTALL_COMMAND.json',dict(arguments=command,installerSha256=sha(exe),dailyInstallationAccessed=False))
 with (OUT/'install.log').open('xb') as log:r=subprocess.run(command,cwd=OUT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=150)
 assert r.returncode==0,r.returncode
 sys.path.insert(0,str(install/'updater/lib'))
 from install_ops import verify_install
 from studio_update import Installed
 config=json.loads((install/'update-config.json').read_text());verified=verify_install(install,config);identity=Installed.load(install)
 assert config['version']==116 and config['displayVersion']=='0.0.0-preview.116' and config['channel']=='preview' and config['internalTest'] is False
 assert config['sourceSha']==plan['source'] and config['rootSha256']==plan['formalRootSha256']
 assert config['githubRepository']=='MisakaPiano/CCB-Studio-Suite' and config['metadataBranch']=='long-term-metadata'
 assert identity.reader==3 and identity.version>115
 assert sha(install/'update-root.json')==plan['formalRootSha256']
 scripts=[]
 for name in ('studio_update.py','github_transport.py','apply_update.py','install_ops.py','residue_cleanup.py','owned_files.py'):
  p=install/'updater/lib'/name;assert sha(p)==sha(ROOT/'tools/update-runtime'/name);scripts.append(dict(path='updater/lib/'+name,sha256=sha(p)))
 classes=[]
 for module in ('asset-library','image-authority','studio-core','studio-desktop'):
  jars=list((install/'app').glob(module+'-*.jar'));assert len(jars)==1
  compiled=ROOT/'modules'/module/'build/classes'/('java' if module=='asset-library' else 'kotlin')/'main';files=[p for p in compiled.rglob('*.class')]
  with zipfile.ZipFile(jars[0]) as jar:
   assert jar.testzip() is None and {n for n in jar.namelist() if n.endswith('.class')}=={p.relative_to(compiled).as_posix() for p in files}
   assert all(jar.read(p.relative_to(compiled).as_posix())==p.read_bytes() for p in files)
   assert not any('ReviewUpdateNativeA' in n or 'StudioTokenContrastTest' in n for n in jar.namelist())
  classes.append(dict(module=module,jar=jars[0].name,sha256=sha(jars[0]),productionClasses=len(files)))
 jar=next((install/'app').glob('studio-desktop-*.jar'));assert sha(jar)=='04381d42afc15e50db1354b32736c1107a7d5df6733e2c6ca57105817653b7e0'
 runtime=json.loads((install/'updater/RUNTIME_MANIFEST.json').read_text());assert all(sha(install/'updater'/x['path'])==x['sha256'] and (install/'updater'/x['path']).stat().st_size==x['bytes'] for x in runtime['files'])
 write('INSTALL_VERIFICATION.json',dict(status='PASS',source=plan['source'],appBuildSource=receipt['appBuildSourceSha'],version=116,installerExitCode=r.returncode,installerBytes=exe.stat().st_size,installerSha256=sha(exe),installation=verified,config=config,scripts=scripts,modules=classes,classMatches=sum(x['productionClasses'] for x in classes),runtimeFiles=len(runtime['files']),nativeAppStarted=False,newJUnit=0,newPythonRegressionMethods=0,public116SignedDiscovery='PENDING_PUBLICATION_AFTER_USER_CONFIRMATION',humanAcceptance='NOT TESTED FOR THIS PACKAGE',independentReview='NOT REQUESTED',dailyInstallationAccessed=False,privateCredentialsRead=False))
 status='PASS'
except BaseException as failure:error=dict(kind=type(failure).__name__,detail=str(failure));raise
finally:
 stop.set();thread.join(timeout=3)
 with (OUT/'memory-samples.jsonl').open('x',encoding='utf-8') as f:
  for value in samples:f.write(json.dumps(value)+'\n')
 write('RUN_RESULT.json',dict(status=status,error=error,seconds=time.monotonic()-start,samples=len(samples),minAvailableBytes=min((x['availableBytes'] for x in samples),default=None),minHFree=min((x['hFree'] for x in samples),default=None),productionSourceChanged=False,appImageConsumedTo=str(destination/'payload'),newGradle=0,newJUnit=0,newRegressionMethods=0,published=False,dailyInstallationModified=False))
print((OUT/'RUN_RESULT.json').read_text(),flush=True)
