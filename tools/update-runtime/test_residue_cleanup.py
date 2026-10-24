"""All deletion targets are fresh synthetic fixtures, never the user's installation."""
import ctypes, hashlib, json, os, subprocess, unittest, uuid
from unittest.mock import patch
from pathlib import Path
from install_ops import atomic_json, immutable_file, verify_install
from residue_cleanup import enroll_program,enroll_download,run,ROLLBACK_SECONDS,FAILED_SECONDS

ROOT=Path(__file__).resolve().parents[2]

class ResidueCleanupTests(unittest.TestCase):
    def setUp(self):
        self.base=Path(os.environ.get('CCB_SYNTHETIC_TEST_ROOT',ROOT/'.local/windows-rc'))/'residue-cleanup-fixtures'/uuid.uuid4().hex[:8]
        self.root=self.base/'programs';self.root.mkdir(parents=True)
        self.data=self.base/'data';self.data.mkdir();(self.data/'original.bin').write_bytes(b'unique synthetic data')
        self.runtime=self.base/'runtime';self.cache=self.runtime/'updates';self.cache.mkdir(parents=True)
        self.now=1_000_000.;self.installer_hash=hashlib.sha256(b'synthetic verified installer').hexdigest()
        self.old,self.old_id=self.program(1,'a');self.current,self.current_id=self.program(2,'b')
        self.old_receipt=enroll_program(self.cache,self.root,self.old,self.old_id,self.installer_hash,self.cache/'install-old.json',self.now)
        enroll_program(self.cache,self.root,self.current,self.current_id,self.installer_hash,self.cache/'install-current.json',self.now)
        self.folder=self.cache/'handoffs/fixture';self.folder.mkdir(parents=True)
        journal=self.cache/'install-current.json'
        atomic_json(journal,dict(status='INSTALLED_NOT_STARTED',expected=self.current_id,target=str(self.current),verification=verify_install(self.current,self.current_id)))
        self.plan=dict(format=1,nonce='b'*64,installed=str(self.old),cache=str(self.cache),approved=dict(**self.current_id,sha256=self.installer_hash))
        atomic_json(self.folder/'plan.json',self.plan)
        atomic_json(self.folder/'result.json',dict(status='COMPLETE',nonce=self.plan['nonce'],installed=str(self.current),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        atomic_json(self.root/'launch-current.json',dict(installed=str(self.current),data=str(self.data),runtime=str(self.runtime)))
        self.starts=[str(uuid.uuid4()),str(uuid.uuid4())]

    def program(self,version,letter):
        expected=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=version,sourceSha=letter*40)
        path=self.root/f'{version}-{letter*12}';(path/'app').mkdir(parents=True)
        entries=[]
        for name in ['CCBStudioSuitePreview.exe','app/main.jar','retired.dll']:
            p=path/name;p.write_bytes((name+str(version)).encode());entries.append(dict(path=name,bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
        atomic_json(path/'INSTALL_MANIFEST.json',dict(format=1,**expected,files=entries))
        (path/'INSTALL_COMPLETE').write_text(f'{version}:{letter*40}');(path/'Uninstall.exe').write_bytes(b'synthetic uninstaller')
        return path,expected

    def cleanup(self,offset=0,start=None,**kwargs):return run(self.current,self.cache,self.data,self.runtime,start,self.now+offset,**kwargs)
    def mature(self):
        self.cleanup(start=self.starts[0]);self.cleanup(start=self.starts[1]);return self.cleanup(ROLLBACK_SECONDS+1)

    def test_success_retires_immediately_without_seven_days_or_extra_cold_starts(self):
        result=self.cleanup();self.assertGreater(result['removed'],0);self.assertFalse(self.old.exists());self.assertTrue((self.current/'retired.dll').exists())
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data');self.assertTrue((self.folder/'result.json').is_file())

    def test_obsolete_manifest_members_removed_unknown_modified_files_and_plugin_tree_retained(self):
        (self.old/'manual-backup').mkdir();(self.old/'manual-backup/keep.bin').write_bytes(b'backup')
        (self.old/'plugins').mkdir();(self.old/'plugins/custom.dll').write_bytes(b'plugin')
        (self.old/'user-note.txt').write_bytes(b'unknown');(self.old/'retired.dll').write_bytes(b'changed by user')
        self.mature();self.assertFalse((self.old/'app/main.jar').exists());self.assertEqual((self.old/'retired.dll').read_bytes(),b'changed by user')
        for name in ('manual-backup/keep.bin','plugins/custom.dll','user-note.txt'):self.assertTrue((self.old/name).exists())

    def test_legacy_manifest_alone_never_grants_automatic_deletion(self):
        legacy,_=self.program(0+3,'c');before=(legacy/'app/main.jar').read_bytes();self.mature();self.assertEqual((legacy/'app/main.jar').read_bytes(),before)

    def test_only_just_superseded_active_program_can_adopt_a_pinned_old_install_record(self):
        os.rename(self.old_receipt,self.base/'retained-original-ownership.json')
        journal=self.cache/'install-prior.json';atomic_json(journal,dict(target=str(self.old),expected=self.old_id,verification=verify_install(self.old,self.old_id)))
        folder=self.cache/'handoffs/prior';folder.mkdir()
        atomic_json(folder/'plan.json',dict(format=1,nonce='c'*64,installed=str(self.root/'0-synthetic'),cache=str(self.cache),approved=dict(**self.old_id,sha256=self.installer_hash)))
        atomic_json(folder/'result.json',dict(status='COMPLETE',nonce='c'*64,installed=str(self.old),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        (self.old/'unknown.txt').write_bytes(b'not owned')
        self.cleanup();self.assertFalse((self.old/'app/main.jar').exists());self.assertEqual((self.old/'unknown.txt').read_bytes(),b'not owned')

    def test_incomplete_or_failed_handoff_never_releases_fallback_or_download(self):
        for status in ('STARTING_B','FAILED_RETAINED'):
            value=json.loads((self.folder/'result.json').read_text());value['status']=status;atomic_json(self.folder/'result.json',value)
            result=self.cleanup(FAILED_SECONDS*2,start=self.starts[0]);self.assertEqual(result['removed'],0);self.assertTrue((self.old/'retired.dll').exists())

    def test_successful_download_retired_only_after_complete_while_incomplete_output_is_bounded_and_known(self):
        downloads=self.cache/'downloads';downloads.mkdir();directory=downloads/str(uuid.uuid4());directory.mkdir()
        (directory/'studio-installer.exe').write_bytes(b'synthetic verified installer');atomic_json(directory/'VERIFIED.json',dict(version=2,sha256=self.installer_hash))
        enroll_download(self.cache,self.root,directory,self.current_id,self.installer_hash,True,self.now)
        result_path=self.folder/'result.json';saved=result_path.read_bytes();value=json.loads(saved);value['status']='FAILED_RETAINED';atomic_json(result_path,value)
        self.cleanup();self.assertTrue(directory.exists());result_path.write_bytes(saved)
        self.cleanup();self.assertFalse(directory.exists())
        partial=downloads/(str(uuid.uuid4())+'.incomplete');partial.mkdir();(partial/'studio-installer.exe').write_bytes(b'owned partial copy');(partial/'unknown.bin').write_bytes(b'unknown')
        enroll_download(self.cache,self.root,partial,self.current_id,self.installer_hash,False,self.now)
        self.cleanup()
        self.assertFalse((partial/'studio-installer.exe').exists());self.assertEqual((partial/'unknown.bin').read_bytes(),b'unknown')

    def test_partial_owned_extraction_retires_after_a_successful_new_program_without_a_timer(self):
        stage=self.root/('3-'+'c'*12+'.'+uuid.uuid4().hex+'.installing');full,expected=self.program(3,'c');os.rename(full,stage)
        enroll_program(self.cache,self.root,stage,expected,self.installer_hash,self.cache/'install-failed.json',self.now)
        self.cleanup();self.assertFalse(stage.exists())

    def test_registered_program_and_backup_protection_override_retirement(self):
        atomic_json(self.root/'ACTIVE_INSTALLATION.json',dict(doNotCleanReferencedDirectories=True,program=str(self.old),retainedBackupWorkspace=str(self.data)))
        self.mature();self.assertTrue((self.old/'app/main.jar').exists());self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')

    def test_file_in_use_defers_whole_program_then_retries_without_changing_update_result(self):
        result=(self.folder/'result.json').read_bytes()
        with immutable_file(self.old/'CCBStudioSuitePreview.exe'):
            value=self.cleanup(ROLLBACK_SECONDS+1);self.assertEqual(value['status'],'DEFERRED');self.assertTrue((self.old/'app/main.jar').exists())
        self.cleanup(ROLLBACK_SECONDS+2);self.assertFalse(self.old.exists());self.assertEqual((self.folder/'result.json').read_bytes(),result)

    def test_access_denied_keeps_file_for_next_run_and_update_stays_complete(self):
        p=self.old/'retired.dll'
        with patch('residue_cleanup.delete_matching',side_effect=PermissionError('injected ACL denial')):
            self.assertEqual(self.cleanup()['status'],'DEFERRED');self.assertTrue(p.exists())
        self.cleanup(ROLLBACK_SECONDS+2);self.assertFalse(p.exists());self.assertEqual(json.loads((self.folder/'result.json').read_text())['status'],'COMPLETE')

    def test_normal_readonly_shipped_launcher_is_deleted_by_its_verified_handle(self):
        p=self.old/'CCBStudioSuitePreview.exe';k=ctypes.WinDLL('kernel32',use_last_error=True);k.SetFileAttributesW.argtypes=[ctypes.c_wchar_p,ctypes.c_ulong]
        self.assertTrue(k.SetFileAttributesW(str(p),1))
        self.cleanup();self.assertFalse(p.exists());self.assertTrue((self.current/'CCBStudioSuitePreview.exe').exists())

    def test_interrupted_cleanup_replays_safely_and_repeated_cleanup_is_idempotent(self):
        def interrupt(_):raise SystemExit('synthetic interruption after one removal')
        with self.assertRaises(SystemExit):self.cleanup(ROLLBACK_SECONDS+1,after_delete=interrupt)
        self.cleanup(ROLLBACK_SECONDS+2);self.assertFalse(self.old.exists());self.assertEqual(self.cleanup(ROLLBACK_SECONDS+3)['removed'],0)

    def test_traversal_and_junction_refuse_any_outside_deletion(self):
        receipt=json.loads(self.old_receipt.read_text());receipt['files'][0]['path']='../../data/original.bin';atomic_json(self.old_receipt,receipt)
        with self.assertRaises(ValueError):self.mature()
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')
        receipt['files'][0]['path']='CCBStudioSuitePreview.exe';atomic_json(self.old_receipt,receipt)
        # A real NTFS junction, without requiring symlink privilege. Never remove it recursively.
        moved=self.old/'retained-app';os.rename(self.old/'app',moved)
        outside=self.base/'outside';outside.mkdir();(outside/'main.jar').write_bytes(b'outside retained')
        proc=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(self.old/'app'),str(outside)],capture_output=True)
        self.assertEqual(proc.returncode,0,proc.stdout+proc.stderr)
        try:
            with self.assertRaises(ValueError):self.mature()
            self.assertEqual((outside/'main.jar').read_bytes(),b'outside retained');self.assertTrue((moved/'main.jar').exists())
        finally:os.rmdir(self.old/'app') # exact newly-created synthetic junction only

    def test_secondary_workspace_and_invalid_protection_record_fail_closed(self):
        other=self.base/'restored';other.mkdir();self.assertEqual(run(self.current,self.cache,other,self.runtime)['status'],'RETAINED_SECONDARY_WORKSPACE')
        atomic_json(self.root/'ACTIVE_INSTALLATION.json',dict(program='../outside'))
        with self.assertRaises(ValueError):self.mature()
        self.assertTrue((self.old/'retired.dll').exists())

if __name__=='__main__':unittest.main(verbosity=2)
