from pathlib import Path
import hashlib,json,subprocess
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
HEAD='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0';CODE='26ef38857be510636dffdb5fcbd59a8d70c85550'
DEST=ROOT/'docs/audits/evidence/TOKEN_CANDIDATE_20261026'
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()==HEAD
assert not subprocess.check_output(['git','diff',CODE,HEAD,'--','modules','tools'],cwd=ROOT)
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools'],cwd=ROOT).strip()
assert not DEST.exists();DEST.mkdir()
def sha(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def copy(p,name):
    q=DEST/name;q.parent.mkdir(parents=True,exist_ok=True)
    with q.open('xb') as f:f.write(p.read_bytes())
def write(name,value):
    with (DEST/name).open('x',encoding='utf-8') as f:json.dump(value,f,indent=2,ensure_ascii=False);f.write('\n')
run=list((ROOT/'.local/windows-preview/evidence').glob('*-token-candidate-app-20261026'));assert len(run)==1;run=run[0]
summary=json.loads((run/'summary.json').read_text());assert summary['exitCode']==0 and summary['sourceStable'] and summary['tests']==0
mapping=[]
for name,digest in json.loads((run/'source-before.json').read_text()).items():
    p=ROOT/name;assert sha(p)==digest,name
    raw=p.read_bytes();git=subprocess.check_output(['git','show',HEAD+':'+name],cwd=ROOT)
    if raw!=git:
        raw.decode('utf-8');git.decode('utf-8');assert raw.replace(b'\r\n',b'\n')==git.replace(b'\r\n',b'\n'),name
    mapping.append(dict(path=name,buildSha256=digest,gitSha256=hashlib.sha256(git).hexdigest(),relationship='EXACT' if raw==git else 'CRLF_LF_ONLY'))
receipt=json.loads((OUT/'candidate/BUILD_RECEIPT.json').read_text());assert receipt['sourceSha']==HEAD and receipt['appBuildSourceSha']==HEAD
assert sha(OUT/'candidate/studio-installer.exe')==receipt['sha256']
installed=json.loads((OUT/'ISOLATED_INSTALL_VERIFICATION.json').read_text());assert installed['status']=='PASS'
old=ROOT/'.local/appearance-text-actions-20261026'
assert sha(old/'candidate/studio-installer.exe')=='ca4cafd37285a2528090247ecc53326b5e78cafa87b569d040578639d8c10ee5'
oldJar=next((old/'isolated-install/app').glob('studio-desktop-*.jar'));assert sha(oldJar)=='1154b2cf6f961cba3d0d20b9846f9ada72703adb4d73bb55c2759301ce15fa19'
lock=json.loads((OUT/'entry-correction/PACKAGE_LOCK.json').read_text())
write('SOURCE_PACKAGE_MAP.json',dict(appCode=CODE,buildPackageHead=HEAD,modulesToolsDiffFromCode='EMPTY',inputs=mapping,
    appBuildEvidence=run.name,appImageBuildOnly=True,newJUnit=0,
    priorTargetedJUnit=dict(source=CODE,evidenceCommit=HEAD,uniqueMethods=1,methodSeconds=7.037,status='PASS',rerun=False,
        link='https://github.com/MisakaPiano/CCB-Studio-Suite/blob/'+HEAD+'/docs/audits/evidence/TOKEN_TEXT_CONTRAST_20261026/run/test-cases.json'),
    installer=dict(path=str(OUT/'candidate/studio-installer.exe'),bytes=receipt['bytes'],sha256=receipt['sha256'],version=receipt['displayVersion'],channel='internal'),
    mainJar=lock['mainJar'],mainJarSha256=lock['mainJarSha256'],installed=installed['installed'],tokenConsumer=installed['tokenConsumer'],
    old48eed=dict(installerSha256=sha(old/'candidate/studio-installer.exe'),mainJarSha256=sha(oldJar),unchanged=True),
    daily115='Not accessed or changed by this task',privateGallery='Not accessed',tagLibraryAdded=False,
    nativeAppStarts=0,humanAcceptance='NOT TESTED for this new package',independentReview='NOT REQUESTED',publicRelease='NOT PUBLISHED / no tag',
    heavySlotReleased='control/SLOT_RELEASED.json'))
copy(ROOT/'docs/audits/evidence/TOKEN_TEXT_CONTRAST_20261026/.gitattributes','.gitattributes')
for p in sorted(run.iterdir()):
    if p.is_file():copy(p,'run/'+p.name)
for name in ('AUTHORIZATION.json','RESOURCE_WAIT_A.json','READY_FOR_SLOT.json','SLOT_GRANTED.json','SLOT_RELEASED.json','BUILD_BUDGET.json','measure-budget.py',
             'build-app.py','COMMAND.json','PLAN.json','RUNNER_IDENTITY.json','runner.log','memory-samples.jsonl','MONITOR_RESULT.json',
             'PACKAGE_COMMAND.json','package-candidate.py','package.log','verify-install.py','ISOLATED_INSTALL_COMMAND.json','ISOLATED_INSTALL_VERIFICATION.json','isolated-install.log',
             'prepare-entry.py','verify-entry.ps1','SeedTheme.java','SEED_COMPILE.json','SEED_RESULT.json','PACKAGE_RESOURCE_OBSERVATION.json'):
    copy(OUT/name,'control/'+name)
for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi'):copy(OUT/'candidate'/name,'package/'+name)
for name in ('INSTALL_MANIFEST.json','update-config.json','update-root.json'):copy(OUT/'isolated-install'/name,'installed/'+name)
copy(OUT/'isolated-install/updater/RUNTIME_MANIFEST.json','installed/RUNTIME_MANIFEST.json')
for p in sorted((OUT/'entry-correction').iterdir()):
    if p.is_file():copy(p,'entry/'+p.name)
copy(Path(__file__),'reproduction/freeze-candidate.py')
files=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write('RAW_MANIFEST.json',dict(appCode=CODE,buildPackageHead=HEAD,files=files,membersIncludingManifest=len(files)+1,
    exclusions='No binary, shortcut binary, screenshot pixels, synthetic workspace/profile/vault, credentials or private data. Only named synthetic control records.'))
print(json.dumps(dict(status='FROZEN',files=len(files)+1,inputs=len(mapping),installerSha256=receipt['sha256'],nativeStarts=0)),flush=True)
