"""App-local TUF client. Cryptographic policy belongs to python-tuf, not this adapter.

The immutable installed package supplies config and root bytes. Writable cache state
never supplies the bootstrap trust anchor. No credentials or installer execution here.
"""
from contextlib import contextmanager
from dataclasses import dataclass
import argparse, hashlib, json, os, re, shutil, uuid
from pathlib import Path
from urllib.parse import urlsplit
from tuf.ngclient import Updater
from tuf.ngclient.config import UpdaterConfig

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
            if url.scheme!='https' or not url.hostname or url.username or url.password or url.query or url.fragment or not url.path.endswith('/'):
                raise UpdateRejected('Update endpoints must be fixed HTTPS base URLs')
        root=(package/'update-root.json').read_bytes()
        if hashlib.sha256(root).hexdigest()!=data.get('rootSha256'):raise UpdateRejected('Installed bootstrap identity changed')
        return Installed(data['version'],data['reader'],data['channel'],data['metadataUrl'],data['targetsUrl'],root)

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

def inspect(updater, installed):
    target=updater.get_targetinfo('studio-installer.exe')
    if target is None:raise UpdateRejected('No authenticated installer published')
    custom=target.unrecognized_fields.get('custom',{})
    for name,value in [('product','ccb-studio-suite'),('platform','windows-x86_64'),('channel',installed.channel)]:
        if custom.get(name)!=value:raise UpdateRejected('Wrong target '+name)
    for name in ['version','minimum_reader','maximum_reader']:
        if type(custom.get(name)) is not int or custom[name]<1:raise UpdateRejected('Invalid target '+name)
    if custom['version']<=installed.version:raise UpdateRejected('Target is not newer than installed version')
    if not custom['minimum_reader']<=installed.reader<=custom['maximum_reader']:raise UpdateRejected('Data reader is outside supported target range')
    if not re.fullmatch('[0-9a-f]{40}',custom.get('sourceSha','')):raise UpdateRejected('Missing source identity')
    if not isinstance(custom.get('displayVersion'),str) or not 1<=len(custom['displayVersion'])<=80:raise UpdateRejected('Missing display version')
    if not isinstance(custom.get('notes'),str) or len(custom['notes'])>16000:raise UpdateRejected('Unsupported release notes')
    if not 0<target.length<=1024**3 or not re.fullmatch('[0-9a-f]{64}',target.hashes.get('sha256','')):raise UpdateRejected('Unsupported target size or hash')
    return target,custom

def execute(installed:Installed,cache:Path,download=False,fetcher=None,disk_usage=shutil.disk_usage):
    with exclusive(cache):
        metadata=cache/'metadata';metadata.mkdir(exist_ok=True)
        targets=cache/'downloads';targets.mkdir(exist_ok=True)
        updater=Updater(str(metadata),installed.metadata_url,str(targets),installed.targets_url,
                        bootstrap=installed.bootstrap,fetcher=fetcher,config=UpdaterConfig(app_user_agent='CCBStudioSuite'))
        target,custom=inspect(updater,installed)
        result={'status':'AVAILABLE','version':custom['version'],'displayVersion':custom['displayVersion'],
                'channel':custom['channel'],'notes':custom['notes'],'sourceSha':custom['sourceSha'],
                'bytes':target.length,'sha256':target.hashes['sha256'],'minimumReader':custom['minimum_reader'],
                'maximumReader':custom['maximum_reader']}
        if download:
            if disk_usage(cache).free<target.length*3+64*1024**2:raise UpdateRejected('Insufficient update staging space')
            stage=targets/(str(uuid.uuid4())+'.incomplete');stage.mkdir()
            file=stage/'studio-installer.exe'
            updater.download_target(target,filepath=str(file))
            # Commit completion only after upstream byte verification and durable local write.
            with file.open('rb+') as stream:os.fsync(stream.fileno())
            target.verify_length_and_hashes(file.read_bytes())
            receipt=stage/'VERIFIED.json'
            with receipt.open('x',encoding='utf-8') as stream:
                json.dump(result,stream,indent=2);stream.flush();os.fsync(stream.fileno())
            final=stage.with_suffix('');os.rename(stage,final)
            result.update(status='DOWNLOADED_VERIFIED',installer=str((final/file.name).resolve()))
        return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--package',required=True);parser.add_argument('--cache',required=True);parser.add_argument('--download',action='store_true')
    args=parser.parse_args()
    try:
        package=Path(args.package).resolve();cache=Path(args.cache).resolve()
        if cache==package or cache.is_relative_to(package) or package.is_relative_to(cache):raise UpdateRejected('Update cache must be separate from installed package')
        result=execute(Installed.load(package),cache,args.download)
    except Exception as error:
        # No URLs, arbitrary server text, local credentials or traceback in product output.
        result={'status':'FAILED','reason':str(error) if isinstance(error,UpdateRejected) else type(error).__name__}
    print(json.dumps(result,ensure_ascii=True))
    return 0 if result['status']!='FAILED' else 1

if __name__=='__main__':raise SystemExit(main())
