from pathlib import Path
import hashlib,json,re,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF005_EXPORT_20261025'
assert not DEST.exists(),'Never replace fixed evidence'
DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def copy(s,t):t.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(s,t)
runs={
 'baseline':'20261024T214330661076Z-perf005-export-baseline-20261025',
 'unbuffered':'20261024T214623011405Z-perf005-export-paired-20261025',
 'final':'20261024T214833673074Z-perf005-export-buffered-20261025'}
metrics={};counts={};cleanups=[]
for name,folder in runs.items():
    src=ROOT/'.local/windows-preview/evidence'/folder
    for p in src.iterdir():
        if p.is_file():copy(p,DEST/'runs'/name/p.name)
    summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable']
    assert summary['tests']==(1 if name=='baseline' else 5)
    counts[name]={k:summary[k] for k in ('tests','failures','errors','skipped','baseCommit','sourceStable')}
    cleanup=json.loads((src/'fixture-cleanup.json').read_text());assert cleanup['status']=='COMPLETE';cleanups.append(cleanup)
    xml=ET.parse(next(src.glob('*OriginalExportPerformanceTest.xml'))).getroot();output=xml.findtext('system-out')
    marker='ORIGINAL_EXPORT_BASELINE=' if name=='baseline' else 'ORIGINAL_EXPORT_PAIRED='
    rows=json.loads(output.split(marker,1)[1].strip());write(DEST/(name+'-samples.json'),rows)
    metrics[name]={}
    for size in sorted({r['payloadBytes'] for r in rows}):
        metrics[name][size]={}
        for kind in ('legacy',) if name=='baseline' else ('legacy','streamed'):
            selected=[r for r in rows if r['payloadBytes']==size and r.get('kind','legacy')==kind]
            metrics[name][size][kind]={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}
source='accdc59e851164ee82870297a0bfc0e626ec55c3'
paths=['modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/LibraryDelivery.kt','modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/OriginalExportPerformanceTest.kt']
before=json.loads((DEST/'runs/final/source-before.json').read_text());identities=[]
for path in paths:
    data=(ROOT/path).read_bytes();fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT)
    assert sha(ROOT/path)==before[path]
    assert data==fixed or data.replace(b'\r\n',b'\n')==fixed
    identities.append(dict(path=path,diskSha256=sha(ROOT/path),gitSha256=hashlib.sha256(fixed).hexdigest(),gitBlob=subprocess.check_output(['git','rev-parse',source+':'+path],cwd=ROOT,text=True).strip(),textRepresentation='exact' if data==fixed else 'CRLF checkout / LF Git'))
old=subprocess.check_output(['git','show','f5b008e:'+paths[0]],cwd=ROOT).decode().replace('\r\n','\n')
test=(ROOT/paths[1]).read_text(encoding='utf-8');oldbody=old.split('):Path {',1)[1].split('\n}\n\ninternal fun startLibraryExport',1)[0]
legacy=test.split('):Path {',1)[1].split('\n}\n\nclass OriginalExportPerformanceTest',1)[0]
assert oldbody.replace('c.workspace','workspace')==legacy
assert subprocess.check_output(['git','diff','f5b008e',source,'--','modules/studio-core','modules/asset-library','modules/image-authority','tools/update-runtime'],cwd=ROOT)==b''
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=5,baselineMethodsSeparately=1,unbufferedMethodsNotAddedAgain=True,cleanupFiles=sum(c['removed'] for c in cleanups),cleanupBytes=sum(c['bytesFreed'] for c in cleanups),limits=['Current measured thread allocations, not retained heap/RSS; CPU is coarse process-host granularity','Small synthetic PNGs with 3/12MiB opaque ancillary payloads; not user images or actual UI workflow','Final latency is not claimed faster; all slower/outlier rounds retained','Desktop Java ownership checked at launch/selected boundaries, not continuously quiet OS; no other project interruption','No packaged/native/user/backup/clipboard or independent acceptance in this segment']))
write(DEST/'SOURCE_MAP.json',dict(source=source,baselineSource='829c2397c47fc5ab1145949669907b1a64aca7f0',firstStreamedSource='4339ed189cc9dbb42fb4982657bf7c2d55fcd89a',fixedLegacyOrigin='f5b008ecf55ed2f6c9248a3fd3412baca6528d54',legacyBodyExactExceptWorkspaceReference=True,identities=identities,compiledInputsRecorded='source-before maps plus sourceStable actual runner result for each run; baseline compiled class bytes were not separately archived',finalUniqueMethods=5,baselineMethodsSeparately=1,intermediateRepeatedMethodsNotAccumulated=True,packageBuilt=False,independentReview='NOT REQUESTED; prepare only, new payload requires specific authorization',userAcceptance='NOT TESTED',oldFailuresAndPendingUnchanged=True))
copy(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Execution ledger

829c239 baseline: 1 executed method PASS; frozen old production export body exactly preserved in test apart from workspace parameter reference. 3/12MiB valid 16x16 PNGs with a synthetic private ancillary chunk, 64KiB generation buffer. No user media or new service. One warmup plus two measured baseline rounds per size.
4339ed1 unbuffered stream: 5 executed methods PASS. Allocation dropped strongly but wall time not improved; record retained. Fixed 256KiB coalescing selected from this actual IO cost, not deleting slower results.
accdc59 final buffered stream: same 5 unique methods PASS, zero failure/error/skip; alternating old/new order over 4 measured pairs per size after one warmup each. Do not add baseline or intermediate/repeated method execution into 11 or count pairs/assertions separately. Baseline 1 and final 5 are listed separately.

Methods cover byte-identical receipts/names/dedup/order, originals/sentinel protection, corrupt later original retaining unpublished partial stage, deterministic cancellation after output opens while waiting for workspace admission, cancellation/unsafe workspace destination, and bounded allocation. Simulated corruption/expected cancellation are assertions inside passing tests, not unexpected failed runs. No GUI/manual, native clipboard, package or full backup acceptance.

All 3 runs used existing OWNER isolation and retained raw JUnit/log/input maps before cleanup. Successful fixtures only retired; old evidence/failure fixtures/day-to-day installation untouched. Single worker/256MiB/2 active processors, Desktop Java launcher check before starting and at selected observations; not continuous whole-machine silence. No Desktop process/directory changed or message sent. JVM timing/CPU and write/force fluctuations do not establish end-to-end UI speedup. Thread allocation difference is the demonstrated result, not RSS savings.

Read-only source setup had one nonexistent WorkbenchController.kt lookup and Windows rg wildcard-path errors; corrected by directory/-g searches. They did not start tests or alter product/safety settings and are not attributed to security software. No unexpected compiled/test failure occurred in this segment. No new ZIP-access retry, old accepted baseline rerun, paid API/private data/suspended scan, product permission or release.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items))
archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert hashlib.sha256(z.read(item['path'])).hexdigest()==item['sha256']
print(json.dumps(dict(source=source,archiveBytes=archive.stat().st_size,archiveSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,analysis=metrics['final']),indent=2))
