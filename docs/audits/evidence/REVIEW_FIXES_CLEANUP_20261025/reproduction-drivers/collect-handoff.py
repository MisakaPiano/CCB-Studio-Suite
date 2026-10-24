"""Explicit synthetic log allowlist. Never copy data roots, vault blobs or keys."""
import hashlib,json,shutil,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];LOCAL=ROOT/'.local/review-fixes-cleanup-20261024'
OUT=ROOT/'docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025';OUT.mkdir(parents=True,exist_ok=True)
def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def copy(src,dst):
    if not src.is_file():return
    dst=OUT/dst;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
def write(path,value):
    p=OUT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=True,indent=2)+'\n')
indexes=[]
for source in sorted(LOCAL.glob('runtime-*')):
    if not source.is_dir():continue
    for name in ('summary.json','tests.log','test-cases.json','fixture-cleanup.json'):copy(source/name,Path('runs')/source.name/name)
    if (source/'summary.json').exists():indexes.append(dict(path='runs/'+source.name,**{k:v for k,v in json.loads((source/'summary.json').read_text()).items() if k in ('status','tests','failures','errors','skipped','baseCommit','sourceStable')}))
for source in (ROOT/'.local/windows-preview/evidence').iterdir():
    if not any(source.name.endswith(x) for x in ('review-six-boundaries','review-fixes-cleanup-final','review-lifecycle-final')):continue
    for p in source.iterdir():
        if p.is_file() and p.suffix in ('.xml','.json','.log'):copy(p,Path('runs')/source.name/p.name)
    indexes.append(dict(path='runs/'+source.name,**{k:v for k,v in json.loads((source/'summary.json').read_text()).items() if k in ('status','tests','failures','errors','skipped','baseCommit','sourceStable')}))
labs=[ROOT/'.local/windows-rc/review-update-native/9dde7da-a01be9b0',ROOT/'.local/windows-rc/review-update-native/9dde7da-retry-0cfb73']
labs += [ROOT/'.local/disposable'/name/'payload/review-update-native' for name in ('ab-c8b3b79620784ac7964b7e6e6e88344c','ab-0896b1e50a0d41e9b06f2ad9402e6859','ab-d060440883a84715bf8b57d9939c3a4a')]
labs.append(Path(json.loads((LOCAL/'native-lab.json').read_text())['lab']))
for lab in labs:
    if not lab.exists():continue # prior raw evidence was already durably copied
    prefix=Path('native')/(lab.parent.parent.name if lab.name=='review-update-native' else lab.name)
    for p in lab.iterdir():
        if p.is_file() and p.suffix in ('.json','.log','.txt'):copy(p,prefix/p.name)
    for folder in ('candidate','candidate-28d09f5-not-tested'):
        for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi','payload/INSTALL_MANIFEST.json','payload/update-config.json','payload/updater/RUNTIME_MANIFEST.json'):
            copy(lab/folder/name,prefix/folder/name)
    for name in ('root.json','targets.json','snapshot.json','timestamp.json','server.json','PUBLISHED.json'):
        copy(lab/'repository'/name,prefix/'repository'/name)
    for folder in (lab/'runtime/updates/handoffs').glob('*'):
        for name in ('plan.json','result.json','B_VERIFIED.json','B_FAILED.json','events.jsonl','b-stdout.log','b-stderr.log'):copy(folder/name,prefix/'handoff'/folder.name/name)
    cleanup=lab/'runtime/updates/cleanup'
    for name in ('events.jsonl','state.json','last-result.json','post-update-process.log'):copy(cleanup/name,prefix/'cleanup'/name)
    for p in (cleanup/'owned').glob('*.json'):copy(p,prefix/'cleanup/owned'/p.name)
    probe=lab/'entrypoint-probe'
    for p in probe.glob('*'):
        if p.is_file() and p.suffix in ('.json','.log','.java'):copy(p,prefix/'entrypoint-probe'/p.name)
