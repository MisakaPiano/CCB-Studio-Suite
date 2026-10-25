from pathlib import Path
import subprocess,json,sys,hashlib
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/appearance-gear-20261026';OLD=ROOT/'.local/candidate-review-20261026'
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()=='22aa8c55ec4b05db29acb443d85717e80eb9d24a'
args=[sys.executable,'-B',str(ROOT/'tools/Build-InternalInstaller.py'),'--app',str(ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'),'--runtime',str(OLD/'updater-runtime'),'--bootstrap',str(ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025/native-final/repository/root.json'),'--destination',str(OUT/'candidate'),'--version','2','--metadata-url','http://127.0.0.1:1/metadata/','--targets-url','http://127.0.0.1:1/targets/','--build-evidence',str(ROOT/'.local/windows-preview/evidence/20261025T213043457341Z-appearance-gear-fix-20261026')]
with (OUT/'PACKAGE_COMMAND.json').open('x') as f:json.dump(args,f,indent=2)
with (OUT/'package.log').open('xb') as f:result=subprocess.run(args,cwd=ROOT,stdout=f,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
assert result.returncode==0,result.returncode
print((OUT/'candidate/BUILD_RECEIPT.json').read_text(),flush=True)