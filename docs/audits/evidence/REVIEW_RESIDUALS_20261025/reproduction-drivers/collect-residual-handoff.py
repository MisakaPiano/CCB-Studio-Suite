"""Copy only synthetic evidence and public configuration. No vault or user data reads."""
import hashlib,json,shutil,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];LOCAL=ROOT/'.local/review-fixes-cleanup-20261024'
OUT=ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025';OUT.mkdir(parents=True,exist_ok=True)
SOURCE=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
LAB=Path(json.loads((LOCAL/'native-lab.json').read_text())['lab'])
FAILED=Path(json.loads((LOCAL/'failed-residual-native-lab.json').read_text())['lab'])
def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):
    p=OUT/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=True,indent=2)+'\n',encoding='utf-8')
def copy(p,name):
    if not p.is_file():return
    q=OUT/name;q.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,q)
run_names=['runtime-20261024T163540953513Z','runtime-20261024T164200357583Z','runtime-20261024T165010681550Z']
for name in run_names:
    for filename in ('summary.json','tests.log','test-cases.json','fixture-cleanup.json'):copy(LOCAL/name/filename,Path('runs')/name/filename)
for name in ('20261024T163530170258Z-review-residual-boundaries','20261024T164151200964Z-review-residual-final'):
    folder=ROOT/'.local/windows-preview/evidence'/name
    for p in folder.iterdir():
        if p.is_file() and p.suffix in ('.json','.xml','.log'):copy(p,Path('runs')/name/p.name)
for lab,prefix in ((FAILED,Path('native-first-failed')),(LAB,Path('native-final'))):
    for name in ('A_DRAIN_WAITING.json','A_HANDOFF_READY.json','A_INPUT_MAP.json','A_NORMAL_EXIT_READY.json','A_STARTED.json','A_TO_B_RESULT.json','a-process.log','B_DRIVER_CLEANUP_REQUEST.json','B_DRIVER_EXIT_OBSERVATION.json','TRANSPORT_REUSE.json'):
        copy(lab/name,prefix/name)
    for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi','payload/INSTALL_MANIFEST.json','payload/update-config.json','payload/updater/RUNTIME_MANIFEST.json'):
        copy(lab/'candidate'/name,prefix/'candidate'/name)
    for folder in (lab/'runtime/updates/handoffs').glob('*'):
        for name in ('plan.json','result.json','B_VERIFIED.json','B_FAILED.json','events.jsonl','b-stdout.log','b-stderr.log'):
            copy(folder/name,prefix/'handoff'/folder.name/name)
    for name in ('events.jsonl','state.json','last-result.json','post-update-process.log'):copy(lab/'runtime/updates/cleanup'/name,prefix/'cleanup'/name)
    for p in (lab/'runtime/updates/cleanup/owned').glob('*.json'):copy(p,prefix/'cleanup/owned'/p.name)
    driver=LOCAL/('native-driver-'+lab.parent.parent.name)
    if driver.is_dir():
        for p in driver.iterdir():
            if p.is_file() and p.suffix=='.json':copy(p,prefix/p.name)
# The final rerun used the same immutable installer and live loopback repository,
# with fresh A/B/cache/data roots. Copy the actual serving repository's metadata.
for name in ('root.json','targets.json','snapshot.json','timestamp.json','server.json','PUBLISHED.json','requests.jsonl'):
    copy(FAILED/'repository'/name,Path('native-final/repository')/name)
for name in ('native-update-driver-residual.py','native-update-driver-residual-final.py','close-native-lab.py','prepare-final-lab.py','collect-residual-handoff.py'):
    copy(LOCAL/name,Path('reproduction-drivers')/name)
