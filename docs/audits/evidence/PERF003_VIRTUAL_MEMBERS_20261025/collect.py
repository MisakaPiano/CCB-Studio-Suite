from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_VIRTUAL_MEMBERS_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'462790ad2d00c5b3e2e10d7e7ea0ab0bbf08faf5','final':'1553d8bdc1e9d63503feae3c90cb4444ac8645ce'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-virtual-members-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 5),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*VirtualMembersPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('VIRTUAL_MEMBERS_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    assert all(r['serializedBytes']<=4*1024*1024 for r in rows)
    metrics[kind]=[]
    for albums,refs in sorted({(r['albums'],r['referencesPerAlbum']) for r in rows}):
        for mode in sorted({r.get('kind','actualLegacy') for r in rows if r['albums']==albums and r['referencesPerAlbum']==refs}):
            selected=[r for r in rows if r['albums']==albums and r['referencesPerAlbum']==refs and r.get('kind','actualLegacy')==mode]
            metrics[kind].append(dict(albums=albums,referencesPerAlbum=refs,totalReferences=selected[0]['totalReferences'],uniqueMembers=selected[0]['uniqueMembers'],serializedBytes=selected[0]['serializedBytes'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
prefix='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/';production=prefix+'VirtualAlbums.kt'
old=subprocess.check_output(['git','show',sources['baseline']+':'+production],cwd=ROOT).decode();new=subprocess.check_output(['git','show',sources['final']+':'+production],cwd=ROOT).decode()
oldLine=next(x for x in old.splitlines() if x.startswith('    fun members('))
start=new.index('    fun members(');end=new.index('    fun save(',start);newBlock=new[start:end].rstrip('\n')
assert old.replace(oldLine,newBlock,1)==new
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/VirtualMembersPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=next(x for x in test.splitlines() if x.startswith('private fun legacyMembers('));assert legacy.replace('private fun legacyMembers(','    fun members(',1).replace('VirtualAlbums.descendants(','descendants(',1)==oldLine
changed=subprocess.check_output(['git','diff','--name-only',sources['baseline'],sources['final'],'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={production,testPath}
identities=[]
for kind in ('baseline','final'):
    for name in [production,testPath,'tools/Run-PreviewChecks.py']:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        assert inputs[kind][name] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=hashlib.sha256(fixed).hexdigest()))
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,productionOnlyMembersBody=True,fullValidationAndDescendantsUnchanged=True,legacyReferenceExactExceptPrivateNameAndQualification=True,finalUniqueMethods=5,baselineRepeatedMethodNotAdded=True,packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=5,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Pure metadata,all LibraryState serialized fixtures <=4MiB,not real galleries','Final two warmups per mode and four same-JVM alternating legacy/current rounds;baseline separate JVM','Ordered first-occurrence union,full validation,unknown ID rejection and 32/33 depth boundary preserved','Set interface/content/order preserved;concrete collection implementation not a promised API','Thread allocated heap not complete UI/RSS/physical IO/universal speed','Existing source-receipt test uses synthetic record only;no provider request','No new UI/schema/asset organization semantics/original movement/cache/update/trust/package change','Single worker/low priority/2processors/256MiB,Desktop gate checks not continuous quiet proof']))
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl'):
    p=ROOT/'.local/perf003-virtual-members-20261025'/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Virtual album membership union

Baseline462790a:one actual method PASS,source stable;32x128,128x512,500x128 synthetic overlapping member references;serializedLibraryState164047/2573003/2562947B,all below existing4MiB budget. Final1553d8b:five unique methods PASS,0fail/error/skip,source stable. Three new methods cover actual allocation plus ordered outputs/immutable serialized metadata;unsorted branches/depth first-occurrence order,unrelated exclusion,empty membership and unknown UUID/string/empty-tree NoSuchElementException;full validation rejects duplicate IDs/images,bad image UUID,missing parent,cycle,unrelated invalid name,33depth while32accepted. Two existing methods cover nested move/remove/reopen without changing originalbytes and immutable generation receipt provenance after queue removal. The latter creates synthetic account/receipt metadata and sends no HTTP. Internal rounds/sizes/branches not separate tests;baseline method not added again.

Only VirtualAlbums.members body changed:descendants performs same full validation,transient ID map replaces repeated single lookup,linkedSet directly accumulates each selected album's member list. Avoids full flattened duplicate-reference ArrayList and toSet capacity based on duplicate total. Same full validation/descendant traversal/first-occurrence order,empty result,unknown key exception,Set content and serialized state. No global cache,model/schema/UI/entry/file organization change,no original copy/move/deletion,same originalbytes and source receipt. Legacy one-line member implementation character-exact except private/name/descendants qualification;all other production file text and all other production modules/tools unchanged.

Final same-JVM four alternating legacy/current rounds per size,two warmups each,counters captured immediately after actual method before JSON/assertion/serialization. Thread cumulative heap allocation and microbenchmark wall time only;fullvalidation UUID parsing remains,no claim of constant memory/no validation cost/wholegallery/frame/RSS/physicalIO/universal speed. CPU coarse ticks and zero samples are not zeroCPU. Fixture images are UUID metadata only in new tests;original existing two tests use tiny synthetic images in freshlyowned isolated roots. Same metadata serialized before/after every call. No paid requests,real galleries/private credentials/paused scan/daily install/window/release/new package/signature/trustchain changes.

Desktop first:single gate checks ownership before starting,one worker/lowpriority/2processors/256MiB;boundary checks are not a continuous quiet-host guarantee. Resource timeouts,if any,have zero executions and are separate from test attempts. This increment has no new failure;all older real failures/user rejections and fixed baselines stay untouched. Only successful own synthetic fixtures retired after external logs sealing;failed/unknown/user/backup roots retained. Independent NOT REQUESTED,user/package NOT TESTED,new Project materials PREPARED ONLY / NOT SENT. No accepted old batch projection/startup baseline rerun. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC persist.
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
