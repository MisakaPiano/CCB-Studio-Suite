"""All deletion targets are fresh synthetic fixtures, never the user's installation."""
import ctypes, hashlib, json, os, shutil, subprocess, unittest, uuid
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

    def legacy_cache_handoff(self,approved,old=None,new=None,origin=None):
        legacy=self.base/'previous-install-name';cache=origin or legacy/'runtime/updates'
        folder=self.cache/'handoffs/legacy-cache';folder.mkdir()
        plan=dict(format=1,nonce='f'*64,cache=str(cache),installed=str(old or legacy/'programs/107-synthetic'),approved=approved)
        atomic_json(folder/'plan.json',plan)
        atomic_json(folder/'result.json',dict(status='COMPLETE',nonce='f'*64,installed=str(new or legacy/'programs/109-synthetic'),installJournal=str(self.cache/'install-current.json'),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        return folder

    def owned_current_download(self):
        folder=self.cache/'downloads'/str(uuid.uuid4());folder.mkdir(parents=True)
        (folder/'studio-installer.exe').write_bytes(b'synthetic verified installer')
        atomic_json(folder/'VERIFIED.json',dict(version=2,sha256=self.installer_hash))
        enroll_download(self.cache,self.root,folder,self.current_id,self.installer_hash,True,self.now)
        return folder

    def test_old_cache_record_is_retained_without_blocking_unrelated_completed_cleanup(self):
        download=self.owned_current_download();protected,_=self.program(111,'d')
        atomic_json(self.root/'ACTIVE_INSTALLATION.json',dict(program=str(protected),workspace=str(self.data)))
        legacy=self.legacy_cache_handoff(dict(version=109,sourceSha='c'*40,channel='internal'))
        proof=(legacy/'plan.json').read_bytes();keep=(protected/'app/main.jar').read_bytes()
        result=self.cleanup();self.assertGreater(result['removed'],0)
        self.assertFalse(self.old.exists());self.assertFalse(download.exists())
        self.assertEqual((legacy/'plan.json').read_bytes(),proof);self.assertEqual((protected/'app/main.jar').read_bytes(),keep)
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data');self.assertEqual(self.cleanup()['removed'],0)

    def test_old_cache_unproven_local_references_still_protect_program_and_download(self):
        download=self.owned_current_download()
        self.legacy_cache_handoff(dict(version=2,sourceSha='b'*40,channel='internal',installer=str(download/'studio-installer.exe')),self.old,self.current)
        self.assertEqual(self.cleanup()['removed'],0);self.assertTrue((self.old/'app/main.jar').exists());self.assertTrue(download.exists())
        self.assertEqual(self.cleanup()['removed'],0)

    def test_old_cache_record_without_projectable_target_fails_closed(self):
        download=self.owned_current_download();self.legacy_cache_handoff(dict(version=109,channel='internal'))
        with self.assertRaises(ValueError):self.cleanup()
        self.assertTrue((self.old/'app/main.jar').exists());self.assertTrue(download.exists())
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')

    def unowned_completed_download(self):
        folder=self.cache/'downloads'/str(uuid.uuid4());folder.mkdir(parents=True)
        (folder/'studio-installer.exe').write_bytes(b'synthetic verified installer')
        atomic_json(folder/'VERIFIED.json',dict(version=2,sourceSha='b'*40,channel='internal',sha256=self.installer_hash))
        self.plan['approved']['installer']=str(folder/'studio-installer.exe');atomic_json(self.folder/'plan.json',self.plan)
        return folder

    def test_legacy_completed_download_without_receipt_is_adopted_and_retired(self):
        download=self.unowned_completed_download();self.assertGreater(self.cleanup()['removed'],0)
        self.assertFalse(download.exists());self.assertEqual(self.cleanup()['removed'],0)

    def test_legacy_completed_download_changed_bytes_or_identity_is_retained(self):
        download=self.unowned_completed_download();(download/'studio-installer.exe').write_bytes(b'unknown replacement')
        self.cleanup();self.assertTrue(download.exists())
        (download/'studio-installer.exe').write_bytes(b'synthetic verified installer')
        atomic_json(download/'VERIFIED.json',dict(version=999,sourceSha='b'*40,channel='internal',sha256=self.installer_hash))
        self.cleanup();self.assertTrue(download.exists());self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')

    def test_legacy_download_adoption_never_releases_foreign_unproven_reference(self):
        download=self.unowned_completed_download()
        self.legacy_cache_handoff(dict(version=2,sourceSha='b'*40,channel='internal',installer=str(download/'studio-installer.exe')),self.old,self.current)
        self.assertEqual(self.cleanup()['removed'],0);self.assertTrue(download.exists());self.assertTrue((self.old/'app/main.jar').exists())

    def test_foreign_drive_bookkeeping_is_projected_without_traversing_old_drive(self):
        download=self.owned_current_download()
        self.legacy_cache_handoff(dict(version=109,sourceSha='c'*40,channel='internal'),Path('Z:/synthetic-old/programs/107-old'),Path('Z:/synthetic-old/programs/109-old'),Path('Z:/synthetic-old/runtime/updates'))
        self.assertGreater(self.cleanup()['removed'],0);self.assertFalse(download.exists())
        self.assertFalse(self.old.exists());self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')

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
        journal=self.cache/'install-prior.json';atomic_json(journal,dict(status='INSTALLED_NOT_STARTED',target=str(self.old),expected=self.old_id,verification=verify_install(self.old,self.old_id)))
        folder=self.cache/'handoffs/prior';folder.mkdir()
        atomic_json(folder/'plan.json',dict(format=1,nonce='c'*64,installed=str(self.root/'0-synthetic'),cache=str(self.cache),approved=dict(**self.old_id,sha256=self.installer_hash)))
        atomic_json(folder/'result.json',dict(status='COMPLETE',nonce='c'*64,installed=str(self.old),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        (self.old/'unknown.txt').write_bytes(b'not owned')
        (self.old/'Uninstall.exe').write_bytes(b'unknown replacement; old journal has no digest for this generated file')
        self.cleanup();self.assertFalse((self.old/'app/main.jar').exists());self.assertEqual((self.old/'unknown.txt').read_bytes(),b'not owned')
        self.assertEqual((self.old/'Uninstall.exe').read_bytes(),b'unknown replacement; old journal has no digest for this generated file')

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

    def complete_target(self,old,new,expected,nonce='d'*64):
        journal=self.cache/('install-'+str(expected['version'])+'.json')
        atomic_json(journal,dict(status='INSTALLED_NOT_STARTED',target=str(new),expected=expected,verification=verify_install(new,expected)))
        folder=self.cache/'handoffs'/nonce[:8];folder.mkdir(exist_ok=True)
        atomic_json(folder/'plan.json',dict(format=1,nonce=nonce,installed=str(old),cache=str(self.cache),approved=dict(**expected,sha256=self.installer_hash)))
        atomic_json(folder/'result.json',dict(status='COMPLETE',nonce=nonce,installed=str(new),installJournal=str(journal),verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        atomic_json(self.root/'launch-current.json',dict(installed=str(new),data=str(self.data),runtime=str(self.runtime)))

    def test_first_current_adoption_never_claims_replaced_generated_uninstaller(self):
        for receipt in (self.cache/'cleanup/owned').glob('*.json'):
            if json.loads(receipt.read_text())['base']==self.current.name:receipt.rename(self.base/'retained-current-receipt.json')
        replaced=b'unknown replacement without install-time ownership proof'.ljust(81,b'!')
        (self.current/'Uninstall.exe').write_bytes(replaced)
        self.cleanup() # First B startup adopts only the pinned manifest, not this uninstaller.
        previous=self.current;self.current,self.current_id=self.program(3,'c')
        self.complete_target(previous,self.current,self.current_id)
        self.cleanup()
        self.assertFalse((previous/'app/main.jar').exists())
        self.assertEqual((previous/'Uninstall.exe').read_bytes(),replaced)

    def test_verified_next_target_survives_old_success_and_pending_handoff_until_its_own_complete(self):
        downloads=self.cache/'downloads';downloads.mkdir();directory=downloads/str(uuid.uuid4());directory.mkdir()
        candidate,expected=self.program(3,'c')
        (directory/'studio-installer.exe').write_bytes(b'synthetic verified installer')
        atomic_json(directory/'VERIFIED.json',dict(version=3,sourceSha=expected['sourceSha'],sha256=self.installer_hash))
        enroll_download(self.cache,self.root,directory,expected,self.installer_hash,True,self.now)
        self.cleanup();self.assertTrue((directory/'studio-installer.exe').exists())
        folder=self.cache/'handoffs/pending-c';folder.mkdir()
        plan=dict(format=1,nonce='e'*64,installed=str(self.current),cache=str(self.cache),approved=dict(**expected,sha256=self.installer_hash,installer=str(directory/'studio-installer.exe')))
        atomic_json(folder/'plan.json',plan);atomic_json(folder/'result.json',dict(status='READY_TO_DRAIN'))
        self.cleanup();self.assertTrue((directory/'studio-installer.exe').exists())
        # Even a retained nondefault source in this cache may reference the same
        # download. Collect references before limiting program-root retirement.
        plan['installed']=str(self.base/'external-program/fixture');atomic_json(folder/'plan.json',plan)
        self.cleanup();self.assertTrue((directory/'studio-installer.exe').exists())
        previous=self.current;self.current=candidate;self.current_id=expected
        self.complete_target(previous,candidate,expected)
        self.cleanup();self.assertTrue(directory.exists(),'A nonterminal handoff still references this exact installer')
        atomic_json(folder/'result.json',dict(status='COMPLETE')) # Not a verified terminal receipt.
        self.cleanup();self.assertTrue(directory.exists(),'An incomplete completion record cannot release its reference')
        atomic_json(folder/'result.json',dict(status='FAILED_RETAINED',nonce=plan['nonce']))
        self.cleanup();self.assertFalse(directory.exists())

    def referenced_download(self):
        directory=self.cache/'downloads'/str(uuid.uuid4());directory.mkdir(parents=True)
        file=directory/'studio-installer.exe';file.write_bytes(b'synthetic verified installer')
        self.assertEqual(hashlib.sha256(file.read_bytes()).hexdigest(),self.installer_hash)
        atomic_json(directory/'VERIFIED.json',dict(**self.current_id,sha256=self.installer_hash))
        enroll_download(self.cache,self.root,directory,self.current_id,self.installer_hash,True,self.now)
        foreign=self.base/'nondefault-programs'/uuid.uuid4().hex;foreign.mkdir(parents=True)
        new=foreign/self.current.name;shutil.copytree(self.current,new)
        journal=self.cache/('install-reference-'+uuid.uuid4().hex+'.json')
        atomic_json(journal,dict(status='INSTALLED_NOT_STARTED',target=str(new),expected=self.current_id,verification=verify_install(new,self.current_id)))
        folder=self.cache/'handoffs'/uuid.uuid4().hex;folder.mkdir()
        plan=dict(format=1,nonce='e'*64,installed=str(foreign/'1-aaaaaaaaaaaa'),cache=str(self.cache),approved=dict(**self.current_id,sha256=self.installer_hash,installer=str(file)))
        result=dict(status='COMPLETE',nonce=plan['nonce'],verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH',installed=str(new),installJournal=str(journal))
        atomic_json(folder/'plan.json',plan);atomic_json(folder/'result.json',result)
        return directory,folder,plan,result,journal

    def assert_reference_retained(self,directory):
        self.cleanup()
        self.assertTrue((directory/'studio-installer.exe').is_file(),'Unverified terminal reference must retain its installer')
        self.assertEqual((directory/'studio-installer.exe').read_bytes(),b'synthetic verified installer')
        self.assertTrue((directory/'VERIFIED.json').is_file())
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')

    def test_cross_root_complete_markers_without_installed_or_journal_keep_download(self):
        for missing in ('installed','installJournal'):
            with self.subTest(missing=missing):
                directory,folder,plan,result,journal=self.referenced_download();result.pop(missing)
                atomic_json(folder/'result.json',result);self.assert_reference_retained(directory)

    def test_cross_root_complete_with_nonexistent_journal_keeps_download(self):
        directory,folder,plan,result,journal=self.referenced_download()
        absent=self.cache/('absent-'+uuid.uuid4().hex+'.json');self.assertFalse(absent.exists())
        result['installJournal']=str(absent);atomic_json(folder/'result.json',result)
        self.assert_reference_retained(directory)

    def test_cross_root_failed_receipt_with_wrong_or_missing_nonce_keeps_download(self):
        for nonce in ('0'*64,None):
            with self.subTest(nonce=nonce):
                directory,folder,plan,result,journal=self.referenced_download()
                failed=dict(status='FAILED_RETAINED')
                if nonce is not None:failed['nonce']=nonce
                atomic_json(folder/'result.json',failed);self.assert_reference_retained(directory)

    def test_cross_root_journal_identity_and_verification_must_match_before_releasing_reference(self):
        for mismatch in ('target','version','sourceSha','channel','product','platform','verification','status','missingStatus'):
            with self.subTest(mismatch=mismatch):
                directory,folder,plan,result,journal=self.referenced_download();value=json.loads(journal.read_text())
                if mismatch=='target':value['target']=str(self.current)
                elif mismatch=='verification':value['verification']={}
                elif mismatch=='status':value['status']='FAILED_RETAINED'
                elif mismatch=='missingStatus':value.pop('status')
                else:value['expected'][mismatch]={'version':3,'sourceSha':'f'*40,'channel':'preview','product':'other-product','platform':'other-platform'}[mismatch]
                atomic_json(journal,value);self.assert_reference_retained(directory)

    def test_cross_root_valid_complete_and_matching_failed_nonce_release_download_immediately(self):
        for status in ('COMPLETE','FAILED_RETAINED'):
            with self.subTest(status=status):
                directory,folder,plan,result,journal=self.referenced_download()
                if status=='FAILED_RETAINED':result=dict(status=status,nonce=plan['nonce'])
                atomic_json(folder/'result.json',result);self.cleanup()
                self.assertFalse(directory.exists());self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')
                self.assertEqual(self.cleanup()['removed'],0,'Repeat cleanup remains safe')

    def test_same_target_version_with_different_installer_digest_does_not_release_download(self):
        directory=self.cache/'downloads'/str(uuid.uuid4());directory.mkdir(parents=True)
        (directory/'studio-installer.exe').write_bytes(b'different synthetic target')
        digest=hashlib.sha256((directory/'studio-installer.exe').read_bytes()).hexdigest()
        enroll_download(self.cache,self.root,directory,self.current_id,digest,True,self.now)
        self.cleanup();self.assertTrue((directory/'studio-installer.exe').exists())

    def test_fresh_failed_download_is_not_released_by_an_older_completion(self):
        directory=self.cache/'downloads'/(str(uuid.uuid4())+'.incomplete');directory.mkdir(parents=True)
        (directory/'studio-installer.exe').write_bytes(b'partial future target')
        future=dict(self.current_id,version=3,sourceSha='c'*40)
        later=(self.cache/'install-current.json').stat().st_mtime+100
        enroll_download(self.cache,self.root,directory,future,self.installer_hash,False,later)
        self.cleanup();self.assertTrue(directory.exists())

    def test_fresh_failed_extraction_waits_for_a_later_completion(self):
        stage=self.root/('3-'+'c'*12+'.'+uuid.uuid4().hex+'.installing');full,expected=self.program(3,'c');os.rename(full,stage)
        later=(self.folder/'result.json').stat().st_mtime+100
        enroll_program(self.cache,self.root,stage,expected,self.installer_hash,self.cache/'failed.json',later)
        self.cleanup();self.assertTrue((stage/'app/main.jar').exists())

if __name__=='__main__':unittest.main(verbosity=2)
