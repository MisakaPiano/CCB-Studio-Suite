from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001005_BOUNDED_READ_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
runs={'baseline':'perf001005-bounded-read-baseline-20261025','red':'perf001005-bounded-read-final-20261025','diagnostic':'perf001005-bounded-read-allocation-diagnostic-20261025','measurementFix':'perf001005-bounded-read-isolated-measurement-20261025','final':'perf001005-bounded-read-fixed-final-20261025'}
expected={'baseline':(1,0,'PASS'),'red':(15,1,'FAILED'),'diagnostic':(1,1,'FAILED'),'measurementFix':(1,0,'PASS'),'final':(17,0,'PASS')}
source='27e562186b0980de5fa5cbb8e975d9153a1ba13d';counts={};cleanup=[];inputs={};failures=[]
for kind,phase in runs.items():
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-'+phase));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'];assert(summary['tests'],summary['failures'],summary['status'])==expected[kind];assert not summary['errors'] and not summary['skipped']
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    if kind=='final':assert summary['baseCommit']==source
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    clean=json.loads((src/'fixture-cleanup.json').read_text())
    if kind in ('red','diagnostic'):assert clean['status']!='COMPLETE' and clean.get('removed',0)==0
    else:assert clean['status']=='COMPLETE'
    cleanup.append(dict(run=kind,**clean))
    for f in src.glob('*TEST-*.xml'):
        suite=ET.parse(f).getroot()
        for case in suite.findall('testcase'):
            failure=case.find('failure')
            if failure is not None:failures.append(dict(run=kind,suite=suite.get('name'),method=case.get('name'),message=failure.get('message')))
