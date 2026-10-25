from pathlib import Path
import hashlib,json,os,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-vertical-tokens-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_VERTICAL_TOKENS_20261025'
SOURCES={'baseline':'c6efb5e36827311da9e919521723a0dd7fce3f97','final':'e9ae9f32498786326ae41552748db346604d97bd'}
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalTokenProjectionPerformanceTest.kt'
def sha(b):return hashlib.sha256(b).hexdigest()
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def checked(p):
    assert p.is_absolute() and p.is_relative_to(ROOT) and p!=ROOT
    for parent in [p,*p.parents]:
        if parent.exists():assert not(os.lstat(parent).st_file_attributes&0x400),str(parent)
        if parent==ROOT:break
    return p
def store(p,b):
    checked(p);assert not p.exists();p.parent.mkdir(parents=True,exist_ok=True);checked(p)
    with p.open('xb') as f:f.write(b)
def write(p,v):store(p,(json.dumps(v,indent=2)+'\n').encode())
runs={}
for kind in ('baseline','final'):
    found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-vertical-tokens-'+kind+'-20261025'));assert len(found)==1
    s=json.loads((found[0]/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']=='PASS'
    assert(s['tests'],s['failures'],s['errors'],s['skipped'])==((2 if kind=='baseline' else 4),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    runs[kind]=(found[0],s)
checked(DEST);assert not DEST.exists();DEST.mkdir()
counts={};metrics={};inputs={};cleanup=[];sampleRows={};fixtureIdentities={}
for kind,(src,s) in runs.items():
    for p in src.iterdir():
        if p.is_file():checked(p);store(DEST/'runs'/kind/p.name,p.read_bytes())
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    out=ET.parse(next(src.glob('*VerticalTokenProjectionPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in out.splitlines() if x.startswith('VERTICAL_TOKEN_PROJECTION=')).split('=',1)[1])
    fixtures=json.loads(next(x for x in out.splitlines() if x.startswith('VERTICAL_SOURCE_FIXTURES=')).split('=',1)[1]);fixtureIdentities[kind]=fixtures
    assert len(rows)==(24 if kind=='baseline' else 48)
    assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['utf16Length']<100000 for r in rows)
    sampleRows[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for count in (128,1024,8192):
        for context in ('mapped-control','fallback'):
            group=[r for r in rows if r['inputTokens']==count and r['context']==context]
            assert len({r['tokenOrderSha256'] for r in group})==1
            for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                metrics[kind].append(dict(inputTokens=count,context=context,kind=mode,outputTokens=selected[0]['outputTokens'],utf16Length=selected[0]['utf16Length'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],tokenOrderSha256=selected[0]['tokenOrderSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
for count in (128,1024,8192):
    for context in ('mapped-control','fallback'):
        assert len({r['tokenOrderSha256'] for rows in sampleRows.values() for r in rows if r['inputTokens']==count and r['context']==context})==1
assert fixtureIdentities['baseline']==fixtureIdentities['final']
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
old=git('show',SOURCES['baseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
expected=old
expressions=[(r'ch in setOf("\"","＂")',r'ch=="\"" || ch=="＂"'),('ch in setOf("\'","＇")','ch=="\'" || ch=="＇"'),(r'Regex("\\p{P}")','verticalPunctuation'),(r'chars("/\\／＼~～〜=＝+＋*＊&＆@＠#＃%％^＾<>＜＞|｜")','rotationSymbols')]
for expr,replacement in expressions:assert expected.count(expr)==1;expected=expected.replace(expr,replacement,1)
fields='    private val verticalPunctuation by lazy { '+expressions[2][0]+' }\n    private val rotationSymbols by lazy { '+expressions[3][0]+'.toSet() }\n'
expected=expected.replace('    private fun presentation(ch:String)=',fields+'    private fun presentation(ch:String)=',1);assert expected==new
t=git('show',SOURCES['final']+':'+test).decode()
chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
constants=old[old.index('    private val force='):old.index('    fun upright(')]
projection=old[old.index('    fun verticalTokens('):old.index('    private fun cls(')].replace('List<Token>','List<TypesetLayout.Token>').replace('mutableListOf<Token>','mutableListOf<TypesetLayout.Token>')
reference='private object RecordedVerticalProjection {\n'+chars+'\n'+constants+projection+'}\n'
assert t[t.index('private object RecordedVerticalProjection {'):t.index('\n/** Actual character projection')].rstrip()==reference.rstrip()
oldTest=git('show',SOURCES['baseline']+':'+test).decode()
golden=oldTest[oldTest.index('    @Test fun fixedSourceVerticalMetadata'):oldTest.rindex('\n}')]
assert golden in t
identities=[]
for kind in ('baseline','final'):
    raw=(LOCAL/'TypesetLayout.before.kt').read_bytes() if kind=='baseline' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    store(DEST/'inputs'/kind/'TypesetLayout.kt',raw)
    identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
for fixture in fixtureIdentities['baseline']:
    name=fixture['path'];raw=(ROOT/name).read_bytes();b=git('show',SOURCES['baseline']+':'+name)
    assert git('show',SOURCES['final']+':'+name)==b and raw==b and sha(raw)==fixture['rawSha256']
    assert all(inputs[kind][name]==sha(raw) for kind in ('baseline','final'))
    store(DEST/'inputs'/'shared'/Path(name).name,raw)
    identities.append(dict(run='both',path=name,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,baselineExecutionSource=counts['baseline']['baseCommit'],executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,productionOnlyTwoLazyConstantsAndThreeConditions=True,originalMappingRulesAndAllOtherProductionTextUnchanged=True,recordedProjectionExactExceptTokenQualification=True,fixedSourceMetadataTestAndFixturesUnchanged=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=4,baselineUniqueMethods=2,baselineAndCasesThreadsRoundsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,sourceFixtureIdentities=fixtureIdentities,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Actual verticalTokens only,not tokens/document validation/layout/render/font metrics/UI','TypesetDocument.validate and serialization<=4MiB outside counters;tokens prepared outside,all output Token fields/styles/input/document snapshot compared outside','2warmups eachmode/4alternating rounds;no cold/firstuse/retained pattern/set costs/peakheap/GC/RSS measurement','Fixed3synthetic public source fixtures verify accepted display/rotation/fontSize metadata perparagraph only;no advances/wrapping/pixels','Lazy delegates and fixed Regex/Set retained;no user input/output cache;quote/dot state percall','Two own threads100rounds verify steady reuse/state,not every initialization race or Windows environment']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','prepare-final.py'):
    p=LOCAL/name
    if p.exists():checked(p);store(DEST/name,p.read_bytes())
store(DEST/'resource-gate.ps1',(ROOT/'.local/performance-resource-gate-v2.ps1').read_bytes());store(DEST/'collect.py',Path(__file__).read_bytes())
store(DEST/'ATTEMPTS_AND_LIMITS.md',b'''# Vertical token fixed preparations

Baseline c6efb5e actual2methodsPASS. Final e9ae9f3 actual4unique methodsPASS;baseline/cases/sizes/rounds/threads are separate,not added. Source and output token hashes stable. Production only2lazy constants and3conditions:direct quote equality,exact punctuation Regex and fixed symbol Set. Existing force/presentation/upright fallback/rich text rules and all other production unchanged. No font/measure/layout/render APIs in timed region or reference subset. Reference subset exactchars/force/uprightFallback/presentation/verticalTokens except realToken qualification. Metadata reference method and3rawpublic synthetic golden fixtures unchanged;not whole typography or editor acceptance.

128/1024/8192codepoint synthetic text,UTF16<100000;actual TypesetDocument.validate/serialization<=4MiB outside counters. Inputtokens from actual tokens outside. Full Token equality includesstyle/ch/display/rotate/forced/alnum;input/document snapshots preserved.4alternating sameJVM samples perpath,2warmups each. New edges cover quote toggles/apostrophes/2-5dots/presentation/upright/fallback/Unicode supplementary andcombining/richstyle/customtokens/emptyhelper exceptionclass+message.2ownthreads100rounds normalshutdown;not all first-init races/longrun. No expected font/layout/pixel fidelity extension. No realimage/store/provider/user admission claim.

Lazydelegates allocated on singletoninit and boundedRegex/symbolSet retained onfirstfallbackuse;firstuse/cold/retained/peakheap/GC/RSS/fullUI/OSphysicalIO not measured. Desktop latestowner2emptychecks30secapart then lowpriority/singleworker2processors256MiB;not continuousquiet or CPU attribution. Ownsuccessfulpayloadcleanup0files,no extra cleanup/kill/dailydata actions. No oldaccepted batch9/startup/sourcecolor tests rerun.

No newfeatures/unified-image-tool integration/realcredentials/providers/paidcalls/pausedscan/dailyinstall/package/tag/release/trust changes. IndependentNOTREQUESTED;human/packageNOTTESTED;newProjectPREPAREDONLY/NOTSENT. Old16indexfa58a50/twofollowonsb5e84da/originalDesktopreport fixed;date/color increments separate. Rejectedfoldertraversal/oldfailures/userrejections retained. Operatorpathincident50de30f priorstate/exactUTC UNKNOWN and prohibition onfutureaccess remain,no new probes. Assets/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED-rootUNKNOWN,PRODUCT_HOLD / NOT RC remain.
''')
store(DEST/'.gitattributes',b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p.read_bytes())) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip';checked(archive);assert not archive.exists()
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert sha(z.read(item['path']))==item['sha256']
print(json.dumps(dict(sources=SOURCES,zipBytes=archive.stat().st_size,zipSha256=sha(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup)),indent=2))
