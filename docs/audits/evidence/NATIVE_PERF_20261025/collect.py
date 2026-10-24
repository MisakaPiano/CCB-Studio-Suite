from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');LOCAL=ROOT/'.local/native-perf-20261025';DEST=ROOT/'docs/audits/evidence/NATIVE_PERF_20261025'
assert not DEST.exists(),'Never replace fixed evidence'
DEST.mkdir(parents=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def copy(f,t):t.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,t)
for r in sorted(LOCAL.glob('run-*')):
    for f in r.rglob('*'):
        if f.is_file():copy(f,DEST/'runs'/r.name/f.relative_to(r))
for phase in ['native-perf-inputs-20261025','native-perf-diagnostics-inputs-20261025','native-perf-final-inputs-20261025','native-perf-barrier-inputs-20261025']:
    r=sorted((ROOT/'.local/windows-preview/evidence').glob('*-'+phase))[-1]
    for f in r.iterdir():
        if f.is_file():copy(f,DEST/'builds'/phase/f.name)
    s=json.loads((r/'summary.json').read_text());assert s['sourceStable'] and s['tests']==0 and s['status']=='COMMAND_SUCCESS'
for script in ['run.py','analyze.py','collect.py']:copy(LOCAL/script,DEST/script)
run=LOCAL/'run-45bf00b6';summary=json.loads((run/'SUMMARY.json').read_text());assert summary['status']=='PASS' and summary['measuredNativeSessions']==2
inputs=json.loads((run/'RUNTIME_INPUTS.json').read_text())
assert all(Path(i['path']).is_file() and sha(Path(i['path']))==i['sha256'] for i in inputs['files'])
assert subprocess.check_output(['git','diff','7c42606','HEAD','--','modules/studio-desktop/src/main','modules/studio-core','modules/asset-library','modules/image-authority','tools/update-runtime'],cwd=ROOT)==b''
probe='modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/NativePerformanceProbe.kt'
write(DEST/'SOURCE_MAP.json',dict(source='4ec1979754b80068c7fc3ea68b1ecdaa43df5fa8',startingCommit='7c42606fae002aca7cdd8aedbb713b462574ccd9',productionKotlinAndUpdateRuntimeUnchanged=True,probe=dict(path=probe,gitSha256=hashlib.sha256(subprocess.check_output(['git','show','4ec1979:'+probe],cwd=ROOT)).hexdigest(),executedSourceDiskSha256=sha(ROOT/probe)),runtimeInputsAllUnchangedAfterNativePair=True,canonicalRun='run-45bf00b6',nativeSessionsPassed=2,junitTestsExecuted=0,packagingAndPackageStartup='NOT TESTED; test classes plus native Window host only',independentReview='NOT REQUESTED; no new payload authorization',q09='FAILED/UNKNOWN unchanged',assetAndEditorHuman='USER_NOT_TESTED',promptAnd115Human='PENDING',product='PRODUCT_HOLD / NOT RC'))
write(DEST/'ENVIRONMENT.json',dict(os='Windows 10 Pro 19045 x64',java='17.0.20.1',testHeapLimitBytes=536870912,activeProcessors=2,window='1320x880 at recorded scale 1.0',seed='48 synthetic 832x1216 PNG originals, unique asset IDs; no real user account/files',nativeHost='Production Workbench and Controller in real Compose Window; no shipped launcher/update bootstrap/default-entrypoint code',sampling='GetProcessTimes / GetProcessMemoryInfo / GetProcessIoCounters / handles every ~100ms; heap and threads at event checkpoints',frameDefinition='completed native content draw AND Window showing; gallery includes initial+settled confirmation frames, 20ms polling; no DWM presentation timestamp',cold='New process first launch after seed, not disk-cold; OS caches unflushed/unknown',duration='Two ~25-second sessions; not hour-level',ambient='One busy attempt deferred; native final before/after snapshots no Desktop launchers; cached test compilation overlap with later Desktop build is not a quiet-machine guarantee',security='No prevention/OS access-denied event observed; protection impact unknown, no changes to security settings',network='HTTP interceptor/admission reject sends; startup updater not invoked',sourceCodeChanges='Only a test probe, its test input-export Gradle task, and task docs'))
(DEST/'ATTEMPTS.md').write_text('''# Attempts, limits, failures retained

1. run-3ada0af6 @ effffba: first readiness timeout, no counter accepted a draw. Controller drained and process exited; final driver failed. Original workspace/runtime fixtures retained. No security interception evidence.
2. run-b4cb02b9 @ 0a6237f: DEFERRED_DESKTOP_BUSY before fixture preparation or native launch. Empty registered payload retained, no product failure.
3. run-14784bf7 @ 0a6237f: diagnostics show Window visible/showing true, one completed draw callback, accepted count zero. Original condition incorrectly required drawing while already showing; native first surface was prepared before showing. Probe-only timing defect, not proof of application or protection failure. One read-only Sky accessibility observation returned only a pane; it was not pixel proof. No UI inputs sent.
4. run-83ee4fe1 @ 1f1bea3: first showing+draw barrier succeeded; gallery completion observer timed out. Parent draw callbacks were retained rather than a reliable barrier for child updates. This failed observation does not prove a gallery product defect. Child exit code 1 and full log recorded; no protection attribution. Final probe uses per-phase explicit draw epochs plus metadata/thumbnail completion, retaining all readiness checks.
5. run-45bf00b6 @ 4ec1979: canonical pair, process-cold and warm-reopen PASS, 48 original hashes checked in each, six round trips in each, thumbnail requests/pixels zero after close, OS process exits confirmed (codes 0). No previous cases added as PASS.

Build-input commands (4) are compile/classpath export COMMAND_SUCCESS, 0 JUnit tests. No PERF-003 or complete old suite rerun. Prior two early native failures used Compose application default exitProcessOnExit; post-application PASS code was unreachable, so child exit 0 was insufficient. Parent still failed on missing PASS; the next failed assertion in captured tool trace proves that the previous exit-code-zero assertion passed, but those two runs did not independently persist child exit JSON. Do not invent missing native exit records. Final host uses exitProcessOnExit=false and final PASS/FAILED_EXIT plus parent exit JSON.

Initial read-only setup command used PowerShell-incompatible brace expansion and did not execute; another source lookup named a missing reference file. These are agent lookup errors, not DLL/OS/protection/product failures. No user file or system setting was changed to fix them.

The monitor gained explicit exit JSON after initial failures; its exact bytes were not archived before those early attempts. Current run.py is the executed final monitor; earlier source-probe commits, literal command arrays, runtime input hashes, original data and original failure files remain. Do not claim the current monitor byte hash for early runs.

Logical process IO includes DLL/cache/IPC work; physical disk bytes or C/H drive attribution are NOT MEASURED. PeakWorkingSet is the OS process counter; private bytes/heap values are sampled maxima. Heap is not RSS. Startup suppresses updater/default-entrypoint/provider paths in test host, so no full packaged startup acceptance. Original failed fixtures are retained; only canonical successful owned payload was sealed/retired after external evidence. No historical cleanup, arbitrary termination, daily install write, release or new Project message.
''',encoding='utf-8')
(DEST/'.gitattributes').write_bytes(b'* -text\n**/* -text\n')
manifest=[dict(path=f.relative_to(DEST).as_posix(),bytes=f.stat().st_size,sha256=sha(f)) for f in sorted(DEST.rglob('*')) if f.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(format=1,files=manifest))
archive=DEST/'RAW_EVIDENCE.zip'
with zipfile.ZipFile(archive,'x',compression=zipfile.ZIP_DEFLATED) as z:
    for f in sorted(DEST.rglob('*')):
        if f.is_file() and f!=archive:z.write(f,f.relative_to(DEST).as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for item in manifest:assert hashlib.sha256(z.read(item['path'])).hexdigest()==item['sha256']
receipt=dict(bytes=archive.stat().st_size,sha256=sha(archive),manifestFiles=len(manifest),zipMembers=len(manifest)+1,source=summary['source'])
write(LOCAL/'COLLECTION_RECEIPT.json',receipt);print(json.dumps(receipt,indent=2))
