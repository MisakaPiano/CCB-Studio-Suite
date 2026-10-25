from pathlib import Path
import hashlib,json,os,re,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-punctuation-ratios-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_PUNCTUATION_RATIOS_20261025'
SOURCES={'reusedBaseline':'3d9abb0e10a61a230e17542a8cc22b5c1c0992e3','final':'e8378806ce950bf3d22fff1dc3e71bc7c0d95548'}
PRIOR_EVIDENCE='838b087a08a7f81eb62d2bef2f2f5f819433e2f8';PRIOR='docs/audits/evidence/PERF005_PUNCTUATION_CLASSES_20261025'
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
oldTest='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalPunctuationClassPerformanceTest.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VerticalPunctuationRatioPerformanceTest.kt'
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
baseline=ROOT/PRIOR/'runs/final'
for p in baseline.iterdir():
    if p.is_file():assert git('show',PRIOR_EVIDENCE+':'+p.relative_to(ROOT).as_posix())==p.read_bytes()
found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-punctuation-ratios-final-20261025'));assert len(found)==1
runs={'reusedBaseline':baseline,'final':found[0]};summaries={}
for kind,folder in runs.items():
    s=json.loads((folder/'summary.json').read_text(encoding='utf-8'));assert s['status']=='PASS' and s['sourceStable'] and(s['tests'],s['failures'],s['errors'],s['skipped'])==(3,0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b'';summaries[kind]=s
old=git('show',SOURCES['reusedBaseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
entries=[('compactPunctuationRatios','doubleArrayOf(.20,.18,.28,.10,.10,.28,.22,.08)'),('loosePunctuationRatios','doubleArrayOf(.08,.06,.12,.04,.04,.12,.10,.02)'),('standardPunctuationRatios','doubleArrayOf(.14,.12,.20,.07,.07,.20,.16,.05)')]
expected=old
for name,expr in entries:assert old.count(expr)==1 and new.count(name)==2;expected=expected.replace(expr,name,1)
expected=expected.replace('    private fun reduction(',''.join('    private val '+name+' by lazy { '+expr+' }\n' for name,expr in entries)+'    private fun reduction(',1);assert expected==new
reduction=new[new.index('    private fun reduction('):new.index('    fun layout(')]
assert not re.search(r'p\[[^\n]*?\]\s*=',reduction) and 'return min(' in reduction
chars=next(x for x in old.splitlines() if 'private fun chars(' in x)
subset=old[old.index('    private val smallPunctuation '):old.index('    data class StoryLayout(')]
subset=re.sub(r'\b(Token|Cell|Line|Layout)\b',r'TypesetLayout.\1',subset);subset=re.sub(r'\b(tokens|verticalTokens|measure)\(',r'TypesetLayout.\1(',subset)
reference='private object RecordedVerticalRatioLayout {\n'+chars+'\n'+subset+'}\n'
t=git('show',SOURCES['final']+':'+test).decode();assert t[t.index('private object RecordedVerticalRatioLayout {'):t.index('\n/** Actual vertical layout')].rstrip()==reference.rstrip()
changed=git('diff','--name-only',SOURCES['reusedBaseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
checked(DEST);assert not DEST.exists();DEST.mkdir()
counts={};metrics={};inputs={};cleanup=[];samples={};identities=[]
for kind,folder in runs.items():
    s=summaries[kind]
    for p in folder.iterdir():
        if p.is_file():checked(p);store(DEST/'runs'/kind/p.name,p.read_bytes())
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')};counts[kind]['newExecutionsInThisIncrement']=0 if kind=='reusedBaseline' else 3
    c=json.loads((folder/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((folder/'source-before.json').read_text(encoding='utf-8'))
    className='VerticalPunctuationClassPerformanceTest' if kind=='reusedBaseline' else 'VerticalPunctuationRatioPerformanceTest'
    out=ET.parse(next(folder.glob('*'+className+'.xml'))).getroot().findtext('system-out')
    marker='VERTICAL_PUNCTUATION_CLASSES=' if kind=='reusedBaseline' else 'VERTICAL_PUNCTUATION_RATIOS='
    rows=json.loads(next(x for x in out.splitlines() if x.startswith(marker)).split('=',1)[1])
    if kind=='reusedBaseline':
        assert len(rows)==48;rows=[dict(r,profile='standard' if r['enabled'] else 'disabled') for r in rows if r['kind']=='current'];assert len(rows)==24
    else:assert len(rows)==96
    assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['utf16Length']<=100000 for r in rows)
    samples[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for count in (128,1024,8192):
        for profile in (('disabled','standard') if kind=='reusedBaseline' else ('disabled','standard','compact','loose')):
            group=[r for r in rows if r['inputCodepoints']==count and r['profile']==profile];assert len({r['layoutSha256'] for r in group})==1
            for mode in sorted({r['kind'] for r in group}):
                selected=[r for r in group if r['kind']==mode];assert len(selected)==4
                metrics[kind].append(dict(inputCodepoints=count,profile=profile,kind=mode,lines=selected[0]['lines'],utf16Length=selected[0]['utf16Length'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],layoutSha256=selected[0]['layoutSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
    raw=(LOCAL/'TypesetLayout.before.kt').read_bytes() if kind=='reusedBaseline' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    store(DEST/'inputs'/kind/'TypesetLayout.kt',raw);identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in ((oldTest if kind=='reusedBaseline' else test),'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
for count in (128,1024,8192):
    for profile in ('disabled','standard'):
        same=[r for rows in samples.values() for r in rows if r['inputCodepoints']==count and r['profile']==profile]
        assert len({(r['layoutSha256'],r['serializedDocumentBytes'],r['lines'],r['utf16Length']) for r in same})==1
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,precedingEvidenceCommit=PRIOR_EVIDENCE,precedingEvidencePath=PRIOR,precedingRawSha256='d026c15e213282c99b4455492d2b6550cf1a7a1208eb1e2f89eb7e0b6efc64a9',executions={k:v['baseCommit'] for k,v in counts.items()},identities=identities,changedModulePaths=changed,productionOnlyThreePrivateLazyRatioArraysAndThreeUses=True,exactNumericOrderFormulaAndAllOtherProductionTextUnchanged=True,privateArraysOnlyFieldAndWhenReferencesNoExposureAndNoWrites=True,recordedCachedClsReductionLayoutExactExceptActualTypeDependencyQualification=True,reusedPrecedingActualCurrentRowsNoBaselineRerun=True,standardAndDisabledDocumentAndLayoutHashesIdenticalToPreceding=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=3,precedingMethodsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,newTestMethodsExecuted=3,newFixtureCleanupFiles=cleanup[1]['removed'],precedingFixtureCleanupNotAdded=True,limits=['Actual vertical layout only,allinputs vertical;no horizontal font measure/render/UI/image/store/provider','Preceding actual3d9current rows reused0newbaseline execution;standard/disabled rawdocument+layout identities match,compact/loose onlynewpaired rows','2warmups eachmode/4alternating rounds,fullLayout/docvalidate/serialization<=4MiB/UTF16<=100000 outsidecounter','3private fixedratio arrays onlyreadbylocalpart,neverreturn/passout;lazydelegate/firstuse/retainedarrays/peakheap/GC/RSS unmeasured','Independent standard/compact/loose vectors1e-9;unknownhelperfallback preserved,not valid document admission','Two ownthreads50rounds rotate3profiles not every lazyinit race/platform/longrun','Disabledcontrol unchanged;wall/alloc variation notattributed to changedratio branch without evidence']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','prepare.py'):
    p=LOCAL/name
    if p.exists():checked(p);store(DEST/name,p.read_bytes())
store(DEST/'resource-gate.ps1',(ROOT/'.local/performance-resource-gate-v2.ps1').read_bytes());store(DEST/'collect.py',Path(__file__).read_bytes())
store(DEST/'ATTEMPTS_AND_LIMITS.md',b'''# Fixed punctuation ratio arrays

Reuse precedingactual3d9abb0 currentrows andrawsuite evidence fixed838b087,0newbaselineexecution. Do notadd preceding3methodstothisincrement3. Newfinale8378803unique methods:pairedactualvertical layout acrossdisabled/standard/compact/loose128/1024/8192,fullLayout/edge/numericvectors,2ownthreadsrotatingprofiles50rounds. Priorstandard/disabled document/layout hashes exactmatched;compact/loose onlynewpairedcomparison. No oldclassification/vertical/richtag/color/acceptedbatch9/native/startup suites rerun. Priorfailure772a66c andasyncobservationmisreports/oldfixedpackets preservedinpreviousreport,notrelabelledPASS.

Production3private lazyfixed8doublearrays+3whenuses only. Exactvalues/order/elsefallback/formula/partindices/layout andallotherproductionunchanged. Arraysonlyfielddeclaration+whenreference,localp onlyreadbyinternalpart,no writes/returnedarrays/externalAPIpasses. Notinput/resultcache;privatereadonlybyimplementation,not generalarrayimmutabilityorreflectionsecurity guarantee. Lazydelegates/firstuse/retained3arrays/peakheap/GC/RSS/coldcost unmeasured. Allinputsvertical=true;referencehorizontalmeasurebranchretainedbutneverexecuted,no fontmeasure/render/image/window/providers.

SyntheticTypesetDocument.validate/UTF16<=100000/serialization<=4MiB outsidecounters,fullstructuredLayout/Token/style/class/advance/wrapping/dimensions/documentbytes/hashes assertedoutside. Actual layout includesparsing/projection/classes/reduction/wrapping,not onlyreduction. Independent10pxvectors correctdirection standard12/10.6/10.8 compact12/10/10.2 loose12/11.2/11.4 andunknownhelperfallbackstandard;unknownhelpernotdocumentadmission. Edges allclasses/styles/widths/tightness/lineheight/newline/empty,concurrentsteadyreadsnotallfirstinitracesorplatforms. Disabledcontrolbranchunchanged,variationnotattributedwithoutproof,CPU0coarsenotzeroCPU.2warmups eachmode/4alternating rounds,noretuningorhidenegatives.

Desktoplatestowner2emptychecks30secapart/lowprioritysingleworker2CPU256MiB andboundaryonly,notwholewindowquiet. Ownnewsuccessfulfixturecleanup0payloadfiles;precedingcleanupalreadycountedthere. Noextra cleanup/kill/release/package/tag/trust/security/MSI/dailyinstall/privatecreds/realcalls/pausedscan/Desktopwrites/newexecutor/features/newProjectsend. Continuousauthority remains,allasyncstatusPENDING_OBSERVATIONuntilrawseen;no0executioninferencefromunreadoutput. Oldpathincidentpriorstate/exactUTCUNKNOWN,nofutureaccess. Assets/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED/rootUNKNOWN/PRODUCT_HOLD. User/packageNOTTESTED/independentNOTREQUESTED/newProjectPREPAREDONLY.
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
print(json.dumps(dict(sources=SOURCES,zipBytes=archive.stat().st_size,zipSha256=sha(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,counts=counts,newFixtureCleanupFiles=cleanup[1]['removed']),indent=2))