copy(LOCAL/'native-residual-final-process.log',Path('native-final/driver-process.log'))
receipt=load(LAB/'candidate/BUILD_RECEIPT.json');assert receipt['sourceSha']==SOURCE
installer=LOCAL/'delivery'/('CCB-Studio-Suite-review-'+SOURCE[:7]+'-windows-x64.exe')
if not installer.exists():shutil.copyfile(LAB/'candidate/studio-installer.exe',installer)
assert digest(installer)==receipt['sha256']
app_evidence=ROOT/'.local/windows-preview/evidence/20261024T164151200964Z-review-residual-final'
inputs=load(app_evidence/'source-before.json');app_inputs={k:v for k,v in inputs.items() if k.startswith(('modules/','tools/windows-launcher/'))}
assert all(digest(ROOT/p)==h for p,h in app_inputs.items())
git_inputs=[]
for path,h in app_inputs.items():
    b=subprocess.check_output(['git','show',SOURCE+':'+path],cwd=ROOT)
    actual=(ROOT/path).read_bytes();mode='EXACT' if b==actual else 'CRLF_LF_ONLY'
    assert b.replace(b'\r\n',b'\n')==actual.replace(b'\r\n',b'\n')
    git_inputs.append(dict(path=path,gitSha256=hashlib.sha256(b).hexdigest(),buildInputSha256=h,relationship=mode))
native=load(OUT/'native-final/NATIVE_UPDATE_SUMMARY.json');assert native['status']=='PASS' and native['credentials']==2
write(Path('FIXED_SOURCE_PACKAGE_MAP.json'),dict(sourceCommit=SOURCE,appBuildCommit=receipt['appBuildSourceSha'],appInputCount=len(app_inputs),appInputs=git_inputs,installerVersion='0.2.2-internal',installerBytes=receipt['bytes'],installerSha256=receipt['sha256'],installerLocalPath=str(installer),installerNotPublished=True,publicTag='v0.0.0-preview.115',publicSource='ff145d051bae286ab3e27cadd41195f8bd4c4e18',publicInstallerBytes=168890283,publicInstallerSha256='356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc',noPublicTrustChanges=True,independentReviewOfNewSource='PENDING'))
write(Path('RUN_INDEX.json'),dict(canonicalJvm='runs/20261024T164151200964Z-review-residual-final',jvmUnique=18,canonicalPython='runs/runtime-20261024T165010681550Z',pythonUnique=56,canonicalNative='native-final',nativeUniqueScenario=1,doNotSumRepeatedRuns=True,firstJvm=dict(tests=20,failures=3,status='FAILED',unclosedOutOfScope='GuidanceSafetyTest.q09CanvasRotateUndoMaskResetApplyAndReopen'),firstNative='FAILED_WINERROR32_RETAINED',oldEvidenceUnchanged='1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0',human115='PENDING',promptComparison='PENDING',independentReview='PENDING'))
frozen={
 'docs/audits/evidence/PROJECT_PHASE_20261024/RAW_EVIDENCE.zip':'dd2da2bcc76c47b6f0b9d4688e30cb2ea99314dd536e271f709a8223b1a9136a',
 'docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/RAW_EVIDENCE.zip':'e14b0d97ec722aac9f31d903f6012625eb97c4a5e5bb215401e58de741f00e0e'}
assert all(digest(ROOT/p)==h for p,h in frozen.items())
assert not subprocess.check_output(['git','diff','1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0','HEAD','--','docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025','docs/audits/evidence/PROJECT_PHASE_20261024'],cwd=ROOT)
write(Path('FROZEN_PRESERVATION.json'),dict(status='PASS',oldRawArchives=frozen,oldEvidenceTreesUnchanged=True,noDailyUpgrade=True,historicalManualCleanupSeparate='../../HISTORICAL_CLEANUP_EXECUTED_20261025.md'))
(OUT/'.gitattributes').write_text('* -text\n**/* -text\n',encoding='ascii')
manifest=[dict(path=p.relative_to(OUT).as_posix(),bytes=p.stat().st_size,sha256=digest(p)) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name not in ('RAW_MANIFEST.json','RAW_EVIDENCE.zip')]
write(Path('RAW_MANIFEST.json'),dict(files=manifest,privateData='EXCLUDED: no credential blobs, user assets, real tokens, private signing material or recovery codes'))
with zipfile.ZipFile(OUT/'RAW_EVIDENCE.zip','w',zipfile.ZIP_DEFLATED) as z:
    for item in manifest:z.write(OUT/item['path'],item['path'])
    z.write(OUT/'RAW_MANIFEST.json','RAW_MANIFEST.json')
print(json.dumps(dict(source=SOURCE,rawFiles=len(manifest),zipBytes=(OUT/'RAW_EVIDENCE.zip').stat().st_size,zipSha256=digest(OUT/'RAW_EVIDENCE.zip'),installer=str(installer),installerSha256=digest(installer)),indent=2))
