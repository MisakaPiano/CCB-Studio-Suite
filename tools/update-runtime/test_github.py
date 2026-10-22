"""Signed synthetic GitHub responses through the real TUF client; no publication."""
from dataclasses import replace
import importlib.util
from io import BytesIO
import json
from pathlib import Path
import unittest
from tuf.api.exceptions import DownloadError
from github_transport import GitHubFetcher, endpoints
from studio_update import execute, Installed, UpdateRejected, create_updater, inspect, authorize_download
from test_client import ClientTests


class Response:
    def __init__(self, body=b'', status=200, location=None):
        self.body=body; self.status=status; self.headers={'Location':location}; self.closed=False
    def stream(self, size, decode_content=False):
        if isinstance(self.body, Exception): raise self.body
        yield from iter(lambda stream=BytesIO(self.body): stream.read(size), b'')
    def close(self): self.closed=True


class Pool:
    def __init__(self): self.routes={}; self.calls=[]; self.responses=[]
    def request(self, method, url, **kwargs):
        self.calls.append((url, kwargs))
        value=self.routes.get(url, (b'',404,None))
        response=Response(*value); self.responses.append(response)
        return response


class GitHubTests(unittest.TestCase):
    def setUp(self):
        self.fixture=ClientTests(); self.fixture.setUp()
        self.path=self.fixture.path; self.pool=Pool()
        self.repository='SyntheticOwner/Release-Fixture'
        metadata,targets=endpoints(self.repository,'preview')
        self.identity=replace(self.fixture.identity,channel='preview',metadata_url=metadata,targets_url=targets,github_repository=self.repository)
        self.payload=b'MZ Synthetic payload, not executable'
        self.publish()

    def publish(self,changes=None):
        custom=dict(channel='preview',displayVersion='0.3.0-preview.2',github=dict(tag='v0.3.0-preview.2',asset='Studio.exe'))
        custom.update(changes or {})
        self.fixture.publish(custom,self.payload)
        for key,value in self.fixture.repo.files.items():
            if key.startswith('metadata/'):
                self.pool.routes[self.identity.metadata_url+key.split('/',1)[1]]=(value,200,None)
        self.asset=self.identity.targets_url+'v0.3.0-preview.2/Studio.exe'
        self.pool.routes[self.asset]=(b'',302,'https://release-assets.githubusercontent.com/synthetic?token=fixture')
        self.cdn='https://release-assets.githubusercontent.com/synthetic?token=fixture'
        self.pool.routes[self.cdn]=(self.payload,200,None)

    def fetcher(self): return GitHubFetcher(self.repository,'preview',self.pool)

    def test_signed_preview_download_and_handoff_reinspection_share_transport(self):
        result=execute(self.identity,self.path,True,self.fetcher())
        self.assertEqual(Path(result['installer']).read_bytes(),self.payload)
        updater,observed=create_updater(self.identity,self.path/'metadata',self.path/'downloads',self.fetcher())
        target,custom=inspect(updater,self.identity); authorize_download(observed,target,self.identity)
        self.assertEqual(target.hashes['sha256'],result['sha256'])
        self.assertEqual(custom['version'],result['version'])
        self.assertTrue(all(not args['redirect'] and not args['retries'] for _,args in self.pool.calls))
        self.assertTrue(all('Authorization' not in args['headers'] for _,args in self.pool.calls))
        self.assertTrue(all(response.closed for response in self.pool.responses))

    def test_untrusted_location_and_wrong_channel_never_fetch_asset(self):
        for i,change in enumerate([{'github':{'tag':'../evil','asset':'x.exe'}},{'github':{'tag':'v1','asset':'https://evil.exe'}},{'channel':'stable'}]):
            self.publish(change); self.pool.calls.clear()
            with self.assertRaises(UpdateRejected):execute(self.identity,self.path/str(i),True,self.fetcher())
            self.assertNotIn(self.asset,[url for url,_ in self.pool.calls])
            self.assertFalse(list((self.path/str(i)).rglob('VERIFIED.json')))

    def test_unsigned_target_cannot_authorize_payload(self):
        url=self.identity.metadata_url+'targets.json'
        self.pool.routes[url]=(self.pool.routes[url][0].replace(b'Studio.exe',b'Attack.exe'),200,None)
        with self.assertRaises(Exception):execute(self.identity,self.path,True,self.fetcher())
        self.assertNotIn(self.asset,[url for url,_ in self.pool.calls])

    def test_corrupt_and_interrupted_download_retained_then_retry(self):
        for i,body in enumerate([b'wrong bytes',OSError('Synthetic disconnect')]):
            self.pool.routes[self.cdn]=(body,200,None)
            with self.assertRaises(Exception):execute(self.identity,self.path/str(i),True,self.fetcher())
            self.assertFalse(list((self.path/str(i)).rglob('VERIFIED.json')))
        self.pool.routes[self.cdn]=(self.payload,200,None)
        self.assertEqual(execute(self.identity,self.path/'1',True,self.fetcher())['status'],'DOWNLOADED_VERIFIED')

    def test_redirect_host_downgrade_and_loop_blocked(self):
        for i,url in enumerate(['http://release-assets.githubusercontent.com/x','https://evil.invalid/x','https://github.com/Other/Repo/x','https://user:secret@objects.githubusercontent.com/x','https://objects.githubusercontent.com:444/x']):
            self.pool.routes[self.asset]=(b'',302,url)
            with self.assertRaises(Exception):execute(self.identity,self.path/str(i),True,self.fetcher())
            self.assertNotIn(url,[u for u,_ in self.pool.calls])
        self.pool.routes[self.asset]=(b'',302,self.cdn)
        self.pool.routes[self.cdn]=(b'',302,self.cdn)
        with self.assertRaises(Exception):execute(self.identity,self.path/'loop',True,self.fetcher())
        self.assertTrue(all(response.closed for response in self.pool.responses))

    def test_metadata_redirect_and_http_failures_do_not_complete(self):
        url=self.identity.metadata_url+'timestamp.json'
        for code in (302,403,404,429,500):
            self.pool.routes[url]=(b'',code,'https://raw.githubusercontent.com/Other/Repo/timestamp.json')
            with self.assertRaises(Exception):execute(self.identity,self.path/str(code),True,self.fetcher())
            self.assertFalse(list((self.path/str(code)).rglob('VERIFIED.json')))

    def test_no_target_before_authorization_and_hash_prefixed_target_supported(self):
        fetcher=self.fetcher()
        with self.assertRaises(DownloadError):list(fetcher.fetch(self.identity.targets_url+'studio-installer.exe'))
        updater,observed=create_updater(self.identity,self.path/'metadata',self.path/'downloads',fetcher)
        target,_=inspect(updater,self.identity);authorize_download(observed,target,self.identity)
        hashed=self.identity.targets_url+target.hashes['sha256']+'.studio-installer.exe'
        self.assertEqual(b''.join(fetcher.fetch(hashed)),self.payload)
        with self.assertRaises(DownloadError):list(fetcher.fetch(self.identity.metadata_url+'../root.json'))

    def test_installed_repository_cannot_override_endpoint(self):
        config=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',version=1,reader=2,channel='preview',
                    metadataUrl=self.identity.metadata_url,targetsUrl=self.identity.targets_url,githubRepository=self.repository,
                    rootSha256=__import__('hashlib').sha256(self.identity.bootstrap).hexdigest())
        (self.path/'update-root.json').write_bytes(self.identity.bootstrap)
        path=self.path/'update-config.json';path.write_text(json.dumps(config))
        self.assertEqual(Installed.load(self.path),self.identity)
        config['githubRepository']='Other/Repo';path.write_text(json.dumps(config))
        with self.assertRaises(UpdateRejected):Installed.load(self.path)

    def test_preview_build_profile_rejects_mixed_channel_and_endpoints(self):
        path=Path(__file__).parents[1]/'Build-InternalInstaller.py'
        spec=importlib.util.spec_from_file_location('build_installer',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        profile=dict(channel='preview',displayVersion='0.3.0-preview.1',githubRepository=self.repository)
        result=module.release_identity(100,self.identity.metadata_url,self.identity.targets_url,profile)
        self.assertFalse(result['internalTest'])
        for change in ({'channel':'stable'},{'displayVersion':'unsafe"name'},{'githubRepository':'Other/Repo'}):
            with self.assertRaises(ValueError):module.release_identity(100,self.identity.metadata_url,self.identity.targets_url,dict(profile,**change))
        with self.assertRaises(ValueError):module.release_identity(65536,'','')

    def test_long_term_branch_pinned_and_old_channel_cannot_be_substituted(self):
        metadata,targets=endpoints(self.repository,'preview','long-term-metadata')
        config=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',version=102,reader=3,channel='preview',
                    metadataUrl=metadata,targetsUrl=targets,githubRepository=self.repository,metadataBranch='long-term-metadata',
                    rootSha256=__import__('hashlib').sha256(self.identity.bootstrap).hexdigest())
        (self.path/'update-root.json').write_bytes(self.identity.bootstrap)
        path=self.path/'update-config.json';path.write_text(json.dumps(config))
        installed=Installed.load(self.path)
        _,observed=create_updater(installed,self.path/'long-meta',self.path/'long-targets')
        self.assertEqual(observed.delegate.metadata,metadata)
        with self.assertRaises(DownloadError):list(observed.delegate.fetch(self.identity.metadata_url+'timestamp.json'))
        config['metadataBranch']='update-metadata';path.write_text(json.dumps(config))
        with self.assertRaises(UpdateRejected):Installed.load(self.path)
        for branch in ('main','../main','long-term-metadata?x=1'):
            with self.assertRaises(ValueError):endpoints(self.repository,'preview',branch)
        spec=importlib.util.spec_from_file_location('build_installer_long',Path(__file__).parents[1]/'Build-InternalInstaller.py')
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        profile=dict(channel='preview',displayVersion='0.0.0-preview.102',githubRepository=self.repository,metadataBranch='long-term-metadata')
        self.assertEqual(module.release_identity(102,metadata,targets,profile)['metadataBranch'],'long-term-metadata')
        with self.assertRaises(ValueError):module.release_identity(102,self.identity.metadata_url,targets,profile)
