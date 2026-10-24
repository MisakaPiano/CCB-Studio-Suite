"""Freeze this diagnostic's exact bytes, without replacing accepted old evidence."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/startup-diagnostic-20261025'
DEST=ROOT/'docs/audits/evidence/STARTUP_DIAG_20261025'
assert not DEST.exists(),'Do not replace a frozen evidence directory'
DEST.mkdir(parents=True)
def sha(path):
    with path.open('rb') as source:return hashlib.file_digest(source,'sha256').hexdigest()
def write(path,value):path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def copy(source,target):target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
for attempt in sorted(LOCAL.glob('run-*')):
    for source in attempt.rglob('*'):
        if source.is_file():copy(source,DEST/'runs'/attempt.name/source.relative_to(attempt))
for name in ('analyze.py','collect.py','INVOCATION_FAILURE.json'):copy(LOCAL/name,DEST/name)
build=ROOT/'.local/windows-preview/evidence/20261024T200245385753Z-startup-diag-inputs-20261025'
for source in build.iterdir():
    if source.is_file():copy(source,DEST/'build'/source.name)
summary=json.loads((build/'summary.json').read_text());assert summary['status']=='COMMAND_SUCCESS' and summary['tests']==0 and summary['sourceStable']
run=LOCAL/'run-b4de1224';result=json.loads((run/'SUMMARY.json').read_text());assert result['status']=='PASS' and result['measuredStartupSessions']==2
source=result['source'];assert source=='8a757a6121c219dd942358d25c7a3707bc1fa8db'
runtime=json.loads((run/'RUNTIME_INPUTS.json').read_text());assert all(sha(Path(item['path']))==item['sha256'] for item in runtime['files'])
before=json.loads((build/'source-before.json').read_text());after=json.loads((build/'source-after.json').read_text()) if (build/'source-after.json').exists() else before
assert before==after
probe='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/NativePerformanceProbe.kt'
assert sha(ROOT/probe)==before[probe]
assert sha(ROOT/probe)==hashlib.sha256(subprocess.check_output(['git','show',source+':'+probe],cwd=ROOT)).hexdigest()
assert subprocess.check_output(['git','diff','6add127',source,'--','modules/studio-desktop/src/main','modules/studio-core','modules/asset-library','modules/image-authority','tools/update-runtime'],cwd=ROOT)==b''
oldzip=ROOT/'docs/audits/evidence/NATIVE_PERF_20261025/RAW_EVIDENCE.zip'
assert sha(oldzip)=='02f92fa93d385eb29d760816d7540b38f048354edb714d8fc1217d2d98cd5714'
command="@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.ProcessId -in 41388,23356 } | Select-Object ProcessId) | ConvertTo-Json -Compress"
alive=subprocess.check_output(['powershell','-NoProfile','-NonInteractive','-Command',command],text=True).strip()
assert not alive,'A measured child PID is still present; do not claim process exit'
write(DEST/'PROCESS_EXIT_CHECK.json',dict(pids=[41388,23356],alive=[],command=command,checkedAfterRecordedNormalExit=True,forcedTermination=False))
write(DEST/'SOURCE_MAP.json',dict(source=source,probeSourceIntroduced='80125d4601355a6f7f22feea98924c6e0c2031be',driverNewlineCorrection=source,startingCommit='6add1274e5cbfbfbe48391b677330464b6dfee16',productionSourceUnchanged=True,buildBaseCommit=summary['baseCommit'],buildInputsDirtyAtBuild=True,buildInputExplanation='Build preceded source commit. Full before hash map retained; stable throughout compilation. Executed Kotlin probe bytes exactly equal fixed source; only Python driver newline comparison corrected after build, not compiled Kotlin/JAR inputs.',probeSha256=sha(ROOT/probe),runtimeFileCount=len(runtime['files']),runtimeInputsRechecked=True,canonicalRun='run-b4de1224',measuredStartupSessions=2,junitExecuted=0,packageBuilt=False,independentReview='NOT REQUESTED; new diagnostic payload not sent',oldBaselineZipSha256=sha(oldzip),oldBaselineEvidenceUnchanged=True,normalExitCodes=[0,0],q09='FAILED/UNKNOWN unchanged',assetAndEditorHuman='USER_NOT_TESTED',promptAnd115Human='PENDING',product='PRODUCT_HOLD / NOT RC'))
(DEST/'FAILURES_AND_LIMITS.md').write_text('''# Attempt ledger and limits

- Initial invocation at 80125d4: embedded isolated Python omitted tools search path; import failed before output-root/fixture/JVM creation. Literal command, output and exit 1 preserved in INVOCATION_FAILURE.json. Exact UTC was not captured; no timestamp invented. Correct invocation uses explicit sys.path/runpy.
- run-42ad01c3 at 80125d4: prelaunch source assertion failed because the existing Gradle text checkout has CRLF while Git has LF (text=auto). Five raw hashes retained; no JVM launched. Only the existing Gradle text file may match via recorded LF text hash in 8a757a6; raw identity stays recorded, binary/probe checks unchanged. Empty owned failure fixture retained, never relabeled PASS.
- run-52ae8690 at 8a757a6: Desktop busy, deliberately deferred before fixture/JVM. Recorded two Desktop launcher PIDs. No interruption/retry loop/termination.
- run-b4de1224 at 8a757a6: one timeline-only startup and one minimum-JFR startup, 2 new measured JVM sessions, 0 JUnit tests. Each original-hash verification has 48 IDs but one unique PNG SHA. Neither session repeats accepted gallery cycles. Separate prepare JVM is setup, not a third measured session. Both measured PIDs exit 0 through normal close, thumbnail pending/pixels 0; later absence independently checked.

One compile/input-export COMMAND_SUCCESS (0 JUnit), no old baseline/test rerun. Build ran on 6add127 dirty source before source freeze; fixed compiled probe bytes verified against retained hash map and later Git source. Driver-only correction does not require recompiling unchanged Kotlin.

JFR minimum config disables environment, system/security properties, JVM information/arguments and other-system-process events; recorded sensitive event counts all zero. Child process receives only retained explicit synthetic environment whitelist. No inherited tokens/private paths supplied to child. Raw JFR kept only after event check, with lossless JSON gzip export and original settings/commands. No private originals/accounts/default entrypoint/provider requests.

Sampling is sparse (20 Java execution samples versus 234 native samples), not exclusive CPU/cause. Native event-loop samples may be waits; do not rank them as busy CPU hotspots. Class-load/compilation durations overlap or nest; do not add them into wall time. JFR bootstrap affects parent-launch-to-main. The first marker also includes test-class/management initialization, not a JVM-only duration. Stage logging perturbs timing. Two modes are not a pre/post product improvement comparison.

Composition commit != first completed content draw != showing confirmation != DWM presentation. Entire process lifetime includes deliberate 2-second settle, hash/close checks and tail exit; it is not startup latency. OS cache unknown (setup warmed caches), classpath-native test host not full Main/launcher/package, physical disk/GPU/global cache/long-duration/user acceptance NOT TESTED. No protection attribution. No production changes, package/release, daily install, suspended directory scanning, paid calls, new Project submission or Desktop work.

Successful current owned payload sealed after external raw evidence and retired: see FIXTURE_CLEANUP.json. Earlier failed fixture, accepted frozen baselines and all old failures/UNKNOWN/manual pending remain. This is not historical-directory cleanup or project acceptance.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
manifest=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=manifest))
archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as zipped:
    for path in sorted(DEST.rglob('*')):
        if path.is_file() and path!=archive:zipped.write(path,path.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as zipped:
    assert zipped.testzip() is None
    for item in manifest:assert hashlib.sha256(zipped.read(item['path'])).hexdigest()==item['sha256']
receipt=dict(source=source,bytes=archive.stat().st_size,sha256=sha(archive),manifestFiles=len(manifest),zipMembers=len(manifest)+1)
write(LOCAL/'COLLECTION_RECEIPT.json',receipt);print(json.dumps(receipt,indent=2))
