from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF003_DIRECTORY_COPY_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def digest(raw):return hashlib.sha256(raw).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
before='3bda776657a2908a2044d81f2c19c7ce542414f5';app='605f89e8f8fd7d8cc9f93ade44135123b471c918'
srcs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf003-directory-copy-final-20261025'));assert len(srcs)==1;src=srcs[0]
for p in src.iterdir():
    if p.is_file():
        target=DEST/'run'/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable']
assert(summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==(2,0,0,0)
assert subprocess.check_output(['git','diff',app,summary['baseCommit'],'--','modules','tools'],cwd=ROOT)==b''
inputs=json.loads((src/'source-before.json').read_text());identities=[]
prefix='modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/'
blocks={
 'GalleryHistory.kt':('        val directory=byId[id]?.takeIf{it.request().recipe().size<=16384}?.let{asset->runCatching {\n            val receipt=Json.parseToJsonElement(asset.request().recipe().decodeToString()).jsonObject','        val directory=byId[id]?.let{asset->\n            val opaque=asset.request().recipe()\n            if(opaque.size>16384)return@let null\n            runCatching {\n            val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject'),
 'GallerySorting.kt':('            require(asset.request().recipe().size<=16384)\n            val receipt=Json.parseToJsonElement(asset.request().recipe().decodeToString()).jsonObject','            val opaque=asset.request().recipe()\n            require(opaque.size<=16384)\n            val receipt=Json.parseToJsonElement(opaque.decodeToString()).jsonObject')}
for filename,(oldBlock,newBlock) in blocks.items():
    name=prefix+filename
    old=subprocess.check_output(['git','show',before+':'+name],cwd=ROOT);new=subprocess.check_output(['git','show',app+':'+name],cwd=ROOT)
    assert old.decode().replace(oldBlock,newBlock,1)==new.decode()
    oldRaw=(ROOT/'.local/perf003-directory-copy-20261025/before'/filename).read_bytes();assert oldRaw.replace(b'\r\n',b'\n')==old
    raw=(ROOT/name).read_bytes();assert digest(raw)==inputs[name] and raw.replace(b'\r\n',b'\n')==new
    for kind,content in [('before',oldRaw),('final',raw)]:
        p=DEST/'inputs'/kind/filename;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(content)
    identities.append(dict(path=name,beforeGitSha256=digest(old),beforeRawSha256=digest(oldRaw),finalGitSha256=digest(new),actualTestInputSha256=digest(raw),rawSourcePreserved=True))
changed=subprocess.check_output(['git','diff','--name-only',before,app,'--','modules','tools'],cwd=ROOT,text=True).splitlines();assert set(changed)=={prefix+n for n in blocks}
clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE'
write(DEST/'SOURCE_MAP.json',dict(beforeSource=before,applicationSource=app,executionSource=summary['baseCommit'],executionModulesAndToolsEqualApplication=True,identities=identities,changedModulePaths=changed,onlyTwoDirectoryRecipeAccessorBlocks=True,fullParsingBudgetsPathsLabelsFallbackAndHashesOutsideBlocksUnchanged=True,javaDefensiveApiUnchanged=True,finalUniqueMethods=2,noNewTestSource=True,newPerformanceBaseline='NOT RUN (same clone mechanism supported by prior recorded-date evidence)',newAllocationSpeedUiRssClaim='NONE',costMechanismReference='PERF003_RECORDED_TIMES_20261025,application492f8cc/evidence3bda776,not a new consumer performance measurement',packageVersion='NONE',packageSha256='NONE (no package built)',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(finalSummary=summary,finalUniqueMethods=2,cleanupFiles=clean['removed'],cleanupBytes=clean['bytesFreed'],limits=['Two existing changed-consumer tests only,not a full suite or new performance baseline','Directory grouping recorded provenance/removed root/same-name roots/preferences/reopen/original bytes','Directory sort identity,new arrivals/reversed order,natural sort/preferences/reopen/original bytes','No new unknown/oversize/corrupt branch execution in this narrow run;unchanged parsing/budget/fallback source compared','First recipe getter remains outside runCatching for grouping,inside for identity as before;oversize still avoids parse','No filesystem scan or original movement by these helpers;existing tests use fresh owned tiny synthetic images','No new thread allocation/frame/RSS/wall speed result claimed','Previous recorded-date timing/allocation not added or promoted as these consumers measurements']))
local=ROOT/'.local/perf003-directory-copy-20261025'
for name in ('resource-deferrals.jsonl','resource-boundaries.jsonl'):
    p=local/name
    if p.exists():shutil.copyfile(p,DEST/name)
shutil.copyfile(ROOT/'.local/performance-resource-gate.ps1',DEST/'resource-gate.ps1')
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Follow-up: two existing directory consumers

Before3bda776/application605f89e. Final two existing methods PASS,0fail/error/skip,source stable:GalleryHistoryTest.directoryGroupingUsesOnlyRecordedProvenanceAndPreferencesSurviveReopen and GallerySortingTest.naturalOrderIndependentAlbumsAndNewArrivalsKeepPreferencesAcrossReopen. These are changed consumers on a new source revision,not a rerun/reopening of the accepted nine-method batch projection or startup baseline. No new test source or benchmark. Prior492f8cc/3bda776 recorded-date study establishes that recipe getter copies an immutable byte array each call;this follow-up uses source-proven same mechanism,not another equivalent synthetic clone baseline. Do not add old four-method results/counts or allocation percentages as new consumer measurements.

Only grouping and directory identity blocks reuse a local getter copy for size and complete decode. First grouping getter remains outside runCatching as its prior takeIf;identity getter remains inside runCatching. Grouping previous<=16KiB guard becomes equivalent>16KiB early null,identity same require<=16KiB. JSON/kind/version/path parsing/recorded provenance/fallback labels/identity key/hash,original images and all other code unchanged. Java Request still defensively clones at constructor/getter,no internal borrowing/global cache/schema/Prompt/request/UI/file organization change. Raw before snapshots copied prior to edit,raw final match executed source-before hashes;CRLF-only normalization equals fixed Git blobs and exact block replacement proves rest unchanged.

Existing owned isolated fixtures cover managed folder IDs with same displayed root name,removed collection label,group/image order,folder folding preferences/reopen/originalbytes;directory identity stable after new arrivals and reversed ID order,separate folder sort preferences/natural ordering/reopen/byte preservation. No new exhaustive corrupt/unknown/16KiB branch run in this narrow follow-up;source equivalence retained and stated instead of reusing date tests as consumer acceptance. Performance metadata mechanism only,no newly measured allocation/speed/frame/RSS/wholeUI/universalWindows or user acceptance. No original path scan/move/delete;test metadata paths are fresh synthetic paths only.

Single explicit resource gate,two empty Desktop owner checks30seconds apart,lowpriority/singleworker2processors256MiB;boundary observations not continuous quiet proof. No unrelated process/security/private data/real credentials/service/paid request/paused user scan/daily install/windows/package/release. Success fresh own fixtures sealed then retired;old failure/unknown/user/backups retained. No new failure,older failures/rejections remain. Independent NOT REQUESTED,user/package NOT TESTED,new Project materials PREPARED ONLY / NOT SENT. Assets/editor USER_NOT_TESTED,115/Prompt PENDING,q09 FAILED/UNKNOWN and PRODUCT_HOLD / NOT RC unchanged.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
items=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=digest(p.read_bytes())) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=items));archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(DEST.rglob('*')):
        if p.is_file() and p!=archive:z.write(p,p.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in items:assert digest(z.read(item['path']))==item['sha256']
print(json.dumps(dict(applicationSource=app,executionSource=summary['baseCommit'],zipBytes=archive.stat().st_size,zipSha256=digest(archive.read_bytes()),manifestFiles=len(items),zipMembers=len(items)+1,finalUniqueMethods=2,seconds=summary['seconds'],cleanupFiles=clean['removed'],cleanupBytes=clean['bytesFreed']),indent=2))
