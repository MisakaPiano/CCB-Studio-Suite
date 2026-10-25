"""UPD-CLEAN-PROTECTION-001: three fresh synthetic fixtures against installed updater bytes.
No actual installation record, user directory, backup, credential, network or process action.
"""
from pathlib import Path
import hashlib,json,sys,time,datetime
OUT=Path('H:/CCB-Studio-Suite/.local/candidate-review-20261026');LIB=OUT/'isolated-install/updater/lib'
sys.path.insert(0,str(LIB))
from residue_cleanup import run,enroll_program
from install_ops import atomic_json,verify_install
from studio_update import UpdateRejected
BASE=OUT/'protection-fixtures';BASE.mkdir()
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,v):atomic_json(p,v)
class Fixture:
 def __init__(self,name):
  self.base=BASE/name;self.programs=self.base/'programs';self.programs.mkdir(parents=True)
  self.data=self.base/'data';self.runtime=self.base/'runtime';self.cache=self.runtime/'updates'
  self.data.mkdir();self.cache.mkdir(parents=True)
  (self.data/'original.bin').write_bytes(b'new synthetic original; never user data')
  (self.programs/'backups').mkdir();(self.programs/'backups/keep.bin').write_bytes(b'synthetic backup')
  self.digest=hashlib.sha256(b'new synthetic installer identity').hexdigest()
  self.program={};self.identity={}
  for v,ch in ((111,'a'),(115,'b'),(116,'c')):
   expected=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=v,sourceSha=ch*40)
   p=self.programs/(str(v)+'-'+ch*12);(p/'app').mkdir(parents=True);entries=[]
   for name in ('CCBStudioSuitePreview.exe','app/main.jar'):
    q=p/name;q.write_bytes((str(v)+':'+name).encode());entries.append(dict(path=name,bytes=q.stat().st_size,sha256=sha(q)))
   write(p/'INSTALL_MANIFEST.json',dict(format=1,**expected,files=entries));(p/'INSTALL_COMPLETE').write_text(str(v)+':'+ch*40);(p/'Uninstall.exe').write_bytes(b'new synthetic uninstaller')
   enroll_program(self.cache,self.programs,p,expected,self.digest,self.cache/('install-'+str(v)+'.json'))
   self.program[v]=p;self.identity[v]=expected
  self.protection=self.programs/'ACTIVE_INSTALLATION.json'
  write(self.protection,dict(doNotCleanReferencedDirectories=True,program=str(self.program[111]),workspace=str(self.data),runtime=str(self.runtime),retainedBackupWorkspace=str(self.programs/'backups')))
 def complete(self,old,new,nonce='d'*64):
  folder=self.cache/'handoffs'/str(new);folder.mkdir(parents=True)
  journal=self.cache/('install-'+str(new)+'.json')
  write(journal,dict(status='INSTALLED_NOT_STARTED',expected=self.identity[new],target=str(self.program[new]),verification=verify_install(self.program[new],self.identity[new])))
  write(folder/'plan.json',dict(format=1,nonce=nonce,installed=str(self.program[old]),cache=str(self.cache),approved=dict(**self.identity[new],sha256=self.digest)))
  write(folder/'result.json',dict(status='COMPLETE',nonce=nonce,installed=str(self.program[new]),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
  return folder
 def current(self,v):write(self.programs/'launch-current.json',dict(installed=str(self.program[v]),data=str(self.data),runtime=str(self.runtime)))
 def cleanup(self,v):return run(self.program[v],self.cache,self.data,self.runtime)
 def protected(self):
  paths=[self.protection,self.data/'original.bin',self.programs/'backups/keep.bin']+list(p for p in self.program[111].rglob('*') if p.is_file())
  return {str(p.relative_to(self.base)):sha(p) for p in paths}
results=[]
def case(name,body):
 start=time.monotonic()
 try:detail=body();result=dict(name=name,status='PASS',seconds=time.monotonic()-start,**detail)
 except BaseException as e:
  result=dict(name=name,status='FAILED',seconds=time.monotonic()-start,error=repr(e));results.append(result)
  (OUT/'PROTECTION_CASES.json').write_text(json.dumps(results,indent=2)+'\n');raise
 results.append(result)
def protected111():
 f=Fixture('protected111-default115');f.complete(111,115);f.current(115);before=f.protected();first=f.cleanup(115);repeat=f.cleanup(115)
 assert f.protected()==before and first['removed']==repeat['removed']==0
 return dict(first=first,repeat=repeat,protectedHashes=before)
def retire115():
 f=Fixture('complete115-to116');f.complete(111,115);f.complete(115,116,nonce='e'*64);f.current(116);before=f.protected()
 (f.program[115]/'unknown-user-note.txt').write_bytes(b'unknown must remain')
 # Unknown member is intentionally added only after the synthetic installed manifest was pinned.
 first=f.cleanup(116);repeat=f.cleanup(116)
 assert first['removed']>0 and repeat['removed']==0 and f.protected()==before
 assert not (f.program[115]/'app/main.jar').exists() and (f.program[115]/'unknown-user-note.txt').read_bytes()==b'unknown must remain'
 return dict(first=first,repeat=repeat,protectedHashes=before,unknownFileKept=True)
def rejectUnproven():
 f=Fixture('unproven-and-bad-record');f.complete(111,115);folder=f.complete(115,116,nonce='f'*64);f.current(116);before=f.protected();oldsha=sha(f.program[115]/'app/main.jar')
 result=json.loads((folder/'result.json').read_text());result['nonce']='0'*64;write(folder/'result.json',result)
 first=f.cleanup(116);assert first['removed']==0 and sha(f.program[115]/'app/main.jar')==oldsha and f.protected()==before
 write(f.protection,dict(program='relative-not-allowed'))
 try:f.cleanup(116)
 except UpdateRejected:rejected=True
 else:rejected=False
 assert rejected and sha(f.program[115]/'app/main.jar')==oldsha
 return dict(first=first,invalidRecordRejected=True,oldProgramRetained=True,protectedHashesBefore=before)
case('UPD-CLEAN-PROTECTION-001/protected111-default115',protected111)
case('UPD-CLEAN-PROTECTION-001/complete115-to116-retire-only115',retire115)
case('UPD-CLEAN-PROTECTION-001/unproven-and-invalid-record-retain',rejectUnproven)
(OUT/'PROTECTION_CASES.json').write_text(json.dumps(results,indent=2)+'\n')
(OUT/'PROTECTION_SUMMARY.json').write_text(json.dumps(dict(status='PASS',cases=len(results),failures=0,source='80ea1167cdc197db30980ed116ee6d30731c994a',enginePath=str(LIB/'residue_cleanup.py'),engineSha256=sha(LIB/'residue_cleanup.py'),scope='native Windows filesystem primitives and packaged production decision functions; all protocol records/exe bytes synthetic; not real115 upgrade or real ACTIVE record migration',junitMethods=0,realDataAccessed=False,privateCredentialsRead=False,fixtures='RETAINED',independentReview='NOT REQUESTED'),indent=2)+'\n')
print('PROTECTION CASES PASS=3; real installation untouched',flush=True)