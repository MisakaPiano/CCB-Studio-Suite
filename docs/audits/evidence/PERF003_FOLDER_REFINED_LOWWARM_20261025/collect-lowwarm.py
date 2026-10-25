from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_FOLDER_REFINED_LOWWARM_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
app='0564cc48fe75a4aecec6e8b0137b762a89f8bc2c';execution='a59a78324b47e0e5f9287f90dfa3d02b45f29346'
candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-folder-descendants-refined-lowwarm-20261025'));assert len(candidates)==1;src=candidates[0]
for p in src.iterdir():
    if p.is_file():
        target=DEST/'run'/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
s=json.loads((src/'summary.json').read_text());assert s['status']=='PASS' and s['sourceStable'] and s['baseCommit']==execution
assert(s['tests'],s['failures'],s['errors'],s['skipped'])==(1,0,0,0)
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/OrganizationDescendantsPerformanceTest.kt'
assert subprocess.check_output(['git','diff','--name-only',app,execution,'--','modules','tools'],cwd=ROOT,text=True).splitlines()==[testPath]
old=subprocess.check_output(['git','show',app+':'+testPath],cwd=ROOT).decode();new=subprocess.check_output(['git','show',execution+':'+testPath],cwd=ROOT)
assert old.replace('repeat(20){verify(org.legacyDescendants(target));verify(org.descendants(target))}','repeat(2){verify(org.legacyDescendants(target));verify(org.descendants(target))}',1)==new.decode()
inputs=json.loads((src/'source-before.json').read_text());assert inputs[testPath] in (sha(new),sha(new.replace(b'\n',b'\r\n')))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt'
raw=(ROOT/'docs/audits/evidence/PERF003_FOLDER_DESCENDANTS_REFINED_20261025/inputs/final/GenerationOrganization.kt').read_bytes();assert sha(raw)==inputs[production]
assert raw.replace(b'\r\n',b'\n')==subprocess.check_output(['git','show',app+':'+production],cwd=ROOT)
output=ET.parse(next(src.glob('*OrganizationDescendantsPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('FOLDER_DESCENDANTS_ALLOCATION=')).split('=',1)[1]);write(DEST/'samples.json',rows)
medians=[]
for folders in (32,256,1000):
    for context in ('root','small','leaf','unknown'):
        for mode in ('legacy','current'):
            selected=[r for r in rows if r['folders']==folders and r['context']==context and r['kind']==mode]
            medians.append(dict(folders=folders,context=context,kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE'
write(DEST/'SOURCE_MAP.json',dict(applicationSource=app,executionSource=execution,onlyChangedModuleInput=testPath,onlyChange='test warmup20to2,no production change',productionInputSha256=inputs[production],testInputSha256=inputs[testPath],diagnosticUniqueMethods=1,noAdditionalFunctionalCoverage=True,originalThreeMethodsNotAddedAgain=True,originalCandidateAndWarmDiagnosticRetained=True,performanceStatus='NOT ADOPTED;lowwarm root still slower,restore original traversal before separate caller optimization',release='NONE',independentReview='NOT REQUESTED',userAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(summary=s,medians=medians,root1000=dict(legacyWallMs=6.03445,currentWallMs=13.39945,legacyAllocatedBytes=1248336,currentAllocatedBytes=124856),classification='Functional measurement PASS but response-performance candidate NOT ADOPTED;no causal attribution',cleanup=clean,limits=['Two warmups per mode,four same-JVM alternating rounds;full context data retained','Desktop50020 created02:19:15 during resource-gate-to-Gradle transition and present during summary window;CPU activity unknown','No quiet host/confirmed JIT cause/first invocation/fullUI/RSS/universal speed conclusion','More-warm cohort not used to erase this slower cohort or force adoption']))
shutil.copyfile(Path(__file__),DEST/'collect-lowwarm.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Refined candidate under original two-warmup condition

Production0564cc4 unchanged,executiona59a783 differs only symmetric warmup20->2. One measurement method PASS,all metadata/DFS/membership checks,source stable;no extra three-method functionality coverage and no1+3count. The1000-node root response13.40ms vs6.03ms slower persists despite allocation124856B vs1248336B. Both earlier01020b1 negative2warm/20warm diagnostic and0564cc4 20warm favorable cohort retained separately. No threshold/assertion removed or negative data overwritten. Root cause of timing variation remainsUNKNOWN;no security/background/JIT attribution without evidence. Decision NOT ADOPTED,restore original traversal and investigate caller multiplicity separately. This is performance rejection with functional JUnit PASS,not relabeled test failure/PASS.

Full admitted StudioState validation/4MiB/bytes/order/32depth unchanged,pure node metadata. Desktop50020 creation02:19:15 falls after gate start02:19:12 but before actualGradle summary start02:19:20;later observation confirms presence,exact CPU/JUnit-sample overlap unknown. Lowpriority/singleworker2processors256MiB,not quiet-host or universal speed proof. No unrelated process/security action/real directories/images/credentials/service/paid calls/paused source/daily install/windows/package/release. Success own fixtures sealed/retired,failed/unknown/user/backups retained. Independent NOT REQUESTED,user/package NOT TESTED,new Project material only prepared;all prior USER_NOT_TESTED/PENDING/q09/PRODUCT_HOLD remain.
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
