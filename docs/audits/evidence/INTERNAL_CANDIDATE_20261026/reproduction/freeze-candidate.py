from pathlib import Path
import hashlib,json,subprocess,sys,zipfile,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/candidate-review-20261026';INSTALL=OUT/'isolated-install'
SOURCE='80ea1167cdc197db30980ed116ee6d30731c994a'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,value):
 with p.open('x',encoding='utf-8') as f:json.dump(value,f,indent=2);f.write('\n')
sys.path.insert(0,str(INSTALL/'updater/lib'))
from install_ops import verify_install
config=json.loads((INSTALL/'update-config.json').read_text());checked=verify_install(INSTALL,config)
assert not subprocess.check_output(['git','diff','--name-only',SOURCE,'HEAD','--','modules','tools']).strip()
assert not subprocess.check_output(['git','diff','--name-only','c39e860252fdd96c9bec5b5d42d1abd988d3ec06',SOURCE,'--','modules','tools']).strip()
assert subprocess.run(['git','merge-base','--is-ancestor','f6598d39298c06ec5644a131e484e814f77b6f45',SOURCE]).returncode==0
build=ROOT/'.local/windows-preview/evidence/20261025T210253688730Z-candidate-review-app-20261026'
inputs=json.loads((build/'source-before.json').read_text());mapping=[]
for name,digest in inputs.items():
 p=ROOT/name;assert sha(p)==digest,name
 git=subprocess.check_output(['git','show',SOURCE+':'+name],cwd=ROOT)
 raw=p.read_bytes();relationship='EXACT' if raw==git else 'CRLF_LF_ONLY'
 assert raw.replace(b'\r\n',b'\n')==git.replace(b'\r\n',b'\n'),name
 mapping.append(dict(path=name,buildInputSha256=digest,gitSha256=hashlib.sha256(git).hexdigest(),relationship=relationship))
installer=OUT/'delivery/CCB-Studio-Suite-review-80ea116-windows-x64.exe'
fixed=dict(sourceCommit=SOURCE,applicationEquivalentCommit='c39e860252fdd96c9bec5b5d42d1abd988d3ec06',includedDownloadTerminalFix='f6598d39298c06ec5644a131e484e814f77b6f45',buildEvidence=build.relative_to(ROOT).as_posix(),buildInputs=mapping,installerVersion=config['displayVersion'],channel=config['channel'],installerPath=str(installer),installerBytes=installer.stat().st_size,installerSha256=sha(installer),installedAfterUserNormalClose=checked,packagedUpdater=sha(INSTALL/'updater/lib/residue_cleanup.py'),bootstrapSha256=sha(INSTALL/'update-root.json'),publicRelease=False,tagCreated=False,dailyInstallationTouched=False,independentNewPackageReview='NOT REQUESTED')
write(OUT/'FIXED_SOURCE_PACKAGE_MAP.json',fixed)
DEST=ROOT/'docs/audits/evidence/INTERNAL_CANDIDATE_20261026';DEST.mkdir()
def copy(source,rel):
 target=DEST/rel;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(source.read_bytes())
copy(ROOT/'handoff/evidence/THEME_P2_PROJECT_REVIEW_20261026/.gitattributes','.gitattributes')
for name in ('AUTHORIZATION_AND_SCOPE.json','PLAN.json','COMMAND.json','RUNNER_IDENTITY.json','memory-samples.jsonl','MONITOR_RESULT.json','runner.log','run-build.py','build-package.py','verify-isolated-install.py','protection-integration.py','launch-smoke.py','BOOTSTRAP_REUSE.json','prepare-runtime-COMMAND.json','prepare-runtime.log','build-installer-COMMAND.json','build-installer.log','PACKAGE_STEP_RESULT.json','ISOLATED_INSTALL_COMMAND.json','isolated-install.log','ISOLATED_INSTALL_VERIFICATION.json','PROTECTION_CASES.json','PROTECTION_SUMMARY.json','SHORTCUT.json','FIXED_SOURCE_PACKAGE_MAP.json'):
 copy(OUT/name,'control/'+name)
for p in build.iterdir():
 if p.is_file():copy(p,'app-build/'+p.name)
for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi','payload/INSTALL_MANIFEST.json','payload/update-config.json','payload/updater/RUNTIME_MANIFEST.json'):
 copy(OUT/'candidate'/name,'package/'+name)
for name in ('COMMAND.json','STARTED.json','stdout.log','stderr.log','RUNNING_OBSERVATION.json','EXIT_OBSERVATION.json'):
 copy(OUT/'startup-smoke'/name,'startup/'+name)
# Deliberately do not copy screenshot bytes or original full user-path record.
write(DEST/'startup/USER_CONFIRMATION.json',dict(userImage='codex-clipboard-13d1a445-b791-4c1f-b182-b6887cc1667d.png; user-owned local image not published',observation='User supplied screenshot shows normally displayed empty creation workbench; user says already normally closed',scope='This startup screen only, not full theme/UI acceptance',agentAltF4Sent=False,agentCloseClickSent=False,agentRestartSent=False,watcherCorrection='EXIT_OBSERVATION.json normalCloseRequestedBy is an unexecuted preparatory intent hard-coded in the watcher. Actual close by user; no agent close action occurred. Original watcher bytes retained.',providerCalls=0,dailyInstallationAccessed=False))
write(DEST/'EVIDENCE_BOUNDARIES.json',dict(build='Actual createDistributable command success; zero JUnit; unchanged compiled inputs legitimately UP-TO-DATE; image generated',preflight='Physical-free2GiB not required; hBudget3630748092 estimated from measured build outputs plus explicit duplicate/app/runtime/log budget. Actual initial commit headroom over6GiB. Temporary Gradle384/256; no test worker.',planTemplateNotes='Raw PLAN inherited testForks1/worker and 896MiB known-limit prose from previous monitor. No JUnit launched, testHeapMiB=null; driver actual admission used3GiB commit headroom including estimated jlink allowance. Historical phrases are not new global requirements.',checks='Installer550 manifest files and2241 compiled classes; 6 packaged updater scripts; 3 unique new all-synthetic protection cases. Not summed into prior JUnit totals.',capture='FrameArrived timeout, then refreshed target+activation window capture timeout. Text AX only window/pane. User screenshot supplies default-page display evidence; no agent screenshot/full visual acceptance.',preparationIssues='Several PowerShell rg commands used wildcard path strings and failed to find/read files; corrected to scoped rg globs/actual paths before build. No product/test failures masked.',bootstrap='Same expired public synthetic root, loopback port1 endpoints; no keys/signatures/trust renewal. Offline review only, online updates unsupported.',retained='Candidate/payload/isolated installation/delivery/new synthetic fixtures retained for user review; no cleanup or daily rollback touched.',next='User can use task-local Start-Studio-Candidate.lnk; do not launch ordinary exe without explicit roots, replace daily115 or publish. New package independent review NOT REQUESTED.'))
copy(OUT/'delivery/README.zh-CN.txt','DELIVERY_README.zh-CN.txt')
copy(Path(__file__),'reproduction/freeze-candidate.py')
files=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(source=SOURCE,files=files,fileCountExcludingManifest=len(files),membersIncludingManifest=len(files)+1,bytesExcludingManifest=sum(x['bytes'] for x in files),excluded='EXE/JAR payloads, screenshot/user-image bytes, workspace/vault/credentials, caches, full process list. Exact installer hash and install manifest supplied; artifacts remain local.'))
print(json.dumps(dict(status='FIXED',source=SOURCE,installerSha256=sha(installer),buildInputs=len(mapping),evidenceFiles=len(files)+1,installedAfterClose=checked)),flush=True)