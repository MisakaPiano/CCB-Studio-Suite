from pathlib import Path
import hashlib,json,subprocess,sys,zipfile,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
PACKAGE=OUT/'candidate';INSTALL=OUT/'isolated-install'
assert OUT==ROOT/'.local/token-candidate-20261026' and not INSTALL.exists()
HEAD='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0';CODE='26ef38857be510636dffdb5fcbd59a8d70c85550'
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,v):
    with (OUT/name).open('x',encoding='utf-8') as f:json.dump(v,f,indent=2);f.write('\n')
receipt=json.loads((PACKAGE/'BUILD_RECEIPT.json').read_text());exe=PACKAGE/'studio-installer.exe';assert sha(exe)==receipt['sha256']
script=(PACKAGE/'installer.nsi').read_text(encoding='utf-8-sig')
assert not any(t.casefold() in script.casefold() for t in ('WriteReg','CreateShortCut','ExecWait','ExecShell','SetShellVarContext'))
for p in (OUT,PACKAGE,*PACKAGE.parents):
    if p==ROOT:break
    assert not p.is_symlink() and not (p.stat().st_file_attributes & 1024)
args=[str(exe),'/S','/D='+str(INSTALL)]
write('ISOLATED_INSTALL_COMMAND.json',dict(arguments=args,installerSha256=sha(exe),scope='New exact task-owned directory; no system registry/shortcuts/default pointers',atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
with (OUT/'isolated-install.log').open('xb') as log:r=subprocess.run(args,cwd=OUT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
assert r.returncode==0,r.returncode
sys.path.insert(0,str(INSTALL/'updater/lib'))
from install_ops import verify_install
from studio_update import Installed
config=json.loads((INSTALL/'update-config.json').read_text());checked=verify_install(INSTALL,config);identity=Installed.load(INSTALL)
assert config['sourceSha']==HEAD and config['channel']=='internal' and config['internalTest'] is True
assert not subprocess.check_output(['git','diff',CODE,HEAD,'--','modules','tools'],cwd=ROOT)
scripts=[]
for name in ('studio_update.py','github_transport.py','install_ops.py','apply_update.py','residue_cleanup.py','owned_files.py'):
    p=INSTALL/'updater/lib'/name;q=ROOT/'tools/update-runtime'/name;assert p.read_bytes()==q.read_bytes(),name
    scripts.append(dict(path='updater/lib/'+name,sha256=sha(p),sourcePath=q.relative_to(ROOT).as_posix()))
classes=[]
for module in ('asset-library','image-authority','studio-core','studio-desktop'):
    jars=list((INSTALL/'app').glob(module+'-*.jar'));assert len(jars)==1
    base=ROOT/'modules'/module/'build/classes'/('java' if module=='asset-library' else 'kotlin')/'main'
    with zipfile.ZipFile(jars[0]) as z:
        assert z.testzip() is None
        assert {n for n in z.namelist() if n.endswith('.class')}=={p.relative_to(base).as_posix() for p in base.rglob('*.class')},module
        count=0
        for p in base.rglob('*.class'):
            assert p.read_bytes()==z.read(p.relative_to(base).as_posix()),str(p);count+=1
        assert count>0
        assert not any('StudioTokenContrastTest' in n for n in z.namelist())
    classes.append(dict(module=module,jar=jars[0].name,sha256=sha(jars[0]),compiledClassMatches=count))
newJar=next((INSTALL/'app').glob('studio-desktop-*.jar'))
oldJar=next((ROOT/'.local/appearance-text-actions-20261026/isolated-install/app').glob('studio-desktop-*.jar'))
tokenClass='com/example/chatbar/desktop/DesktopStudioResultControlsKt.class'
with zipfile.ZipFile(newJar) as new,zipfile.ZipFile(oldJar) as old:
    newClass=new.read(tokenClass);oldClass=old.read(tokenClass)
    assert newClass!=oldClass
    assert newClass==(ROOT/'modules/studio-desktop/build/classes/kotlin/main'/tokenClass).read_bytes()
    token=dict(path=tokenClass,newSha256=hashlib.sha256(newClass).hexdigest(),old48eedSha256=hashlib.sha256(oldClass).hexdigest(),oldCandidateChanged=False)
rt=json.loads((INSTALL/'updater/RUNTIME_MANIFEST.json').read_text());assert all(sha(INSTALL/'updater'/x['path'])==x['sha256'] and (INSTALL/'updater'/x['path']).stat().st_size==x['bytes'] for x in rt['files'])
write('ISOLATED_INSTALL_VERIFICATION.json',dict(status='PASS',appCode=CODE,buildPackageHead=HEAD,
    installerExitCode=r.returncode,installerBytes=exe.stat().st_size,installerSha256=sha(exe),installed=checked,
    identityLoaded=True,config=config,packagedUpdaterSources=scripts,packagedClasses=classes,tokenConsumer=token,runtimeFilesVerified=len(rt['files']),
    registryCommandsInNSIS=False,dailyInstallationAccessed=False,privateGalleryAccessed=False,tagLibraryAdded=False,newJUnit=0,native='NOT STARTED',humanAcceptance='NOT TESTED FOR THIS PACKAGE'))
print(json.dumps(dict(status='PASS',files=checked['files'],classMatches=sum(x['compiledClassMatches'] for x in classes),tokenClassChanged=True)),flush=True)
