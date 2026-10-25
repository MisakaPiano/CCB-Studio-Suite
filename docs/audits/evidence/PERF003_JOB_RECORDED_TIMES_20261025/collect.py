from pathlib import Path
import hashlib,json,re,shutil,statistics,subprocess,sys,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf003-job-times-20261025'
DEST=ROOT/'docs/audits/evidence/PERF003_JOB_RECORDED_TIMES_20261025'
assert len(sys.argv)==2 and re.fullmatch('[a-f0-9]{40}',sys.argv[1])
SOURCES={'baseline':'3d7369963ec942fed58b1a1963d2c5d9ccc397e4','final':sys.argv[1]}
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
runs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-job-times-'+kind+'-20261025'));assert len(candidates)==1
    s=json.loads((candidates[0]/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']=='PASS'
    assert (s['tests'],s['failures'],s['errors'],s['skipped'])==((1 if kind=='baseline' else 3),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    runs[kind]=(candidates[0],s)
assert not DEST.exists();DEST.mkdir(parents=True)
counts={};metrics={};inputs={};cleanup=[]
for kind,(src,s) in runs.items():
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    out=ET.parse(next(src.glob('*RecordedJobTimesPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in out.splitlines() if x.startswith('RECORDED_JOB_TIMES=')).split('=',1)[1])
    assert all(r['serializedStateBytes']<=4*1024*1024 for r in rows);assert len(rows)==(12 if kind=='baseline' else 24)
    write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for jobs in (128,512,2048):
        group=[r for r in rows if r['jobs']==jobs];assert len({r['dates'] for r in group})==1
        for mode in sorted({r.get('kind','actualLegacy') for r in group}):
            selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
            metrics[kind].append(dict(jobs=jobs,resultReferences=selected[0]['resultReferences'],dates=selected[0]['dates'],serializedStateBytes=selected[0]['serializedStateBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryCatalogProjection.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/RecordedJobTimesPerformanceTest.kt'
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={production,test}
old=git('show',SOURCES['baseline']+':'+production).decode();new=git('show',SOURCES['final']+':'+production).decode()
before='    return imported+jobs.flatMap{job->(job.results+job.composedResults).map{it to job.createdAt}}.toMap()'
after='    val recorded=imported.toMutableMap()\n    jobs.forEach{job->\n        job.results.forEach{recorded[it]=job.createdAt}\n        job.composedResults.forEach{recorded[it]=job.createdAt}\n    }\n    return recorded'
assert old.replace(before,after,1)==new
t=git('show',SOURCES['final']+':'+test).decode()
oldFn=old[old.index('fun libraryRecordedTimes('):old.index('\nfun libraryDateMatches(')]
ref=t[t.index('private fun recordedJobTimes('):t.index('\n\n/** Pure job-result')]
assert oldFn.replace('fun libraryRecordedTimes(','private fun recordedJobTimes(',1)==ref
identities=[]
for kind in ('baseline','final'):
    raw=(LOCAL/'LibraryCatalogProjection.before.kt').read_bytes() if kind=='baseline' else (ROOT/production).read_bytes()
    b=git('show',SOURCES[kind]+':'+production)
    assert sha(raw)==inputs[kind][production] and raw.replace(b'\r\n',b'\n')==b
    p=DEST/'inputs'/kind/'LibraryCatalogProjection.kt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
    identities.append(dict(run=kind,path=production,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,baselineExecutionSource=counts['baseline']['baseCommit'],executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,productionOnlyResultDateMapFold=True,fullImportedReceiptParseAndAllOtherProductionTextUnchanged=True,fullOldFunctionReferenceExactExceptName=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=3,baselineUniqueMethods=1,baselineAndRoundsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Synthetic JobRecord metadata/full StudioState.validate and<=4MiB;no queue/provider/real credentials/actual recipe request or asset-store admission/execution claim','Expected first key order from independent linked set,last values by last matching job;not timestamp sort','Ordinary then composed results perjob;first insertion order/lastinputwins preserved,all jobstates eligible as before','Two warmups each mode/four alternating same-JVM rounds;counter before assertions/JSON','No wholeUI/native/frame/RSS/peakheap/GC/physicalIO/universalWindows or Prompt acceptance','Map interface/entries/iteration order required,not concrete collection implementation']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl'):
    p=LOCAL/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate-v2.ps1',DEST/'resource-gate.ps1');shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Job result date map aggregation

Baseline3d73699 single actualAPI method versus final3 unique methods (2 RecordedJobTimesPerformanceTest plus1 existing malformedOversizedUnknownReceiptsAndJobOverridesMatchLegacyWithoutPayloadAccess). Baseline/rounds/sizes/historical tests not added to3. No accepted batch9/startup or old imported-receipt byte-copy allocation benchmark reruns. Production only libraryRecordedTimes job-result fold replacing temporary concatenated lists/Pairs/flatten/mapplus with call-local map assignments. Exact old wholefunction retained in private reference exceptname. Imported receipt fullJSON/16KiB/kind/version/date/error catches/defensiveAPI and all other production including tag default reuse unchanged. No validation/schema/request/provider/queue/source or file changes.

128/512/2048 synthetic JobRecords with shared resultIDs,empty ordinary lists,4ordinary/2composed results,random input orderseed179+count,COMPLETED/FAILEDboth. Full StudioState(version3).validate and serialized<=4MiB actually called. Synthetic-unused recipe not real request admission;metadata-only snapshots,no payloads/asset-store/provider/credential/queue execution. Independent expectation first key orderedset then last matching job timestamp prevents accidentally selecting latest timestamp/sorting;ordinary then composed originalorder. New snapshot/input bytes asserted stable. Additional imported/empty/duplicate/crosscall test and existing malformed/oversized/unknown receipts/0negative/job-overrides check date/error/order behavior. No global cache;returned Map interface/entries/iteration order preserved,not concrete class or mutablecast guarantees.

Two warmups each mode/four alternating same-JVM rounds,counters captured before assertion/serialization/JSON. Allocation/wall/CPU local metadata observations,not peakheap/GC/RSS/UI/physicalIO/universalWindows. Desktop first with explicit two emptyowner checks30seconds apart then lowpriority singleworker/2processors256MiB,boundaries not continuous quiet/CPU attribution. Existing runner retires only successful own payload roots after sealing evidence;old failures/unknown/backups/daily data retained. Prior failures/rejections and rejected folder traversal cohorts unchanged. No windows/real images/accounts/credentials/services/paidAPI/paused scanning/daily install/security/package/version/tag/release/trust changes. IndependentNOT REQUESTED,human/packageNOT TESTED,newProjectPREPAREDONLY/NOTSENT;16history indexfa58a50/originalDesktopreport fixed. Asset/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED-rootUNKNOWN,PRODUCT_HOLD / NOT RC remain.
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
print(json.dumps(dict(sources=SOURCES,zipBytes=archive.stat().st_size,zipSha256=sha(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup)),indent=2))
