"""Explicit user-authorized retirement of one verified redundant build payload.
Not an updater run, baseline sweeper, or retroactive disposable registration.
"""
from pathlib import Path
from contextlib import ExitStack
import hashlib,json,shutil,sys,time
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/unused-artifacts-cleanup-20261027';CAND=ROOT/'.local/relocation-117-20261027/candidate-preview-117';TARGET=CAND/'payload'
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from install_ops import no_reparse,immutable_file,atomic_json
from owned_files import relative,held,held_parents,delete_matching,prune_empty
SHA='bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,v):atomic_json(OUT/name,v)
def overlaps(a,b):return a==b or a.is_relative_to(b) or b.is_relative_to(a)
assert ROOT==Path.cwd() and TARGET==Path('H:/CCB-Studio-Suite/.local/relocation-117-20261027/candidate-preview-117/payload')
no_reparse(TARGET);assert TARGET.is_relative_to(ROOT/'.local')
installer=no_reparse(CAND/'studio-installer.exe');assert sha(installer)==SHA
receipt=json.loads((CAND/'BUILD_RECEIPT.json').read_text());assert receipt['sha256']==SHA and receipt['version']==117 and receipt['sourceSha']=='b9b240a248d428ec158c341b8c127969fd54cb64'
published=json.loads((ROOT/'docs/audits/evidence/PREVIEW_117_PUBLICATION_20261027/RELEASE_STATE.json').read_text());assert published['tagSource']==receipt['sourceSha'] and published['assets']['CCB-Studio-Suite-0.0.0-preview.117-windows-x64.exe']['sha256']==SHA
protection_hashes={};protected=[]
for record in (ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json',ROOT/'.local/PROTECTED_ARTIFACTS.json'):
 if not record.exists():continue
 no_reparse(record);protection_hashes[str(record)]=sha(record)
 def collect(v):
  if isinstance(v,dict):
   for x in v.values():collect(x)
  elif isinstance(v,list):
   for x in v:collect(x)
  elif isinstance(v,str) and Path(v).is_absolute():protected.append(Path(v).absolute())
 collect(json.loads(record.read_text(encoding='utf-8-sig')))
assert not any(overlaps(TARGET,p) for p in protected),'Protected activity or backup overlap'
if not TARGET.exists():
 assert (CAND/'PAYLOAD_RETIRED.json').exists();print('ALREADY_RETIRED');raise SystemExit(0)
manifest=TARGET/'INSTALL_MANIFEST.json';proof=ROOT/'docs/audits/evidence/PREVIEW_117_PUBLICATION_20261027/public-raw/INSTALL_MANIFEST.json'
assert sha(manifest)==sha(proof),'Must match the manifest from actual public117 isolated installation'
value=json.loads(manifest.read_text());assert value['sourceSha']==receipt['sourceSha'] and value['version']==117 and value['channel']=='preview'
items=[];seen=set();directories=[]
for item in value['files']:
 parts=relative(item['path']);assert item['path'].casefold() not in seen;seen.add(item['path'].casefold())
 p=no_reparse(TARGET.joinpath(*parts));assert p.is_file() and p.stat().st_size==item['bytes'] and sha(p)==item['sha256'];items.append(item)
items.append(dict(path='INSTALL_MANIFEST.json',bytes=manifest.stat().st_size,sha256=sha(manifest)));expected={x['path'].casefold() for x in items};actual=set()
for p in TARGET.rglob('*'):
 no_reparse(p)
 if p.is_file():actual.add(p.relative_to(TARGET).as_posix().casefold())
 elif p.is_dir():directories.append(p.relative_to(TARGET).as_posix())
 else:raise RuntimeError('Unknown filesystem object; retain')
assert actual==expected,'Unknown or missing files; retain whole payload'
# Prove every executable can be exclusively retired BEFORE the first deletion.
for item in items:
 p=TARGET/item['path']
 if p.suffix.lower() in ('.exe','.dll','.pyd'):
  with held_parents(p),held(p,True):pass
before=shutil.disk_usage(ROOT).free
plan=dict(kind='EXPLICIT_AUTHORIZED_REDUNDANT_UNPACKED_RELEASE_COPY',target=str(TARGET),installer=str(installer),installerSha256=SHA,source=receipt['sourceSha'],manifestSha256=sha(manifest),ownership='This task Build-InternalInstaller output; every byte matches the trusted actual public117 NSIS manifest',files=items,directories=directories,protectedRecords=protection_hashes,old116Baseline='PRESERVED',userData='NOT ACCESSED',userAuthorization='Clean no-longer-needed files; retain fixed installers, evidence, daily installation and backups',beforeFreeBytes=before)
write('EXACT_RETIREMENT_PLAN.json',plan)
materials=CAND/'release-inputs';assert not materials.exists();materials.mkdir()
for name in ('INSTALL_MANIFEST.json','update-config.json','update-root.json'):
 shutil.copyfile(TARGET/name,materials/name);assert sha(materials/name)==sha(TARGET/name)
results=[];removed=0;freed=0
with immutable_file(installer) as stream:
 assert hashlib.file_digest(stream,'sha256').hexdigest()==SHA
 for item in items:
  p=TARGET/item['path']
  try:status=delete_matching(p,item)
  except (OSError,ValueError) as error:status='RETAIN_'+type(error).__name__
  results.append(dict(path=item['path'],status=status))
  if status=='REMOVED':removed+=1;freed+=item['bytes']
 prune_empty(TARGET,items,directories)
after=shutil.disk_usage(ROOT).free
for name,digest in protection_hashes.items():assert sha(Path(name))==digest
assert sha(installer)==SHA
result=dict(status='COMPLETE' if removed==len(items) and not TARGET.exists() else 'PARTIAL_SAFE_RETAIN',target=str(TARGET),removedFiles=removed,removedBytes=freed,results=results,beforeFreeBytes=before,afterFreeBytes=after,installerRetained=True,installerSha256=SHA,releaseInputs=str(materials),old116BaselineRetained=True,activityAndBackupRecordsUnchanged=True,dailyInstallationTouched=False,productionCodeChanged=False,publishedArtifactsChanged=False)
write('RESULT.json',result)
atomic_json(CAND/'PAYLOAD_RETIRED.json',{k:v for k,v in result.items() if k!='results'})
print(json.dumps({k:v for k,v in result.items() if k!='results'}),flush=True)
