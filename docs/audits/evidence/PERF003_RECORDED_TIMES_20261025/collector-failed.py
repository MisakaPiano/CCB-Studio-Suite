from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_RECORDED_TIMES_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'e66aeaeecbca6a6ccbc9b4ec5e70a97a4dde8945','final':'492f8cc17d260a826a5a35019976dd90e450b7c4'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-recorded-times-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 4),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*LibraryRecordedTimesPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('RECORDED_TIMES_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    assert all(r['recipeBytes']<=16384 for r in rows)
    metrics[kind]=[]
    for assets,padding in sorted({(r['assets'],r['paddingBytes']) for r in rows}):
        for mode in sorted({r.get('kind','actualLegacy') for r in rows if r['assets']==assets and r['paddingBytes']==padding}):
            selected=[r for r in rows if r['assets']==assets and r['paddingBytes']==padding and r.get('kind','actualLegacy')==mode]
            metrics[kind].append(dict(assets=assets,paddingBytes=padding,recipeBytes=selected[0]['recipeBytes'],totalRecipeBytes=selected[0]['totalRecipeBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/LibraryCatalogProjection.kt'
old=subprocess.check_output(['git','show',sources['baseline']+':'+production],cwd=ROOT).decode();new=subprocess.check_output(['git','show',sources['final']+':'+production],cwd=ROOT).decode()
oldBlock='        if(a.request().recipe().size>16384)return@runCatching null\n        val receipt=Json.parseToJsonElement(a.request().recipe().decodeToString()).jsonObject'
newBlock='        val opaque=a.request().recipe()\n        if(opaque.size>16384)return@runCatching null\n        val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject'
assert old.replace(oldBlock,newBlock,1)==new
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/LibraryRecordedTimesPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=test[test.index('private fun legacyRecordedTimes('):test.index('/** Catalog metadata')].rstrip('\n')
original=old[old.index('fun libraryRecordedTimes('):old.index('\nfun libraryDateMatches(')].replace('fun libraryRecordedTimes(','private fun legacyRecordedTimes(',1).rstrip('\n');assert legacy==original
changed=subprocess.check_output(['git','diff','--name-only',sources['baseline'],sources['final'],'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={production,testPath}
identities=[]
assetApi='modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetStore.java'
for kind in ('baseline','final'):
    for name in [production,testPath,assetApi,'tools/Run-PreviewChecks.py']:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=hashlib.sha256(fixed).hexdigest()))
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,productionOnlySecondRecipeAccessorAvoided=True,fullJsonParsingBudgetRunCatchingDateAndJobsUnchanged=True,defensiveAssetApiUnchanged=True,legacyFunctionExactExceptPrivateName=True,finalUniqueMethods=4,baselineRepeatedMethodNotAdded=True,packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=4,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Constructed Asset/Request catalog metadata only,no image payload/store admission/original integrity claim','Per-recipe <=16KiB budget;aggregate catalog metadata not a single LibraryState or4MiB document','Final two warmups per mode and four same-JVM alternating legacy/current rounds;baseline separate JVM','Same defensive getter and complete JSON parse;no borrowed immutable internal array/cache','Unknown/invalid/oversize dates remain absent;zero/negative values and job last-wins/order preserve old behavior','Thread allocated heap not complete UI/RSS/physical IO/universal speed','Desktop process43340 created during baseline;exact method CPU activity unknown,not quiet host','Single worker/low priority/2processors/256MiB,no private data/real calls/paused scans/package']))
local=ROOT/'.local/perf003-recorded-times-20261025'
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl','finish_test.py'):
    p=local/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate.ps1',DEST/'resource-gate.ps1')
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Recorded date projection and opaque metadata copies

Baselinee66aeae:one actual API method PASS with constructed128/1024/1024 Asset+Request catalog records,414/1182/12446B each recipe,source stable. Not real admitted images:payloads do not exist,declared1byte length and zero fingerprint are synthetic record fields only. This measures metadata-only public libraryRecordedTimes;no store opens/reads/writes/integrity claims. Per-record recipe<=16KiB;aggregate catalog records are not one persisted4MiB LibraryState. Final492f8cc:four unique methods PASS,0fail/error/skip,source stable. Three new methods cover actual paired allocation/content/order and unchanged recipebytes;wrong kind/version/missing/bad timestamp/array/null/malformed/empty/16384accepted vs16385refused/zero/negative/duplicate catalog ID/job-composed overrides and last-wins map order exact old result;constructor and recipe getter defensive cloning after caller-array mutation plus independently rebuilt date maps/empty catalog. One existing method covers unknown-date no invented defaults,invalid date refusal and unchanged multi-tag album identities. Synthetic job account SYNTHETIC endpoint only,unused recipe;no provider/HTTP/client instantiated. Baseline/rounds/sizes/input variants not added as unique tests.

Only two accessor expressions in recorded date import projection replaced by one local defensive copy used for length and decode. Full16KiB budget,runCatching,decode/Json object and kind/version/required long timestamp checks,imported map order and duplicate/last-wins job override all character-identical. Java Request still clones on construction and each recipe getter,no API/backing-array exposure/change. Legacy whole function character-exact except private/name. Entire remaining production file and all other modules/tools unchanged. No generalized metadata parser rewrite/cache/schema/Prompt/request/date UI/organization/update/signature/trust change.

Two warmups each/four alternating same-JVM legacy/current rounds;counter immediately after projection before JSON/assertions/recipe verification. Thread cumulative heap only,not wholeUI/RSS/physicalIO/real-gallery/universal speed. Huge optional synthetic padding deliberately exercises allowed opaque metadata size,not a claim about normal recipe distributions. Separate-JVM actual baseline and final paired samples distinguished. Full JSON parse/allocation remains;zero allocation or skipped verification not claimed.

Resource gate is an explicit one-shot command,not automation;two empty Desktop ownership checks30seconds apart then lowpriority/single worker2processors256MiB. Desktop43340 creation01:15:14.690271Z falls inside baseline01:15:13.707946Z..+17.046s;later observation retains PID/time,but exact CPU/JUnit-sample overlap unknown. No sustained quiet-host/speed claim and no unrelated process/security action. Final boundary observations recorded separately. Successful fresh own fixture roots sealed then retired,payload file count0;unknown/oldfailed/user/backup content retained. No new failure,older real failures/rejections stay. Independent NOT REQUESTED,user/package NOT TESTED,new Project materials PREPARED ONLY / NOT SENT. No original image operations/private data/credentials/real service/paused scan/daily app/window/package/release. Accepted old batch/startup baselines not rerun. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC persist.
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
