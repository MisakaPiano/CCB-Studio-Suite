"""Synthetic filesystem cases; ROOT is rebound only to a fresh disposable fixture."""
import ctypes,json,os,subprocess,unittest,uuid
from pathlib import Path
import workspace_artifacts as w

REAL_ROOT=w.ROOT
TEST_ROOT=None

class WorkspaceArtifactTests(unittest.TestCase):
    def setUp(self):
        self.original=w.ROOT
        self.suite=TEST_ROOT/uuid.uuid4().hex;self.suite.mkdir()
        w.ROOT=self.suite
        self.folder=w.create('case');self.payload=self.folder/'payload'
        self.proof=self.suite/'.local/evidence.json';self.proof.write_text('{"synthetic":"PASS"}')
        self.file=self.payload/'generated.bin';self.file.write_bytes(b'synthetic generated file')
    def tearDown(self):w.ROOT=self.original
    def seal(self,status='PASS'):return w.seal(self.folder,[self.proof],status)
    def test_success_removes_owned_payload_preserves_proof_and_repeats_safely(self):
        (self.payload/'empty').mkdir();self.seal()
        self.assertEqual(w.cleanup(self.folder)['status'],'COMPLETE')
        self.assertFalse(self.payload.exists());self.assertTrue(self.proof.exists())
        self.assertEqual(w.cleanup(self.folder)['removed'],0)
    def test_unknown_and_changed_files_are_retained(self):
        self.seal();self.file.write_bytes(b'user modification')
        unknown=self.payload/'unknown.txt';unknown.write_text('keep')
        self.assertEqual(w.cleanup(self.folder)['status'],'DEFERRED_OR_UNKNOWN_RETAINED')
        self.assertTrue(self.file.exists());self.assertTrue(unknown.exists())
    def test_failed_run_fixture_is_retained(self):
        self.seal('FAILED');self.assertEqual(w.cleanup(self.folder)['status'],'RETAINED_FAILED_FIXTURES');self.assertTrue(self.file.exists())
    def test_changed_or_missing_proof_blocks_before_any_deletion(self):
        self.seal();self.proof.write_text('changed')
        with self.assertRaises(ValueError):w.cleanup(self.folder)
        self.assertTrue(self.file.exists());self.proof.unlink()
        with self.assertRaises(OSError):w.cleanup(self.folder)
        self.assertTrue(self.file.exists())
    def test_legacy_folder_and_modified_manifest_never_gain_ownership(self):
        legacy=self.suite/'.local/old-tests';legacy.mkdir()
        with self.assertRaises(ValueError):w.cleanup(legacy)
        self.seal();(self.folder/'MANIFEST.json.gz').write_bytes(b'not the sealed inventory')
        with self.assertRaises(ValueError):w.cleanup(self.folder)
        self.assertTrue(self.file.exists())
    def test_activity_backup_record_overrides_disposable_registration(self):
        self.seal();w.write(self.suite/'.local/ACTIVE_STUDIO_INSTALLATION.json',dict(retainedBackupWorkspace=str(self.payload)))
        with self.assertRaises(ValueError):w.cleanup(self.folder)
        self.assertTrue(self.file.exists())
    def test_in_use_file_is_retried_and_keeps_identity(self):
        self.seal()
        from owned_files import held
        with held(self.file):self.assertEqual(w.cleanup(self.folder)['status'],'DEFERRED_OR_UNKNOWN_RETAINED');self.assertTrue(self.file.exists())
        self.assertEqual(w.cleanup(self.folder)['status'],'COMPLETE')
    def test_junction_refused_without_touching_outside_target(self):
        outside=self.suite/'outside';outside.mkdir();original=outside/'keep.bin';original.write_bytes(b'outside')
        junction=self.payload/'escape'
        proc=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(junction),str(outside)],capture_output=True)
        self.assertEqual(proc.returncode,0,proc.stdout+proc.stderr)
        try:
            with self.assertRaises(ValueError):self.seal()
            self.assertEqual(original.read_bytes(),b'outside')
        finally:os.rmdir(junction) # exact synthetic link only, never its target
    def test_no_evidence_and_duplicate_seal_are_rejected(self):
        with self.assertRaises(ValueError):w.seal(self.folder,[])
        self.seal()
        with self.assertRaises(ValueError):self.seal()

    def test_java_created_deep_fixture_paths_are_inventoried_and_removed(self):
        deep=self.payload/('long-'+('x'*70))/('nested-'+('y'*70))/('asset-'+('z'*70)+'.blob')
        w.io_path(deep.parent).mkdir(parents=True)
        w.io_path(deep).write_bytes(b'long synthetic path')
        self.assertGreater(len(str(deep)),300);self.seal()
        self.assertEqual(w.cleanup(self.folder)['status'],'COMPLETE')
        self.assertFalse(w.io_path(deep).exists())

