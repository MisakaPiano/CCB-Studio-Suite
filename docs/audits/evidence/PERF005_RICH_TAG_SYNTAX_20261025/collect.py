from pathlib import Path
import hashlib,json,os,re,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-rich-tag-syntax-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_RICH_TAG_SYNTAX_20261025'
SOURCES={'baseline':'73259d51414ccd95d9a2f911c845084d00215bc2','final':'9987d85638c38bf64641f3c19bd4836084c905a5'}
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/RichTagSyntaxPerformanceTest.kt'
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
    found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-rich-tag-syntax-'+kind+'-20261025'));assert len(found)==1
    s=json.loads((found[0]/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']=='PASS'
    assert(s['tests'],s['failures'],s['errors'],s['skipped'])==((1 if kind=='baseline' else 3),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    runs[kind]=(found[0],s)
# Validate exact production/reference identity before creating the evidence destination.
old=git('show',SOURCES['baseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
expr=r'Regex("^([a-zA-Z_]+)\\s*=\\s*(.+)$")';assert old.count(expr)==1
expected=old.replace(expr,'richTagSyntax',1).replace('    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {','    private val richTagSyntax by lazy { '+expr+' }\n    private fun patch(tag:String,s:TypesetStyle):TypesetStyle? {',1);assert expected==new
t=git('show',SOURCES['final']+':'+test).decode()
subset=old[old.index('    fun tokens('):old.index('    private val force=')]
reference='private object RecordedRichTagTokens {\n'+re.sub(r'\bToken\b','TypesetLayout.Token',subset)+'}\n'
assert t[t.index('private object RecordedRichTagTokens {'):t.index('\n/** Actual rich text')].rstrip()==reference.rstrip()
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
checked(DEST);assert not DEST.exists();DEST.mkdir()
counts={};metrics={};inputs={};cleanup=[];sampleRows={};identities=[]
for kind,(src,s) in runs.items():
    for p in src.iterdir():
        if p.is_file():checked(p);store(DEST/'runs'/kind/p.name,p.read_bytes())
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    out=ET.parse(next(src.glob('*RichTagSyntaxPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in out.splitlines() if x.startswith('RICH_TAG_TOKENS=')).split('=',1)[1]);assert len(rows)==(36 if kind=='baseline' else 72)
    assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['utf16Length']<=100000 for r in rows)
    sampleRows[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for count in (32,128,512):
        for context in ('plain','simple-tags','attribute-tags'):
            group=[r for r in rows if r['units']==count and r['context']==context];assert len({r['tokenOrderSha256'] for r in group})==1
            for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                metrics[kind].append(dict(units=count,context=context,kind=mode,outputTokens=selected[0]['outputTokens'],utf16Length=selected[0]['utf16Length'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],tokenOrderSha256=selected[0]['tokenOrderSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
    raw=(LOCAL/'TypesetLayout.before.kt').read_bytes() if kind=='baseline' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    store(DEST/'inputs'/kind/'TypesetLayout.kt',raw);identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
for count in (32,128,512):
    for context in ('plain','simple-tags','attribute-tags'):
        assert len({r['tokenOrderSha256'] for rows in sampleRows.values() for r in rows if r['units']==count and r['context']==context})==1
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,baselineExecutionSource=counts['baseline']['baseCommit'],executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,productionOnlyOneLazyFixedRegexAndOneUse=True,exactPatternOptionsAndAllOtherProductionTextUnchanged=True,recordedTokensAndPatchExactExceptTokenQualification=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=3,baselineUniqueMethods=1,baselineAndCasesThreadsRoundsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Actual tokens call only;not document validation/layout/render/font metrics/UI','TypesetDocument.validate/serialization<=4MiB/UTF16<=100000,style/fullToken output/hash/document snapshot outside counters','2warmups eachmode/4alternating rounds;no cold/firstuse/retainedPattern costs/peakheap/GC/RSS measurement','Plain/simple tags control original branch unchanged;small wall variation not attributed to Regex reuse','Exact old tokens+patch with real Token type;aliases/malformed/unknown/escaped brackets/quoted/numeric defaults/clamps/color retention/depth and length errors compared plus independent assertions','Two own threads100rounds verify steady state stack/result isolation,not every first initialization race/Windows environment']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','prepare-final.py'):
    p=LOCAL/name
    if p.exists():checked(p);store(DEST/name,p.read_bytes())
store(DEST/'resource-gate.ps1',(ROOT/'.local/performance-resource-gate-v2.ps1').read_bytes());store(DEST/'collect.py',Path(__file__).read_bytes())
store(DEST/'ATTEMPTS_AND_LIMITS.md',b'''# Fixed rich attribute tag syntax

Baseline73259d5 actual1methodPASS;final9987d85 actual3unique methodsPASS. No baseline/size/context/sample/edge/threads/assertions summed. Only oneprivate lazyfixedRegex andoneuse,exact pattern/options/matchEntire. No input/result/Matcher cache;all other production unchanged. Source reference exactoldtokens+patch exceptreal Tokenqualification,no font/layout objectcopy. No previousvertical/color/acceptedbatch9/native/startup test reruns. Same-JVM old/current4alternating rounds after2warmups each,thread allocation/cpu/localwall only.

Synthetic valid document32/128/512 repeatedunits,plain/simpletags/attribute tags;fullTypesetDocument.validate/serialization<=4MiB/UTF16<=100000 outside counters. Timed tokens call includes parsing/codepoint Token allocation,notdocument validation/fonts/measure/layout/render/UI/store/realuser admission. Full Token field/style/order equality and hardcodedstyle vector,inputdocument bytes andbase style preserved. Edge test covers aliases,quoted/numeric defaults/clamps/malformed/unknown literal/escapedbrackets/mixedUnicode/invalidcolorsyntax retained,999nest accepted/1000rejected,100000charsaccepted/100001rejected andexceptionclass+message. Directhelper invalid/malformed cases are not claims of document admission. Two ownthreads100rounds normalshutdown;not all lazyinit races/platforms/longrun.

One lazydelegate allocated on singletoninit,one Patternretained on firstattribute/unknown-tag fallback;cold/firstuse/retained/peakheap/GC/RSS unmeasured. Plain/simpletag paths do not use Regex;control allocation/wall changes are not attributed tomodifiedbranch,rootcauseofJIT/noise UNKNOWN. CPU0coarse notzeroCPU. Desktoplatestowner twoemptychecks30secapart andlater boundary only,not wholewindowquiet. Lowprioritysingleworker2processors256MiB. Ownsuccessfulpayloadcleanup0files;no extra cleanup/kill/dailydata. No newProjectsend/package/tag/release/trust/security/MSI/privatecredentials/realcalls/pausedscan/Desktopwrites.

Continuoususerauthority06:35/07:07JST supersedes older temporaryonlyregistration;ordinarycheckpointdoesnotstop. Currenthandoff clarified without expandingfeatures. Originalfixed16/twofollowon/date/color/vertical/Desktopreports unchanged. Previouscollectorprep failures/asyncobservation correction/rejectedfoldertraversal/userrejections/oldtests retained. Operatorpathincident50de30f priorstate/exactUTC UNKNOWN,nofutureaccess. Assets/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED/rootUNKNOWN/PRODUCT_HOLD remain. IndependentNOTREQUESTED/humanandpackageNOTTESTED,newProjectPREPAREDONLY/NOTSENT.
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