write(DEST/'FAILURES_RETAINED.json',failures)
metrics={}
for kind in ('measurementFix','final'):
    output=ET.parse(next((DEST/'runs'/kind).glob('*BoundedReadPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split('BOUNDED_READ_ALLOCATION=',1)[1].strip());write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]=[]
    for size in sorted({r['payloadBytes'] for r in rows}):
        for mode in ('legacy','current'):
            selected=[r for r in rows if r['payloadBytes']==size and r['kind']==mode]
            metrics[kind].append(dict(payloadBytes=size,kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
old=subprocess.check_output(['git','show','8dc3133:modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StateJournal.kt'],cwd=ROOT).decode()
new=subprocess.check_output(['git','show',source+':modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StateJournal.kt'],cwd=ROOT).decode()
assert old[:old.index('fun readBounded(')]==new[:new.index('fun readBounded(')]
assert old[old.index('fun writeNewForced('):]==new[new.index('fun writeNewForced('):]
test=subprocess.check_output(['git','show',source+':modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/BoundedReadPerformanceTest.kt'],cwd=ROOT).decode()
start=test.index('private fun legacyBoundedRead(');legacy=test[start:test.index('class BoundedReadPerformanceTest {',start)].rstrip()+'\n'
assert legacy.replace('private fun legacyBoundedRead(','fun readBounded(',1)==old[old.index('fun readBounded('):old.index('fun writeNewForced(')].rstrip()+'\n'
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StateJournal.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/BoundedReadPerformanceTest.kt','modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/ExportClipboardBoundedReadTest.kt','tools/Run-PreviewChecks.py']
identities=[]
for path in paths:
    fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT);data=(ROOT/path).read_bytes()
    assert sha(ROOT/path)==inputs['final'][path] and (data==fixed or data.replace(b'\r\n',b'\n')==fixed)
    identities.append(dict(path=path,testInputSha256=sha(ROOT/path),gitSha256=hashlib.sha256(fixed).hexdigest()))
main=paths[0];assert len({inputs[k][main] for k in ('red','diagnostic','measurementFix')})==1
write(DEST/'SOURCE_MAP.json',dict(source=source,productionSource=source,earlierProductionCandidate='ad2b9c0a6b31c2a405221538012c525077604d89',baseline='8dc3133f7538cc4422a02fe4f9b66406b8c743df',identities=identities,productionSameAcrossRedAndMeasurementFix=True,finalAdds64KiBReadRequestBound=True,legacyFixedBodyMatches=True,journalAndWriterOutsideReaderUnchanged=True,finalUniqueMethods=17,earlierExecutionsNotAdded=True,release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=17,failuresRetained=failures,cleanup=cleanup,limits=['Earlier actual baseline includes JSON recording in allocation counter; not used for causal before/after numbers','Final same-JVM alternating4 legacy/current rounds samples counter before JSON assembly; same256KiB overhead threshold retained','Correcting the test alone passes without production changes; final separately caps64KiB native read requests; finer JSON initialization/recording allocation attribution UNKNOWN,not blamed on Desktop/security','Stale size/growing/shrinking scenarios use deterministic synthetic InputStreams,not certification of all filesystem races','NOFOLLOW/safePath and error mapping retained; no new privileged symlink/junction creation','CPU timer may return0 for sub-tick reads; not zero CPU or system-wide savings','Different runs/Desktop overlap not quiet-host or RSS/UI/package acceptance']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Bounded read execution and failure ledger

8dc3133 actual old reader baseline:1 PASS,first harness sampled allocations inside JSON construction; retain raw but use final exact legacy/current paired comparison for conclusions. ad2b9c0 production/a07edb4 input:15 executed,1 allocation assertion FAILED,14 PASS; Desktop task never executed,not skipped/pass.69e15e7 diagnostic:1 FAILED,exact3MiB sample4,081,288B. Both failed owned roots retained,not deleted or relabeled.

The measurement included result-record JSON construction.1815e9e logs exact row,9f71304 moves sampling before any JSON without relaxing the one-input-size+256KiB threshold;4395656 supplies legacy constant import before execution. Production reader unchanged across both failures and corrected single-method probe. Corrected probe1 PASS with same-JVM legacy body copied character-exact from8dc3133 except function signature name/private. Finer cold class/JSON allocation attribution not independently profiled. Final27e5621 separately caps each initial read request64KiB so the FileChannel adapter need not allocate/cache an original-sized native temporary buffer; this local cap does not configure the JVM/global native cache or prove RSS savings. Do not blame Desktop,security software,or call this a removed product data-loss bug.

Final27e5621:17 unique PASS,0failure/error/skip,source stable.5 reader methods cover actual paired allocation/bytes,8 real file boundaries/exact/over-budget/missing/directory/error and old invalid budget behavior,short partial reads with stale hints/both growth and shrink versus legacy EOF bytes and max+1 actual consumption,IO faults in initial/growth reads returning no partial result,and64KiB maximum bulk requests with a zero-result read preserving bytes. Deterministic InputStreams model stale size hints; not actual all-OS race certification.4 StudioCore methods cover original/project/reopen,unknown/staged/corrupt/future documents,deleted chain entries,and synthetic DPAPI secrets excluded from documents/backups.3 restore+2 large-backup methods cover complete/corrupt/incomplete/unknown/interrupted recovery and33MiB opaque archive.1 synthetic collection unchanged scan,1 Prompt interruption idempotence,1 pure Transferable method for static/APNG/corrupt/multiple flavors and original bytes. No OS clipboard or user/window/service interaction. Internal cases/rounds/earlier15+1+1/baseline not added as more methods.

File size comes from same opened NOFOLLOW FileChannel and is allocation hint only. Dynamic EOF/max+1 sentinel remains; growth fallback bounded,shrink returns actual bytes,IO throws and existing public error mapping stays. No digest/format/journal/credential/backup/update validation removed; state envelope/commit/writer outside reader byte-identical. Stable case allocates one source-sized return buffer; no global retained cache. Native file-channel temporary buffers/RSS not measured as thread heap. Existing source store/request/trust configuration untouched,no new package/release/daily install.

Desktop priority:boundary launch owner checks,one worker/low priority/2processors/256MiB heap; not continuous quiet proof. During first RED runner Desktop PID312 creation23:07:28Z observed23:08:04Z; precise sample/activity overlap unknown. Later diagnostics/final checks remain separate. Never kill/clean Desktop/security or retry MSI changes. Successful own fixtures retired after logs;2 failed roots retained with evidence; earlier lock RED/q09/user rejections unchanged. No user directory/real private data/credentials/paid service/new Project send. Original assets/editor USER_NOT_TESTED,115/Prompt PENDING and PRODUCT_HOLD / NOT RC unchanged.
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
print(json.dumps(dict(source=source,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanup=cleanup),indent=2))
