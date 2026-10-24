from pathlib import Path
import hashlib,json,platform,shutil,statistics,subprocess,zipfile

root=Path('H:/CCB-Studio-Suite')
local=root/'.local/perf-organization-20261025'
dest=root/'docs/audits/evidence/PERF_ORGANIZATION_20261025'
assert not (dest/'RAW_MANIFEST.json').exists(), 'Never overwrite fixed evidence'
if dest.exists():assert {p.name for p in dest.iterdir()}<={'runs','baseline-batch-listing.json'}, 'Only resume own interrupted collection stage'
dest.mkdir(parents=True,exist_ok=True)
def write(p,value):p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(data):return hashlib.sha256(data).hexdigest()
def latest(phase):return sorted((root/'.local/windows-preview/evidence').glob('*-'+phase))[-1]
for phase,label in [('perf-organization-baseline-20261025','interrupted'),('perf-organization-baseline-serial-20261025','baseline'),('perf-organization-final-20261025','final')]:
    source=latest(phase);target=dest/'runs'/label;target.mkdir(parents=True,exist_ok=True)
    for f in source.iterdir():
        if f.is_file():shutil.copyfile(f,target/f.name)
    if label=='interrupted':assert (target/'INTERRUPTED.json').exists()
    else:
        summary=json.loads((target/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable']
        assert summary['tests']==(1 if label=='baseline' else 9)
shutil.copyfile(local/'baseline-batch-listing.json',dest/'baseline-batch-listing.json')
write(dest/'AMBIENT_OBSERVATION.json',dict(status='NOT_CONTINUOUSLY_RECORDED',baseline='Desktop JVMs observed during baseline build; later PowerShell pipeline returned no rows and created no JSON file',final='No Desktop JVM observed in tool snapshots at final start/end; no continuous monitor or quiet-machine guarantee'))
shutil.copyfile(root/'modules/studio-desktop/build/performance-20261025/batch-listing.json',dest/'final-batch-listing.json')
shutil.copyfile(root/'modules/studio-desktop/build/performance-20261025/paired-comparison.json',dest/'paired-comparison.json')
measurements=json.loads((dest/'paired-comparison.json').read_text())['measurements']
medians={str(n):{phase:{k:statistics.median([r[k] for r in measurements if r['images']==n and r['phase']==phase]) for k in ['wallMs','threadCpuMs','allocatedBytes']} for phase in ['legacy','indexed-including-build','indexed-reused-snapshot']} for n in [100,1000,10000]}
write(dest/'MEDIANS.json',medians)
source='98666fba90e0ebc10eb1b800a788b803ac211e1e'
inputs=[]
before=json.loads((dest/'runs/final/source-before.json').read_text())
for part,files in [('main',['GalleryBatchIndex.kt','GalleryOrganization.kt','GalleryWorkspace.kt']),('test',['OrganizationPerformanceTest.kt','OrganizationPerformanceLegacy.kt'])]:
    for name in files:
        path='modules/studio-desktop/src/'+part+'/kotlin/io/github/misakapiano/ccbstudio/desktop/'+name
        git=subprocess.check_output(['git','show',source+':'+path],cwd=root);disk=(root/path).read_bytes()
        assert sha(disk)==before[path]
        assert git.replace(b'\r\n',b'\n')==disk.replace(b'\r\n',b'\n')
        inputs.append(dict(path=path,gitSha256=sha(git),executedDiskSha256=sha(disk),relation='EXACT' if git==disk else 'CRLF_LF_ONLY'))
old=subprocess.check_output(['git','show','69f53d314abe0aa58730ac5ed4aadffd1ff7b940:modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryOrganization.kt'],cwd=root).decode()
old=old[old.index('internal fun organizationFolders('):old.index('internal fun organizationOpen(')]
legacy=(root/'modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/OrganizationPerformanceLegacy.kt').read_text(encoding='utf-8')
assert legacy[legacy.index('internal fun legacyOrganizationFolders('):].replace('internal fun legacyOrganizationFolders(','internal fun organizationFolders(',1).strip()==old.strip()
write(dest/'SOURCE_MAP.json',dict(sourceCommit=source,baselineCommit='69f53d314abe0aa58730ac5ed4aadffd1ff7b940',projectClosureCommit='cc745d17d7ced54d867655c72bd189eeb953dab1',inputs=inputs,legacyFunctionMatchesFixedSource=True,installerBuilt=False,appImageBuilt=False,publishedRelease='v0.0.0-preview.115 (unchanged; excludes this optimization and f6598d3 cleanup fix)',runtimeValidation='Gradle test JVM/offscreen Compose; no native package validation',independentPerformanceReview='PENDING',userAssetAndEditorAcceptance='USER_NOT_TESTED',q09='FAILED/UNKNOWN',promptComparison='PENDING'))
write(dest/'ENVIRONMENT.json',dict(os=platform.platform(),python=platform.python_version(),java='17.0.20.1',testHeapBytes=268435456,activeProcessors=2,gradleWorkers=1,networkDependencies='offline/strict verification',clock='UTC in run records; user-facing Asia/Tokyo',input='100/1000/10000 synthetic in-memory image origins; 10 per batch; 2 warmups and 4 alternating paired rounds',nativeApplicationOpened=False,realProviderCalls=0,userDataRead=False,pausedCollectionsResumed=False,ambient='Initial run interrupted after Desktop build appeared; serial baseline started with no Desktop JVM but another Desktop build appeared before completion, so baseline wall time is exploratory. Final run observed no Desktop JVM at start/end; not continuous system-wide monitoring. Thread CPU is coarse (15.625ms Windows quantum).'))
(dest/'ATTEMPTS.md').write_text('''# Attempts and counts

- First direct embedded-Python runner invocation failed to import workspace_artifacts before Gradle. A wrapper inserts the tools path; no production change was made for this launcher issue.
- First Gradle attempt was interrupted before test execution when Desktop compilation appeared. Log and source input hashes retained; NOT TESTED, not PASS. Its unsealed fixtures retained, no historical cleanup.
- Serial baseline: 1 method PASS at 69f53d3, unchanged production from e4d3d5c. The 9 measurements inside it are scenarios, not extra tests. Desktop started another build before it completed; use this for scale discovery, not a quiet-machine wall-time claim.
- Final: 9 unique methods PASS at 98666fb, 0 failures/errors/skips. Includes the baseline method again, so never add 1+9. Four paired timing rounds and inner assertions are not additional test cases. No JVM/UI/native update regressions unrelated to changed grouping were repeated.
- Legacy comparison source extraction initially used the Python Windows default codec and failed before writing; corrected to UTF-8. This is fixture preparation, not a product test failure.
- Evidence collection first stopped on the absent ambient-process JSON (an empty PowerShell pipeline had created no file); resumed its own incomplete stage, recording the observation limitation explicitly. No fixed manifest/archive existed or was replaced.
- No new performance independent review or user acceptance. Project's prior P2 closure covers cleanup source only and is not performance approval. q09 old FAILED/UNKNOWN and user rejections retained elsewhere; no new q09 run.
''',encoding='utf-8')
shutil.copyfile(root/'tools/Run-PreviewChecks.py',dest/'Run-PreviewChecks.py')
shutil.copyfile(Path(__file__),dest/'collect.py')
(dest/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
manifest=[dict(path=f.relative_to(dest).as_posix(),bytes=f.stat().st_size,sha256=sha(f.read_bytes())) for f in sorted(dest.rglob('*')) if f.is_file()]
write(dest/'RAW_MANIFEST.json',dict(format=1,files=manifest))
archive=dest/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for f in sorted(dest.rglob('*')):
        if f.is_file() and f!=archive:z.write(f,f.relative_to(dest).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in manifest:assert sha(z.read(item['path']))==item['sha256']
receipt=dict(path=str(archive),bytes=archive.stat().st_size,sha256=sha(archive.read_bytes()),manifestFiles=len(manifest),zipMembers=len(manifest)+1,source=source,medians=medians)
write(local/'COLLECTION_RECEIPT.json',receipt);print(json.dumps(receipt,indent=2))
