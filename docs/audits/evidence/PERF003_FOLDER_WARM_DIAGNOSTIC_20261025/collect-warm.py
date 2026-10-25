from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_FOLDER_WARM_DIAGNOSTIC_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
app='01020b1f82bff95dfacdb75f6318b3d2d258c626';execution='9512e88a9f78df9e373a8a1eb182992819475706'
candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-folder-descendants-warm-diagnostic-20261025'));assert len(candidates)==1;src=candidates[0]
for p in src.iterdir():
    if p.is_file():
        target=DEST/'run'/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
s=json.loads((src/'summary.json').read_text());assert s['status']=='PASS' and s['sourceStable'] and s['baseCommit']==execution
assert(s['tests'],s['failures'],s['errors'],s['skipped'])==(1,0,0,0)
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/OrganizationDescendantsPerformanceTest.kt'
changed=subprocess.check_output(['git','diff','--name-only',app,execution,'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert changed==[testPath]
old=subprocess.check_output(['git','show',app+':'+testPath],cwd=ROOT).decode();new=subprocess.check_output(['git','show',execution+':'+testPath],cwd=ROOT)
assert old.replace('repeat(2){verify(org.legacyDescendants(target));verify(org.descendants(target))}','repeat(20){verify(org.legacyDescendants(target));verify(org.descendants(target))}',1)==new.decode()
inputs=json.loads((src/'source-before.json').read_text());assert inputs[testPath] in (sha(new),sha(new.replace(b'\n',b'\r\n')))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt'
priorRaw=(ROOT/'docs/audits/evidence/PERF003_FOLDER_DESCENDANTS_20261025/inputs/final/GenerationOrganization.kt').read_bytes();assert sha(priorRaw)==inputs[production]
assert priorRaw.replace(b'\r\n',b'\n')==subprocess.check_output(['git','show',app+':'+production],cwd=ROOT)
output=ET.parse(next(src.glob('*OrganizationDescendantsPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('FOLDER_DESCENDANTS_ALLOCATION=')).split('=',1)[1]);write(DEST/'samples.json',rows)
medians=[]
for folders in (32,256,1000):
    for context in ('root','small','leaf','unknown'):
        selectedContext=[r for r in rows if r['folders']==folders and r['context']==context];assert len({r['orderSha256'] for r in selectedContext})==1
        for mode in ('legacy','current'):
            selected=[r for r in selectedContext if r['kind']==mode]
            medians.append(dict(folders=folders,context=context,kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE'
write(DEST/'SOURCE_MAP.json',dict(applicationSource=app,executionSource=execution,onlyChangedModuleInput=testPath,onlyChange='symmetric warmup2to20,no production change',productionInputSha256=inputs[production],productionEqualsPriorCandidateRaw=True,testInputSha256=inputs[testPath],diagnosticUniqueMethods=1,noAdditionalFunctionalCoverage=True,originalThreeMethodsNotAddedAgain=True,oldTwoWarmupArchive='4ed5cae62fc29993d7191f29b508c6a68ae86f0c5e9574ed85a62a1694a5a7e8,7314ef9',oldNegativeCohortRetained=True,performanceStatus='HOLD;different warmup cohort changes direction,no JIT root cause/first-use acceptance',release='NONE',independentReview='NOT REQUESTED',userAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(summary=s,medians=medians,oldRoot1000TwoWarmup=dict(legacyWallMs=5.78785,currentWallMs=12.4791),newRoot1000TwentyWarmup=dict(legacyWallMs=8.8963,currentWallMs=7.87515),classification='Warmup-sensitive observation,not a source fix or erasure of old slower cohort',cleanup=clean,limits=['No first invocation/cold cache/fullUI/RSS/universal speed guarantee','No tracing of JIT compilation tiers or isolated cause,root cause UNKNOWN','Symmetric20warmups still same fixed synthetic trees/four paired rounds,not added tests','Same production code,unit1PASS does not close performanceHOLD']))
local=ROOT/'.local/perf003-folder-descendants-20261025'
boundary=local/'resource-boundaries.jsonl'
if boundary.exists():shutil.copyfile(boundary,DEST/'resource-boundaries.jsonl')
shutil.copyfile(Path(__file__),DEST/'collect-warm.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Twenty-warmup diagnostic only

Application01020b1 unchanged;test execution9512e88 differs only repeat2->20 for both warmup modes. One measurement method PASS,source stable,all contexts/metadata/order checks;not three new regression methods and not1+3total. Original two-warmup same-JVM slower root12.48ms vs5.79ms is preserved in7314ef9/rawSHA4ed5cae6... and remains material. This diagnostic root7.88ms vs8.90ms changes direction,while tiny contexts remain mixed;no actual JIT-tier trace or isolated causal proof. Warmup sensitivity is an observation,not confirmed root cause or first-use/fullUI performance acceptance. No assertion/threshold removed,production not normalized/changed,no old raw log/package overwritten. No source fix in this run,HOLD remains pending separate refined source evidence.

Pure admitted StudioState metadata within4MiB,full validation outside timed helper,2processors256MiB/singleworker/lowpriority/Desktop-first gate. No old accepted batch/startup rerun or unrelated suite. No real directories/images/credentials/service/paid calls/paused scan/daily app/window/package/release/security changes. Success own fixtures sealed/retired,oldfailures/unknown/user/backups retained. Independent NOT REQUESTED,user/package NOT TESTED,new Project only prepared. Prior USER_NOT_TESTED/PENDING/q09/PRODUCT_HOLD boundaries remain.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert sha(z.read(item['path']))==item['sha256']
print(json.dumps(dict(zipBytes=archive.stat().st_size,zipSha256=sha(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,diagnosticUniqueMethods=1,application=app,execution=execution),indent=2))
