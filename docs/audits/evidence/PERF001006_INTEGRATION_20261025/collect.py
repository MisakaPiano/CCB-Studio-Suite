from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001006_INTEGRATION_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
srcs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf001006-copy-read-integration-20261025'));assert len(srcs)==1;src=srcs[0]
summary=json.loads((src/'summary.json').read_text());source='1b6e3b62838e69f4e811d57066c36a26d209f308';app='6dfadc02f544ce5394cf4e628f410ca695939a3d'
assert summary['baseCommit']==source and summary['sourceStable'] and summary['status']=='PASS' and (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==(3,0,0,0)
for p in src.iterdir():
    if p.is_file():
        target=DEST/'run'/p.name;target.parent.mkdir(exist_ok=True);shutil.copyfile(p,target)
assert subprocess.check_output(['git','diff',app,source,'--','modules','tools'],cwd=ROOT)==b''
metrics={}
for cls,label in [('CollectionHashPerformanceTest','COLLECTION_HASH_ALLOCATION'),('IngressCopyPerformanceTest','INGRESS_COPY_ALLOCATION')]:
    output=ET.parse(next(src.glob('*'+cls+'.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith(label+'=')).split('=',1)[1]);write(DEST/(cls+'-samples.json'),rows)
    metrics[cls]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
identities=[];before=json.loads((src/'source-before.json').read_text())
paths=['LibraryRepository.kt','StateJournal.kt','StudioWorkspace.kt','AssetBytesBuffer.kt','OriginalFileCopy.kt']
for name in paths:
    path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/'+name;fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT);data=(ROOT/path).read_bytes()
    assert sha(ROOT/path)==before[path] and (fixed==data or fixed==data.replace(b'\r\n',b'\n'));identities.append(dict(path=path,testInputSha256=before[path],gitSha256=hashlib.sha256(fixed).hexdigest()))
clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE'
write(DEST/'SOURCE_MAP.json',dict(executionSource=source,applicationSource=app,applicationInputsEqual=True,identities=identities,finalUniqueMethods=3,noNewProductionChange=True,oldRunsNotAdded=True,release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(currentMedians=metrics,finalUniqueMethods=3,cleanup=clean,historicalReferences={'scan':'cb819b4 / docs/audits/evidence/PERF006_COLLECTION_HASH_20261025','intake':'3086895 / docs/audits/evidence/PERF001005_INGRESS_20261025'},limits=['Integrated observation after actual low-level code changes,not rerunning an unchanged or accepted003/startup baseline','Historical separate JVMs and scheduling are references only,no causal wall/CPU/RSS/UI or real-library claim','Current scan2 synthetic3/12MiB ancillary files,current managed-copy one per call;auto collections paused and no user paths','Prior perfsample counters include small JSON-record overhead unlike newer isolated-counter fixtures;no precision cross-fixture comparison','Source/state/asset count/replay/cancellation checkpoints/liveness asserted;no new archive/render/clipboard/update/package acceptance']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Integration after actual IO/byte/journal production changes

One final invocation at1b6e3b6,application inputs exactly6dfadc0 (only prior report commit differs).3 actual unique methods PASS,0failure/error/skip,source stable. Existing classes reused to observe the newly changed low-level production combination:explicit syntheticUNCHANGED scans,managed intake copy/replay,progress callback can read workspace on separate thread and cancellation checkpoints only completed files. No tests/source/production changed in this integration increment. Internal source files/rounds/cancel points and prior counts not added. No repeated acceptedPERF003 or native/startup session,old failures remain fixed.

Scan current2x12MiB input thread allocation26,558,272B versus historicalcb819b4 sample52,008,544B;differentJVMs and software combination,reference only. Managed copy12MiB6,375,312B versus historical3086895 sample6,389,648B:roughly unchanged allocation,do not claim a further material intake memory gain. All wall/CPU/slow rounds retained,no causal speed/RSS/GUI or user-volume assertion. Current input state/source hashes/count/replay and progress-liveness/cancel assertions pass. Auto collections paused in performance fixtures,othercase only new owned synthetic source;no watcher/user scans.

Desktop busy deferred start,one low-priority single-worker2processor256MiB JVM after launch owner gate empty;not continuous quiet-host evidence. No process/security/Desktop write,private credentials/real service,new package/release/daily install/Project payload. Successful own fixture341files204,604,712B retired after logs,old/failure/baseline/backup/unknown content preserved. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN,PRODUCT_HOLD / NOT RC unchanged. New evidence only prepared for review,not sent.
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
print(json.dumps(dict(executionSource=source,applicationSource=app,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics),indent=2))
