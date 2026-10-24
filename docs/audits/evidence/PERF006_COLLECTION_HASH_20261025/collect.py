from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF006_COLLECTION_HASH_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'225318f7284599840874397383225b75fe45b6db','final':'cb819b4d81183ddda7e374f77abbfc141f57e663'}
counts={};metrics={};cleanup=[]
for kind in ('baseline','final'):
    runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf006-collection-hash-'+kind+'-20261025'));assert len(runs)==1;src=runs[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable'] and summary['baseCommit']==sources[kind]
    assert (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 5),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    output=ET.parse(next(src.glob('*CollectionHashPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split('COLLECTION_HASH_ALLOCATION=',1)[1].strip());write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
identities=[]
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryRepository.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/CollectionHashPerformanceTest.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetStore.java','tools/Run-PreviewChecks.py']
for kind in ('baseline','final'):
    before=json.loads((DEST/'runs'/kind/'source-before.json').read_text())
    for path in paths:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+path],cwd=ROOT)
        assert before[path] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=path,testInputSha256=before[path],gitSha256=hashlib.sha256(fixed).hexdigest()))
unchanged=['modules/asset-library','modules/image-authority','modules/studio-desktop','tools','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt']
assert subprocess.check_output(['git','diff',sources['baseline'],sources['final'],'--',*unchanged],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(sources=sources,identities=identities,unchangedPaths=unchanged,finalUniqueMethods=5,baselineNotAddedAgain=True,productionChange='LibraryRepository.scan reuses same fully hashed input bytes; no read/decode/budget/state/checkpoint or idempotent recovery rule removed',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=5,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Separate JVMs and real file write/force; no causal latency or RSS improvement claim','Both runs Gradle low priority,single worker,2 processors,256MiB test heap','Owner checks before launch found no Desktop Java,not continuous quiet proof','Two different16x16 valid PNGs with3/12MiB private ancillary payload per file,not real image/scale diversity','Two files per payload size,unchanged explicit synthetic scan; automatic collections stay paused,no watcher/user scan started']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text("# Synthetic collection input digest reuse\n\nBaseline225318f (test definition07d838c with documentation-only225318f):1 actual API method PASS. Two distinct synthetic16x16 PNGs per3/12MiB private ancillary size,initial import,one unchanged warmup,3 measured unchanged scans. No user sources or watcher; collections paused and automaticFolders empty throughout. Identity/status/asset count/source digests checked outside each measurement. Measured costs include unchanged scan's existing catalog/source verification and bounded ByteArray read; no invented isolated SHA timing.\n\nFinalcb819b4:5 unique methods PASS,0failure/error/skip,source stable. Performance baseline method repeated,plus existing cancellation/checkpoints/front-end workspace-read liveness,legacy committed image adoption,changed/missing source with image/Prompt labels/backup/restore,and Prompt commit-interruption idempotence across reopen. Internal2files/rounds/two interruption points not counted as separate tests. No new failure or blind rerun. Prior copy lock RED,q09 and user rejections retained separately.\n\nProduction diff is confined to scan's one input-read scope: same bytes and fullSHA return together,comparison/decode/observation/source recipe reuse thatSHA rather than hashing identical bytes again. Source path safety,before/after file attributes,read budgets,decode rules,source records,label/version logic,previousAssetId,commit/checkpoint/finally/cancellation,scan file/depth limits unchanged. No request/Prompt generation or comparison feature,service access,credential/data format/update logic change. Shared helper/AssetStore/Desktop remain unchanged. Hash comparison remains full,not metadata-only or skipped verification.\n\nSeparate JVMs,real filesystem/store/history/force costs,raw slower/outlier samples retained. Both runs existing low-priority one-worker runner with2processors/256MiB heap. Boundary owner checks empty before starts; not continuous quiet host proof or causal speed/RSS/UI/large real directory claim. The deterministic source change removes one repeated digest of the in-memory snapshot,not existing original verification.\n\nDesktop busy initially deferred baseline; code/test review continued without a heavy start. After final BUILD SUCCESSFUL,Desktop Java PID51720,parent49160 was observed at2026-10-24T23:01:25Z,created23:00:16.670315Z during the final runner interval. Exact sample/activity overlap UNKNOWN; no quiet-host claim. No process/security change or termination. Successful own synthetic fixtures retired after external logs only. No real gallery/private data/credentials/paid service,resumed user directory,new package/release/daily install or new Project payload. New evidence prepared only. Asset/editor user-not-tested,115/Prompt pending,q09 failed/unknown and PRODUCT_HOLD / NOT RC remain.\n",encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert hashlib.sha256(z.read(item['path'])).hexdigest()==item['sha256']
print(json.dumps(dict(sources=sources,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup)),indent=2))
