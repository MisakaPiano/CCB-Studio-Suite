from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_FOLDER_DESCENDANTS_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'e3989bc1f5e5f4a99caffee7419e4bb95d8f8b83','final':'01020b1f82bff95dfacdb75f6318b3d2d258c626'};counts={};cleanup=[];metrics={};inputs={}
for kind in ('baseline','final'):
    candidates=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-folder-descendants-'+kind+'-20261025'));assert len(candidates)==1;src=candidates[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['sourceStable'] and summary['status']=='PASS'
    assert subprocess.check_output(['git','diff',sources[kind],summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
    assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 3),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text())
    output=ET.parse(next(src.glob('*OrganizationDescendantsPerformanceTest.xml'))).getroot().findtext('system-out');rows=json.loads(next(x for x in output.splitlines() if x.startswith('FOLDER_DESCENDANTS_ALLOCATION=')).split('=',1)[1]);write(DEST/(kind+'-samples.json'),rows)
    assert all(r['serializedStateBytes']<=4*1024*1024 for r in rows)
    metrics[kind]=[]
    for folders in sorted({r['folders'] for r in rows}):
        for context in ('root','small','leaf','unknown'):
            selectedContext=[r for r in rows if r['folders']==folders and r['context']==context]
            for mode in sorted({r.get('kind','actualLegacy') for r in selectedContext}):
                selected=[r for r in selectedContext if r.get('kind','actualLegacy')==mode]
                assert len({r['orderSha256'] for r in selectedContext})==1
                metrics[kind].append(dict(folders=folders,context=context,members=selected[0]['members'],serializedStateBytes=selected[0]['serializedStateBytes'],orderSha256=selected[0]['orderSha256'],kind=mode,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
production='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt'
oldRawGit=subprocess.check_output(['git','show',sources['baseline']+':'+production],cwd=ROOT);newRawGit=subprocess.check_output(['git','show',sources['final']+':'+production],cwd=ROOT);old=oldRawGit.decode();new=newRawGit.decode()
oldLine=next(x for x in old.splitlines() if x.startswith('    fun descendants('));start=new.index('    fun descendants(');end=new.index('    fun path(',start);newBlock=new[start:end].rstrip('\n')
assert old.replace(oldLine,newBlock,1)==new
finalRaw=(ROOT/production).read_bytes();assert sha(finalRaw)==inputs['final'][production] and finalRaw.replace(b'\r\n',b'\n')==newRawGit
beforeCandidates=[oldRawGit,oldRawGit.replace(b'\n',b'\r\n')]
for after in (newBlock.encode(),newBlock.replace('\n','\r\n').encode()):
    for before in (oldLine.encode(),oldLine.replace('\n','\r\n').encode()):
        if after in finalRaw:beforeCandidates.append(finalRaw.replace(after,before,1))
matches=[b for b in beforeCandidates if sha(b)==inputs['baseline'][production] and b.replace(b'\r\n',b'\n')==oldRawGit];assert matches
rawSources={'baseline':matches[0],'final':finalRaw}
for kind,raw in rawSources.items():
    p=DEST/'inputs'/kind/'GenerationOrganization.kt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
testPath='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/OrganizationDescendantsPerformanceTest.kt';test=subprocess.check_output(['git','show',sources['final']+':'+testPath],cwd=ROOT).decode()
legacy=next(x for x in test.splitlines() if x.startswith('private fun GenerationOrganization.legacyDescendants('))
assert oldLine.replace('    fun descendants(','private fun GenerationOrganization.legacyDescendants(',1).replace('flatMap{descendants(it.id)}','flatMap{legacyDescendants(it.id)}',1)==legacy
changed=subprocess.check_output(['git','diff','--name-only',sources['baseline'],sources['final'],'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={production,testPath}
identities=[]
for kind in ('baseline','final'):
    for name in (production,testPath,'tools/Run-PreviewChecks.py'):
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+name],cwd=ROOT)
        expected=sha(rawSources[kind]) if name==production else inputs[kind][name]
        assert inputs[kind][name]==expected
        if name!=production:assert expected in (sha(fixed),sha(fixed.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,testInputSha256=inputs[kind][name],gitSha256=sha(fixed)))
write(DEST/'SOURCE_MAP.json',dict(sources=sources,executionSource=counts['final']['baseCommit'],baselineExecutionSource=counts['baseline']['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,productionOnlyDescendantsBody=True,allOrganizationValidationPathBatchArchiveQueueAndOtherProductionTextUnchanged=True,legacyBodyExactExceptExtensionPrivateAndRecursiveName=True,rawInputHashAndCanonicalGitBothVerified=True,finalUniqueMethods=3,baselineRepeatedMethodNotAdded=True,cancelledRootOnlyGate='f6d680c;0 test executions;superseded before start',parentAdjacencyIndex='NOT IMPLEMENTED;avoid imposing whole-tree index on small/leaf lookup',remainingConsumerMultiplicity='NOT FIXED in this increment;see notes',packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=3,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Pure validated StudioState folder-node metadata <=4MiB,32depth/1000nodes,no real directory traversal','Full StudioState validation outside timed helper but actually executed before every fixture','Final two warmups per mode/four alternating same-JVM rounds per root/small/leaf/unknown context','Single ordered accumulator,still scans original folder list per visited node;asymptotic scan cost not removed','Set interface/content/DFS iteration preserved,concrete set implementation not promised','No new graph admission inside descendants;original full validator preserved,invalid graphs refused there','Callers repeated per-batch subtree calculation remains an identified separate issue','Thread heap not full UI/RSS/physicalIO/universal speed;no global/parent index/cache']))
local=ROOT/'.local/perf003-folder-descendants-20261025'
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl','ROOT_ONLY_GATE_CANCELLED','PROTOTYPE_AND_CONSUMERS.md','finish_test.py'):
    p=local/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate.ps1',DEST/'resource-gate.ps1');shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Folder node descendants, separate from accepted batch image projection

Root-only sourcef6d680c gate cancelled before any test start;no phase directory,0 Gradle/JUnit executions,cancellation tool exit1 recorded,not a product failure/PASS. Sourcee3989bc expanded same baseline method to root/two-node subtree/leaf/missing root for32/256/1000folders,shuffled seed91+size,full StudioState(version3).validate actually executed before traversal and serialized state<=4MiB. Actual baseline one methodPASS at document executiond30e94d,modules/tools equal e3989bc. Final01020b1 three unique methodsPASS,0fail/error/skip,source stable. No repeated baseline/rounds/sizes/context counts added.

Actual paired old/new four rounds with two warmups/mode per context,expected root membership independent and old full order reference;metadata original bytes/order hash checked after every call. Additional methods hard-code unsorted DFS branch order/subtrees/unrelated exclusion/unknown UUID or arbitrary string/empty graph/reparented independent snapshots;original validator rejects duplicate IDs,invalid UUID/name/kind,missing parent,cycle,33depth and1001folders,32depth accepted. Validation is not removed or moved into helper;public helper still assumes admitted graph as before. Invalid direct traversal is not newly accepted/visited-deduped. Old function body exact except private extension signature and recursive name. Set interface/content/first-occurrence DFS order preserved,not concrete collection class.

Single linked result replaces per-node flatten and set merging;original folder scan/child-order/DFS recursion remains. No parent adjacency map/global/per-state cache/schema/path/batch/archive/manual capacity/queue/UI change. Parent-index option not implemented:baseline leaf/small costs are tiny and whole-tree temporary indexing could impose costs;no unmeasured index performance result claimed. Still O(folders*visited) scan complexity and repeated caller membership predicates remain unaddressed,not whole performance completion. Full production file outside this method and all other production/modules/tools unchanged. Before/final raw sources exactly match executed source hashes and CRLF-only normalization equals fixed Git,source not normalized to satisfy checks.

Bare thread allocation/wall/CPU helper observations,not whole UI/RSS/physicalIO/universalWindows/no-CPU assertions. Timers coarse,zero not zero CPU;metadata serialization/validation/assertions/order hashing outside counter. Desktop first explicit gate,two empty ownership checks30seconds apart,lowpriority/singleworker2processors256MiB,boundaries not continuous quiet proof. Successful own synthetic roots sealed then retired,old failed/unknown/user/backups untouched. No new failure;older failures/rejections preserved. No real folders/images/credentials/providers/paid calls/paused scan/daily install/windows/package/release. Independent NOT REQUESTED,user/package NOT TESTED,new Project material PREPARED ONLY / NOT SENT. Accepted old batch9/startup not rerun. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC remain.
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
