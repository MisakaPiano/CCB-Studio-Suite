from pathlib import Path
import subprocess,json,sys
ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
HEAD='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0'
assert Path.cwd().resolve()==ROOT.resolve() and OUT==ROOT/'.local/token-candidate-20261026'
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==HEAD
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
matches=list((ROOT/'.local/windows-preview/evidence').glob('*-token-candidate-app-20261026'));assert len(matches)==1
evidence=matches[0];summary=json.loads((evidence/'summary.json').read_text());assert summary['exitCode']==0 and summary['sourceStable'] and summary['tests']==0
args=[sys.executable,'-B',str(ROOT/'tools/Build-InternalInstaller.py'),
      '--app',str(ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'),
      '--runtime',str(ROOT/'.local/candidate-review-20261026/updater-runtime'),
      '--bootstrap',str(ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025/native-final/repository/root.json'),
      '--destination',str(OUT/'candidate'),'--version','2',
      '--metadata-url','http://127.0.0.1:1/metadata/','--targets-url','http://127.0.0.1:1/targets/',
      '--build-evidence',str(evidence)]
with (OUT/'PACKAGE_COMMAND.json').open('x',encoding='utf-8') as f:json.dump(args,f,indent=2)
with (OUT/'package.log').open('xb') as f:r=subprocess.run(args,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
assert r.returncode==0,r.returncode
print((OUT/'candidate/BUILD_RECEIPT.json').read_text(),flush=True)
