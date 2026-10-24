from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001005_INGRESS_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def copy(s,t):t.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(s,t)
runs={'baseline':'20261024T222102689096Z-perf001005-ingress-copy-baseline-20261025','final':'20261024T222652610446Z-perf001005-ingress-copy-final-20261025'}
counts={};metrics={};cleanup=[]
for kind,folder in runs.items():
    src=ROOT/'.local/windows-preview/evidence'/folder
    for p in src.iterdir():
        if p.is_file():copy(p,DEST/'runs'/kind/p.name)
    summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable']
    assert summary['tests']==(1 if kind=='baseline' else 4)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    output=ET.parse(next(src.glob('*IngressCopyPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split('INGRESS_COPY_ALLOCATION=',1)[1].strip());write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
source='3086895c49d4f6f41231a187aee110abe0782013';before=json.loads((DEST/'runs/final/source-before.json').read_text());identities=[]
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryRepository.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/IngressCopyPerformanceTest.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/FileIntegrity.kt']
for path in paths:
    data=(ROOT/path).read_bytes();fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT)
    assert sha(ROOT/path)==before[path] and (data==fixed or data.replace(b'\r\n',b'\n')==fixed)
    identities.append(dict(path=path,diskSha256=sha(ROOT/path),gitSha256=hashlib.sha256(fixed).hexdigest(),textRepresentation='exact' if data==fixed else 'CRLF checkout / LF Git'))
assert subprocess.check_output(['git','diff','5bfa51d',source,'--','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/FileIntegrity.kt','modules/asset-library','modules/image-authority','tools/update-runtime'],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(source=source,baselineSource='a6a366ee9349796238ac73cbb2c3cb54e9614748',sharedHelperUnchangedFrom='5bfa51df9fdc632902cd329212d0b2fdaf9ba379',identities=identities,finalUniqueMethods=4,baselineRepeatedMethodNotAddedAgain=True,oldStoreAndFileLimitsUnchanged=True,independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=4,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Separate JVMs, not paired latency improvement proof; thread allocations not RSS','Metadata/history cost still grows across rounds and remains; new-vs-size growth bound2MiB catches original-sized buffers, not total fixed budget','Performance fixtures pause collections and use owned folders only; existing pure batch test starts no scan','No GUI/user folder/private data/native clipboard/real service/update or independent acceptance']))
copy(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Intake copy execution ledger

a6a366e baseline:1 actual method PASS,3/12MiB synthetic PNG ancillary payloads,one warmup+3 new IDs/rounds per size; each copy retains full expected observations/source and replay checks. No scan started.
3086895 final:4 unique methods PASS,0 failure/error/skip. Same performance method now asserts source-size growth does not add whole-file arrays; modified secondary bytes stay COPY_PENDING and are not replaced;32MiB+1 opaque asset still rejected with CORRUPT after retaining secondary/original; existing24-item partial-cancel/reopen test keeps two metadata checkpoints and resumes safely. Internal24 items/rounds and baseline repeated method are not added as more methods.

Only retainIngressBatch copy/verification implementation changed:existing-target check remains full SHA, new copy delegates to already tested shared helper, final destination streaming fingerprint remains size+SHA with explicit32MiB cap. COPY_PENDING/PRESENT/finally/progress/return/order are unchanged. FingerprintFile and shared helper unchanged, earlier12-method evidence not rerun/counted anew. No unexpected failed test in this segment; prior shared-helper RED and q09 remain in fixed earlier evidence.

Current thread allocation is the result, not RSS or complete import UI throughput. Before/after separate JVMs, real write/force/history costs and system scheduling vary; retain slower timing/CPU rounds, no speedup claim. No continuous Desktop lease/quiet host proof, only launch/boundary owner checks. Single worker/256MiB/2 processors, Desktop not interrupted. Successful own fixtures retired after logs; user data/old evidence/failure roots untouched. No real credentials/service, resumed user scanning, new release/package/daily install, external Project payload or Desktop write.
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
print(json.dumps(dict(source=source,bytes=archive.stat().st_size,sha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics),indent=2))
