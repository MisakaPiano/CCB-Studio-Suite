"""App-local TUF client. Cryptographic policy belongs to python-tuf, not this adapter.

The immutable installed package supplies config and root bytes. Writable cache state
never supplies the bootstrap trust anchor. No credentials or installer execution here.
"""
from contextlib import contextmanager
from dataclasses import dataclass
import argparse, hashlib, json, os, re, shutil, time, uuid
from pathlib import Path
from urllib.parse import urlsplit
from tuf.ngclient import Updater
from tuf.ngclient.config import UpdaterConfig
from tuf.ngclient.fetcher import FetcherInterface
from tuf.ngclient.urllib3_fetcher import Urllib3Fetcher
from github_transport import GitHubFetcher, endpoints

class UpdateRejected(ValueError):
    pass

@dataclass(frozen=True)
class Installed:
    version: int
    reader: int
    channel: str
    metadata_url: str
    targets_url: str
    bootstrap: bytes
    source_sha: str = ''
    github_repository: str = ''

    @staticmethod
    def load(package: Path):
        # Caller must use the installed app directory, never a downloaded config.
        data=json.loads((package/'update-config.json').read_text(encoding='utf-8'))
        if data.get('format')!=1 or data.get('product')!='ccb-studio-suite' or data.get('platform')!='windows-x86_64':
            raise UpdateRejected('Unsupported installed update configuration')
        for name in ['version','reader']:
            if type(data.get(name)) is not int or data[name]<1:raise UpdateRejected('Invalid installed '+name)
        if data.get('channel') not in ['internal','preview','stable']:raise UpdateRejected('Invalid channel')
        for name in ['metadataUrl','targetsUrl']:
            url=urlsplit(data[name])
            internal_loopback=(data.get('internalTest') is True and data['channel']=='internal' and url.scheme=='http' and url.hostname=='127.0.0.1')
            if (url.scheme!='https' and not internal_loopback) or not url.hostname or url.username or url.password or url.query or url.fragment or not url.path.endswith('/'):
                raise UpdateRejected('Update endpoints must be fixed HTTPS base URLs')
        root=(package/'update-root.json').read_bytes()
        if hashlib.sha256(root).hexdigest()!=data.get('rootSha256'):raise UpdateRejected('Installed bootstrap identity changed')
        source=data.get('sourceSha','')
        if source and not re.fullmatch('[0-9a-f]{40}',source):raise UpdateRejected('Invalid installed source identity')
        repository=data.get('githubRepository','')
        if 'githubRepository' in data and (not isinstance(repository,str) or not repository):
            raise UpdateRejected('Invalid installed GitHub repository')
        if repository:
            try:expected=endpoints(repository,data['channel'])
            except ValueError:raise UpdateRejected('Invalid installed GitHub configuration') from None
            if (data['metadataUrl'],data['targetsUrl'])!=expected:raise UpdateRejected('GitHub endpoint identity mismatch')
        return Installed(data['version'],data['reader'],data['channel'],data['metadataUrl'],data['targetsUrl'],root,source,repository)

def create_updater(installed,metadata,targets,fetcher=None,progress=None):
    delegate=fetcher or (GitHubFetcher(installed.github_repository,installed.channel) if installed.github_repository
                        else Urllib3Fetcher(app_user_agent='CCBStudioSuite'))
    observed=ProgressFetcher(delegate,progress or (lambda value:None))
    updater=Updater(str(metadata),installed.metadata_url,str(targets),installed.targets_url,
                    bootstrap=installed.bootstrap,fetcher=observed,config=UpdaterConfig(app_user_agent='CCBStudioSuite'))
    return updater,observed

def authorize_download(observed,target,installed):
    if installed.github_repository:
        try:observed.delegate.authorize(target)
        except (ValueError,AttributeError):raise UpdateRejected('Invalid authenticated GitHub release location') from None

@contextmanager
def exclusive(cache:Path):
    import msvcrt
    cache.mkdir(parents=True,exist_ok=True)
    with (cache/'client.lock').open('a+b') as stream:
        stream.seek(0,2)
        if stream.tell()==0:stream.write(b'0');stream.flush()
        stream.seek(0)
        try:msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
        except OSError:raise UpdateRejected('Another update operation owns this cache') from None
        try:yield
        finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)

