from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-color-syntax-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_COLOR_SYNTAX_20261025'
SOURCES={'baseline':'1a007ab11f7fa50520d57387d2000e3d2f1cebf9','final':'b8b225d6951617b35b8f5e3d5b82950511681e3c'}
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
runs={}
for kind in ('baseline','final'):
    found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-color-syntax-'+kind+'-20261025'));assert len(found)==1
    s=json.loads((found[0]/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']=='PASS'
    assert(s['tests'],s['failures'],s['errors'],s['skipped'])==((1 if kind=='baseline' else 5),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    runs[kind]=(found[0],s)
assert not DEST.exists();DEST.mkdir(parents=True)
counts={};metrics={};inputs={};cleanup=[];sampleRows={}
for kind,(src,s) in runs.items():
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    out=ET.parse(next(src.glob('*TypesetColorSyntaxPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in out.splitlines() if x.startswith('TYPESET_COLOR_SYNTAX=')).split('=',1)[1])
    assert len(rows)==(24 if kind=='baseline' else 48)
    assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['colorCalls']==4*r['texts'] for r in rows)
    sampleRows[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for texts in (32,128,500):
        for context in ('hex-named','functional'):
            group=[r for r in rows if r['texts']==texts and r['context']==context]
            assert len({r['argbOrderSha256'] for r in group})==1
            for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                metrics[kind].append(dict(texts=texts,context=context,kind=mode,colorCalls=selected[0]['colorCalls'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],argbOrderSha256=selected[0]['argbOrderSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
for texts in (32,128,500):
    for context in ('hex-named','functional'):
        assert len({r['argbOrderSha256'] for rows in sampleRows.values() for r in rows if r['texts']==texts and r['context']==context})==1
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColors.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColorSyntaxPerformanceTest.kt'
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
old=git('show',SOURCES['baseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
expressions=[('functionSyntax',r'Regex("([a-z]+)\\((.*)\\)",RegexOption.DOT_MATCHES_ALL)'),('whitespaceSyntax',r'Regex("\\s+")'),('numberSyntax',r'Regex("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?")')]
expected=old
for key,expr in expressions:assert old.count(expr)==1;expected=expected.replace(expr,key,1)
fields=''.join('    private val '+key+' by lazy { '+expr+' }\n' for key,expr in expressions)
expected=expected.replace('object TypesetColors {\n','object TypesetColors {\n'+fields,1);assert expected==new
t=git('show',SOURCES['final']+':'+test).decode()
oldObject=old[old.index('object TypesetColors {'):].rstrip()
reference=t[t.index('private object RecordedTypesetSyntaxColors {'):t.index('\n\n/** Actual parser calls')].rstrip()
assert oldObject.replace('object TypesetColors {','private object RecordedTypesetSyntaxColors {',1)==reference
identities=[]
for kind in ('baseline','final'):
    raw=(LOCAL/'TypesetColors.before.kt').read_bytes() if kind=='baseline' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    p=DEST/'inputs'/kind/'TypesetColors.kt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
    identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
resources=['modules/studio-core/src/main/resources/typeset/css-named-colors.json','modules/studio-core/src/test/resources/typeset/source-colors.json','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TypesetColorTest.kt']
for name in resources:
    raw=(ROOT/name).read_bytes();b=git('show',SOURCES['baseline']+':'+name)
    assert git('show',SOURCES['final']+':'+name)==b and raw.replace(b'\r\n',b'\n')==b
    assert all(inputs[kind][name]==sha(raw) for kind in ('baseline','final'))
    p=DEST/'inputs'/'shared'/Path(name).name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
    identities.append(dict(run='both',path=name,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,baselineExecutionSource=counts['baseline']['baseCommit'],executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,productionOnlyThreeLazyFixedRegexFieldsAndThreeUses=True,originalPatternsOptionsParsingAndValidationTextUnchanged=True,oldParserReferenceExactExceptPrivateObjectName=True,allOtherProductionAndResourcesUnchanged=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=5,baselineUniqueMethods=1,baselineAndCasesThreadsRoundsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Synthetic validated TypesetDocument32/128/500text layers,4colors/text,serialized<=4MiB;no image/store/font/provider/realdata admission','Actual parse calls in equal map result-list wrapper;full document.validate/expectedARGB/snapshot checks outside counter','Two warmups eachmode/four alternating same-JVM rounds;counter before validation/JSON','Fixed3lazy Regex only,no input/result/matcher cache;3lazydelegates allocated on singleton init and3Patterns retained when firstused','Cold/first-use init,peakheap/GC/RSS/wholeUI/render/export/physicalIO/universalWindows not measured','HEX/named/transparent branch unchanged;control allocation same;small wall variation not attributed to regex reuse','Concurrent2threads/100rounds/7values verifiesreuse,not all initialization races/platforms/longrun','Existing independent162Canvas cases preserve originalone-unitpremultiplied tolerance;not162JUnitmethods']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','candidate-source-check.json','prepare-candidate.py','prepare-final.py'):
    p=LOCAL/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate-v2.ps1',DEST/'resource-gate.ps1');shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Fixed color syntax compilation reuse

Baseline1a007ab actual1 methodPASS;finalb8b225d actual5 unique methodsPASS:3new measured/exactoutcome/concurrent methods plus2existing TypesetColorTest independent162sourceCanvas/invalidretainedstyle methods. Do not add baseline/rounds/sizes/colors/cases/threads/assertions to5. No accepted batch9/startup or previous date/tag allocation tests rerun. Production only3private lazyfixedRegex fields and3uses,exact same text/options/matchEntire/split/matches;old parser entireobject exactexceptprivate name in reference. All other production/resources/162sourcecases/oldtolerance unchanged. Source rawbytes exactlymatchsource-before andCRLFcanonicalGit.

Metadata-only document32/128/500textlayers,4colors/text,fullTypesetDocument.validate and<=4MiB serialization beforetiming. Timed region actualcolors.map{parse} includes same resultlist wrapper,bothHEX/named/transparent andfunctionalRGB/HSL/HWB. ExpectedARGB/documentbytes outsidecounters. Not totaldocument.validate/render/font/window/assetstore admission/realuser image tests. Newexacttest all162trackedvalidvalues+syntheticextra/13invalid cases compareARGB or exceptionclass/message to exactoldparser;five hardcodedARGB values. Existing162Canvas independentpixels keepone-unit tolerance andraster1x1,no editorUI. Two ownthreads100rounds7values bounded/gracefulshutdown verifyfresh per-callmatches/colors,notall initialization races or longrun.

2warmups eachmode/4alternating same-JVM rounds. Lazydelegates allocated on objectinit,patterns retained lazily on corresponding functionalpath;no userinput/result/matcher cache. Cold/firstuse/peakheap/GC/RSS/globalWindows/wholeeditor performance unmeasured. HEXcontrol allocation unchanged,small wall/JIT variations notattributed to unmodifiedbranch;functional benefit only measuredscope. Prior folder traversal rejected cohorts and oldfailures/unknown/userrejections remain. Standalone threadCPU coarse,0notzeroCPU.

Observation-record correction:docs commit3d1251f at04:43:39UTC said baseline notexecuted before model read result;raw summary actuallystarted04:43:00.224508UTC andfinished~04:43:21.032508UTC. This should have been outcome PENDING_OBSERVATION,not proof of0execution. Oldcommit/raw retained,currentrecords corrected,no new testexecutions from this correction. Do not confuse with earlier job-date gates' actual0executions. Root path incident remains separately50de30f withpriorstate/exactUTC UNKNOWN,no new access/investigation ofthatpath.

Desktoppriority via twofresh emptyJavaowner checks30seconds apart thenlowpriority/singleworker2processors256MiB;notcontinuousquiet/CPU attribution. Ownsuccessfulfixturepayloads sealedthenretiredbyexistingrunner;oldfailed/unknown/backups/daily data retained. No realprivatecredentials/providers/paidcalls/pausedscan/dailyinstall/package/tag/release/trust changes. IndependentNOTREQUESTED,human/packageNOTTESTED,newProjectPREPAREDONLY/NOTSENT;old16indexfa58a50/twofollowonsb5e84da/originalDesktopreport fixed. Assets/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED-rootUNKNOWN,PRODUCT_HOLD / NOT RC remain.
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
