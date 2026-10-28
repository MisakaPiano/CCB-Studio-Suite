from pathlib import Path
import sys,json,base64,hashlib
r=Path('H:/CCB-Studio-Suite');o=Path(__file__).parent
sys.path[:0]=[str(r/'.local/preview-publication-116-20261026'),str(r/'.local/windows-rc/updater-runtime-115')]
from github_session import Session,REPO
from tuf.api.metadata import Metadata
s=Session();release=json.loads((o/'RELEASE_STATE.json').read_text());pre=json.loads((o/'PREFLIGHT.json').read_text());native=json.loads((o/'PUBLIC_CHANNEL_VERIFICATION.json').read_text());assert native['status']=='PASS'
live=s.api('GET','/repos/'+REPO+'/releases/'+str(release['releaseId']));assert live['prerelease'] and not live['draft'] and live['tag_name']=='v0.0.0-preview.119'
assert '<!-- public119-verification-20261028 -->' in live['body'];assert {a['name'] for a in live['assets']}==set(pre['assets'])
for a in live['assets']:
 expected=pre['assets'][a['name']];assert a['size']==expected['bytes'] and a.get('digest')=='sha256:'+expected['sha256']
ref=s.api('GET','/repos/'+REPO+'/git/ref/heads/long-term-metadata')['object']['sha'];files={}
for name in ['1.root.json','targets.json','snapshot.json','timestamp.json']:
 value=s.api('GET','/repos/'+REPO+'/contents/preview/windows-x86_64/metadata/'+name+'?ref='+ref);files[name]=base64.b64decode(value['content'])
pin='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a';assert hashlib.sha256(files['1.root.json']).hexdigest()==pin
root=Metadata.from_bytes(files['1.root.json']);roles={n:Metadata.from_bytes(files[n+'.json']) for n in ['targets','snapshot','timestamp']}
for n,m in roles.items():root.signed.verify_delegate(n,m.signed_bytes,m.signatures)
roles['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(files['targets.json']);roles['timestamp'].signed.snapshot_meta.verify_length_and_hashes(files['snapshot.json'])
t=roles['targets'].signed.targets['studio-installer.exe'];assert t.hashes['sha256']==native['installerSha256'] and t.unrecognized_fields['custom']['version']==119
read=s.api('GET','/repos/'+REPO+'/contents/README.md?ref=main');assert base64.b64decode(read['content']).decode()==(o/'PUBLIC_MAIN_README_119.txt').read_text(encoding='utf-8')
pub=o/'signed-public-metadata';pub.mkdir(exist_ok=True)
for n,b in files.items():(pub/n).write_bytes(b)
result=dict(status='PASS',release=release['url'],releaseId=live['id'],tagSource=release['tagSource'],installerSha256=t.hashes['sha256'],assetsUnchanged=True,preRelease=True,metadataHead=ref,metadataVersions={n:m.signed.version for n,m in roles.items()},rootUnchanged=True,rootSha256=pin,readmeBlob=read['sha'],mainOnlyReadme=True,public118To119='PASS',dailyInstallation='NOT_MODIFIED')
(o/'FINAL_PUBLIC_READBACK.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
