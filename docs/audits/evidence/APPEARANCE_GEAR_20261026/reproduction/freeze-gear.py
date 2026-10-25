from pathlib import Path
import hashlib,json,subprocess,sys
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/appearance-gear-20261026';SOURCE='22aa8c55ec4b05db29acb443d85717e80eb9d24a'
DEST=ROOT/'docs/audits/evidence/APPEARANCE_GEAR_20261026';DEST.mkdir()
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):
 with p.open('x',encoding='utf-8') as f:json.dump(v,f,indent=2);f.write('\n')
def copy(p,name):
 q=DEST/name;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes(p.read_bytes())
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
build=ROOT/'.local/windows-preview/evidence/20261025T213043457341Z-appearance-gear-fix-20261026'
inputs=json.loads((build/'source-before.json').read_text());mapped=[]
for name,digest in inputs.items():
 p=ROOT/name;assert sha(p)==digest,name
 raw=p.read_bytes();git=subprocess.check_output(['git','show',SOURCE+':'+name])
 assert raw.replace(b'\r\n',b'\n')==git.replace(b'\r\n',b'\n'),name
 mapped.append(dict(path=name,buildSha256=digest,gitSha256=hashlib.sha256(git).hexdigest(),relationship='EXACT' if raw==git else 'CRLF_LF_ONLY'))
exe=OUT/'candidate/studio-installer.exe';receipt=json.loads((OUT/'candidate/BUILD_RECEIPT.json').read_text());assert sha(exe)==receipt['sha256']
write(DEST/'FIXED_SOURCE_PACKAGE_MAP.json',dict(sourceCommit=SOURCE,baseEvidence='2eb4de1c442bf9647cfb013917b6fe55b8837517',installerVersion='0.2.2-internal',installerPath=str(exe),installerBytes=exe.stat().st_size,installerSha256=sha(exe),buildEvidence=str(build),inputs=mapped,packagedClassVerification=json.loads((OUT/'ISOLATED_INSTALL_VERIFICATION.json').read_text()),publicRelease=False,dailyInstallationChanged=False))
copy(ROOT/'handoff/evidence/THEME_P2_PROJECT_REVIEW_20261026/.gitattributes','.gitattributes')
for p in build.iterdir():
 if p.is_file():copy(p,'run/'+p.name)
for name in ('run-fix.py','PLAN.json','COMMAND.json','RUNNER_IDENTITY.json','runner.log','memory-samples.jsonl','MONITOR_RESULT.json','package-fix.py','PACKAGE_COMMAND.json','package.log','verify-install.py','ISOLATED_INSTALL_COMMAND.json','isolated-install.log','ISOLATED_INSTALL_VERIFICATION.json','USER_REQUESTED_REOPEN.json','USER_REOPEN_RESULT.json','USER_THEME_ACCEPTANCE.json','README.zh-CN.txt'):
 copy(OUT/name,'control/'+name)
for name in ('Launch-Candidate.ps1','Probe.ps1','MODE.txt','LAUNCH_PROBE.json','PROBE_RESULT.json','PROBE_VERIFIED.json','LAUNCH_APP.json','APP_IDENTITY_OBSERVATION.json','APP_VERIFIED.json'):
 copy(OUT/'entry-correction'/name,'entry/'+name)
for p in (OUT/'entry-correction').glob('LAUNCH_REVIEW_*.json'):copy(p,'entry/'+p.name)
for name in ('BUILD_RECEIPT.json','compile.log','installer.nsi','payload/INSTALL_MANIFEST.json','payload/update-config.json','payload/updater/RUNTIME_MANIFEST.json'):
 copy(OUT/'candidate'/name,'package/'+name)
write(DEST/'PREVIOUS_CANDIDATE_FINDINGS.json',dict(oldSource='80ea1167cdc197db30980ed116ee6d30731c994a',oldEvidence='2eb4de1c442bf9647cfb013917b6fe55b8837517',oldInstallerSha256='2db2f03f655aefff13103b3f63a603b346a6091c98c491af10c6def43521eaef',userRejected=['Gear cannot open','Later run used default LOCALAPPDATA workspace instead of isolation'],oldTaskPointer='Referenced %LOCALAPPDATA%/CCBStudioSuite/workspace-v3, corroborating reported path. Actual later invocation unknown; old stored shortcut args correctly quoted.',oldEntryCorrection='Old actual shortcut probe and APP verified args/env/new explicit workspace. Physical Escape stopped Computer Use; no further inputs then. No private default workspace read or cleaning.',gearRootCause='Production gear not registered in native titlebar interactive regions; interpreted as caption. New code registers/removes actual measured bounds.',knownPreparationFailure='Read-only path helper used PSIsContainer on .NET DirectoryInfo and looped; only own command session interrupted. Bounded checks replaced it. No data writes or product-test failure.',manualFullThemePass='Preserved in USER_THEME_ACCEPTANCE; separate FFFFFF text-action contrast finding OPEN.'))
copy(Path(__file__),'reproduction/freeze-gear.py')
files=[dict(path=p.relative_to(DEST).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(DEST.rglob('*')) if p.is_file()]
write(DEST/'RAW_MANIFEST.json',dict(source=SOURCE,files=files,membersIncludingManifest=len(files)+1,bytesExcludingManifest=sum(x['bytes'] for x in files),excluded='App/installer binaries, user screenshots, actual workspace/vault, private default data, credentials. Artifacts local with fixed hashes.'))
print(json.dumps(dict(status='FROZEN',source=SOURCE,inputs=len(mapped),files=len(files)+1,installerSha256=sha(exe))),flush=True)