for name in ('native-update-driver.py','close-native-lab.py','prepare-final-lab.py','entrypoint-probe.py','EntrypointConcurrencyProbe.java','final-native-observation.py','collect-handoff.py'):
    copy(LOCAL/name,Path('reproduction-drivers')/name)
for name in ('FROZEN_BASELINE_PRESERVATION.json','HISTORICAL_RESIDUE_READ_ONLY.json','WORKSPACE_HISTORICAL_READ_ONLY.json','WORKSPACE_HISTORICAL_ENRICHED.json','CONFIRMED_WORKSPACE_PROGRAM_CANDIDATES.json','SHARED_TEST_ROOTS_EXACT.json','PUBLIC_115_OBSERVATION.json','INVENTORY_INTERRUPTED_FOR_BOUNDED_SCAN.json','INVENTORY_SECOND_INTERRUPTED.json'):
    copy(LOCAL/name,Path('observations')/name)
source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
files=subprocess.check_output(['git','diff','--name-only','2489844984e2ad13f2f0c9d614702d37a7e1d615',source],cwd=ROOT,text=True).splitlines()
write(Path('CHANGED_FILES.json'),dict(base='2489844984e2ad13f2f0c9d614702d37a7e1d615',source=source,files=files))
final=labs[-1];receipt=json.loads((final/'candidate/BUILD_RECEIPT.json').read_text());assert receipt['sourceSha']==source
delivery=LOCAL/'delivery';delivery.mkdir(exist_ok=True);installer=delivery/'CCB-Studio-Suite-review-da2ae81-windows-x64.exe'
shutil.copyfile(final/'candidate/studio-installer.exe',installer);assert digest(installer)==receipt['sha256']
appsource=json.loads((ROOT/'.local/windows-preview/evidence/20261024T152825132857Z-review-lifecycle-final/source-before.json').read_text())
unchanged={p:h for p,h in appsource.items() if p.startswith(('modules/','tools/windows-launcher/'))}
assert all(digest(ROOT/p)==h for p,h in unchanged.items())
write(Path('FIXED_SOURCE_PACKAGE_MAP.json'),dict(sourceCommit=source,appBuildCommit=receipt['appBuildSourceSha'],appInputsUnchangedSinceBuild=unchanged,installerSha256=receipt['sha256'],installerBytes=receipt['bytes'],installerLocalPath=str(installer),release='UNPUBLISHED_INTERNAL_2',publicReleaseUnchanged='v0.0.0-preview.115',publicSource='ff145d051bae286ab3e27cadd41195f8bd4c4e18',publicInstallerSha256='356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc',realServices='NOT_TESTED',human115='PENDING',independentReview='PENDING'))
write(Path('RUN_INDEX.json'),dict(canonicalJvm='runs/20261024T152825132857Z-review-lifecycle-final',canonicalPython='runs/runtime-20261024T154544162428Z',canonicalNative='native/'+final.parent.parent.name,uniqueCounts=dict(jvm=38,python=49),doNotSumAttempts=True,runs=indexes,oldReviewPreserved='2489844',userRejectedSevenDayPolicy='SUPERSEDED_BY_IMMEDIATE_AFTER_COMPLETE',oldFailuresAndUserRejections='PRESERVED_NOT_OVERRIDDEN',interimMetadataRetention='Earlier internal raw root/targets were not retained outside disposable labs; final canonical native evidence includes all public signed metadata'))
manifest=[dict(path=p.relative_to(OUT).as_posix(),bytes=p.stat().st_size,sha256=digest(p)) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name not in ('RAW_EVIDENCE.zip','RAW_MANIFEST.json')]
write(Path('RAW_MANIFEST.json'),dict(files=manifest,privateData='EXCLUDED: no user assets, credential blobs, real tokens, signing keys or recovery codes'))
archive=OUT/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for item in manifest:z.write(OUT/item['path'],item['path'])
    z.write(OUT/'RAW_MANIFEST.json','RAW_MANIFEST.json')
print(json.dumps(dict(evidence=str(OUT),files=len(manifest),zipSha256=digest(archive),zipBytes=archive.stat().st_size,installerSha256=digest(installer)),indent=2))
