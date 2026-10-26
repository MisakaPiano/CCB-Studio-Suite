from pathlib import Path
import json,sys
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-candidate-116-20261026'
record=json.loads((OUT/'INSTALL_FIXTURE_OWNER.json').read_text());folder=Path(record['root']);assert folder.parent==ROOT/'.local/disposable'
assert json.loads((OUT/'RUN_RESULT.json').read_text())['status']=='PASS'
assert json.loads((OUT/'INSTALL_VERIFICATION.json').read_text())['status']=='PASS'
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as artifacts
proofs=[OUT/'INSTALL_VERIFICATION.json',OUT/'RUN_RESULT.json',OUT/'PUBLICATION_MANIFEST.json',OUT/'SHA256SUMS.txt']
sealed=artifacts.seal(folder,proofs,'PASS');retired=artifacts.cleanup(folder)
(OUT/'INSTALL_FIXTURE_RETIREMENT.json').write_text(json.dumps(dict(root=str(folder),sealedFiles=len(sealed['files']),sealedBytes=sealed['totalBytes'],candidateRetained=str(OUT/'candidate/studio-installer.exe'),dailyInstallationAccessed=False,**retired),indent=2)+'\n',encoding='utf-8')
print((OUT/'INSTALL_FIXTURE_RETIREMENT.json').read_text())
