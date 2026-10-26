from pathlib import Path
import hashlib,json,subprocess,sys
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/update-chain-20261026/retry-b';plan=json.loads((OUT/'PLAN.json').read_text());folder=Path(plan['root'])
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as artifacts
frozen=json.loads((OUT/'FROZEN.json').read_text());evidence=Path(frozen['evidence']);manifest=evidence/'RAW_MANIFEST.json'
assert hashlib.sha256(manifest.read_bytes()).hexdigest()==frozen['manifestSha256']
for item in json.loads(manifest.read_text())['files']:
 p=evidence/item['path'];assert p.stat().st_size==item['bytes'] and hashlib.sha256(p.read_bytes()).hexdigest()==item['sha256']
assert json.loads((OUT/'RUN_RESULT.json').read_text())['status']=='PASS'
# Exact fresh root only. Caller separately confirmed no process image under it.
proofs=[OUT/'FROZEN.json',OUT/'RUN_RESULT.json',OUT/'native/NATIVE_UPDATE_SUMMARY.json',OUT/'native/CLEANUP_ASSERTIONS.json',OUT/'review-installer.exe']
sealed=artifacts.seal(folder,proofs,'PASS');result=artifacts.cleanup(folder)
(OUT/'OWNED_LAB_RETIREMENT.json').write_text(json.dumps(dict(root=str(folder),sealedFiles=len(sealed['files']),sealedBytes=sealed['totalBytes'],retainedInstaller=str(OUT/'review-installer.exe'),evidenceKept=str(evidence),dailyInstallationAccessed=False,**result),indent=2)+'\n',encoding='utf-8')
print((OUT/'OWNED_LAB_RETIREMENT.json').read_text())
