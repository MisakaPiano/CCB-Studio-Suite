from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001007_JOURNAL_COMMIT_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'6966bc0ed80b312bea7a93efb76646fd590a645b','final':'6dfadc02f544ce5394cf4e628f410ca695939a3d'}
counts={};metrics={};cleanup=[];inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf001007-journal-commit-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['baseCommit']==sources[kind] and summary['sourceStable'] and summary['status']=='PASS'
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((2 if kind=='baseline' else 12),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*JournalCommitPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(line for line in output.splitlines() if line.startswith('JOURNAL_COMMIT_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
    if kind=='final':
        unicode=json.loads(next(line for line in output.splitlines() if line.startswith('JOURNAL_UNICODE_CASES=')).split('=',1)[1]);assert len(unicode)==6
        assert all(c['encodedBodyContainsSurrogate'] for c in unicode[3:]);assert all(not c['persistedEqualsInMemoryState'] for c in unicode[4:]);write(DEST/'UNICODE_CASES.json',unicode)
path='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StateJournal.kt'
old=subprocess.check_output(['git','show',sources['baseline']+':'+path],cwd=ROOT).decode();new=subprocess.check_output(['git','show',sources['final']+':'+path],cwd=ROOT).decode()
assert old[:old.index('    private fun encode(')]==new[:new.index('    private data class EncodedState(')]
assert old[old.index('fun safePath('):]==new[new.index('fun safePath('):]
oldEncoding=old[old.index('        next.validate()',old.index('private fun encode(')):old.index('        return bytes',old.index('private fun encode('))]
newEncoding=new[new.index('        next.validate()',new.index('private fun encode(')):new.index('        // UTF-8 may replace',new.index('private fun encode('))]
assert oldEncoding==newEncoding
assert 'fun requireCapacity(next:StudioState) {encode(next)}' in old and 'fun requireCapacity(next:StudioState) {encode(next)}' in new
identities=[]
paths=[path,'modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/JournalCommitPerformanceTest.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt','tools/Run-PreviewChecks.py']
for kind in ('baseline','final'):
    for name in paths:
        data=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (hashlib.sha256(data).hexdigest(),hashlib.sha256(data.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=hashlib.sha256(data).hexdigest()))
unchanged=['modules/asset-library','modules/image-authority','modules/studio-desktop','tools','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt']
assert subprocess.check_output(['git','diff',sources['baseline'],sources['final'],'--',*unchanged],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(sources=sources,identities=identities,unchangedPaths=unchanged,encodeValidationAndBytesExpressionIdentical=True,fullOpenAuditAndReaderWriterIdentical=True,capacityPreflightAndActualCommitBothEncode=True,finalUniqueMethods=12,baselineRepeatedMethodsNotAdded=True,release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=12,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Two independent JVMs; prior test/warmup/JSON pooling/JIT/GC/filesystem scheduling not a causal latency or RSS comparison','ASCII synthetic job-detail64/256KiB inputs,one warmup+4 actual workspace.save calls per size,counters before JSON/digest/output','Any surrogate-bearing encoded body uses original envelope decode; emoji/unpaired high/low branches exercised; no uniform performance improvement claim','Historical unpaired surrogate normalization leaves persisted state different from live state; snapshot mismatch guard retained,not fixed/hidden','Full capacity preflight/actual encode,validation,stored envelope,chain/hash/fault/poison behavior retained','Controlled own process halt73 probes test crash boundaries; no unrelated process killed','Desktop launch owner checks,not continuous quiet host proof; no GUI/package/user/independent acceptance']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Journal post-commit body parsing increment

Baseline6966bc0:2 actual methods PASS,workspace.save64/256KiB synthetic job-detail payload,1warmup+4 measured saves per size,and6 Unicode/legacy-snapshot cases. Final6dfadc0:12 unique PASS,0fail/error/skip,source stable. Baseline repeated methods/internal6Unicode/2crash points not added. No new failed test or blind rerun; old bounded-reader measurement failures/lock RED/q09/user rejection remain fixed separately.

Only encode's private runtime result and commit's extraction of body change. Preflight and actual commit each still independently validate/encode; no cross-call prepared-state cache (could change mutable-input/preflight-poison semantics),no capacity/integrity check removed. Same next.validate/body/envelope bytes/hash/4MiB limit expression character-exact; entire open chain audit and reader/writer suffix character-exact. Write/staged fault/atomic move/previous hash/creation revision/state/version body/committed fault order unchanged. Encoded reusableBody is set only when serialized body has no UTF-16 surrogate; otherwise use exact old decode-from-encoded-envelope path. Runtime class is not Serializable/new on-disk format.

Final3 new methods:actual save allocation/persisted body/hash/state/reopen;ASCII/Chinese/escape/emoji/lone high/lone low actual UTF-8 disk body versus live committedBody and snapshot guards;version2 exact source body retained through version-only3 commit/reopen then correctly rejected after substantive change.9 existing methods cover over-limit preflight remains readable/writable,unknown/staged/corrupt/future/deleted chain,both staged/committed own subprocess halt73 boundaries,3restore cases,explicit2to3 migration with opaque encrypted blobs/backup,and interrupted design commit cannot promote orphan verified provenance. These are synthetic engine/storage probes,no true account/NAI/LLM/installer/GUI/user operations.

All3 surrogate-bearing cases exercised old parse path. Both lone surrogate cases keep historical persisted-vs-live state difference and UPDATE_SNAPSHOT_MISMATCH behavior;not a new Unicode repair or support guarantee. Fast path metrics cover ASCII detail only. Counter sampled aftersave and before any JSON/digest assertions;separate JVM cohorts/previous tests/pooling/JIT/GC/file force scheduling not controlled. Thread allocation result,not RSS/physicalIO/general UI or universal speed acceptance. Single-worker low-priority2processor256MiB runner,Desktop ownership checked before starts but not continuous quiet lease. No process/security change,real private data,credentials/paid service,resumed user scans,new package/release/daily install or Project send. Successful own fixtures retired after logs;failed roots/old evidence/backups/unknown content untouched. Asset/editor USER_NOT_TESTED,115/Prompt pending,q09 unknown and PRODUCT_HOLD / NOT RC remain.
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
print(json.dumps(dict(sources=sources,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup)),indent=2))
