from pathlib import Path
import sys,json,hashlib,datetime,time,shutil,subprocess
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';APP=Path('H:/CCB-Studio-Suite-App')
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from install_ops import no_reparse,verify_install
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
baseline=json.loads((OUT/'DAILY_READ_ONLY_BASELINE.json').read_text(encoding='utf-8'))
state=json.loads((APP/'launch-current.json').read_text(encoding='utf-8'));assert state['version']==118
current=no_reparse(Path(state['installed']));config=json.loads((current/'update-config.json').read_text(encoding='utf-8'))
assert current==APP/'118-a1202e24f7a0' and config['sourceSha']=='a1202e24f7a0cba8a53f43bf035b67e2134ca2aa'
assert Path(state['data'])==APP/'data/workspace' and Path(state['runtime'])==APP/'data/runtime'
assert sha(current/'update-root.json')=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
assert sha(APP/'ACTIVE_INSTALLATION.json')==baseline['protectionSha256'] and (APP/'111-1c9385afc38c').is_dir()
verification=verify_install(current,config);cache=APP/'data/runtime/updates';matches=[]
for folder in (cache/'handoffs').iterdir():
 p=folder/'result.json'
 if p.is_file():
  v=json.loads(p.read_text(encoding='utf-8'))
  if v.get('installed')==str(current):matches.append(v)
assert len(matches)==1 and matches[0]['status']=='COMPLETE' and matches[0]['verification']=='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'
deadline=time.monotonic()+90
while time.monotonic()<deadline and ((APP/'117-b9b240a248d4').exists() or any((cache/'downloads').iterdir())):time.sleep(.5)
oldGone=not (APP/'117-b9b240a248d4').exists();downloads=list((cache/'downloads').iterdir());staging=list(APP.glob('*.installing'))
assert oldGone and not downloads and not staging,'Daily automatic cleanup not complete'
cmd='Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -eq '+"'"+str(current/'CCBStudioSuitePreview.exe')+"'"+' } | Select-Object -ExpandProperty ProcessId'
pids=subprocess.check_output(['powershell','-NoProfile','-Command',cmd],text=True).split();assert pids
value=dict(status='PASS',atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),version=118,source=config['sourceSha'],source117=baseline['source'],method='USER_OPERATED_NORMAL_GUI_UPDATE',applicationResult='COMPLETE',applicationVerification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH',programVerification=verification,formalRootUnchanged=True,dataAndRuntimeLocationUnchanged=True,dataModifiedByInstaller=matches[0]['dataModifiedByInstaller'],privateSnapshotsOrCredentialsRead=False,applicationSuccessAssertionNotIndependentPrivateDataAudit=True,old117RootRemovedWhile118Running=oldGone,running118ProcessCount=len(pids),downloadsRemaining=len(downloads),installingRemaining=len(staging),protected111Preserved=True,protectionRecordUnchanged=True,hFree=shutil.disk_usage(APP).free,userFunctionalAcceptance='UNCHANGED_PENDING')
(OUT/'DAILY_UPDATE_VERIFICATION.json').write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8');print(json.dumps(value))
