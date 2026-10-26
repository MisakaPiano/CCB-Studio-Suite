from pathlib import Path
import base64,hashlib,json,shutil,sys,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';C=Path('C:/CCB-Studio-Suite-Test-118-20261027/b');FIX=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027');E=ROOT/'docs/audits/evidence/PREVIEW_117_PUBLICATION_20261027'
sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'));sys.path.insert(0,str(ROOT/'.local/windows-rc/updater-runtime-115'));sys.path.append(str(ROOT/'tools/update-runtime'))
from github_session import Session,REPO
from owned_files import delete_matching
from tuf.api.metadata import Metadata
SHA='3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d';PIN='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a';PREFIX='preview/windows-x86_64/metadata/'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
assert json.loads((OUT/'PUBLIC_CHANNEL_VERIFICATION.json').read_text(encoding='utf-8'))['status']=='PASS';assert sha(C/'studio-installer.exe')==SHA
pre=json.loads((OUT/'PREFLIGHT.json').read_text(encoding='utf-8'));state=json.loads((OUT/'RELEASE_STATE.json').read_text(encoding='utf-8'));s=Session();live=s.api('GET','/repos/'+REPO+'/releases/'+str(state['releaseId']))
assert live['prerelease'] and not live['draft'] and live['tag_name']=='v0.0.0-preview.118'
assert {x['name'] for x in live['assets']}==set(pre['assets'])
for x in live['assets']:
 expected=pre['assets'][x['name']];assert x['size']==expected['bytes'] and x.get('digest')=='sha256:'+expected['sha256']
assert '<!-- public118-verification-20261027 -->' in live['body']
ref=s.api('GET','/repos/'+REPO+'/git/ref/heads/long-term-metadata')['object']['sha'];files={}
for name in ('1.root.json','targets.json','snapshot.json','timestamp.json'):
 obj=s.api('GET','/repos/'+REPO+'/contents/'+PREFIX+name+'?ref='+ref);files[name]=base64.b64decode(obj['content'])
assert hashlib.sha256(files['1.root.json']).hexdigest()==PIN
root=Metadata.from_bytes(files['1.root.json']);roles={n:Metadata.from_bytes(files[n+'.json']) for n in ('targets','snapshot','timestamp')}
for name,m in roles.items():root.signed.verify_delegate(name,m.signed_bytes,m.signatures)
roles['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(files['targets.json']);roles['timestamp'].signed.snapshot_meta.verify_length_and_hashes(files['snapshot.json'])
target=roles['targets'].signed.targets['studio-installer.exe'];assert target.hashes['sha256']==SHA and target.unrecognized_fields['custom']['version']==118
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');readme=base64.b64decode(read['content']);assert readme.decode('utf-8').replace('\r\n','\n')==(OUT/'PUBLIC_MAIN_README_118.txt').read_text(encoding='utf-8').replace('\r\n','\n')
write(OUT/'FINAL_PUBLIC_READBACK.json',dict(status='PASS',release=state['url'],releaseId=state['releaseId'],preRelease=True,assetsUnchanged=True,confirmedInstallerSha256=SHA,metadataHead=ref,metadataVersions={n:m.signed.version for n,m in roles.items()},rootUnchanged=True,readmeBlob=read['sha'],mainOnlyReadme=True,dailyInstallation='SEPARATE_GUI_VERIFICATION_PENDING'))

public=OUT/'signed-public-metadata';public.mkdir(exist_ok=True)
for name,blob in files.items():(public/name).write_bytes(blob)
print(json.dumps(dict(status='PASS',metadataHead=ref,rootUnchanged=True,installerSha256=SHA,readme=read['sha'])))
