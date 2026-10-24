import json,sys,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import workspace_artifacts as w
folder=w.create('ab');lab=folder/'payload/review-update-native';lab.mkdir()
(lab/'SYNTHETIC_ONLY.txt').write_text('CCB_REVIEW_UPDATE_LAB')
record=dict(lab=str(lab),artifactRoot=str(folder),source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip())
(ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').write_text(json.dumps(record))
print(json.dumps(record))
