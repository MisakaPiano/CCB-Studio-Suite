import sys,json,subprocess,hashlib
from pathlib import Path
root=Path('H:/CCB-Studio-Suite');sys.path.insert(0,str(root/'tools/update-runtime'))
from studio_update import Installed,execute,create_updater,inspect,authorize_download
from github_transport import GitHubFetcher
from install_ops import install_candidate,verify_install
from release_repository import current
folder=root/'.local/windows-rc/github-ab-prepared'
out=root/'.local/windows-rc/evidence/github-ab-local-install';out.mkdir()
app=root/'.local/windows-rc/github-ab-apps/100-A';assert not app.exists()
result=subprocess.run([str(folder/'build-100/studio-installer.exe'),'/S','/D='+str(app)],creationflags=subprocess.CREATE_NO_WINDOW,timeout=120)
assert result.returncode==0
config=json.loads((app/'update-config.json').read_text());a=verify_install(app,config);identity=Installed.load(app)
cache=root/'.local/windows-rc/github-ab-local-cache'
data=root/'.local/windows-rc/github-ab-synthetic-data';data.mkdir();(data/'original.bin').write_bytes(b'SYNTHETIC ORIGINAL KEEP BYTES')
before={p.name:p.read_bytes() for p in data.iterdir()}
_,files=current(folder/'metadata-store')
class Response:
    status=200
    headers={}
    def __init__(self,value):self.value=value
    def stream(self,size,decode_content=False):
        if isinstance(self.value,Path):
            with self.value.open('rb') as stream:
                while chunk:=stream.read(size):yield chunk
        else:yield self.value
    def close(self):pass
class Pool:
    def request(self,method,url,**kwargs):
        if url.startswith(identity.metadata_url):
            name=url[len(identity.metadata_url):]
            if name in files:return Response(files[name])
            missing=Response(b'');missing.status=404;return missing
        assert url==identity.targets_url+'v0.0.0-preview.101/CCB-Studio-Suite-channel-test-101.exe',url
        return Response(folder/'build-101/studio-installer.exe')
fetcher=GitHubFetcher(identity.github_repository,identity.channel,Pool())
download=execute(identity,cache,True,fetcher)
updater,observed=create_updater(identity,cache/'metadata',cache/'downloads',GitHubFetcher(identity.github_repository,identity.channel,Pool()))
target,custom=inspect(updater,identity);authorize_download(observed,target,identity)
final,journal,record=install_candidate(Path(download['installer']),target,custom,app,cache,data)
b=verify_install(final,custom)
assert before=={p.name:p.read_bytes() for p in data.iterdir()}
assert verify_install(app,config)==a
report=dict(status='PASS',transport='SYNTHETIC_GITHUB_RESPONSE_WITH_REAL_INSTALLER_BYTES',a=a,b=b,download=download,installedB=str(final),journal=str(journal),dataBytesUnchanged=True,oldInstallUnchanged=True,applicationRestart='NOT TESTED',realGitHubNetwork='NOT TESTED')
(out/'receipt.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
