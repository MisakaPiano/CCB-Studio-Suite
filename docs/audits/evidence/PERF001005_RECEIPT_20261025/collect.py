from pathlib import Path
import hashlib,json,shutil,statistics,subprocess,xml.etree.ElementTree as ET,zipfile
ROOT=Path('H:/CCB-Studio-Suite');DEST=ROOT/'docs/audits/evidence/PERF001005_RECEIPT_20261025'
assert not DEST.exists();DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
source='ec75083a664642de3237697398eb2445c3876c43'
runs=list((ROOT/'.local/windows-preview/evidence').glob('*-perf001005-copy-receipt-final-20261025'))
assert len(runs)==1
src=runs[0];summary=json.loads((src/'summary.json').read_text())
assert summary['baseCommit']==source and summary['status']=='PASS' and summary['sourceStable']
assert (summary['tests'],summary['failures'],summary['errors'],summary['skipped'])==(16,0,0,0)
for p in src.iterdir():
    if p.is_file():
        target=DEST/'run'/p.name;target.parent.mkdir(exist_ok=True);shutil.copyfile(p,target)
metrics={}
for cls,label in [('TypesetOriginalPerformanceTest','TYPESET_ORIGINAL_ALLOCATION'),('IngressCopyPerformanceTest','INGRESS_COPY_ALLOCATION'),('OriginalExportPerformanceTest','ORIGINAL_EXPORT_PAIRED')]:
    output=ET.parse(next(src.glob('*'+cls+'.xml'))).getroot().findtext('system-out')
    rows=json.loads(output.split(label+'=',1)[1].strip());write(DEST/(cls+'-samples.json'),rows)
    metrics[cls]=[]
    for size in sorted({r['payloadBytes'] for r in rows}):
        for kind in sorted({r.get('kind','current') for r in rows if r['payloadBytes']==size}):
            selected=[r for r in rows if r['payloadBytes']==size and r.get('kind','current')==kind]
            metrics[cls].append(dict(payloadBytes=size,kind=kind,medians={k:statistics.median(r[k] for r in selected) for k in ('wallMs','cpuMs','allocatedBytes')}))
before=json.loads((src/'source-before.json').read_text());identities=[]
paths=subprocess.check_output(['git','diff-tree','--no-commit-id','--name-only','-r',source],cwd=ROOT,text=True).splitlines()
for path in paths:
    p=ROOT/path;fixed=subprocess.check_output(['git','show',source+':'+path],cwd=ROOT);data=p.read_bytes()
    assert sha(p)==before[path] and (data==fixed or data.replace(b'\r\n',b'\n')==fixed)
    identities.append(dict(path=path,diskSha256=sha(p),gitSha256=hashlib.sha256(fixed).hexdigest(),textRepresentation='exact' if data==fixed else 'CRLF checkout / LF Git'))
unchanged=['modules/asset-library','modules/studio-core/src/main/kotlin/io/github/misakapiano/ccbstudio/core/FileIntegrity.kt','modules/image-authority','tools/update-runtime']
assert subprocess.check_output(['git','diff','3086895',source,'--',*unchanged],cwd=ROOT)==b''
write(DEST/'SOURCE_MAP.json',dict(source=source,changedInputs=identities,unchangedFrom3086895=unchanged,sourceStable=True,finalUniqueMethods=16,oldRunsNotAdded=True,release='NONE',independentReview='NOT REQUESTED',userAndPackage='NOT TESTED'))
cleanup=json.loads((src/'fixture-cleanup.json').read_text());assert cleanup['status']=='COMPLETE'
write(DEST/'ANALYSIS.json',dict(medians=metrics,finalUniqueMethods=16,cleanup=cleanup,scheduling='Gradle9.1 --priority low, single worker,2 processors,256MiB test heap',comparisonLimits=['Current3/12MiB allocation, not RSS or complete UI latency','Separate JVMs from earlier intake/typeset baseline; low priority first applied here, not a causal latency comparison','Gallery legacy/current4 alternating rounds in same JVM; retain every raw sample','No continuous Desktop quiet lease; process ownership checked only before launch','Old RED lock failure and q09 failure remain in their immutable earlier evidence']))
shutil.copyfile(Path(__file__),DEST/'collect.py')
(DEST/'ATTEMPTS_AND_LIMITS.md').write_text('''# Receipt digest follow-up: one execution,16 unique methods

Fixed source ec75083: optional extra copy receipt digest is omitted only for TypesetExport original and LibraryRepository managed copies, whose actual destination full fingerprint remains. AssetStore source verification, FileIntegrity,32MiB intake limit and runtime update code are unchanged. Gallery still requests the extra receipt digest and requires a non-null SHA in COMPLETE.json. Runtime-only nullable return has no stored schema change.

Same final source executes5 shared/typeset performance and edge methods,2 existing typeset regressions,3 managed copy methods,1 existing24-item batch recovery method,5 gallery methods.16 total,0 failures/errors/skips. Earlier baseline/final rounds are not added. New no-receipt branch is exercised at all8 boundary lengths and actual full destination SHA checked; original-byte preservation, unknown target refusal, corruption/cancellation/partial recovery/publication and blocked-output workspace-read liveness remain covered. No decoder certification from opaque signature fixtures.

One final runner invocation here. Earlier attempt before handoff did not create this phase directory; no prior execution result claimed. A read-only PowerShell brace expansion typo while preparing evidence produced a parser error; it did not run a test or modify source. All actual runner output is preserved. No new product/test failure in this segment; earlier3d935c3 lock RED and q09 FAILED/UNKNOWN stay fixed separately.

Gradle9.1 --help checked locally before choosing --priority low. Scoped to this Suite runner and child processes, no global OS/security/Desktop configuration change. Owner check before launch found no Desktop Java worker. A boundary check after BUILD SUCCESSFUL observed Desktop Java PID48132,parent49644,creation2026-10-24T22:48:37Z. Its exact activity/sample overlap is UNKNOWN; this is not a quiet-host measurement. Prior observations and slow samples retained. Different scheduling modes and separate JVMs prevent attributing wall/CPU differences to digest removal. Primary benefit is bounded allocation already established; no new speed/RSS/user/package/release acceptance claim.

Own synthetic fixtures sealed and retired after logs only; no real gallery/private credentials/services, user scans, Desktop writes/process termination, old evidence deletion, new package/release/daily install or Project payload. Current materials prepared only, no external review send. Original user-not-tested assets/editor,115/Prompt pending,q09 unknown and PRODUCT_HOLD / NOT RC remain.
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
print(json.dumps(dict(source=source,zipBytes=archive.stat().st_size,zipSha256=sha(archive),manifestFiles=len(items),zipMembers=len(items)+1,medians=metrics,cleanup=cleanup),indent=2))
