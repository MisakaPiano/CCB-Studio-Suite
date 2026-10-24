from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001005_WORKSPACE_BYTES_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'b6abdc92d5b47f256e84e4c3da0695b5be3a6e03','final':'66ed23c44357b10de198eb2a488475b472b6ac81'}
counts={};metrics={};cleanup=[]
for kind in ('baseline','final'):
    runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf001005-workspace-bytes-'+kind+'-20261025'));assert len(runs)==1;src=runs[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable'] and summary['baseCommit']==sources[kind]
    assert (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 7),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    output=ET.parse(next(src.glob('*WorkspaceBytesPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split('WORKSPACE_BYTES_ALLOCATION=',1)[1].strip());write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
identities=[]
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/WorkspaceBytesPerformanceTest.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetStore.java','tools/Run-PreviewChecks.py','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/AssetBytesBuffer.kt']
for kind in ('baseline','final'):
    before=json.loads((DEST/'runs'/kind/'source-before.json').read_text())
    for path in paths:
        if path not in before:
            assert kind=='baseline' and path.endswith('/AssetBytesBuffer.kt');continue
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+path],cwd=ROOT)
        assert before[path] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=path,testInputSha256=before[path],gitSha256=hashlib.sha256(fixed).hexdigest()))
unchanged=['modules/asset-library','modules/image-authority','modules/studio-desktop','tools','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt']
assert subprocess.check_output(['git','diff',sources['baseline'],sources['final'],'--',*unchanged],cwd=ROOT)==b''
workspacePath='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt'
oldWorkspace=subprocess.check_output(['git','show',sources['baseline']+':'+workspacePath],cwd=ROOT).decode()
newWorkspace=subprocess.check_output(['git','show',sources['final']+':'+workspacePath],cwd=ROOT).decode()
assert oldWorkspace.replace('import java.io.ByteArrayOutputStream\n','').replace('ByteArrayOutputStream().also {assets.export(UUID.fromString(id),it)}.toByteArray()','AssetBytesBuffer().also {assets.export(UUID.fromString(id),it)}.bytes()')==newWorkspace
write(DEST/'SOURCE_MAP.json',dict(sources=sources,identities=identities,unchangedPaths=unchanged,finalUniqueMethods=7,baselineNotAddedAgain=True,productionChange='StudioWorkspace.bytes collector only; same assets.export inside same workspace operation and lock; fixed owned blocks copied before store buffer reuse',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=7,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Separate JVMs and real file write/force; no causal latency or RSS improvement claim','Both runs Gradle low priority,single worker,2 processors,256MiB test heap','Owner checks before launch found no Desktop Java,not continuous quiet proof','3/12MiB opaque synthetic binary fixtures,not image decode/UI or real content diversity','API still returns complete encoded bytes and keeps workspace/store serialization; no new cancellation/decoder/format behavior or global cache']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text("# Verified asset byte return allocation\n\nBaselineb6abdc9:1 actual StudioWorkspace.bytes method PASS,3/12MiB opaque synthetic files,one warmup+4 measured calls per size. Counters sampled immediately after the API returns,before any JSON construction,digest assertions or output. Final66ed23c:7 unique methods PASS,0failure/error/skip,source stable. Baseline repeated method/internal cases not counted as more tests.\n\nOnly StudioWorkspace.bytes's collector changes from ByteArrayOutputStream to module-internal AssetBytesBuffer plus one unused import removal. Exact whole-file replacement assertion in collector verifies no other StudioWorkspace code changed. Same synchronized operation and assets.export call; store source verification/admission/drain and256MiB stored-asset bound unchanged. New collector copies input before source's64KiB transfer buffer reuse,keeps private chunks,returns complete bytes or throws,one exact small block can be returned directly; no global cache/schema/extra source read/metadata-only trust or decode. Full result still required,large cases allocate approximately2x encoded source versus geometric older growth,not streaming-to-caller/RSS proof. Public calls still return independent arrays.\n\nFinal4 new methods:actual allocation/bytes with2N+512KiB bound;10 size edges0/1/63/64/65535/65536/65537/131071/131072/131073 and independently mutable returns/source/state/count preservation;corrupt stored blob cannot return partial bytes or replace source while state remains;reused producer buffer,mixed scalar/bulk writes,offsets/zero/bad range preserve owned bytes.3 existing integration methods cover project/undo/reopen,changed/missing image andPrompt provenance/labels/backup/restore,and Prompt commit interruption idempotence across reopen. Internal cases not separate tests. No new failed attempt in this increment; earlier bounded-read allocation measurement failures and lock RED/q09 preserved separately,not relabeled.\n\nBefore/after separate JVMs,actual full original verification and memory copy costs; no causal wall-clock/CPU/RSS/complete UI or real-library claim. Counters exclude JSON from the outset. Existing low-priority single-worker runner,2processors/256MiB test heap. Owner checks before launches found no Desktop Java;not continuous quiet proof. No process/permission/security changes,real files/galleries/credentials/service,user directory scan,new package/release/daily install or Project send. Successful own fixtures retired after logs,failed/old evidence/unknown/user content retained. Asset/editor user acceptance NOT TESTED,115/Prompt pending,q09 unknown and PRODUCT_HOLD / NOT RC stay.\n",encoding='utf-8')
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
