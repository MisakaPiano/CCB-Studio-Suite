from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF005_EDITOR_ORIGINAL_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
sources={'baseline':'a04d452f86c4fb568aabcad6826e62b02608ae90','final':'5a91e999adab7d458dd7d1c6e7e2e4f444d0c892'}
counts={};metrics={};cleanup=[]
for kind in ('baseline','final'):
    runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf005-editor-original-'+kind+'-20261025'));assert len(runs)==1;src=runs[0]
    for p in src.iterdir():
        if p.is_file():
            target=DEST/'runs'/kind/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,target)
    summary=json.loads((src/'summary.json').read_text());assert summary['status']=='PASS' and summary['sourceStable'] and summary['baseCommit']==sources[kind]
    assert (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==((1 if kind=='baseline' else 5),0,0,0)
    counts[kind]={k:summary[k] for k in ('tests','failures','errors','skipped','sourceStable','baseCommit','seconds','startedUtc')}
    clean=json.loads((src/'fixture-cleanup.json').read_text());assert clean['status']=='COMPLETE';cleanup.append(clean)
    output=ET.parse(next(src.glob('*EditorOriginalPerformanceTest.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split('EDITOR_ORIGINAL_ALLOCATION=',1)[1].strip());write(DEST/(kind+'-samples.json'),rows)
    metrics[kind]={size:{k:statistics.median(r[k] for r in rows if r['payloadBytes']==size) for k in ('wallMs','cpuMs','allocatedBytes')} for size in sorted({r['payloadBytes'] for r in rows})}
identities=[]
paths=['modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/StudioWorkspace.kt','modules/studio-core/src/test/kotlin/io/github/misakapiano/ccbstudio/core/EditorOriginalPerformanceTest.kt','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt','modules/asset-library/src/main/java/io/github/misakapiano/ccbstudio/assets/AssetStore.java','tools/Run-PreviewChecks.py']
for kind in ('baseline','final'):
    before=json.loads((DEST/'runs'/kind/'source-before.json').read_text())
    for path in paths:
        fixed=subprocess.check_output(['git','show',sources[kind]+':'+path],cwd=ROOT)
        assert before[path] in (hashlib.sha256(fixed).hexdigest(),hashlib.sha256(fixed.replace(b'\n',b'\r\n')).hexdigest())
        identities.append(dict(run=kind,path=path,testInputSha256=before[path],gitSha256=hashlib.sha256(fixed).hexdigest()))
unchanged=['modules/asset-library','modules/image-authority','modules/studio-desktop','tools','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/OriginalFileCopy.kt']
assert subprocess.check_output(['git','diff',sources['baseline'],sources['final'],'--',*unchanged],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(sources=sources,identities=identities,unchangedPaths=unchanged,finalUniqueMethods=5,baselineNotAddedAgain=True,productionChange='StudioWorkspace.export ORIGINAL branch only, existing rendered branch equivalent after ORIGINAL early return',release='NONE',independentReview='NOT REQUESTED',humanAndPackage='NOT TESTED'))
write(DEST/'ANALYSIS.json',dict(medians=metrics,counts=counts,finalUniqueMethods=5,cleanupFiles=sum(c['removed'] for c in cleanup),cleanupBytes=sum(c['bytesFreed'] for c in cleanup),limits=['Separate JVMs and real file write/force; no causal latency or RSS improvement claim','Both runs Gradle low priority,single worker,2 processors,256MiB test heap','Owner checks before launch found no Desktop Java,not continuous quiet proof','16x16 valid PNG with synthetic ancillary bytes,not real image diversity','Existing synchronous editor export remains workspace-serialized with no new cancellation API; no general UI responsiveness claim']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Existing editor original export: one baseline and one final run

Baseline a04d452:1 actual API performance method PASS.3/12MiB private ancillary PNG payload,one warmup and3 measured exports per size; checks output/source bytes and exact COMPLETE.txt plus state. No cloned old production path. Fixed final5a91e99:5 unique methods PASS,0 failure/error/skip,source stable. Baseline repeated method/internal samples not added as more cases.

Final method coverage:bounded source-size allocation; historical first-byte jpeg/png naming,selection order/duplicates,ignore edit layers/missing font and recipes in raw outputs,state/assets unchanged after reopening; corrupt later source retains earlier output without COMPLETE or source replacement; invalid/empty/foreign selection preserves unknown existing content; existing batch export regression also checks PRIVACY/NORMAL output,original bytes/undo and backup. Opaque format headers certify historical raw-byte behavior only,not decoding or new formats.

Only StudioWorkspace.export ORIGINAL uses unchanged shared copy with an actual byte-count/SHA receipt. Existing post-transfer assets.read verification retained; old filename/receipt/new output directory/per-file atomic move/COMPLETE-last rules preserved. Rendered paths still decode/render/encode,admit derived assets and keep privacy recipes policy. Source store,shared helper,request logic,update code,all Desktop files unchanged. Prior shared-helper16-method run not repeated or counted here. Empty invalid assets still fail the historical first-byte naming check; not a new format/cancellation/transaction feature.

No failed product/test execution in these two runs. A Python -c quoting error while assembling test additions did not execute the edit script; a local script applied the intended changes before the fixed source commit. Earlier read-only PowerShell error belongs to previous receipt segment. Old lock RED,q09 and all prior user rejections remain fixed in their own evidence. No blind reruns.

Both use existing low-priority single-worker runner,Desktop ownership checked before starts,no kill or Desktop changes. Boundary process observation does not prove host quiet. Raw slower/outlier samples retained,no speed/RSS/complete GUI or user acceptance claim. Successful own fixtures retired after external logs only,not old/unknown/user data. No real gallery,private credentials,paid calls,user directory scans,new package/release/daily install or Project payload.
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