def inspect(updater, installed, allow_current=False):
    target=updater.get_targetinfo('studio-installer.exe')
    if target is None:raise UpdateRejected('No authenticated installer published')
    custom=target.unrecognized_fields.get('custom',{})
    for name,value in [('product','ccb-studio-suite'),('platform','windows-x86_64'),('channel',installed.channel)]:
        if custom.get(name)!=value:raise UpdateRejected('Wrong target '+name)
    for name in ['version','minimum_reader','maximum_reader']:
        if type(custom.get(name)) is not int or custom[name]<1:raise UpdateRejected('Invalid target '+name)
    if custom['version']<installed.version or (custom['version']==installed.version and not allow_current):
        raise UpdateRejected('Target is not newer than installed version')
    if not custom['minimum_reader']<=installed.reader<=custom['maximum_reader']:raise UpdateRejected('Data reader is outside supported target range')
    output=custom.get('data_version',installed.reader)
    if type(output) is not int or not installed.reader<=output<=3 or not custom['minimum_reader']<=output<=custom['maximum_reader']:
        raise UpdateRejected('Unsupported target output data version')
    if not re.fullmatch('[0-9a-f]{40}',custom.get('sourceSha','')):raise UpdateRejected('Missing source identity')
    if not isinstance(custom.get('displayVersion'),str) or not 1<=len(custom['displayVersion'])<=80:raise UpdateRejected('Missing display version')
    if not isinstance(custom.get('notes'),str) or len(custom['notes'])>16000:raise UpdateRejected('Unsupported release notes')
    if type(custom.get('installedBytes')) is not int or not 0<custom['installedBytes']<=4*1024**3:raise UpdateRejected('Invalid installation size')
    if not 0<target.length<=1024**3 or not re.fullmatch('[0-9a-f]{64}',target.hashes.get('sha256','')):raise UpdateRejected('Unsupported target size or hash')
    if custom['version']==installed.version and (not installed.source_sha or custom['sourceSha']!=installed.source_sha or output!=installed.reader):
        raise UpdateRejected('Current version target identity does not match installed package')
    return target,custom

class ProgressFetcher(FetcherInterface):
    """Observe actual target bytes; TUF still owns length/hash verification."""
    def __init__(self,delegate,report):
        self.delegate=delegate;self.report=report;self.total=None

    def _fetch(self,url):
        received=0;start=last=time.monotonic()
        for chunk in self.delegate.fetch(url):
            received+=len(chunk);now=time.monotonic()
            if self.total is not None and (now-last>=0.25 or received>=self.total):
                self.report({'phase':'DOWNLOADING','received':received,'total':self.total,
                             'bytesPerSecond':int(received/max(now-start,0.001))})
                last=now
            yield chunk
        if self.total is not None:
            self.report({'phase':'VERIFYING','received':received,'total':self.total})

def execute(installed:Installed,cache:Path,download=False,fetcher=None,disk_usage=shutil.disk_usage,progress=None):
    report=progress or (lambda value:None)
    report({'phase':'CHECKING'})
    with exclusive(cache):
        metadata=cache/'metadata';metadata.mkdir(exist_ok=True)
        targets=cache/'downloads';targets.mkdir(exist_ok=True)
        updater,observed=create_updater(installed,metadata,targets,fetcher,report)
        target,custom=inspect(updater,installed,allow_current=True)
        authorize_download(observed,target,installed)
        result={'status':'AVAILABLE','version':custom['version'],'displayVersion':custom['displayVersion'],
                'channel':custom['channel'],'notes':custom['notes'],'sourceSha':custom['sourceSha'],
                'bytes':target.length,'installedBytes':custom['installedBytes'],'sha256':target.hashes['sha256'],'minimumReader':custom['minimum_reader'],
                'maximumReader':custom['maximum_reader'],'dataVersion':custom.get('data_version',installed.reader)}
        if custom['version']==installed.version:
            result['status']='UP_TO_DATE'
            return result
        if download:
            if disk_usage(cache).free<target.length*3+64*1024**2:raise UpdateRejected('Insufficient update staging space')
            stage=targets/(str(uuid.uuid4())+'.incomplete');stage.mkdir()
            file=stage/'studio-installer.exe'
            observed.total=target.length
            report({'phase':'DOWNLOADING','received':0,'total':target.length,'bytesPerSecond':0})
            updater.download_target(target,filepath=str(file))
            # Commit completion only after upstream byte verification and durable local write.
            with file.open('rb+') as stream:os.fsync(stream.fileno())
            with file.open('rb') as stream:target.verify_length_and_hashes(stream)
            receipt=stage/'VERIFIED.json'
            with receipt.open('x',encoding='utf-8') as stream:
                json.dump(result,stream,indent=2);stream.flush();os.fsync(stream.fileno())
            final=stage.with_suffix('');os.rename(stage,final)
            result.update(status='DOWNLOADED_VERIFIED',installer=str((final/file.name).resolve()))
            report({'phase':'DOWNLOADED_VERIFIED','received':target.length,'total':target.length})
        return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--package',required=True);parser.add_argument('--cache',required=True);parser.add_argument('--download',action='store_true')
    parser.add_argument('--progress')
    args=parser.parse_args()
    try:
        package=Path(args.package).resolve();cache=Path(args.cache).resolve()
        if cache==package or cache.is_relative_to(package) or package.is_relative_to(cache):raise UpdateRejected('Update cache must be separate from installed package')
        progress=None
        if args.progress:
            path=Path(args.progress).resolve()
            if path.parent.parent!=cache/'operations' or not path.parent.is_dir() or path.name!='progress.json':
                raise UpdateRejected('Invalid progress destination')
            def progress(value):
                temporary=path.with_suffix('.tmp')
                temporary.write_text(json.dumps(value),encoding='utf-8');os.replace(temporary,path)
        result=execute(Installed.load(package),cache,args.download,progress=progress)
    except Exception as error:
        # No URLs, arbitrary server text, local credentials or traceback in product output.
        result={'status':'FAILED','reason':str(error) if isinstance(error,UpdateRejected) else type(error).__name__}
    print(json.dumps(result,ensure_ascii=True))
    return 0 if result['status']!='FAILED' else 1

if __name__=='__main__':raise SystemExit(main())
