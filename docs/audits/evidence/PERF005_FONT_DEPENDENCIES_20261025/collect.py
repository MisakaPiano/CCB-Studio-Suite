from pathlib import Path
import hashlib,json,os,re,statistics,subprocess,sys,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf005-font-dependencies-20261025'
DEST=ROOT/'docs/audits/evidence/PERF005_FONT_DEPENDENCIES_20261025'
assert len(sys.argv)==2 and re.fullmatch('[a-f0-9]{40}',sys.argv[1]),'Explicit frozen application source required after final runtime'
SOURCES={'baseline':'b049e4c7bdc292a26e5623313c1c1d91933ca9b9','final':sys.argv[1]}
prod='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetFonts.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/DocumentFontDependenciesPerformanceTest.kt'
layout='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/TypesetLayout.kt'
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
    found=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-font-dependencies-'+kind+'-20261025'));assert len(found)==1
    s=json.loads((found[0]/'summary.json').read_text(encoding='utf-8'));assert s['sourceStable'] and s['status']=='PASS'
    assert(s['tests'],s['failures'],s['errors'],s['skipped'])==((1 if kind=='baseline' else 3),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b'';runs[kind]=(found[0],s)
old=git('show',SOURCES['baseline']+':'+prod).decode();new=git('show',SOURCES['final']+':'+prod).decode()
begin=old.index('    fun documentFonts(');end=old.index('    fun verifyRenderable(',begin);function=old[begin:end]
newFunction='''    fun documentFonts(document:TypesetDocument):List<String> {
        val out=ArrayList<String>()
        document.pages.forEach { p->
            out.add(p.story.fontFamily)
            p.texts.forEach { t->
                out.add(t.style.fontFamily)
                TypesetLayout.tokens(t.text,t.style).forEach { out.add(it.style.fontFamily) }
            }
        }
        document.presets.forEach { out.add(it.text.style.fontFamily) }
        return out
    }
'''
assert old[:begin]+newFunction+old[end:]==new
t=git('show',SOURCES['final']+':'+test).decode();reference='private object RecordedDocumentFontDependencies {\n'+function+'}\n'
assert t[t.index('private object RecordedDocumentFontDependencies {'):t.index('\n/** Font family strings')].rstrip()==reference.rstrip()
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines();assert set(changed)=={prod,test}
assert git('show',SOURCES['baseline']+':'+layout)==git('show',SOURCES['final']+':'+layout)
checked(DEST);assert not DEST.exists();DEST.mkdir()
counts={};metrics={};inputs={};cleanup=[];samples={};identities=[]
for kind,(folder,s) in runs.items():
    for p in folder.iterdir():
        if p.is_file():checked(p);store(DEST/'runs'/kind/p.name,p.read_bytes())
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((folder/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((folder/'source-before.json').read_text(encoding='utf-8'))
    out=ET.parse(next(folder.glob('*DocumentFontDependenciesPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in out.splitlines() if x.startswith('DOCUMENT_FONT_DEPENDENCIES=')).split('=',1)[1]);assert len(rows)==(24 if kind=='baseline' else 48)
    assert all(r['serializedDocumentBytes']<=4*1024*1024 and r['fontReferences']==r['pages']*(1+8*(1+512))+8 for r in rows)
    samples[kind]=rows;write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for pages in (1,4,16):
        for context in ('plain','inline'):
            group=[r for r in rows if r['pages']==pages and r['context']==context];assert len({r['fontOrderSha256'] for r in group})==1
            for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                metrics[kind].append(dict(pages=pages,context=context,kind=mode,texts=selected[0]['texts'],fontReferences=selected[0]['fontReferences'],serializedDocumentBytes=selected[0]['serializedDocumentBytes'],fontOrderSha256=selected[0]['fontOrderSha256'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
    raw=(LOCAL/'TypesetFonts.before.kt').read_bytes() if kind=='baseline' else (ROOT/prod).read_bytes()
    b=git('show',SOURCES[kind]+':'+prod);assert sha(raw)==inputs[kind][prod] and raw.replace(b'\r\n',b'\n')==b
    store(DEST/'inputs'/kind/'TypesetFonts.kt',raw);identities.append(dict(run=kind,path=prod,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
    for name in (test,'tools/Run-PreviewChecks.py'):
        b=git('show',SOURCES[kind]+':'+name);assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
for pages in (1,4,16):
    for context in ('plain','inline'):
        assert len({r['fontOrderSha256'] for rows in samples.values() for r in rows if r['pages']==pages and r['context']==context})==1
raw=(ROOT/layout).read_bytes();b=git('show',SOURCES['baseline']+':'+layout);assert raw.replace(b'\r\n',b'\n')==b and all(inputs[kind][layout]==sha(raw) for kind in ('baseline','final'))
store(DEST/'inputs'/'shared'/'TypesetLayout.kt',raw);identities.append(dict(run='both',path=layout,rawInputSha256=sha(raw),gitBlobSha256=sha(b)))
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,executions={k:v['baseCommit'] for k,v in counts.items()},identities=identities,changedModulePaths=changed,productionOnlyDocumentFontsBodyAggregation=True,fullActualTokensParsingOrderDuplicatesAndPresetBaseOnlyPreserved=True,fontLoadingCacheActivateVerifyRenderableAndAllOtherProductionUnchanged=True,recordedDocumentFontsExpressionExact=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=3,baselineUniqueMethods=1,noMethodsCasesThreadsRoundsSummed=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Only documentFonts family string aggregation and complete actual tokens parsing,no fontload/activate/resolve/measure/render/image/window/workspace','1/4/16pages8texts/page512codepoints/text8presets,synthetic font aliases stringsonly;document.validate/serialized<=4MiB/order/fullList/docbytes outsidecounter','Same public List values/order not concrete implementation or unsupported mutable casts','2warmups eachmode/4alternating rounds;no cold/firstuse/peakheap/GC/RSS/wholeeditor/export/actual font backup admission claims','Hidden/empty/story/presetbaseonly/unknown/deep/overlong parsing andinputdocument preserved;invalidhelperpreset not documentadmission','Two ownthreads50rounds steady-state snapshots not all platforms/initraces/longrun']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl','prepare-final.py','CALLER_BOUNDARY.json'):
    p=LOCAL/name
    if p.exists():checked(p);store(DEST/name,p.read_bytes())
store(DEST/'resource-gate.ps1',(ROOT/'.local/performance-resource-gate-v2.ps1').read_bytes());store(DEST/'collect.py',Path(__file__).read_bytes())
store(DEST/'ATTEMPTS_AND_LIMITS.md',b'''# Document font dependency aggregation

Baseline b049e4c actual1method andfinalexplicitfrozen application source actual3unique methods;baseline/sizes/contexts/rounds/cases/threads notadded. Production onlydocumentFonts aggregationbody,exactstoryfont->eachtextbase->eachactualtokensfont->presetsbase,all duplicates/order preserved. No skips/dedup/aliasnormalization/crosscallcache. Fulloriginaltokens parse anderrors/length/depthbudget remain. Oldexpressionreference exact,no fontloading/cachecopy. Existingloadedfontcache/activation/renderability andotherproduction unchanged. Public Listvalues/order semantics,not concreteclass/mutablecast compatibility.

Synthetic1/4/16pagesx8texts512codepoints,plain/inlinefonttags8presets,completeTypesetDocument.validate/serialized<=4MiB/fullfontreferenceorder/hash/documentbyte snapshots outsidecounter. Fakefontaliases onlystrings,not registered/loaded/activated/resolved/installed/measured/rendered. Objectcacheinitoutsidecounter,no workspace/assets/privatefonts/providers/userdata. ActualdocumentFonts timedincludescompleteactualtokens parsing/aggregation,notonlylist appendorfulldocumentvalidation. Independenttinyfontordervector;edgeempty/hidden/locked/story/presetbase-only/unknown/deep/overlong errors,mutablecopyisolation;invalidpresethelpertextnotdocumentadmission.2ownthreads50rounds,notallplatforms/longrun.

2warmups eachmode/4alternating sameJVM rounds;threadallocation/localwall/cpu only,CPU0coarse notzeroCPU. No cold/firstuse/retained/peakheap/GC/RSS/wholeeditor/image/font/export/backupadmission claims. Desktoplatestowner2emptychecks30secapart/lowprioritysingleworker2CPU256MiB,busywaitandboundariesonly,notwholewindowquiet. AllasynchronouspendingstatesPENDING_OBSERVATION,no0executioninferencewithoutclearreceipt. Resourcegateexpired0executions ifany separatelyrecorded,no automaticPASS.

Existingratioexperiment e837880/2c35296/e0ae1d4 NOTADOPTED andloosewallregression/oldfailurevector772a66c/asyncobservationmisreports/rejectedtraversals/userrejections/pathincidentUNKNOWN retained. No oldacceptedbatch9/native/startup/richtag/vertical/classification/ratio suitesrerun. No newProjectsend/executor/features/unified-image-tool/privatecreds/realcalls/pausedscan/dailyinstall/package/tag/release/trust/security/MSI/Desktopwrites. Continuousauthority stays,alltempSuiteabsoluteownership/conflict/linkpreflight,nofuturewrongpathaccess. Asset/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED/rootUNKNOWN/PRODUCT_HOLD;user/packageNOTTESTED/independentNOTREQUESTED/newProjectPREPAREDONLY.
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
