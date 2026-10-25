from pathlib import Path
import hashlib,json,os,re,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-punctuation-classes-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_PUNCTUATION_CLASSES_20261025'
SOURCES={'failedBaseline':'772a66c8cbee574cf82aa4e2688eaa4f1d13af86','baseline':'d82f804d73557ab4e6fa633f2655835fcc3e6d03','final':'3d9abb0e10a61a230e17542a8cc22b5c1c0992e3'}
PHASES={'failedBaseline':'baseline','baseline':'corrected-baseline','final':'final'}
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalPunctuationClassPerformanceTest.kt'
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
for kind in SOURCES:
    found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-punctuation-classes-'+PHASES[kind]+'-20261025'));assert len(found)==1
    s=json.loads((found[0]/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']==('FAILED' if kind=='failedBaseline' else 'PASS')
    assert(s['tests'],s['failures'],s['errors'],s['skipped'])==((1,1,0,0) if kind=='failedBaseline' else ((1 if kind=='baseline' else 3),0,0,0))
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    runs[kind]=(found[0],s)
old=git('show',SOURCES['baseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
assert git('show',SOURCES['failedBaseline']+':'+prod).decode()==old
failedTest=git('show',SOURCES['failedBaseline']+':'+test).decode();baseTest=git('show',SOURCES['baseline']+':'+test).decode()
assert failedTest.replace('listOf(12.0,10.8,10.6)','listOf(12.0,10.6,10.8)',1)==baseTest
cls=old[old.index('    private fun cls('):old.index('    private fun reduction(')];fields=[];expected=old
for line in cls.splitlines():
    if not line.strip().startswith('in chars('):continue
    condition,value=line.strip().split('->');expr=condition[3:];name=value.strip('"')+'Punctuation';assert old.count(expr)==1
    expected=expected.replace(expr,name,1);fields.append('    private val '+name+' by lazy { '+expr+'.toSet() }\n')
assert len(fields)==5;expected=expected.replace('    private fun cls(ch:String)=',''.join(fields)+'    private fun cls(ch:String)=',1);assert expected==new
chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
subset=old[old.index('    private fun cls('):old.index('    data class StoryLayout(')]
subset=re.sub(r'\b(Token|Cell|Line|Layout)\b',r'TypesetLayout.\1',subset);subset=re.sub(r'\b(tokens|verticalTokens|measure)\(',r'TypesetLayout.\1(',subset)
reference='private object RecordedVerticalPunctuationLayout {\n'+chars+'\n'+subset+'}\n'
t=git('show',SOURCES['final']+':'+test).decode();assert t[t.index('private object RecordedVerticalPunctuationLayout {'):t.index('\n/** Actual vertical layout')].rstrip()==reference.rstrip()
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
checked(DEST);assert not DEST.exists();DEST.mkdir()
counts={};metrics={};inputs={};cleanup=[];sampleRows={};identities=[]
for kind,(src,s) in runs.items():
    for p in src.iterdir():
        if p.is_file():checked(p);store(DEST/'runs'/kind/p.name,p.read_bytes())
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable','status')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']==('RETAINED_FAILED_FIXTURES' if kind=='failedBaseline' else 'COMPLETE');cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    xml=ET.parse(next(src.glob('*VerticalPunctuationClassPerformanceTest.xml'))).getroot();out=xml.findtext('system-out') or ''
    if kind=='failedBaseline':
        assert 'VERTICAL_PUNCTUATION_CLASSES=' not in out
        assert 'actual <10.6>' in xml.find('testcase/failure').attrib['message']
    else:
        rows=json.loads(next(x for x in out.splitlines() if x.startswith('VERTICAL_PUNCTUATION_CLASSES=')).split('=',1)[1]);assert len(rows)==(24 if kind=='baseline' else 48)
        assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['utf16Length']<=100000 for r in rows)
        sampleRows[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
        for count in (128,1024,8192):
            for enabled in (False,True):
                group=[r for r in rows if r['inputCodepoints']==count and r['enabled']==enabled];assert len({r['layoutSha256'] for r in group})==1
                for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                    selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                    metrics[kind].append(dict(inputCodepoints=count,enabled=enabled,kind=mode,lines=selected[0]['lines'],utf16Length=selected[0]['utf16Length'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],layoutSha256=selected[0]['layoutSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
    raw=(LOCAL/'TypesetLayout.before.kt').read_bytes() if kind!='final' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    store(DEST/'inputs'/kind/'TypesetLayout.kt',raw);identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
for count in (128,1024,8192):
    for enabled in (False,True):
        assert len({r['layoutSha256'] for rows in sampleRows.values() for r in rows if r['inputCodepoints']==count and r['enabled']==enabled})==1
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,executions={k:v['baseCommit'] for k,v in counts.items()},identities=identities,changedModulePaths=changed,productionOnlyFiveLazyFixedSetsAndFiveMembershipUses=True,exactStringsCategoryOrderAndAllOtherProductionTextUnchanged=True,recordedCharsClsReductionLayoutExactExceptActualTypeAndDependencyQualification=True,baselineCorrectionOnlyExpectedSideVectorPrecisionUnchanged=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=3,correctedBaselineUniqueMethods=1,failedBaselineUniqueMethods=1,failedBaselineFailures=1,noMethodsCasesThreadsRoundsSummed=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,fixtureCleanup=cleanup,cleanupFiles=sum(c['removed'] for c in cleanup),limits=['Actual layout on all vertical=true inputs;tokens/projection/reduction/wrapping included,not render/font measurement/UI','TypesetDocument.validate/serialization<=4MiB/UTF16<=100000 and fullLayout/hash/snapshot validation outside counters','Exactold reference includes horizontal measure branch qualified to real dependency but never executed;allinputs vertical invariant','5lazy fixedSets retained,firstuse/delegate/retained/peakheap/GC/RSS unmeasured','Disabled punctuation branch unchanged;wall/alloc variation not attributed to changedcls path without evidence','Initial vector1FAIL before samples,onlyexpected before/after sides corrected1e-9 unchanged;oldraw/source/fixture retained,notproductfail orPASS','Two ownthreads50rounds normalshutdown not all lazy initialization races or allWindows']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','prepare-final.py','prepare-final-v2.py','BASELINE_VECTOR_FAILURE.json'):
    p=LOCAL/name
    if p.exists():checked(p);store(DEST/name,p.read_bytes())
store(DEST/'resource-gate.ps1',(ROOT/'.local/performance-resource-gate-v2.ps1').read_bytes());store(DEST/'collect.py',Path(__file__).read_bytes())
store(DEST/'ATTEMPTS_AND_LIMITS.md',b'''# Vertical punctuation class fixed sets

Initial772a66c baseline1method1FAIL before performance samples:independent vector reversed before/after existingcoefficient indices. Actual comma10.6 not10.8,followingChinese10.8 not10.6. d82f804 corrects expected vectoronly,1e-9 precision unchanged,productsourceunchanged. Originalraw/source/fixture31a2a3258b1f46e3b424dfffdf4d1408 retained,oldpreparation script/drafts neverapplied. Correctedbaseline d82f804 actual1PASS,final3d9abb0 actual3uniquePASS;allseparate,no sumofmethod/sizes/cases/threadrounds. Failedfixturecleanup RETAINED_FAILED_FIXTURES,successcleanup COMPLETE0payloadfiles. No productbug/securitycausality claimfromtestexpectationerror.

Productiononly5lazyfixedSetpreparations+5membershipreferences;exactstrings/categoryorder/when/default,allotherproductionunchanged includingreduction numeric arrays/formula/wrapping/tokenprojection/richtag/fontmetrics/rendering. Same types/classes in exactoldchars/cls/reduction/layout reference exceptactual data/dependency qualification. Reference retains horizontalmeasurebranch butallactualtestinputsassertvertical=true,branchneverexecuted. ExistingFontRenderContext objectinit outsidecounters,notfonttest. Actual layout includes tokens/verticalprojection/classification/reduction/lines/dimensions;no render/image/store/provider/realuserdata admission.

128/1024/8192codepoints enabled/disabledpunctuation,validTypesetDocument.validate/serialization<=4MiB/UTF16<=100000 outsidecounter;fullstructuredLayout/cells/Token/style/class/advance/wrapping/dimensions/hash/documentbytes verifiedoutside. Independent10pxA/comma/Chineseclasses/advances/dimensions anddisabled12each. Edges allcategorysymbolstrings,richstyles,quotes/dotcollapse/Unicode,standardcompactloose,width10/48/384,letterspacing3,size6/1000,lineheight2,empty/newlines;notfont/pixelgolden.2ownthreads50rounds orderlyshutdown,notallinitializationraces/platforms/longrun.

2warmups eachmode/4alternating sameJVM rounds.5lazydelegatesallocatedonobjectinit,fixedSets/codepointstringsretainedonfirstcategoryuse;firstuse/cold/retained/peakheap/GC/RSS/fullUI unmeasured. Disabledcontrol avoidscls,changes inunchangedpath wall/alloc notattributed toSetreuse,rootcauses UNKNOWN. CPU0 coarse notzeroCPU. Desktoplatestowner2emptyboundaries30secapart/lowprioritysingleworker2CPU256MiB,notwholewindowquiet. No previouslyacceptedvertical/richtag/color/batch9/native/startup tests rerun.

Continuousauthority stays;no ordinarycheckpoint stop/newProjectsend/newexecutor/UIfeatures/unified-image-tool/realcreds/paidcalls/pausedscan/dailyinstall/release/package/tag/trust/security/MSI/Desktopwrites. Originalfixedpackets/oldfailures/rejectedtraversals/userrejections/operatorpathincident50de30f priorstate/exactUTC UNKNOWN andno futureaccess preserved. Assets/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED/rootUNKNOWN/PRODUCT_HOLD. Human/packageNOTTESTED/independentNOTREQUESTED/newProjectPREPAREDONLY.
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
