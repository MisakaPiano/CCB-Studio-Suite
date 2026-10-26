from pathlib import Path
import base64,hashlib,json,shutil,sys,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-117-20261027';C=ROOT/'.local/relocation-117-20261027/candidate-preview-117';FIX=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027');E=ROOT/'docs/audits/evidence/PREVIEW_117_PUBLICATION_20261027'
sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'));sys.path.insert(0,str(C/'payload/updater/lib'));sys.path.append(str(ROOT/'tools/update-runtime'))
from github_session import Session,REPO
from owned_files import delete_matching
from tuf.api.metadata import Metadata
SHA='bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9';PIN='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a';PREFIX='preview/windows-x86_64/metadata/'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
assert json.loads((OUT/'PUBLIC_CHANNEL_VERIFICATION.json').read_text())['status']=='PASS';assert sha(C/'studio-installer.exe')==SHA
pre=json.loads((OUT/'PREFLIGHT.json').read_text());state=json.loads((OUT/'RELEASE_STATE.json').read_text());s=Session();live=s.api('GET','/repos/'+REPO+'/releases/'+str(state['releaseId']))
assert live['prerelease'] and not live['draft'] and live['tag_name']=='v0.0.0-preview.117'
assert {x['name'] for x in live['assets']}==set(pre['assets'])
for x in live['assets']:
 expected=pre['assets'][x['name']];assert x['size']==expected['bytes'] and x.get('digest')=='sha256:'+expected['sha256']
assert '<!-- public117-verification-20261027 -->' in live['body']
ref=s.api('GET','/repos/'+REPO+'/git/ref/heads/long-term-metadata')['object']['sha'];files={}
for name in ('1.root.json','targets.json','snapshot.json','timestamp.json'):
 obj=s.api('GET','/repos/'+REPO+'/contents/'+PREFIX+name+'?ref='+ref);files[name]=base64.b64decode(obj['content'])
assert hashlib.sha256(files['1.root.json']).hexdigest()==PIN
root=Metadata.from_bytes(files['1.root.json']);roles={n:Metadata.from_bytes(files[n+'.json']) for n in ('targets','snapshot','timestamp')}
for name,m in roles.items():root.signed.verify_delegate(name,m.signed_bytes,m.signatures)
roles['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(files['targets.json']);roles['timestamp'].signed.snapshot_meta.verify_length_and_hashes(files['snapshot.json'])
target=roles['targets'].signed.targets['studio-installer.exe'];assert target.hashes['sha256']==SHA and target.unrecognized_fields['custom']['version']==117
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');readme=base64.b64decode(read['content']);assert readme==(OUT/'PUBLIC_MAIN_README_117.txt').read_text(encoding='utf-8').encode('utf-8')
write(OUT/'FINAL_PUBLIC_READBACK.json',dict(status='PASS',release=state['url'],releaseId=state['releaseId'],preRelease=True,assetsUnchanged=True,confirmedInstallerSha256=SHA,metadataHead=ref,metadataVersions={n:m.signed.version for n,m in roles.items()},rootUnchanged=True,readmeBlob=read['sha'],mainOnlyReadme=True,dayInstallation='NOT ACCESSED OR UPDATED'))
assert not E.exists();E.mkdir(parents=True);(E/'.gitattributes').write_text('* -text\n')
for p in OUT.iterdir():
 if p.is_file() and p.suffix in ('.py','.json','.log','.txt'):shutil.copyfile(p,E/p.name)
for folder in ('public-raw',):
 for p in (OUT/folder).rglob('*'):
  if p.is_file():dest=E/folder/p.relative_to(OUT/folder);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
metadata=E/'signed-public-metadata';metadata.mkdir()
for name,blob in files.items():(metadata/name).write_bytes(blob)
publication=json.loads((OUT/'METADATA_PUBLICATION.json').read_text());prepared=Path(publication['preparedGeneration'])
for p in prepared.iterdir():
 if p.is_file() and p.name.endswith(('.targets.json','.snapshot.json')):shutil.copyfile(p,metadata/p.name)
for folder in OUT.glob('metadata-attempt-*'):
 if (folder/'PREPARED.json').exists():dest=E/folder.name;dest.mkdir();shutil.copyfile(folder/'PREPARED.json',dest/'PREPARED.json')
owner=json.loads((FIX/'OWNER.json').read_text());assert owner==dict(kind='EXPLICIT_SUITE_117_PUBLIC_CHAIN_FIXTURE',suite=str(ROOT),task='preview-publication-117-20261027',dailyInstallation=False)
for p in [FIX,*FIX.parents]:assert not p.is_symlink() and not p.is_junction()
items=[];dirs=[]
for p in FIX.rglob('*'):
 assert not p.is_symlink() and not p.is_junction()
 if p.is_file():items.append(dict(path=p.relative_to(FIX).as_posix(),bytes=p.stat().st_size,sha256=sha(p)))
 elif p.is_dir():dirs.append(p)
write(E/'PUBLIC_FIXTURE_RETIREMENT_MANIFEST.json',dict(root=str(FIX),kind=owner['kind'],files=items,evidenceRetained=True,notUserProfile=True))
for item in items:assert delete_matching(FIX/item['path'],item)=='REMOVED'
for p in sorted(dirs,key=lambda p:len(p.parts),reverse=True):p.rmdir()
FIX.rmdir();write(E/'PUBLIC_FIXTURE_RETIREMENT.json',dict(status='COMPLETE',files=len(items),bytes=sum(x['bytes'] for x in items),rootGone=True,originalBaselineUnmodified=True))
final=dict(status='PUBLISHED_PUBLIC_CHAIN_VERIFIED_DAILY_NOT_UPDATED',version='0.0.0-preview.117',source='b9b240a248d428ec158c341b8c127969fd54cb64',appBuildSource='c601d50a0b57de765353c6f0e600ddc2e9807738',candidateEvidence='3d4a97f7c6303bf7c81798abbabfdc7057312a36',release=state['url'],releaseId=state['releaseId'],installerSha256=SHA,installerBytes=169002371,metadataCommit=publication['commit'],readmeCommit=json.loads((OUT/'README_PUBLICATION.json').read_text())['commit'],rootSha256=PIN,publicChain='Actual116 bundled client -> real signed public117 -> old helper NSIS isolated installation ->117 UP_TO_DATE',publicNativeGui='NOT TESTED IN THIS CASE',candidateNativeEvidence='RELOCATION_PREVIEW_117_20261027 retained; not rerun or counted again',daily116='NOT ACCESSED OR UPDATED',pending=['asset/editor full human acceptance','115 experience','Prompt comparison','actual user taskbar pin after move','independent review not requested'],crossRootGone=True,hFree=shutil.disk_usage(ROOT).free,hDailyUpdateEstimatedPeak=169002371+252893352+64*1024**2,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat())
write(OUT/'FINAL_PUBLICATION_RESULT.json',final);write(E/'FINAL_PUBLICATION_RESULT.json',final)
entries=[dict(path=p.relative_to(E).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(E.rglob('*')) if p.is_file()];write(E/'RAW_MANIFEST.json',dict(format=1,files=entries))
print(json.dumps(dict(status=final['status'],rawEvidenceFiles=len(entries),retiredFiles=len(items),hFree=final['hFree'],dailyUpgradeHeadroom=final['hFree']>=final['hDailyUpdateEstimatedPeak'])),flush=True)
