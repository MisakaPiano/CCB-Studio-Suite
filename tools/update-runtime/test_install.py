"""Installer safety boundaries with a synthetic executor, not real A-to-B acceptance."""
import hashlib,json,uuid,unittest,os
from pathlib import Path
from types import SimpleNamespace
from tuf.api.metadata import TargetFile
from studio_update import UpdateRejected
from install_ops import install_candidate,verify_install
ROOT=Path(__file__).resolve().parents[2]

class InstallTests(unittest.TestCase):
    def setUp(self):
        base=Path(os.environ.get('CCB_SYNTHETIC_TEST_ROOT',ROOT/'.local/windows-rc'))/'install-ops-fixtures';base.mkdir(exist_ok=True)
        self.path=base/uuid.uuid4().hex;self.path.mkdir();self.old=self.path/'programs/old';self.old.mkdir(parents=True)
        (self.old/'retain.txt').write_bytes(b'old installation retained')
        self.data=self.path/'data';self.data.mkdir();(self.data/'original.bin').write_bytes(b'unique original')
        self.cache=self.path/'cache';(self.cache/'downloads').mkdir(parents=True)
        self.exe=self.cache/'downloads/studio-installer.exe';self.exe.write_bytes(b'synthetic executable fixture')
        self.target=TargetFile.from_data('studio-installer.exe',self.exe.read_bytes(),['sha256'])
        self.custom=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=2,sourceSha='b'*40,installedBytes=100000)

    def payload(self,exe,stage):
        stage.mkdir();file=stage/'CCBStudioSuitePreview.exe';file.write_bytes(b'synthetic payload; not an executable')
        manifest=dict(format=1,**{k:v for k,v in self.custom.items() if k!='installedBytes'},files=[dict(path=file.name,bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest())])
        (stage/'INSTALL_MANIFEST.json').write_text(json.dumps(manifest));(stage/'INSTALL_COMPLETE').write_text('fixture')
        return 0

    def install(self,run=None,**kwargs):return install_candidate(self.exe,self.target,self.custom,self.old,self.cache,self.data,run=run or self.payload,**kwargs)
    def retained(self):
        self.assertEqual((self.old/'retain.txt').read_bytes(),b'old installation retained')
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique original')

    def test_success_is_installed_not_started_and_preserves_sources(self):
        final,journal,record=self.install();self.assertTrue(final.exists());self.assertFalse(Path(record['stage']).exists())
        self.assertEqual(json.loads(journal.read_text())['status'],'INSTALLED_NOT_STARTED');self.retained()

    def test_independent_install_parent_may_contain_data_without_treating_it_as_program(self):
        self.old=self.path/'independent-preview/old';self.old.mkdir(parents=True)
        (self.old/'retain.txt').write_bytes(b'old installation retained')
        self.data=self.old.parent/'data/workspace';self.data.mkdir(parents=True)
        (self.data/'original.bin').write_bytes(b'unique original')
        runtime=self.old.parent/'data/runtime';runtime.mkdir(parents=True)
        (runtime/'settings.json').write_bytes(b'synthetic runtime configuration')
        final,journal,record=self.install()
        self.assertEqual(final.parent,self.old.parent)
        self.assertFalse(final.is_relative_to(self.data))
        self.assertEqual((runtime/'settings.json').read_bytes(),b'synthetic runtime configuration')
        self.assertEqual(record['status'],'INSTALLED_NOT_STARTED');self.retained()

    def test_tampered_executable_is_never_executed(self):
        self.exe.write_bytes(b'tampered');called=[]
        with self.assertRaises(Exception):self.install(lambda *_:called.append(True))
        self.assertFalse(called);self.retained();self.assertFalse(list(self.old.parent.glob('*.installing')))

    def test_installer_failure_retains_partial_without_publish(self):
        def failed(exe,stage):stage.mkdir();(stage/'partial').write_bytes(b'partial');return 18
        with self.assertRaises(UpdateRejected):self.install(failed)
        self.assertEqual(len(list(self.old.parent.glob('*.installing'))),1);self.assertFalse((self.old.parent/'2-bbbbbbbbbbbb').exists());self.retained()

    def test_manifest_wrong_identity_hash_or_unknown_file_is_rejected(self):
        for issue in ['identity','hash','extra','traversal']:
            def failed(exe,stage):
                self.payload(exe,stage);p=stage/'INSTALL_MANIFEST.json';m=json.loads(p.read_text())
                if issue=='identity':m['product']='other'
                if issue=='hash':m['files'][0]['sha256']='0'*64
                if issue=='extra':(stage/'unknown').write_bytes(b'not declared')
                if issue=='traversal':m['files'][0]['path']='../data/original.bin'
                p.write_text(json.dumps(m));return 0
            with self.subTest(issue=issue),self.assertRaises(UpdateRejected):self.install(failed)
            self.retained()

    def test_existing_version_and_data_overlap_refuse_before_install(self):
        final=self.old.parent/'2-bbbbbbbbbbbb';final.mkdir();(final/'keep').write_bytes(b'existing')
        with self.assertRaises(UpdateRejected):self.install()
        self.assertEqual((final/'keep').read_bytes(),b'existing');self.retained()
        self.custom['version']=3
        with self.assertRaises(UpdateRejected):install_candidate(self.exe,self.target,self.custom,self.old,self.cache,self.old.parent/'3-bbbbbbbbbbbb',run=self.payload)

    def test_download_stays_immutable_during_executor(self):
        def locked(exe,stage):
            with self.assertRaises(PermissionError):exe.write_bytes(b'changed during execution')
            return self.payload(exe,stage)
        self.install(locked);self.retained()

    def test_insufficient_space_has_no_install_stage(self):
        with self.assertRaises(UpdateRejected):self.install(disk_usage=lambda _:SimpleNamespace(free=1))
        self.assertFalse(list(self.old.parent.glob('*.installing')));self.retained()
