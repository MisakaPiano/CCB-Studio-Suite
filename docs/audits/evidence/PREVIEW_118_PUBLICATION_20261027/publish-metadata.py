from pathlib import Path
import base64,datetime,hashlib,json,sys,uuid
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';C=Path('C:/CCB-Studio-Suite-Test-118-20261027/b')
sys.path.insert(0,str(ROOT/'.local/preview-publication-116-20261026'));sys.path.insert(0,str(C/'payload/updater/lib'));sys.path.append(str(ROOT/'tools/update-runtime'))
from github_session import Session,REPO,ApiError
from github_transport import GitHubFetcher
from release_repository import prepare,current,target_from_build
from signing_custody import local_signer
from install_ops import atomic_json
from tuf.api.metadata import Metadata
from tuf.api.metadata import TargetFile
PIN='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a';SHA='3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d';PREFIX='preview/windows-x86_64/metadata/'
def write(name,v):atomic_json(OUT/name,v)
def note(t):print(t,flush=True)
state=json.loads((OUT/'RELEASE_STATE.json').read_text());assert not state['draft'] and state['preRelease'] and state['tagSource']=='a1202e24f7a0cba8a53f43bf035b67e2134ca2aa'
target=target_from_build(C,(C/'RELEASE_NOTES.md').read_text(encoding='utf-8'),state['tag'],'CCB-Studio-Suite-0.0.0-preview.118-windows-x64.exe')
assert target.hashes['sha256']==SHA
if not (OUT/'PUBLIC_INSTALLER_BYTE_VERIFICATION.json').exists():
 fetcher=GitHubFetcher(REPO,'preview',metadata_branch='long-term-metadata');fetcher.authorize(target);digest=hashlib.sha256();size=0
 for chunk in fetcher.fetch(fetcher.targets+target.path):digest.update(chunk);size+=len(chunk)
 assert digest.hexdigest()==SHA and size==target.length
 write('PUBLIC_INSTALLER_BYTE_VERIFICATION.json',dict(status='PASS',bytes=size,sha256=digest.hexdigest(),source='Actual public GitHub release attachment streamed; no duplicate installer retained',tufChannel='Not switched at this step',providerCalls=0));note('PUBLIC_INSTALLER_BYTES_PASS')
s=Session();bootstrap=(C/'payload/update-root.json').read_bytes();assert hashlib.sha256(bootstrap).hexdigest()==PIN
assert hashlib.sha256((ROOT/'.local/signing-custody-v1/root.json').read_bytes()).hexdigest()==PIN
# Controlled adapter only: existing approved roles, memory-only, no root signer/rotation.
signers={role:local_signer(ROOT/'.local/signing-custody-v1',role) for role in ('targets','snapshot','timestamp')}
for attempt in range(1,4):
 folder=OUT/('metadata-attempt-'+str(attempt));folder.mkdir()
 ref=s.api('GET','/repos/'+REPO+'/git/ref/heads/long-term-metadata');parent=ref['object']['sha'];commit=s.api('GET','/repos/'+REPO+'/git/commits/'+parent);base_tree=commit['tree']['sha']
 def content(name):
  value=s.api('GET','/repos/'+REPO+'/contents/'+PREFIX+name+'?ref='+parent);return base64.b64decode(value['content'])
 files={name:content(name) for name in ('1.root.json','targets.json','snapshot.json','timestamp.json')}
 assert files['1.root.json']==bootstrap
 root=Metadata.from_bytes(bootstrap);old={role:Metadata.from_bytes(files[role+'.json']) for role in ('targets','snapshot','timestamp')}
 for role,m in old.items():root.signed.verify_delegate(role,m.signed_bytes,m.signatures)
 old['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(files['targets.json']);old['timestamp'].signed.snapshot_meta.verify_length_and_hashes(files['snapshot.json'])
 version=old['targets'].signed.targets['studio-installer.exe'].unrecognized_fields['custom']['version'];assert version==117,'Channel advanced; do not overwrite or regress'
 store=folder/'store';gen=uuid.uuid4().hex;prior=store/'generations'/gen;prior.mkdir(parents=True)
 for name,blob in files.items():(prior/name).write_bytes(blob)
 atomic_json(store/'CURRENT.json',dict(generation=gen,repository=REPO,channel='preview',rootVersion=1,files={name:hashlib.sha256(blob).hexdigest() for name,blob in files.items()}))
 receipt=prepare(store,bootstrap,REPO,'preview',signers,target=target);prepared,blobs=current(store);assert blobs['1.root.json']==bootstrap
 changed={name:blob for name,blob in blobs.items() if name!='1.root.json' and (name not in files or blob!=files[name])}
 assert set(changed)<= {'targets.json','snapshot.json','timestamp.json',str(old['targets'].signed.version+1)+'.targets.json',str(old['snapshot'].signed.version+1)+'.snapshot.json'}
 tree=[]
 for name,blob in changed.items():
  obj=s.api('POST','/repos/'+REPO+'/git/blobs',dict(content=base64.b64encode(blob).decode(),encoding='base64'),expected=(201,))
  tree.append(dict(path=PREFIX+name,mode='100644',type='blob',sha=obj['sha']))
 new_tree=s.api('POST','/repos/'+REPO+'/git/trees',dict(base_tree=base_tree,tree=tree),expected=(201,))
 new_commit=s.api('POST','/repos/'+REPO+'/git/commits',dict(message='Publish Preview118 verified installer target; retain existing root',tree=new_tree['sha'],parents=[parent]),expected=(201,))
 atomic_json(folder/'PREPARED.json',dict(parent=parent,commit=new_commit['sha'],files={name:hashlib.sha256(blob).hexdigest() for name,blob in changed.items()},receipt=receipt,rootUnchanged=True,signerRoles=list(signers)))
 try:updated=s.api('PATCH','/repos/'+REPO+'/git/refs/heads/long-term-metadata',dict(sha=new_commit['sha'],force=False))
 except ApiError:
  latest=s.api('GET','/repos/'+REPO+'/git/ref/heads/long-term-metadata')
  if latest['object']['sha']!=parent:note('METADATA_CONCURRENT_ADVANCE_RELOAD');continue
  raise
 assert updated['object']['sha']==new_commit['sha']
 roles={name:Metadata.from_bytes(blob).signed.version for name,blob in blobs.items() if name in ('targets.json','snapshot.json','timestamp.json')}
 write('METADATA_PUBLICATION.json',dict(status='PUBLISHED_VERIFICATION_PENDING',commit=new_commit['sha'],parent=parent,versions=roles,rootSha256=PIN,rootUnchanged=True,installerSha256=SHA,releaseVersion=118,source=state['tagSource'],changedPaths=[x['path'] for x in tree],signerRoles=list(signers),privateMaterialExported=False,force=False,legacyChannelModified=False,preparedGeneration=str(store/'generations'/receipt['generation'])))
 note('METADATA_PUBLISHED '+new_commit['sha']);break
else:raise RuntimeError('Metadata contention retained; do not force push')
