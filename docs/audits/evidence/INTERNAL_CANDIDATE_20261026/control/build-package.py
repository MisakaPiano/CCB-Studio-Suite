from pathlib import Path
import hashlib,json,os,subprocess,sys,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/candidate-review-20261026'
SOURCE='80ea1167cdc197db30980ed116ee6d30731c994a'
assert Path.cwd().resolve()==ROOT.resolve()
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==SOURCE
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,value):
 with (OUT/name).open('x',encoding='utf-8') as f:json.dump(value,f,indent=2);f.write('\n')
def run(name,args):
 write(name+'-COMMAND.json',dict(arguments=list(map(str,args)),startedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
 with (OUT/(name+'.log')).open('xb') as log:r=subprocess.run(list(map(str,args)),cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
 assert r.returncode==0,(name,r.returncode)
 print(name+' PASS',flush=True)
python=Path(sys.executable)
bootstrap=ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025/native-final/repository/root.json'
assert sha(bootstrap)=='efc8fdda742b6cc5f7fe4edcb38f75eba9bfcbe0210e07eb888d25f76c3eb892'
write('BOOTSTRAP_REUSE.json',dict(path=str(bootstrap),sha256=sha(bootstrap),nature='unchanged historical synthetic public root only; expired; no private key/signing/renewal/trust configuration changes',automaticUpdate='not operational in this offline internal review candidate',metadataUrl='http://127.0.0.1:1/metadata/',targetsUrl='http://127.0.0.1:1/targets/'))
run('prepare-runtime',[python,'-B',ROOT/'tools/Prepare-UpdaterRuntime.py','--destination',OUT/'updater-runtime','--source',ROOT/'.local/windows-rc/updater-downloads','--source',ROOT/'.local/windows-rc/wheels'])
evidence=ROOT/'.local/windows-preview/evidence/20261025T210253688730Z-candidate-review-app-20261026'
run('build-installer',[python,'-B',ROOT/'tools/Build-InternalInstaller.py','--app',ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview','--runtime',OUT/'updater-runtime','--bootstrap',bootstrap,'--destination',OUT/'candidate','--version','2','--metadata-url','http://127.0.0.1:1/metadata/','--targets-url','http://127.0.0.1:1/targets/','--build-evidence',evidence])
write('PACKAGE_STEP_RESULT.json',dict(status='BUILT_NOT_INSTALLED',source=SOURCE,buildEvidence=str(evidence),installerSha256=sha(OUT/'candidate/studio-installer.exe'),dailyInstallationTouched=False,publicRelease=False,trustChanged=False))