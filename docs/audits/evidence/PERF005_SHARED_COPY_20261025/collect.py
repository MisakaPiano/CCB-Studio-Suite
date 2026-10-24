from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF005_SHARED_COPY_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def copy(s,t):t.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(s,t)
runs={
 'typeset-baseline':'20261024T215916470257Z-perf005-typeset-original-baseline-20261025',
 'shared-first':'20261024T220421882721Z-perf005-shared-original-final-20261025',
 'lock-red':'20261024T221148040176Z-perf005-copy-lock-red-20261025',
 'final':'20261024T221256135199Z-perf005-shared-original-fixed-20261025'}
counts={};metrics={};cleanups=[]
for name,folder in runs.items():
    src=ROOT/'.local/windows-preview/evidence'/folder
    for p in src.iterdir():
        if p.is_file():copy(p,DEST/'runs'/name/p.name)
    summary=json.loads((src/'summary.json').read_text());counts[name]={k:summary[k] for k in ('status','tests','failures','errors','skipped','sourceStable','baseCommit')}
    assert summary['sourceStable']
    assert summary['status']==('FAILED' if name=='lock-red' else 'PASS')
    assert summary['tests']=={'typeset-baseline':1,'shared-first':11,'lock-red':1,'final':12}[name]
    cleanup=json.loads((src/'fixture-cleanup.json').read_text());cleanups.append(cleanup)
    assert cleanup['status']==('RETAINED_FAILED_FIXTURES' if name=='lock-red' else 'COMPLETE')
    metrics[name]={}
    for xml in src.glob('*.xml'):
        output=ET.parse(xml).getroot().findtext('system-out') or ''
        for marker,label in [('TYPESET_ORIGINAL_ALLOCATION=','typeset'),('ORIGINAL_EXPORT_PAIRED=','gallery')]:
            if marker not in output:continue
            rows=json.loads(output.split(marker,1)[1].strip());write(DEST/(name+'-'+label+'-samples.json'),rows)
            metrics[name][label]={}
            for size in sorted({r['payloadBytes'] for r in rows}):
                metrics[name][label][size]={}
                for kind in ('legacy','streamed') if label=='gallery' else ('observed',):
                    selected=[r for r in rows if r['payloadBytes']==size and (label!='gallery' or r['kind']==kind)]
                    metrics[name][label][size][kind]={key:statistics.median(r[key] for r in selected) for key in ('wallMs','cpuMs','allocatedBytes')}
source='5bfa51df9fdc632902cd329212d0b2fdaf9ba379'
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetExport.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TypesetOriginalPerformanceTest.kt','modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/LibraryDelivery.kt','modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/OriginalExportPerformanceTest.kt']
before=json.loads((DEST/'runs/final/source-before.json').read_text());identities=[]
for path in paths:
    data=(ROOT/path).read_bytes();fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT)
    assert sha(ROOT/path)==before[path]
    assert data==fixed or data.replace(b'\r\n',b'\n')==fixed
    identities.append(dict(path=path,diskSha256=sha(ROOT/path),gitSha256=hashlib.sha256(fixed).hexdigest(),textRepresentation='exact' if data==fixed else 'CRLF checkout / LF Git'))
