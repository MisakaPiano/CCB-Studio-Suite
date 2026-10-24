import datetime,hashlib,json,subprocess,urllib.request
from pathlib import Path
from tuf.api.metadata import Metadata
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025/public';OUT.mkdir(parents=True,exist_ok=True)
def fetch(url):
    req=urllib.request.Request(url,headers={'User-Agent':'CCB-Suite-read-only-audit','Cache-Control':'no-cache'})
    with urllib.request.urlopen(req,timeout=30) as r:
        b=r.read(4*1024*1024+1);assert len(b)<=4*1024*1024;return b
def save(name,b):
    (OUT/name).write_bytes(b);return dict(path=name,bytes=len(b),sha256=hashlib.sha256(b).hexdigest())
refs=subprocess.check_output(['git','ls-remote','origin','refs/heads/main','refs/heads/long-term-metadata','refs/tags/v0.0.0-preview.115'],cwd=ROOT,text=True)
sha=next(x.split()[0] for x in refs.splitlines() if x.endswith('refs/heads/long-term-metadata'))
save('suite-refs.txt',refs.encode())
release=json.loads(fetch('https://api.github.com/repos/MisakaPiano/CCB-Studio-Suite/releases?per_page=100'))
previews=[r for r in release if r['prerelease'] and not r['draft']]
latest=max(previews,key=lambda r:r['published_at']);assert latest['tag_name']=='v0.0.0-preview.115'
projection={k:latest[k] for k in ('id','tag_name','html_url','published_at','prerelease','draft')}
projection['assets']=[{k:a[k] for k in ('id','name','size','digest','browser_download_url')} for a in latest['assets']]
save('latest-preview.json',(json.dumps(projection,indent=2)+'\n').encode())
base='https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/'+sha+'/preview/windows-x86_64/metadata/'
raw={name:fetch(base+('1.root' if name=='root' else name)+'.json') for name in ('root','targets','snapshot','timestamp')}
metadata={k:Metadata.from_bytes(v) for k,v in raw.items()}
root=metadata['root'];root.verify_delegate('root',root)
for role in ('targets','snapshot','timestamp'):root.verify_delegate(role,metadata[role])
metadata['timestamp'].signed.snapshot_meta.verify_length_and_hashes(raw['snapshot'])
metadata['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(raw['targets'])
rows=[save(('1.root' if name=='root' else name)+'.json',b) for name,b in raw.items()]
target=next(t for t in metadata['targets'].signed.targets.values() if t.unrecognized_fields.get('custom',{}).get('version')==115)
assert target.hashes['sha256']=='356e749bd346af4a1d419a0d8b48b1e27d2314e6fce5763fa0933917247862dc'
summary=dict(observedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),metadataCommit=sha,files=rows,publicTarget=target.to_dict(),signatureAndLengthHashLinks='PASS',installerDownloadedAgain=False,rootMatchesInstalled115=hashlib.sha256(raw['root']).hexdigest()=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a',metadataExpiry={k:metadata[k].signed.expires.isoformat() for k in metadata},noPublicationOrMutation=True)
save('PUBLIC_IDENTITY.json',(json.dumps(summary,indent=2)+'\n').encode());print(json.dumps(summary,indent=2))
