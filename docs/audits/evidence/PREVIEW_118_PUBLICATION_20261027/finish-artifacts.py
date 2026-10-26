from pathlib import Path
import os,sys,json,hashlib,shutil,subprocess,datetime,stat
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';C=Path('C:/CCB-Studio-Suite-Test-118-20261027');E=ROOT/'docs/audits/evidence/PREVIEW_118_PUBLICATION_20261027'
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from owned_files import delete_matching,prune_empty,held_parents
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
assert json.loads((OUT/'DAILY_UPDATE_VERIFICATION.json').read_text(encoding='utf-8'))['status']=='PASS'
assert json.loads((C/'OWNER.json').read_text(encoding='utf-8'))['kind']=='AUTHORIZED_SUITE_118_PUBLICATION_FIXTURE'
for p in [C,*C.parents]:assert not p.is_symlink() and not p.is_junction()
running=subprocess.check_output(['powershell','-NoProfile','-Command',"Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -like 'C:\\CCB-Studio-Suite-Test-118-20261027\\*' } | Select-Object -ExpandProperty ProcessId"],text=True).strip();assert not running
returns=[(C/'b/studio-installer.exe',ROOT/'.local/preview-candidate-118-20261027/candidate-r3/studio-installer.exe','3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d'),(C/'retained117/studio-installer.exe',ROOT/'.local/relocation-117-20261027/candidate-preview-117/studio-installer.exe','bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9')]
restored=[]
for source,dest,digest in returns:
 assert source.is_file() and sha(source)==digest and not dest.exists()
 shutil.copy2(source,dest);assert sha(dest)==digest and dest.stat().st_size==source.stat().st_size
 restored.append(dict(source=str(source),restored=str(dest),bytes=dest.stat().st_size,sha256=digest))
write(OUT/'FIXED_INSTALLERS_RESTORED.json',dict(status='PASS',files=restored,retainedFinal118AndOriginal117=True))
assert not E.exists();E.mkdir(parents=True);(E/'.gitattributes').write_text('* -text\n',encoding='utf-8')
# Copy only audit records and scripts; no runtime, credentials, private daily data or signing custody.
for p in OUT.iterdir():
 if p.is_file() and p.suffix in ('.py','.java','.json','.log','.txt','.md'):
  assert p.stat().st_size<6*1024**2
  shutil.copyfile(p,E/p.name)
for folder in ('public-native','signed-public-metadata'):
 for p in (OUT/folder).rglob('*'):
  if p.is_file() and p.suffix in ('.json','.jsonl','.log','.txt'):
   dest=E/folder/p.relative_to(OUT/folder);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
for folder in OUT.glob('metadata-attempt-*'):
 p=folder/'PREPARED.json'
 if p.exists():dest=E/folder.name;dest.mkdir();shutil.copyfile(p,dest/p.name)
# Inventory this exclusive fixture without traversing any reparse point.
files=[];dirs=[];links=[]
def scan(base):
 with os.scandir(base) as entries:
  for entry in entries:
   p=Path(entry.path);assert p.absolute().is_relative_to(C);info=entry.stat(follow_symlinks=False)
   if info.st_file_attributes&stat.FILE_ATTRIBUTE_REPARSE_POINT:
    target=os.readlink(p);resolved=Path(target.removeprefix('\\\\?\\'))
    assert p.is_junction() and resolved.is_relative_to(C)
    links.append(dict(path=p.relative_to(C).as_posix(),target=target));continue
   if entry.is_dir(follow_symlinks=False):dirs.append(p.relative_to(C).as_posix());scan(p)
   elif entry.is_file(follow_symlinks=False):files.append(dict(path=p.relative_to(C).as_posix(),bytes=info.st_size,sha256=sha(p)))
   else:raise AssertionError('Unknown fixture object')
scan(C)
inventory=dict(root=str(C),owner='AUTHORIZED_SUITE_118_PUBLICATION_FIXTURE',files=files,directories=dirs,links=links,evidenceRetained=True,originalFixedInstallersRestored=True)
write(OUT/'PUBLIC_FIXTURE_RETIREMENT_MANIFEST.json',inventory);write(E/'PUBLIC_FIXTURE_RETIREMENT_MANIFEST.json',inventory)
for item in files:assert delete_matching(C/item['path'],item)=='REMOVED',item['path']
for link in links:
 p=C/link['path'];assert p.is_junction() and os.readlink(p)==link['target']
 with held_parents(p):os.rmdir(p)
prune_empty(C,files,dirs);assert not C.exists()
result=dict(status='COMPLETE',files=len(files),bytes=sum(x['bytes'] for x in files),linksNonrecursivelyUnlinked=len(links),rootGone=True,unknownFilesDeleted=0,protectedDailyBackupsAndBaselinesUntouched=True,fixedInstallersRestored=True,hFree=shutil.disk_usage(ROOT).free,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat())
write(OUT/'PUBLIC_FIXTURE_RETIREMENT.json',result);write(E/'PUBLIC_FIXTURE_RETIREMENT.json',result)
print(json.dumps(result))