assert subprocess.check_output(['git','diff','accdc59',source,'--','modules/asset-library','modules/image-authority','tools/update-runtime','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioRestore.kt'],cwd=ROOT)==b''
write(DEST/'ANALYSIS.json',dict(counts=counts,medians=metrics,finalUniqueMethods=12,repeatedPriorMethodsNotAccumulated=True,retainedFailureRoot=cleanups[2]['root'],cleanupFiles=sum(c.get('removed',0) for c in cleanups if c['status']=='COMPLETE'),cleanupBytes=sum(c.get('bytesFreed',0) for c in cleanups if c['status']=='COMPLETE'),limits=['Typeset before/after are distinct executed JVMs, not a same-JVM latency improvement proof','Gallery measurements alternate actual old and current pipeline within final JVM; no end-to-end UI/RSS claim','Blocking-read regression stalls a synthetic writer checkpoint, not real slow hardware; TypesetExport existing outer serialization stays unchanged','A Desktop Java PID53124/parent11548 was observed at a later boundary, creation time not retained and measurement overlap UNKNOWN; keep all timing/outliers and do not claim a quiet host','No true credentials/private media/paused directory/native clipboard/package or independent user acceptance']))
write(DEST/'SOURCE_MAP.json',dict(source=source,typesetBaselineSource='d8eb0cb5570c2265f7e81230bb1cec534acd019f',sharedFirst='72b1c2987583efa2797813b9dad8bd02e8793c30',lockRedSource='3d935c349c09a114f6a57c0753b7f5779fdfe955',identities=identities,finalUniqueMethods=12,redFailures=1,earlierSuccessfulMethodsNotAddedAgain=True,formatSchemaAndStoreAdmissionUnchanged=True,packageBuilt=False,independentReview='NOT REQUESTED; new payload authorization required',assetAndEditorHuman='USER_NOT_TESTED',q09='FAILED / cause UNKNOWN unchanged',promptAnd115='PENDING'))
copy(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Shared-copy attempt ledger

d8eb0cb: actual typeset original export baseline, 1 method PASS, 3/12MiB synthetic PNG ancillary payload. One warmup plus 3 measured rounds, full output/recipe/provenance/document/original verification.
72b1c29: shared bounded copying introduced, 11 unique methods PASS. Original signatures use at most12 bytes; actual file stays byte-identical. Other rendering/font/privacy code untouched. Existing original filename injected during staging is never replaced (ordinary non-replacing move inside private stage; original atomic batch publication unchanged).
3d935c3: new liveness regression actually FAILS, 1 method/1 assertion failure, no errors/skips. Holding a synthetic writer checkpoint while the helper owns workspace.operation blocks independent workspace.state read. Release/join is in finally, no force kill or leaked owned thread. FAILED fixture retained, removed0, not relabeled PASS. This is a helper lock-range defect, not inferred OS/security interference.
5bfa51d: brief workspace admission obtains store reference, then AssetStore.export keeps its own serialized operation/drain registration; output IO no longer adds a workspace-monitor hold in this helper. TypesetExport's pre-existing outer workspace.operation is unchanged. Final12 unique methods PASS, failures/errors/skips0; no accumulation of previous1/11/5 etc. Headless JVM, not natural GUI latency or general concurrent-write/close certification.

Final scope:5 typeset/performance/helper methods,2 existing typeset export/font/variant regressions,5 gallery original-export regressions. Scenarios/rounds are not separate methods. Full completion/recipe/source receipts, corrupt source, collision, prefix/tail buffer boundaries, cancellation and original preservation remain. Store fingerprinting/export checks not removed, no format/version migration. Valid16x16 PNG ancillary payloads for allocation tests; separate opaque signature fixtures test format-dispatch equivalence, not valid raster-decoder acceptance.

All runs use existing OWNER roots; successful payloads sealed/retired after raw logs retained, failed root preserved. Single worker/256MiB/2 processors. Desktop ownership checked before starts and selected boundaries; later PID53124 was observed but its creation time was not retained, so overlap UNKNOWN. Do not filter slower samples or claim uncontended/OS-cold benchmarking. Thread allocations are not RSS. Typeset timing before/after separate JVM; final gallery old/new paired. Original lookup/setup failures and q09 remain in earlier fixed ledgers, not hidden.

No new Project submission, ZIP TOOL_ACCESS_LIMIT retry, release/package/daily install, private data/credentials, paid calls, suspended scan, safety setting or Desktop modification. User/independent/package acceptance pending. Continuous performance authorization remains active; checkpoint is not full PERF completion or a permission pause.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert hashlib.sha256(z.read(item['path'])).hexdigest()==item['sha256']
print(json.dumps(dict(source=source,archiveBytes=archive.stat().st_size,archiveSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,finalMedians=metrics['final']),indent=2))
