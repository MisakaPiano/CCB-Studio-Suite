from pathlib import Path
import sys,os,json,hashlib,msvcrt,datetime
from contextlib import contextmanager
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/retire111-workspace-20261027';APP=Path('H:/CCB-Studio-Suite-App');OLD=APP/'111-1c9385afc38c';CURRENT=APP/'118-a1202e24f7a0'
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
from install_ops import no_reparse,verify_install,atomic_json
from studio_update import exclusive
from owned_files import delete_matching,prune_empty
import workspace_artifacts as a
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
proof=a.read(OUT/'UNINSTALL111_PROOF.json');assert proof['status']=='PASS' and proof['oldUninstallMatches']
no_reparse(OLD);no_reparse(CURRENT);assert OLD.parent==APP and OLD.name=='111-1c9385afc38c'
config=a.read(CURRENT/'update-config.json');assert config['version']==118 and config['sourceSha']=='a1202e24f7a0cba8a53f43bf035b67e2134ca2aa'
default=(APP/'launch-current.json').read_bytes();assert Path(json.loads(default)['installed'])==CURRENT
currentProof=verify_install(CURRENT,config);manifest=a.read(OLD/'INSTALL_MANIFEST.json');assert sha(OLD/'INSTALL_MANIFEST.json')==proof['manifestSha256']
files=list(manifest['files'])
for name in ('INSTALL_MANIFEST.json','INSTALL_COMPLETE','Uninstall.exe'):
 p=OLD/name;files.append(dict(path=name,bytes=p.stat().st_size,sha256=sha(p)))
assert sha(OLD/'Uninstall.exe')==proof['uninstallSha256']
actual=[];dirs=[]
for p,isdir in a.walk(OLD):
 if isdir:dirs.append(p.relative_to(OLD).as_posix())
 else:actual.append(p.relative_to(OLD).as_posix())
assert set(actual)=={x['path'] for x in files} and len(actual)==len(files)
for x in files:
 p=OLD/x['path'];assert p.stat().st_size==x['bytes'] and sha(p)==x['sha256']
plan=dict(root=str(OLD),files=files,directories=dirs,manifestSha256=proof['manifestSha256'],installerSha256=proof['installerSha256'],uninstallIndependentlyProved=True,unknownFiles=0,userAuthorized=True)
a.write(OUT/'EXACT111_RETIREMENT_PLAN.json',plan)
records=[APP/'ACTIVE_INSTALLATION.json',ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json'];before=[]
for i,p in enumerate(records):
 raw=no_reparse(p).read_bytes();v=json.loads(raw);assert Path(v['program'])==OLD and v['doNotCleanReferencedDirectories'] is True
 (OUT/('protected-record-'+str(i)+'-before.private.json')).write_bytes(raw)
 before.append((p,raw,v))
@contextmanager
def entry_lock():
 with (APP/'entrypoints.lock').open('r+b') as stream:
  stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
  try:yield
  finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
with exclusive(APP/'data/runtime/updates'),entry_lock():
 assert (APP/'launch-current.json').read_bytes()==default
 for p,raw,v in before:assert p.read_bytes()==raw
 # Change only the stale program pointer; all data/runtime/backup and protection flags persist.
 for p,raw,v in before:
  new=dict(v,program=str(CURRENT));atomic_json(p,new)
  assert json.loads(p.read_text(encoding='utf-8'))==new
 migration=dict(status='MIGRATED_PROGRAM_POINTER_ONLY',records=len(records),oldVersion=111,currentVersion=118,defaultEntryUnchanged=True,allOtherFieldsUnchanged=True,doNotCleanReferencedDirectoriesRemainsTrue=True,backupsAndDataNotTraversed=True,sourceManifestSha256=proof['manifestSha256'])
 a.write(OUT/'PROTECTION_MIGRATION.json',migration)
 removed=0;bytesFreed=0;retained=[]
 for x in files:
  try:status=delete_matching(OLD/x['path'],x)
  except (OSError,ValueError) as error:status=type(error).__name__
  if status=='REMOVED':removed+=1;bytesFreed+=x['bytes']
  else:retained.append(dict(path=x['path'],status=status))
 directoryResults=prune_empty(OLD,files,dirs)
 assert not retained and not OLD.exists(),retained
 assert (APP/'launch-current.json').read_bytes()==default
 for p,raw,v in before:
  new=json.loads(p.read_text(encoding='utf-8'));assert new==dict(v,program=str(CURRENT))
assert verify_install(CURRENT,config)==currentProof
value=dict(status='COMPLETE',root=str(OLD),old111RootGone=True,files=removed,bytesFreed=bytesFreed,unknownFilesDeleted=0,protectionMigration=migration,current118ProgramUnchanged=True,default118EntryUnchanged=True,userDataOrCredentialContentRead=False,backupsNotTraversed=True,directories=directoryResults,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat())
a.write(OUT/'RETIRE111_RESULT.json',value)
fixture=Path(proof['fixture']);a.seal(fixture,[OUT/'UNINSTALL111_PROOF.json',OUT/'RETIRE111_RESULT.json'],'PASS');cleanup=a.cleanup(fixture);assert cleanup['status']=='COMPLETE';a.write(OUT/'PROOF_FIXTURE_RETIREMENT.json',cleanup)
print(json.dumps({k:v for k,v in value.items() if k!='directories'}))
