import hashlib,json,os,uuid,unittest,msvcrt
from pathlib import Path
from unittest.mock import patch
from repair_install import repair_program
from apply_update import create_launcher
from studio_update import UpdateRejected
ROOT=Path(__file__).resolve().parents[2]

class ProgramRepairTests(unittest.TestCase):
    def setUp(self):
        self.base=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('repair-'+uuid.uuid4().hex);self.base.mkdir()
        self.parent=self.base/'programs';self.parent.mkdir()
        self.old=self.parent/'2-original';self.old.mkdir();(self.old/'CCBStudioSuitePreview.exe').write_bytes(b'broken original')
        (self.old/'unknown.bin').write_bytes(b'unknown file must remain')
        self.data=self.base/'data';(self.data/'assets').mkdir(parents=True)
        for n in ['.owner.lock','assets/.owner.lock']:(self.data/n).touch()
        (self.data/'preview-root.txt').write_bytes(b'CCBStudioSuite/WindowsStudio/v3')
        (self.data/'private-fixture.bin').write_bytes(b'synthetic original data')
        self.runtime=self.base/'runtime';self.runtime.mkdir();(self.runtime/'settings.json').write_bytes(b'synthetic settings')
        self.entry=dict(format=1,version=2,installed=str(self.old),data=str(self.data),runtime=str(self.runtime))
        (self.parent/'launch-current.json').write_text(json.dumps(self.entry))
        self.package=self.base/'new-package';self.package.mkdir()
        cfg=dict(format=1,product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=2,reader=3,
            sourceSha='b'*40,metadataUrl='https://example.invalid/meta/',targetsUrl='https://example.invalid/targets/',
            rootSha256=hashlib.sha256(b'synthetic root').hexdigest())
        (self.package/'update-config.json').write_text(json.dumps(cfg))
        (self.package/'update-root.json').write_bytes(b'synthetic root')
        (self.package/'CCBStudioSuitePreview.exe').write_bytes(b'trusted replacement')
        (self.package/'INSTALL_COMPLETE').write_bytes(b'fixture complete')
        self.manifest=dict(format=1,**{k:cfg[k] for k in ['product','platform','channel','version','sourceSha']},files=[
            dict(path=p.name,bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
            for p in self.package.iterdir() if p.name!='INSTALL_COMPLETE'])
        (self.package/'INSTALL_MANIFEST.json').write_text(json.dumps(self.manifest))
        self.before=self.snapshot()
    def snapshot(self):
        return {str(p):p.read_bytes() for root in [self.old,self.data,self.runtime] for p in root.rglob('*') if p.is_file()}
    def retained(self):self.assertEqual(self.before,self.snapshot())
    def test_broken_program_repaired_to_fresh_sibling_without_opening_data(self):
        calls=[]
        def publish(final,data,runtime,**kw):
            calls.append((final,data,runtime,kw));return self.parent/'Launch-CCB-Studio.lnk'
        final,journal,result=repair_program(self.package,self.old,launcher=publish)
        self.assertNotEqual(final,self.old);self.assertEqual((final/'CCBStudioSuitePreview.exe').read_bytes(),b'trusted replacement')
        self.assertEqual(result['status'],'PROGRAM_REPAIRED_DATA_NOT_OPENED')
        self.assertFalse(result['dataVerified']);self.assertFalse(result['credentialsOpened']);self.retained()
        self.assertEqual(calls[0][3]['expected_current'],self.entry)
        self.assertEqual(calls[0][3]['repair_manifest_sha256'],hashlib.sha256((self.package/'INSTALL_MANIFEST.json').read_bytes()).hexdigest())
    def test_tampered_package_rejected_before_copy_or_entry_change(self):
        (self.package/'CCBStudioSuitePreview.exe').write_bytes(b'tampered')
        with self.assertRaises(UpdateRejected):repair_program(self.package,self.old)
        self.retained();self.assertFalse([p for p in self.parent.glob('*.repair-*') if not p.name.startswith('.')])
    def test_wrong_registered_root_or_downgrade_rejected(self):
        for change in [dict(installed=str(self.parent/'other')),dict(version=3)]:
            entry=dict(self.entry);entry.update(change);(self.parent/'launch-current.json').write_text(json.dumps(entry))
            with self.assertRaises(UpdateRejected):repair_program(self.package,self.old)
        self.retained();self.assertFalse([p for p in self.parent.glob('*.repair-*') if not p.name.startswith('.')])
    def test_format_migration_is_not_hidden_inside_program_repair(self):
        (self.data/'preview-root.txt').write_bytes(b'CCBStudioSuite/WindowsStudio/v4');self.before=self.snapshot()
        with self.assertRaisesRegex(UpdateRejected,'data format'):repair_program(self.package,self.old)
        self.retained()
    def test_data_overlap_is_refused_without_relocating_original(self):
        entry=dict(self.entry,data=str(self.old/'user-data'));(self.parent/'launch-current.json').write_text(json.dumps(entry))
        with self.assertRaisesRegex(UpdateRejected,'overlap'):repair_program(self.package,self.old)
        self.retained()
    def test_live_workspace_refuses_repair_without_killing_any_process(self):
        with (self.data/'.owner.lock').open('r+b') as f:
            msvcrt.locking(f.fileno(),msvcrt.LK_NBLCK,1)
            try:
                with self.assertRaisesRegex(UpdateRejected,'Close Studio'):repair_program(self.package,self.old)
            finally:f.seek(0);msvcrt.locking(f.fileno(),msvcrt.LK_UNLCK,1)
        self.retained();self.assertFalse([p for p in self.parent.glob('*.repair-*') if not p.name.startswith('.')])
    def test_entrypoint_race_keeps_replacement_and_original_as_failed_not_published(self):
        def race(*a,**kw):raise UpdateRejected('Default entrypoint changed during update')
        with self.assertRaises(UpdateRejected):repair_program(self.package,self.old,launcher=race)
        results=[json.loads(p.read_text()) for p in (self.parent/'.repair-receipts').glob('*.json')]
        self.assertEqual(results[-1]['status'],'FAILED_RETAINED')
        self.assertTrue(list(self.parent.glob('*.repair-*')));self.retained()
    def test_same_version_publication_requires_package_reference_and_exact_entry(self):
        import shutil
        from types import SimpleNamespace
        (self.package/'updater').mkdir()
        (self.package/'updater/CCBStudioLauncher.exe').write_bytes(b'unexecuted synthetic launcher')
        self.manifest['files']=[dict(path=p.relative_to(self.package).as_posix(),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
            for p in self.package.rglob('*') if p.is_file() and p.name not in ['INSTALL_COMPLETE','INSTALL_MANIFEST.json']]
        (self.package/'INSTALL_MANIFEST.json').write_text(json.dumps(self.manifest))
        final=self.parent/'2-repair';shutil.copytree(self.package,final)
        with self.assertRaisesRegex(UpdateRejected,'Same-version'):create_launcher(final,self.data,self.runtime,expected_current=self.entry)
        reference=hashlib.sha256((self.package/'INSTALL_MANIFEST.json').read_bytes()).hexdigest()
        def shortcut(args,**kw):
            Path(args[2]).write_bytes(b'synthetic shortcut')
            return SimpleNamespace(returncode=0)
        with patch('apply_update.subprocess.run',side_effect=shortcut):
            link=create_launcher(final,self.data,self.runtime,expected_current=self.entry,repair_manifest_sha256=reference)
        self.assertTrue(link.is_file());self.retained()
        current=json.loads((self.parent/'launch-current.json').read_text())
        self.assertEqual(current['installed'],str(final));self.assertEqual(current['version'],2)
        self.assertEqual(current['data'],str(self.data));self.assertEqual(current['runtime'],str(self.runtime))
