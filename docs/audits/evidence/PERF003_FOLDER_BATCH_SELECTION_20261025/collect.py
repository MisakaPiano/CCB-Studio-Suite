from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/perf003-folder-batches-20261025'
DEST=ROOT/'docs/audits/evidence/PERF003_FOLDER_BATCH_SELECTION_20261025'
SOURCES={'baseline':'974e1007da207a47a27153cb34aec7380c98ff69','final':'1256c64ddd4291f585be4ff8d086f98a404a972b'}
def sha(b):return hashlib.sha256(b).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
assert not DEST.exists();DEST.mkdir(parents=True)
counts={};metrics={};inputs={};cleanup=[]
for kind in ('baseline','final'):
    runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-folder-batches-'+kind+'-20261025'));assert len(runs)==1
    src=runs[0];summary=json.loads((src/'summary.json').read_text(encoding='utf-8'))
    assert summary['sourceStable'] and summary['status']=='PASS'
    assert (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((3 if kind=='baseline' else 4),0,0,0)
    assert git('diff',SOURCES[kind],summary['baseCommit'],'--','modules','tools')==b''
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','baseCommit','seconds','startedUtc','sourceStable')}
    clean=json.loads((src/'fixture-cleanup.json').read_text(encoding='utf-8'));assert clean['status']=='COMPLETE';cleanup.append(clean)
    inputs[kind]=json.loads((src/'source-before.json').read_text(encoding='utf-8'))
    xml=next(src.glob('*FolderBatchProjectionPerformanceTest.xml'))
    output=ET.parse(xml).getroot().findtext('system-out')
    rows=json.loads(next(x for x in output.splitlines() if x.startswith('FOLDER_BATCH_PREDICATES=')).split('=',1)[1])
    assert all(r['serializedStateBytes']<=4*1024*1024 for r in rows)
    assert len(rows)==(48 if kind=='baseline' else 96)
    write(DEST/(kind+'-samples.json'),rows);metrics[kind]=[]
    for folders in sorted({r['folders'] for r in rows}):
        for context in ('root','leaf'):
            for archived in (False,True):
                group=[r for r in rows if r['folders']==folders and r['context']==context and r['archivedOnly']==archived]
                assert len({r['selected'] for r in group})==1
                for mode in sorted({r.get('kind','actualLegacy') for r in group}):
                    selected=[r for r in group if r.get('kind','actualLegacy')==mode];assert len(selected)==4
                    metrics[kind].append(dict(folders=folders,batches=selected[0]['batches'],context=context,archivedOnly=archived,kind=mode,selected=selected[0]['selected'],serializedStateBytes=selected[0]['serializedStateBytes'],medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
core='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/GenerationOrganization.kt'
cards='modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryOrganization.kt'
view='modules/studio-desktop/src/main/kotlin/io/github/misakapiano/ccbstudio/desktop/GalleryWorkspace.kt'
test='modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/FolderBatchProjectionPerformanceTest.kt'
desktopTest='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/FolderSubtreeCardProjectionTest.kt'
changed=git('diff','--name-only',SOURCES['baseline'],SOURCES['final'],'--','modules','tools').decode().splitlines()
assert set(changed)=={core,cards,view,test,desktopTest}
identities=[]
for name in (core,cards,view):
    for kind in ('baseline','final'):
        raw=(LOCAL/'before'/Path(name).name).read_bytes() if kind=='baseline' else (ROOT/name).read_bytes()
        fixed=git('show',SOURCES[kind]+':'+name)
        assert sha(raw)==inputs[kind][name] and raw.replace(b'\r\n',b'\n')==fixed
        p=DEST/'inputs'/kind/Path(name).name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
        identities.append(dict(run=kind,path=name,rawInputSha256=sha(raw),gitBlobSha256=sha(fixed)))
old=git('show',SOURCES['baseline']+':'+core).decode();new=git('show',SOURCES['final']+':'+core).decode()
helper=new[new.index('    /** Same lazy eligibility'):new.index('    fun path(')]
assert old.replace('    fun path(',helper+'    fun path(',1)==new
oldCards=git('show',SOURCES['baseline']+':'+cards).decode();newCards=git('show',SOURCES['final']+':'+cards).decode()
assert oldCards.count('org.batches.filter{it.parent in org.descendants(f.id)}')==2
assert oldCards.replace('org.batches.filter{it.parent in org.descendants(f.id)}','org.batchesInSubtree(f.id)').replace('org.batches.filter{it.parent in org.descendants(node.id)}','org.batchesInSubtree(node.id)')==newCards
oldView=git('show',SOURCES['baseline']+':'+view).decode();newView=git('show',SOURCES['final']+':'+view).decode()
before='        "organizations","folder"->c.state.generationOrganization.batches.filter{it.archivedAt!=null&&(g.organizationKind=="organizations"||it.parent in c.state.generationOrganization.descendants(g.organizationId!!))}.flatMap{batchIndex.images(it.id)}.toSet()'
after='        "organizations","folder"->{val org=c.state.generationOrganization\n            val batches=if(g.organizationKind=="organizations")org.batches.filter{it.archivedAt!=null}else org.batchesInSubtree(g.organizationId,archivedOnly=true)\n            batches.flatMap{batchIndex.images(it.id)}.toSet()}'
assert oldView.replace(before,after,1)==newView
oldTest=git('show',SOURCES['baseline']+':'+test).decode();newTest=git('show',SOURCES['final']+':'+test).decode()
def reference(s):return s[s.index('private fun recordedFolderBatchFilter'):s.index('\n\nclass FolderBatchProjectionPerformanceTest')]
assert reference(oldTest)==reference(newTest)
for kind in ('baseline','final'):
    for name in (test,'tools/Run-PreviewChecks.py')+((desktopTest,) if kind=='final' else ()):
        fixed=git('show',SOURCES[kind]+':'+name);expected=inputs[kind][name]
        assert expected in (sha(fixed),sha(fixed.replace(b'\n',b'\r\n')))
        identities.append(dict(run=kind,path=name,rawInputSha256=expected,gitBlobSha256=sha(fixed)))
write(DEST/'SOURCE_MAP.json',dict(sources=SOURCES,executionSource=counts['final']['baseCommit'],identities=identities,changedModulePaths=changed,originalDescendantsAndValidationUnchanged=True,onlyThreeProductionFilesChanged=True,onlyHelperInsertionAndFourConsumerReplacements=True,legacyReferenceTextUnchanged=True,rawInputsAndCanonicalGitVerified=True,finalUniqueMethods=4,baselineUniqueMethods=3,baselineAndRoundsNotAdded=True,priorTraversalCandidates='NOT ADOPTED;negative evidence124bd4e retained',oldAcceptedBatchNineAndStartup='NOT RERUN',package='NONE',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Pure synthetic metadata/full StudioState validation <=4MiB;no assets/payloads or store admission claim','Two warmups per mode/four alternating same-JVM rounds;counter captured before JSON/assertions','No UI/frame/RSS/peakheap/GC/physicalIO/universal speed measurements','Original traversal unchanged;per-selection subtree allocation only once,not a global/per-state cache','Folder cards/tree/breadcrumb retain inclusion of current+archived metadata;view filter retains archived-only','Null ID eligibility short circuit and batch input order preserved','Pure desktop card adapter run;no native tree/breadcrumb/whole-view interaction test','Prior failed/rejected cohorts preserved;not combined into final method count']))
for name in ('PLAN.md','resource-deferrals.jsonl','resource-boundaries.jsonl'):
    p=LOCAL/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate-v2.ps1',DEST/'resource-gate.ps1')
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Folder batch selection increment, not a traversal rewrite

Baseline974e100 executed3 unique methods:one repeated membership predicate benchmark plus2 retained GenerationFolderContractTest methods. Final1256c64 executed4 unique methods:3 core helper methods and1 pure desktop folder-card metadata adapter. Baseline/contracts/measurement rounds/fixture sizes are NOT added to final4. Accepted old batch image-index9 and native/startup baselines not rerun. All source inputs stable;raw production bytes match source-before and CRLF-only normalization matches fixedGit. Changed production limited to inserted helper and four consumer expressions;original descendants,validation,schema,GalleryBatchIndex,paths/labels,queue/capacity/archive storage/remember/layout remain unchanged.

Caller originally computed same subtree for every eligible batch. New helper computes it once per nonempty eligible selection and filters in original batch order. ArchivedOnly defaultfalse intentionally retains existing tree/card/breadcrumb inclusion of unarchived metadata;view folder filtertrue retains original archived-only condition. Organizations root still direct archived filter. Nullable ID is intentional:empty/noarchived eligibility returns empty before dereference;nonempty eligible null still NullPointerException as old. No new graph validation/caching/index or deleted membership checks. Additional tests cover input order/archive/unplaced/null/unknown/empty/independent reparented snapshots;pure card test covers nested root/current+archived IDs/row order and reparented snapshots. Does not automate native tree/breadcrumb/whole-view interaction or user acceptance.

32/128/256 folders,64/256/512 batches,shuffled input,both root/leaf and archived policies,synthetic image UUID metadata only. Full StudioState(version3).validate and <=4MiB before timing,state bytes verified after every call. Expected IDs independent from traversal. Two warmups each old/current,four alternating rounds per context. Counter captured immediately after API before assertions/serialization. Cumulative thread allocation/wall/CPU only,not peak heap/RSS/GC/UI response/universal Windows. Original single traversal's asymptotic scans remain;earlier single-accumulator traversal candidates rejected after inconsistent lowwarm wall results,negative reports and raw evidence124bd4e unchanged. Current change removes repeated calls and does not rehabilitate those candidates.

Desktop has resource priority;two empty owner checks30seconds apart before lowpriority singleworker tests. This is not proof of continuous desktop quiet/CPU absence. Any observed process overlap recorded separately,CPU attribution UNKNOWN. Successful own synthetic roots sealed then retired by existing runner;old failures/unknown/backups/daily data not touched. No real images/directories/credentials/providers/paid calls/paused scanning/daily install/windows/package/release/trust changes. New Project material PREPARED ONLY / NOT SENT;independent NOT REQUESTED,user/package NOT TESTED. Assets/editor USER_NOT_TESTED,115experience/Promptcompare PENDING,q09 FAILED/root UNKNOWN,PRODUCT_HOLD / NOT RC remain. Existing release/update/cleanup evidence and original Desktop handoff stay fixed;no new application artifact/hash.
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
