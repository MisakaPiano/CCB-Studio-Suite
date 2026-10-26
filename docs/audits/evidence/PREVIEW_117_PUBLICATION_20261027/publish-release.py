from pathlib import Path
import datetime,hashlib,json,sys
from urllib.parse import urlencode
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-117-20261027';C=ROOT/'.local/relocation-117-20261027/candidate-preview-117'
sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'));sys.path.insert(0,str(C/'payload/updater/lib'))
from github_session import Session,REPO
from install_ops import atomic_json,immutable_file
SHA='bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9';SOURCE='b9b240a248d428ec158c341b8c127969fd54cb64';TAG='v0.0.0-preview.117'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(n,v):atomic_json(OUT/n,v)
pre=json.loads((OUT/'PREFLIGHT.json').read_text());assets=pre['assets'];assert pre['userConfirmed'] and pre['candidateSource']==SOURCE
for entry in assets.values():assert sha(Path(entry['localPath']))==entry['sha256']
s=Session();state_file=OUT/'RELEASE_STATE.json';state=json.loads(state_file.read_text()) if state_file.exists() else dict(source=SOURCE,tag=TAG,assets={},userApproved=True,status='NOT_CREATED')
if 'releaseId' not in state:
 releases=s.api('GET','/repos/'+REPO+'/releases?per_page=100');assert not any(x['tag_name']==TAG for x in releases),'Do not overwrite another release'
 body=dict(tag_name=TAG,target_commitish=SOURCE,name='0.0.0-preview.117 — invited Preview',body=(C/'RELEASE_NOTES.md').read_text(encoding='utf-8'),draft=True,prerelease=True)
 write('CREATE_RELEASE_REQUEST.json',dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),body=body,confirmedInstallerSha256=SHA))
 release=s.api('POST','/repos/'+REPO+'/releases',body,expected=(201,));assert release['draft'] and release['prerelease']
 state.update(releaseId=release['id'],status='DRAFT_CREATED');write('RELEASE_STATE.json',state);print('DRAFT_CREATED '+str(release['id']),flush=True)
release=s.api('GET','/repos/'+REPO+'/releases/'+str(state['releaseId']));assert release['tag_name']==TAG and release['prerelease']
for name,entry in assets.items():
 found=[a for a in s.api('GET','/repos/'+REPO+'/releases/'+str(release['id'])+'/assets') if a['name']==name]
 if found:
  assert len(found)==1 and found[0]['size']==entry['bytes'] and found[0].get('digest')=='sha256:'+entry['sha256'];asset=found[0]
 else:
  assert release['draft'];url=release['upload_url'].split('{')[0]+'?'+urlencode(dict(name=name))
  write('UPLOAD_REQUEST_'+name.replace('.','_')+'.json',dict(name=name,bytes=entry['bytes'],sha256=entry['sha256']))
  with immutable_file(Path(entry['localPath'])) as stream:
   assert hashlib.file_digest(stream,'sha256').hexdigest()==entry['sha256'];stream.seek(0)
   asset=s.api('POST',url,expected=(201,),stream=stream,length=entry['bytes'])
  assert asset['size']==entry['bytes'] and asset.get('digest')=='sha256:'+entry['sha256']
 state['assets'][name]=dict(id=asset['id'],bytes=asset['size'],sha256=entry['sha256']);write('RELEASE_STATE.json',state);print('ASSET_VERIFIED '+name,flush=True)
actual=s.api('GET','/repos/'+REPO+'/releases/'+str(release['id'])+'/assets');assert {x['name'] for x in actual}==set(assets)
assert all(x['size']==assets[x['name']]['bytes'] and x.get('digest')=='sha256:'+assets[x['name']]['sha256'] for x in actual)
if release['draft']:
 write('PUBLISH_RELEASE_REQUEST.json',dict(releaseId=release['id'],confirmedInstallerSha256=SHA,userConfirmed=True,assetVerification='ALL_FOUR_PASS'))
 release=s.api('PATCH','/repos/'+REPO+'/releases/'+str(release['id']),dict(draft=False,prerelease=True))
assert release['prerelease'] and not release['draft']
obj=s.api('GET','/repos/'+REPO+'/git/ref/tags/'+TAG)['object']
if obj['type']=='tag':obj=s.api('GET','/repos/'+REPO+'/git/tags/'+obj['sha'])['object']
assert obj['type']=='commit' and obj['sha']==SOURCE
state.update(status='PUBLIC_PRERELEASE_METADATA_PENDING',url=release['html_url'],publishedAt=release['published_at'],tagSource=obj['sha'],preRelease=True,draft=False)
write('RELEASE_STATE.json',state);print(json.dumps(state),flush=True)
