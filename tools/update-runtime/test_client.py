"""Synthetic trust/client boundary tests; not an installer or A-to-B acceptance."""
import hashlib, json, os, shutil, subprocess, sys, tempfile, unittest
from pathlib import Path
from types import SimpleNamespace
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/update-lab'))
from test_trust import Repository
from tuf.api.metadata import Metadata, TargetFile, MetaFile
from studio_update import Installed, UpdateRejected, execute, exclusive

class ClientTests(unittest.TestCase):
    def setUp(self):
        base=ROOT/'.local/windows-rc/runtime-client-fixtures';base.mkdir(exist_ok=True)
        self.path=Path(tempfile.mkdtemp(dir=base));self.repo=Repository()
        self.identity=Installed(1,2,'internal','https://updates.invalid/metadata/','https://updates.invalid/targets/',self.repo.bootstrap)
        self.publish()

    def publish(self,changes=None,payload=b'MZ synthetic trust fixture; NOT an executable installer'):
        custom={'product':'ccb-studio-suite','platform':'windows-x86_64','channel':'internal','version':2,
                'minimum_reader':2,'maximum_reader':2,'sourceSha':'a'*40,'displayVersion':'0.2.0-internal','notes':'Synthetic release'}
        custom.update(changes or {})
        self.repo.publish(custom=custom)
        target=TargetFile.from_data('studio-installer.exe',payload,['sha256']);target.unrecognized_fields['custom']=custom
        targets=Metadata.from_bytes(self.repo.files['metadata/targets.json']);targets.signed.targets={'studio-installer.exe':target};targets.sign(self.repo.signers['targets'])
        self.repo.files['metadata/targets.json']=targets.to_bytes()
        snapshot=Metadata.from_bytes(self.repo.files['metadata/snapshot.json']);snapshot.signed.meta['targets.json']=MetaFile.from_data(2,targets.to_bytes(),['sha256']);snapshot.sign(self.repo.signers['snapshot'])
        self.repo.files['metadata/snapshot.json']=snapshot.to_bytes()
        timestamp=Metadata.from_bytes(self.repo.files['metadata/timestamp.json']);timestamp.signed.snapshot_meta=MetaFile.from_data(2,snapshot.to_bytes(),['sha256']);timestamp.sign(self.repo.signers['timestamp'])
        self.repo.files['metadata/timestamp.json']=timestamp.to_bytes();self.repo.files['targets/studio-installer.exe']=payload

    def test_check_then_verified_download_no_install(self):
        before=execute(self.identity,self.path,fetcher=self.repo.fetcher)
        self.assertEqual(before['status'],'AVAILABLE');self.assertNotIn('installer',before)
        after=execute(self.identity,self.path,True,self.repo.fetcher)
        file=Path(after['installer']);self.assertEqual(after['status'],'DOWNLOADED_VERIFIED')
        self.assertEqual(hashlib.sha256(file.read_bytes()).hexdigest(),after['sha256'])
        self.assertTrue((file.parent/'VERIFIED.json').is_file());self.assertFalse(file.parent.name.endswith('.incomplete'))

    def test_wrong_identity_version_and_reader_block_before_download(self):
        for field,value in [('channel','stable'),('platform','linux'),('product','other'),('version',1),('version',True),('minimum_reader',3),('maximum_reader',1),('sourceSha','unknown'),('notes',42)]:
            with self.subTest(field=field,value=value):
                self.publish({field:value});cache=self.path/(field+str(value))
                with self.assertRaises(UpdateRejected):execute(self.identity,cache,True,self.repo.fetcher)
                self.assertFalse(list(cache.rglob('VERIFIED.json')))

    def test_low_space_no_download_or_completion(self):
        with self.assertRaisesRegex(UpdateRejected,'space'):
            execute(self.identity,self.path,True,self.repo.fetcher,lambda _:SimpleNamespace(free=1))
        self.assertFalse(list(self.path.rglob('VERIFIED.json')))

    def test_changed_download_has_no_success_receipt(self):
        self.repo.files['targets/studio-installer.exe']=b'changed bytes'
        with self.assertRaises(Exception):execute(self.identity,self.path,True,self.repo.fetcher)
        self.assertTrue(list((self.path/'downloads').glob('*.incomplete')))
        self.assertFalse(list(self.path.rglob('VERIFIED.json')))

    def test_disconnect_preserves_metadata_for_retry(self):
        execute(self.identity,self.path,fetcher=self.repo.fetcher)
        existing=(self.path/'metadata/root.json').read_bytes()
        self.repo.fetcher.disconnect=True
        with self.assertRaises(Exception):execute(self.identity,self.path,True,self.repo.fetcher)
        self.assertEqual(existing,(self.path/'metadata/root.json').read_bytes())
        self.assertFalse(list(self.path.rglob('VERIFIED.json')))

    def test_other_process_cannot_enter_same_cache(self):
        source=Path(__file__).parent
        code="import sys;from pathlib import Path;sys.path.insert(0,sys.argv[1]);from studio_update import exclusive;\nwith exclusive(Path(sys.argv[2])): print('ACQUIRED')"
        with exclusive(self.path):
            result=subprocess.run([sys.executable,'-c',code,str(source),str(self.path)],capture_output=True,text=True)
            self.assertNotEqual(result.returncode,0);self.assertNotIn('ACQUIRED',result.stdout)
        result=subprocess.run([sys.executable,'-c',code,str(source),str(self.path)],capture_output=True,text=True)
        self.assertEqual(result.returncode,0);self.assertIn('ACQUIRED',result.stdout)

    def test_installed_root_hash_and_endpoint_are_required(self):
        config={'format':1,'product':'ccb-studio-suite','platform':'windows-x86_64','channel':'internal','version':1,'reader':2,
                'metadataUrl':self.identity.metadata_url,'targetsUrl':self.identity.targets_url,'rootSha256':hashlib.sha256(self.repo.bootstrap).hexdigest()}
        (self.path/'update-root.json').write_bytes(self.repo.bootstrap)
        def write():(self.path/'update-config.json').write_text(json.dumps(config))
        write();self.assertEqual(Installed.load(self.path),self.identity)
        config['metadataUrl']='http://updates.invalid/metadata/';write()
        with self.assertRaises(UpdateRejected):Installed.load(self.path)
        config['metadataUrl']=self.identity.metadata_url;config['rootSha256']='0'*64;write()
        with self.assertRaises(UpdateRejected):Installed.load(self.path)

if __name__=='__main__':unittest.main()
