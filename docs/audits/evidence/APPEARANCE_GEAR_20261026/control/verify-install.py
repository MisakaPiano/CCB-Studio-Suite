from pathlib import Path
import hashlib,json,os,subprocess,sys,zipfile,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/appearance-gear-20261026';PACKAGE=OUT/'candidate';INSTALL=OUT/'isolated-install'
assert not INSTALL.exists()
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,value):
 with (OUT/name).open('x',encoding='utf-8') as f:json.dump(value,f,indent=2);f.write('\n')
receipt=json.loads((PACKAGE/'BUILD_RECEIPT.json').read_text());exe=PACKAGE/'studio-installer.exe';assert sha(exe)==receipt['sha256']
nsi=(PACKAGE/'installer.nsi').read_text(encoding='utf-8-sig')
assert not any(t.casefold() in nsi.casefold() for t in ('WriteReg','CreateShortCut','ExecWait','ExecShell','SetShellVarContext'))
command=[str(exe),'/S','/D='+str(INSTALL)]
write('ISOLATED_INSTALL_COMMAND.json',dict(arguments=command,installerSha256=sha(exe),scope='new exact task-owned directory only; no system/registry/shortcuts/default pointers',atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
with (OUT/'isolated-install.log').open('xb') as log:r=subprocess.run(command,cwd=OUT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
assert r.returncode==0,r.returncode
sys.path.insert(0,str(INSTALL/'updater/lib'))
from install_ops import verify_install
from studio_update import Installed
config=json.loads((INSTALL/'update-config.json').read_text());verified=verify_install(INSTALL,config);identity=Installed.load(INSTALL)
assert config['sourceSha']=='22aa8c55ec4b05db29acb443d85717e80eb9d24a' and config['channel']=='internal' and config['internalTest'] is True
sources=[]
for name in ('studio_update.py','github_transport.py','install_ops.py','apply_update.py','residue_cleanup.py','owned_files.py'):
 p=INSTALL/'updater/lib'/name;q=ROOT/'tools/update-runtime'/name
 assert p.read_bytes()==q.read_bytes(),name
 sources.append(dict(path='updater/lib/'+name,sha256=sha(p),sourcePath=q.relative_to(ROOT).as_posix()))
classes=[]
for module in ('asset-library','image-authority','studio-core','studio-desktop'):
 jars=list((INSTALL/'app').glob(module+'-*.jar'));assert len(jars)==1
 base=ROOT/'modules'/module/'build/classes'/('java' if module=='asset-library' else 'kotlin')/'main'
 with zipfile.ZipFile(jars[0]) as z:
  assert z.testzip() is None
  count=0
  for p in base.rglob('*.class'):
   rel=p.relative_to(base).as_posix();assert p.read_bytes()==z.read(rel),(module,rel);count+=1
  assert count>0
 classes.append(dict(module=module,jar=jars[0].name,jarSha256=sha(jars[0]),compiledClassMatches=count))
rt=json.loads((INSTALL/'updater/RUNTIME_MANIFEST.json').read_text());assert all(sha(INSTALL/'updater'/x['path'])==x['sha256'] and (INSTALL/'updater'/x['path']).stat().st_size==x['bytes'] for x in rt['files'])
write('ISOLATED_INSTALL_VERIFICATION.json',dict(status='PASS',source=config['sourceSha'],installerExitCode=r.returncode,installerSha256=sha(exe),installerBytes=exe.stat().st_size,identityLoaded=True,installed=verified,packagedUpdaterSources=sources,packagedClasses=classes,runtimeFilesVerified=len(rt['files']),config=config,registryCommandsInNSIS=False,dailyInstallationAccessed=False,newJUnitMethods=0,applicationStartup='NOT TESTED',humanAcceptance='NOT TESTED'))
print(json.dumps(dict(status='PASS',installation=verified,classMatches=sum(x['compiledClassMatches'] for x in classes),updaterSourceMatches=len(sources))),flush=True)