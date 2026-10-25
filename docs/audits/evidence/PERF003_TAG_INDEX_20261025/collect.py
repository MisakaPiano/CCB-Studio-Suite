from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_TAG_INDEX_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'c82b463ba06f00f0251458f724fcc924622b1b35','final':'490c59686870d6acdfce03b7bad7b1034160c97c'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-tag-index-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 4),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*LibraryTagIndexPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('TAG_INDEX_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    assert all(r['serializedBytes']<=4*1024*1024 for r in rows)
    metrics[kind]=[]
    for assets in sorted({r['uniqueLabeledAssets'] for r in rows}):
        for mode in sorted({r.get('kind','actualLegacy') for r in rows if r['uniqueLabeledAssets']==assets}):
            selected=[r for r in rows if r['uniqueLabeledAssets']==assets and r.get('kind','actualLegacy')==mode]
            metrics[kind].append(dict(uniqueLabeledAssets=assets,scopeReferences=selected[0]['scopeReferences'],tagsPerAsset=16,distinctTags=64,serializedBytes=selected[0]['serializedBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryTagIndex.kt'
old=subprocess.check_output(['git','show',sources['baseline']+':'+production],cwd=ROOT).decode();new=subprocess.check_output(['git','show',sources['final']+':'+production],cwd=ROOT).decode()
oldStart=old.index('    val assetCount');oldEnd=old.index('    fun suggestions(');newStart=new.index('    val assetCount');newEnd=new.index('    fun suggestions(')
assert old[:oldStart]+new[newStart:newEnd]+old[oldEnd:]==new
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/LibraryTagIndexPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=test[test.index('private class LegacyLibraryTagIndex('):test.index('/** Scoped management')].rstrip('\n')
original=old[old.index('class LibraryTagIndex('):old.index('\nfun matchesLibraryTags(')].replace('class LibraryTagIndex(','private class LegacyLibraryTagIndex(',1).rstrip('\n');assert legacy==original
changed=subprocess.check_output(['git','diff','--name-only',sources['baseline'],sources['final'],'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={production,testPath}
identities=[]
for kind in ('baseline','final'):
    for name in [production,testPath,'tools/Run-PreviewChecks.py']:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=hashlib.sha256(fixed).hexdigest()))
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,productionOnlyConstructorInitialization=True,suggestionsMatchingAndRangeSelectionTextUnchanged=True,legacyClassExactExceptPrivateName=True,finalUniqueMethods=4,baselineRepeatedMethodNotAdded=True,packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=4,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),preparationFailure='Explicit UTF-8 correction;0 Gradle/JUnit executions,see PREPARATION_FAILURE.md',limits=['Pure management-label metadata,all LibraryState serialized fixtures <=4MiB,no Prompt tags/real galleries','Final two warmups per mode and four same-JVM alternating legacy/current rounds;baseline separate JVM','Unique scoped asset count,one count per asset/tag,case-sensitive identity,insertion order,matching and suggestions unchanged','Per-asset HashSet reset each asset;ID set and scratch set constructor-local,not retained cache','Thread allocated heap not whole UI/RSS/physical IO/universal speed','Single worker/low priority/2processors/256MiB,Desktop gate checks not continuous quiet proof']))
local=ROOT/'.local/perf003-tag-index-20261025'
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl','PREPARATION_FAILURE.md','finish_test.py'):
    p=local/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Scoped management tag index

Baselinec82b463:one actual API method PASS,128/1024/8192 unique labeled assets,16tags each/64distincttags,scope repeated quarter IDs plus one missing-label ID,serializedLibraryState23963/191067/1527899B within4MiB. Final490c596:four unique methods PASS,0fail/error/skip,source stable. Three new methods cover actual paired allocation/count/order/serialized input unchanged;duplicate asset references/duplicate per-asset tags/case-sensitive tag identity/first-key order/out-of-scope exclusion/missing labels/Unicode/natural suggestion order/trim and case behavior exact old equivalence;empty/unknown scopes and rebuilt snapshots after mutable inputs change,no cross-scope/cache contamination. One existing tag prediction test covers unique scoped counts,exact/any/all matching and suggestions. Rounds/sizes/tags are not separate tests;baseline not added again.

Only LibraryTagIndex constructor initialization changed:one temporary unique-ID set instead of two,one constructor-local reusable per-asset seen set,linked count map directly updated in original first-occurrence order. Same per-asset duplicate elimination/case-sensitive String equality,missing-label IDs still count as scoped assets but contribute no tags. Scratch set cleared before each asset;no source mutation/retained corpus/global cache. The returned map remains a snapshot. All suggestions,exact tag matching and selection code character-identical,all other production modules/tools unchanged. Full old class retained in final test character-exact except private/name. Input aliases and original Prompt tags never participate. No UI/model/data ownership/file-operation/update/trust changes.

Two warmups each/four alternating same-JVM rounds at each size,counter captured immediately after constructor before JSON/assertions/input serialization. Bare metadata constructor thread heap/wall/CPU only,not complete gallery responsiveness/RSS/physicalIO/universal speed. Unavoidable ID sets/count maps and boxed counts remain;not zero allocation. Baseline original API has own separate JVM. No image reads/writes,files,service or private data in any selected method. The UTF-8 preparation failure is recorded separately with0 tests;first script stopped before changing the baseline test and was corrected,not relabeled as a product PASS/FAIL. No Gradle/JUnit failure in this increment,all previous failures/rejections unchanged.

Desktop first;two empty ownership checks30seconds apart before starting,low priority/single worker2processors256MiB. Boundary emptiness is not continuous quiet-host proof. No unrelated process termination/security changes,real galleries/credentials/paid requests/paused scans/daily install/windows/package/release. Successful own fixture roots sealed then retired;failure/unknown/user/backups untouched. Independent NOT REQUESTED,user/package NOT TESTED,new Project materials PREPARED ONLY / NOT SENT. Accepted batch projection/startup not rerun. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC remain.
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
