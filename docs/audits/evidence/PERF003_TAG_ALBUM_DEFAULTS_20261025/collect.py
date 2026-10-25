from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf003-tag-album-defaults-20261025'
DEST=ROOT/'docs/audits/evidence/PERF003_TAG_ALBUM_DEFAULTS_20261025'
SOURCES={'baseline':'45068dfbf2213f8d8fc2041aa1b4a30e81024efb','final':'52e0bf05a5844f85c59d3bb79a71b7f2c3182858'}
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
assert not DEST.exists();DEST.mkdir(parents=True)
counts={};metrics={};inputs={};cleanup=[]
for kind in ('baseline','final'):
    runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-tag-album-defaults-'+kind+'-20261025'));assert len(runs)==1
    src=runs[0];s=json.loads((src/'summary.json').read_text(encoding='utf-8'))
    assert s['sourceStable'] and s['status']=='PASS'
    assert (s['tests'],s['failures'],s['errors'],s['skipped'])==((1 if kind=='baseline' else 4),0,0,0)
    assert git('diff',SOURCES[kind],s['baseCommit'],'--','modules','tools')==b''
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    counts[kind]={k:s[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    c=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert c['status']=='COMPLETE';cleanup.append(c)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    output=ET.parse(next(src.glob('*TagAlbumDefaultsPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(next(x for x in output.splitlines() if x.startswith('TAG_ALBUM_DEFAULTS=')).split('=',1)[1])
    assert all(r['serializedStateBytes']<=4*1024*1024 for r in rows);assert len(rows)==(12 if kind=='baseline' else 24)
    write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for assets in (128,512,2048):
        group=[r for r in rows if r['assets']==assets];assert len({r['memberships'] for r in group})==1
        for mode in sorted({r.get('kind','actualLegacy') for r in group}):
            selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
            metrics[kind].append(dict(assets=assets,memberships=selected[0]['memberships'],serializedStateBytes=selected[0]['serializedStateBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryCatalogProjection.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TagAlbumDefaultsPerformanceTest.kt'
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines()
assert set(changed)=={production,test}
old=git('show',SOURCES['baseline']+':'+production).decode();new=git('show',SOURCES['final']+':'+production).decode()
assert old.replace('    val tagHashes=mutableMapOf<String,String>()','    val tagHashes=mutableMapOf<String,String>()\n    var defaultRecipe:NovelAiGenerationRecipe?=null',1).replace('NovelAiGenerationHistoryEntry(id="tag:$hash:$id",images=listOf(image))','NovelAiGenerationHistoryEntry(id="tag:$hash:$id",images=listOf(image),recipe=defaultRecipe?:NovelAiGenerationRecipe().also{defaultRecipe=it})',1)==new
testText=git('show',SOURCES['final']+':'+test).decode()
start=old.index('fun galleryTagAlbums(');end=old.index('\n\n/** Visible order',start)
refStart=testText.index('private fun recordedTagAlbums(');refEnd=testText.index('\n\n/** Tag-album',refStart)
assert old[start:end].replace('fun galleryTagAlbums(','private fun recordedTagAlbums(',1)==testText[refStart:refEnd]
authority=['modules/image-authority/src/main/kotlin/com/example/chatbar/domain/image/NovelAiStudioModels.kt','modules/image-authority/src/main/kotlin/com/example/chatbar/domain/image/NovelAiImageGuidance.kt','modules/image-authority/src/main/kotlin/com/example/chatbar/domain/prompt/CharacterNaiPromptDefaults.kt']
identities=[]
for kind in ('baseline','final'):
    raw=(LOCAL/'LibraryCatalogProjection.before.kt').read_bytes() if kind=='baseline' else (ROOT/production).read_bytes()
    fixed=git('show',SOURCES[kind]+':'+production)
    assert sha(raw)==inputs[kind][production] and raw.replace(b'\r\n',b'\n')==fixed
    p=DEST/'inputs'/kind/'LibraryCatalogProjection.kt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
    identities.append(dict(run=kind,path=production,rawInputSha256=sha(raw),gitBlobSha256=sha(fixed)))
    for name in (test,'tools/Run-PreviewChecks.py',*authority):
        b=git('show',SOURCES[kind]+':'+name)
        assert inputs[kind][name] in (sha(b),sha(b.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=inputs[kind][name],gitBlobSha256=sha(b)))
assert git('diff',SOURCES['baseline'],SOURCES['final'],'--',*authority)==b''
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,productionOnlyLazyCallLocalCanonicalDefaultReuse=True,fullOldFunctionReferenceExactExceptName=True,allOtherProductionAndAuthorityTextUnchanged=True,perEntryCreatedAtConstructorPreserved=True,noGlobalCacheOrEntryImageReuse=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=4,baselineUniqueMethods=1,baselineAndRoundsNotAdded=True,package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Pure synthetic management-label metadata <=4MiB;explicit label constraints checked,no private Repository.validate invocation or real store-admission claim','Canonical NovelAiGenerationRecipe constructor remains authority,only immutable default val/empty/null contents reused within one call','Entry constructor and per-entry currentTimeMillis default remain;tests bound each timestamp around call,do not claim distinct timestamp values/count clock invocations','Two warmups each mode/four alternating same-JVM rounds;counter captured before validation/JSON','No wholeUI/native/frame/RSS/peakheap/GC/physicalIO/universalWindows or Prompt acceptance','Direct helper duplicate-tag test preserves old output but is not admitted management-label data']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl'):
    p=LOCAL/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate-v2.ps1',DEST/'resource-gate.ps1');shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Tag-album immutable default recipe, metadata only

Baseline45068df actual1 methodPASS;final52e0bf0 actual4 unique methodsPASS:3 new tag-default methods plus existing multiTagAlbumsHaveUniqueKeysAndUnknownDatesStayUnknown. Baseline1,internal warmups/rounds/fixture sizes and historical checks NOT added to4. No accepted batch9/startup reruns. Production diff only LibraryCatalogProjection galleryTagAlbums local nullable defaultRecipe and canonical constructor argument. Entire old function exact in private reference exceptname. Canonical image-authority Prompt/model/default factory/request code untouched;no manual copy of default Prompt text,request logic or credential identity. This is synthetic management-tag projection metadata,not actual generation or Prompt comparison.

Old per-membership default recipe constructs deterministic immutable settings/guidance/precise defaults and trims canonical constant negative text. New constructor still called canonically once on first membership perprojection;no objects for emptyinput,no global/crosscall cache. Default image remains per-membership,history entry remains per-membership with its original default per-entry createdAt clock evaluation. Do not hoist entry/timestamp/ID or alter recipe values. Tests check full canonical recipe equality,labels/order/firstoccurrence IDdedup/missing or empty-label fallback/key/seed/index/images unchanged,input bytes stable,per-entry timestamps within actual call bounds. Bounds do not prove timestamps all differ/count invocations;unchanged constructor source supports latter. Copied changed metadata cannot affect sibling or subsequent projections;new call gets independent recipe. Duplicate-tag direct helper case deliberately not an admitted-label claim.

128/512/2048 UUID metadata IDs,8tags/labeled asset,missing every13th/empty every11th,repeated input every17th,seed163+count shuffle. Explicit original label count/UUID/tag cardinality/unique/nonblank/length constraints checked and serialized LibraryState <=4MiB;private Repository.validate not invoked,no images/store admission or real assets claimed. Full canonical/default/key assertions and input serialization outside measured counters. Baseline2warmups/4rounds;final2warmups each mode/4alternating same-JVM rounds. Cumulative thread allocation/wall/CPU only,not peakheap/GC/RSS/UI/universalWindows,no zero-CPU claim from coarse timers.

Desktop priority resource gate,two emptyowner checks30seconds apart,lowpriority/singleworker2processors256MiB;boundary samples not continuous quiet/CPU proof. Successful own synthetic roots sealed and retired byexistingrunner;oldfailed/unknown/backups/daily data retained. Prior folder traversal negative cohorts remain NOT ADOPTED,all oldfailures/user rejections kept. No windows/real credentials/services/paid calls/paused collection/daily install/packaging/version/tag/release/trust changes. New Project PREPARED ONLY / NOT SENT,independentNOT REQUESTED,human/packageNOT TESTED;16history index fa58a50 and original Desktop report boundary fixed. Asset/editorUSER_NOT_TESTED,115/PromptPENDING,q09FAILED/rootUNKNOWN,PRODUCT_HOLD / NOT RC remain.
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
