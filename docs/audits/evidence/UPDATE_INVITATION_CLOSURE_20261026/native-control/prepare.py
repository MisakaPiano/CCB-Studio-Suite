from pathlib import Path
import hashlib,json,sys,subprocess,zipfile
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/update-chain-20261026/retry-b'
sys.path.insert(0,str(ROOT/'tools'))
import workspace_artifacts as artifacts
folder=artifacts.create('ab');lab=folder/'payload/review-update-native';lab.mkdir()
(lab/'SYNTHETIC_ONLY.txt').write_text('CCB_REVIEW_UPDATE_LAB',encoding='ascii')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
probe=ROOT/'modules/studio-desktop/build/libs/studio-desktop-review-update-probe.jar'
assert sha(probe)=='d81a414a564e38bc3f1bc689eeaa44672f28b81edde6c20fbe437e104bb3e563'
assert not subprocess.check_output(['git','diff','248485824e1d6d2201ceedf7d14ff66e251d344d','HEAD','--','modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/ReviewUpdateNativeA.kt'],cwd=ROOT)
old=ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025/reproduction-drivers/native-update-driver-residual-final.py'
text=old.read_text(encoding='utf-8')
text=text.replace('ROOT=Path(__file__).resolve().parents[2]',"ROOT=Path('H:/CCB-Studio-Suite')")
text=text.replace("LAB=Path(json.loads((ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])",'LAB=Path('+repr(str(lab))+')')
text=text.replace("EVIDENCE=ROOT/'.local/review-fixes-cleanup-20261024'/('native-driver-'+LAB.parent.parent.name)",'EVIDENCE=Path('+repr(str(OUT/'native'))+')')
text=text.replace("str(ROOT/'.local/review-fixes-cleanup-20261024/close-native-lab.py')",'str(Path('+repr(str(OUT/'close-native-lab.py'))+'))')
(OUT/'driver.py').write_text(text,encoding='utf-8')
close=(ROOT/'.local/review-fixes-cleanup-20261024/close-native-lab.py').read_text(encoding='utf-8')
# Adapt only the old harness's path lookup, never use the shared old lab record.
close=close.replace('ROOT=Path(__file__).resolve().parents[2]',"ROOT=Path('H:/CCB-Studio-Suite')")
close=close.replace("LAB=Path(json.loads((ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])",'LAB=Path('+repr(str(lab))+')')
(OUT/'close-native-lab.py').write_text(close,encoding='utf-8')
record=dict(root=str(folder),lab=str(lab),source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),originalDriverSha256=sha(old),driverSha256=sha(OUT/'driver.py'),probeJarSha256=sha(probe),probeSourceUnchangedSince='248485824e1d6d2201ceedf7d14ff66e251d344d',formalTrustModified=False,keys='Fresh ephemeral test signers only, memory-only in existing internal_repository; no formal keys read',priorCandidate='RETAINED_UNCHANGED',dailyInstallation='NOT_ACCESSED')
(OUT/'PLAN.json').write_text(json.dumps(record,indent=2),encoding='utf-8');print(json.dumps(record,indent=2))
