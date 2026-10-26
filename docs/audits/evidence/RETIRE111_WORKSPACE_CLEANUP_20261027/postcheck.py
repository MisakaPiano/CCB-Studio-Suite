from pathlib import Path
import sys,json,hashlib,shutil,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/retire111-workspace-20261027';APP=Path('H:/CCB-Studio-Suite-App')
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
import workspace_artifacts as a
from install_ops import verify_install
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
current=a.read(APP/'launch-current.json');program=APP/'118-a1202e24f7a0';assert current['version']==118 and Path(current['installed'])==program
check=verify_install(program,a.read(program/'update-config.json'));assert check['manifestSha256']=='8af3972681326b95fa2faa97dfea07d66bb28c1e3b4a45e85ed937b65967021c'
assert not (APP/'111-1c9385afc38c').exists()
for i,p in enumerate([APP/'ACTIVE_INSTALLATION.json',ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json']):
 before=a.read(OUT/('protected-record-'+str(i)+'-before.private.json'));after=a.read(p)
 assert after==dict(before,program=str(program)) and after['doNotCleanReferencedDirectories'] is True
w=a.read(OUT/'WORKSPACE_RETIREMENT_RESULT.json');old=a.read(OUT/'RETIRE111_RESULT.json')
for copy in w['oldProgramCopies']:assert not Path(copy['root']).exists()
for name in ('appearance-gear-20261026','appearance-text-actions-20261026','token-candidate-20261026'):
 package=ROOT/'.local'/name/'candidate';receipt=a.read(package/'BUILD_RECEIPT.json');assert sha(package/'studio-installer.exe')==receipt['sha256']
 assert (package/'release-inputs/INSTALL_MANIFEST.json').exists()
for name,digest in [('preview-candidate-118-20261027/candidate-r3','3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d'),('relocation-117-20261027/candidate-preview-117','bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9')]:assert sha(ROOT/'.local'/name/'studio-installer.exe')==digest
temp=[]
for p in [OUT/'PROOF_FIXTURE_RETIREMENT.json',*sorted(OUT.glob('WORKSPACE_PROOF_RETIRED_*.json'))]:
 v=a.read(p);assert v['status']=='COMPLETE';temp.append(v)
value=dict(status='COMPLETE',applicationVersion=118,installedProgram=check,dailyProgramDirectories=[p.name for p in APP.iterdir() if p.is_dir() and p.name!='data'],old111Gone=True,protectionRecordsMigrated=2,dataRuntimeBackupFieldsUnchanged=True,privateDataOrCredentialsRead=False,oldProgramCopiesRetired=6,oldFilesRemoved=old['files']+w['totalOldProgramFiles']+sum(x['removed'] for x in w['successfulDisposables']),netOldBytesRemoved=old['bytesFreed']+w['totalOldProgramBytes']+sum(x['bytesFreed'] for x in w['successfulDisposables']),temporaryProofFilesRetired=sum(x['removed'] for x in temp),temporaryProofBytesRetired=sum(x['bytesFreed'] for x in temp),unknownFilesDeleted=0,preserved=['fixed install packages and 107/114/115/116 compatibility baselines','review evidence and failure records','failed/unsealed disposable payloads','user data, configuration, credentials, plugins and backups','source and signing custody','shared build dependency caches'],hFreeObserved=shutil.disk_usage(ROOT).free,hFreeObservationIsNotNetCleanupAccounting=True,noProductionSourceChange=True,newRelease=False,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat())
a.write(OUT/'POSTCHECK.json',value);print(json.dumps(value,indent=2))
