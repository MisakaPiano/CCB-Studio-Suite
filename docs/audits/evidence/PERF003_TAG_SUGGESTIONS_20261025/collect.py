from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_TAG_SUGGESTIONS_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(raw):return hashlib.sha256(raw).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'f996e4ed48529af204ab6e6be9ef0ff5cf19c4bd','final':'fa58a50c44a592df0f0c8745ba773c244d7442d9'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-tag-suggestions-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 4),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*TagSuggestionsPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('TAG_SUGGESTIONS_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    assert all(r['serializedBytes']<=4*1024*1024 for r in rows)
    metrics[kind]=[]
    for assets in sorted({r['assets'] for r in rows}):
        for mode in sorted({r.get('kind','actualLegacy') for r in rows if r['assets']==assets}):
            selected=[r for r in rows if r['assets']==assets and r.get('kind','actualLegacy')==mode]
            metrics[kind].append(dict(assets=assets,tagCount=selected[0]['tagCount'],matchedTags=selected[0]['matchedTags'],serializedBytes=selected[0]['serializedBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
prefix='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/'
tagPath=prefix+'LibraryTagIndex.kt';sortPath=prefix+'GallerySorting.kt'
oldTag=subprocess.check_output(['git','show',sources['baseline']+':'+tagPath],cwd=ROOT).decode();newTag=subprocess.check_output(['git','show',sources['final']+':'+tagPath],cwd=ROOT).decode()
oldMethod=oldTag[oldTag.index('    fun suggestions('):oldTag.index('\n}\nfun matchesLibraryTags(')]
newMethod=newTag[newTag.index('    fun suggestions('):newTag.index('\n}\nfun matchesLibraryTags(')]
assert oldTag.replace(oldMethod,newMethod,1)==newTag
oldSort=subprocess.check_output(['git','show',sources['baseline']+':'+sortPath],cwd=ROOT).decode();newSort=subprocess.check_output(['git','show',sources['final']+':'+sortPath],cwd=ROOT).decode()
helper='''/** One synchronous sort owns its keys; no cross-query or retained corpus cache. */
internal fun galleryNaturalComparator():Comparator<String> {
    val keys=HashMap<String,List<String>>()
    return Comparator{a,b->compareNaturalKeys(keys.getOrPut(a){naturalKey(a)},keys.getOrPut(b){naturalKey(b)})}
}
'''
assert oldSort.replace('private fun compareNaturalKeys(',helper+'private fun compareNaturalKeys(',1)==newSort
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/TagSuggestionsPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=test[test.index('private fun legacySuggestions('):test.index('/** Management-label suggestion')].rstrip('\n')
assert oldMethod.replace('    fun suggestions(text:String):List<String> =','private fun legacySuggestions(counts:Map<String,Int>,text:String):List<String> =',1)==legacy
changed=subprocess.check_output(['git','diff','--name-only',sources['baseline'],sources['final'],'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={tagPath,sortPath,testPath}
identities=[]
for kind in ('baseline','final'):
    for name in [tagPath,sortPath,testPath,'tools/Run-PreviewChecks.py']:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (sha(fixed),sha(fixed.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=sha(fixed)))
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,onlySuggestionsAndCallLocalComparatorAddition=True,existingNaturalKeyAndCompareAlgorithmsAndAllOtherProductionTextUnchanged=True,legacySuggestionsExactExceptPrivateSignatureAndCountsParameter=True,constructorCountsMatchingSelectionUnchanged=True,finalUniqueMethods=4,baselineRepeatedMethodNotAdded=True,packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=4,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Pure management labels,LibraryState fixtures <=4MiB,fixed shuffled seed71+size,query-space-tag-space','Index construction/serialization/output verification outside measured query','Final two warmups per mode/four same-JVM alternating rounds;baseline separate JVM','Prefix-first/natural keys/stable ties/case/trim/Unicode/source scope preserved','Natural key map is call-local,temporary live keys scale with matched tags;peak heap/retention/GC/RSS not measured','Reduced cumulative thread allocation not zero allocation/wholeUI/physicalIO/universal speed','Desktop24008 created during final interval,exact CPU/JUnit-sample overlap UNKNOWN,not quiet host','Single worker/lowpriority2processors256MiB,no user data/services/UI/package']))
local=ROOT/'.local/perf003-tag-suggestions-20261025'
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl','finish_test.py'):
    p=local/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate.ps1',DEST/'resource-gate.ps1')
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Management tag suggestion sorting

Baselinef996e4e:one actual suggestions API method PASS,128/1024/4096 assets,three valid tags(tagN/stageN/shared),fixed shuffled input seed71+size;query " tag " matches256/2048/8192 tags,prefix tags first then natural numeric order. LibraryState11391/92079/374703B within4MiB. Finalfa58a50:four unique methods PASS,0fail/error/skip,source stable. Three new methods cover actual paired allocation/result/unchanged serialized metadata;18 representative strings every pair old/new exact comparison and stable padded/case ties with Unicode/Arabic digits/mixed suffix/empty;query prefix priority/case/trim/Unicode/empty/unmatched/repeated independent queries/scopes/count/source metadata unchanged. One existing method covers scoped unique counts/predictions/exact/any/all tag matching. Pair matrix/rounds/sizes/tags are not additional tests;baseline not added again.

Only suggestions body and one internal comparator factory added. Query trimmed once,old case-insensitive contains and prefix-first logic retained;comparison uses the very same private naturalKey/compareNaturalKeys functions. Map of original string to natural token list is owned by each synchronous sort and never stored in a field/global/cross-query structure;returned result list does not capture comparator. Existing galleryNaturalCompare,Regex/Locale/numeric trimming/tie algorithm unchanged,all constructor/count/matching/selection and other production text unchanged. Old suggestion body exact except private signature/counts parameter in final test. No original Prompt tags/request/UI/model/date/grouping/identity/source-file behavior touched.

Each size2warmups per mode/4alternating same-JVM legacy/current rounds,counters immediately after query before assertion/JSON/serialization. Index construction outside timing. Cumulative thread heap/wall/CPU samples only,not wholeUI/RSS/physicalIO/universal speed or zero allocation. Temporary live derived-key map O(matched tags) is a memory tradeoff;peak live heap/GC timing/RSS not measured. No global/cross-query retention but immediate memory return toOS not claimed. Known padded query/three-tag/shuffled distribution is synthetic,not all user query shapes;empty/single matches may add tiny factory/map overhead.

One explicit gate,two empty Desktop owner checks30seconds apart,lowpriority/singleworker2processors256MiB. Desktop24008 creation01:33:58.725884Z falls in final01:33:43.971816Z..+20.922s;later observation records PID and creation but exact CPU/JUnit-sample overlap unknown. Not continuous quiet host,not attribution of speed changes to background. No unrelated process/security/global Locale/private data/real credentials/provider/paid calls/user paused scan/daily app/window/package/release. Fresh success synthetic fixture roots sealed then retired,payload files0;old failure/unknown/user/backups retained. No new failure,older failures/rejections unchanged. Independent NOT REQUESTED,user/package NOT TESTED,new Project materials PREPARED ONLY / NOT SENT;accepted old batch/startup not rerun. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC persist.
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
print(json.dumps(dict(sources=sources,zipBytes=archive.stat().st_size,zipSha256=sha(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup)),indent=2))
