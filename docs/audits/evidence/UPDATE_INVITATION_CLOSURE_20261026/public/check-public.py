from pathlib import Path
import hashlib,json,sys,time
BASE=Path(__file__).resolve().parent
ROOT=BASE.parents[1]
LIB=ROOT/'.local/token-candidate-20261026/isolated-install/updater/lib'
sys.path.insert(0,str(LIB))
from studio_update import Installed,execute
PACKAGE=ROOT/'.local/windows-rc/performance-107/115-ff145d051bae'
cache=BASE/'public-check-cache';cache.mkdir()
inputs={name:hashlib.sha256((PACKAGE/name).read_bytes()).hexdigest() for name in ('update-config.json','update-root.json')}
start=time.monotonic()
try:
 result=execute(Installed.load(PACKAGE),cache)
 report=dict(status='PASS',result=result)
except Exception as error:
 report=dict(status='FAILED',error=type(error).__name__,detail=str(error)[:1000])
report.update(seconds=time.monotonic()-start,readOnlyBaseline=str(PACKAGE),clientLib=str(LIB),inputs=inputs,downloadedInstaller=False,dailyInstallationAccessed=False,privateCredentialsRead=False)
(BASE/'PUBLIC_CHANNEL_CHECK.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
sys.exit(0 if report['status']=='PASS' else 1)
