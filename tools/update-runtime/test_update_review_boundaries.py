"""Only the new review boundaries; all fixtures are synthetic and retained."""
import importlib.util,json,os,subprocess,uuid,unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch
import apply_update as au
from studio_update import UpdateRejected
from test_verification_wait import VerificationWaitTests
from test_program_repair import ProgramRepairTests
from internal_installer_inputs import validate_committed_sources,PACKAGING_REQUIRED

ROOT=Path(__file__).resolve().parents[2]

class AckAdmissionTests(unittest.TestCase):
    setUp=VerificationWaitTests.setUp
    publish=VerificationWaitTests.publish
    call=VerificationWaitTests.call

    def test_required_fields_and_types_fail_closed(self):
        for key in self.ack:
            wrong=dict(self.ack);del wrong[key];self.publish(wrong)
            with self.subTest(missing=key),self.assertRaises(UpdateRejected):self.call(lambda _:None)
        for key,value in [('nonce',1),('version',True),('sourceSha',[]),('snapshot',[]),('dataRoot',None),('dataVersion',True),('dataVersion','3'),('format',True),('format',2),('dataRoot','relative')]:
            wrong=dict(self.ack);wrong[key]=value;self.publish(wrong)
            with self.subTest(key=key,value=value),self.assertRaises(UpdateRejected):self.call(lambda _:None)
        self.publish();self.assertEqual(self.call(lambda _:None),self.ack)

    def test_ack_with_cancel_or_failed_never_wins(self):
        self.publish()
        for name in ['CANCEL','B_FAILED.json']:
            marker=self.folder/name;marker.touch()
            with self.subTest(marker=name),self.assertRaises(UpdateRejected):self.call(lambda _:None)
            marker.unlink() # Only this test's marker, no source/user files.

    def test_ack_after_final_sleep_deadline_rejected(self):
        def late(_):self.now=3;self.publish()
        with self.assertRaisesRegex(UpdateRejected,'deadline'):self.call(late,timeout=2)

    def proof(self):
        self.publish();proof={}
        au.wait_for_b(self.folder,self.child,self.plan,self.identity,self.custom,self.data,lambda *a,**k:None,
            clock=lambda:self.now,sleep=lambda _:None,proof=proof)
        return proof

    def admit(self,proof):
        return au.admit_publication(self.folder,proof,self.plan,self.identity,self.custom,self.data,
            lambda s,**kw:self.states.append((s,kw)),clock=lambda:self.now)

    def test_cancel_or_failure_during_preparation_is_rejected(self):
        proof=self.proof()
        for name in ['CANCEL','B_FAILED.json']:
            marker=self.folder/name;marker.touch()
            with self.subTest(marker=name),self.assertRaises(UpdateRejected):self.admit(proof)
            self.assertFalse((self.folder/'PUBLICATION_ADMITTED.json').exists());marker.unlink()

    def test_expired_or_changed_ack_at_final_admission_rejected(self):
        proof=self.proof();self.now=1800
        with self.assertRaisesRegex(UpdateRejected,'deadline'):self.admit(proof)
        self.now=0;self.publish(dict(self.ack,version=3))
        with self.assertRaises(UpdateRejected):self.admit(proof)
        self.assertFalse((self.folder/'PUBLICATION_ADMITTED.json').exists())

    def test_success_admission_marker_and_cancel_lock_exclusion(self):
        import msvcrt
        proof=self.proof()
        with (self.folder/'publication.lock').open('a+b') as stream:
            stream.write(b'0');stream.flush();stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
            try:
                with self.assertRaises(OSError):self.admit(proof)
            finally:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
        self.admit(proof)
        self.assertEqual(json.loads((self.folder/'PUBLICATION_ADMITTED.json').read_text())['nonce'],self.plan['nonce'])
        self.assertEqual(self.states[-1][0],'COMMITTING')

class PublicationBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.root=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('publication-'+uuid.uuid4().hex);self.root.mkdir()
        self.final=self.root/'2-new';(self.final/'updater').mkdir(parents=True)
        self.data=self.root/'data';self.data.mkdir();self.runtime=self.root/'runtime';self.runtime.mkdir()
        self.old=dict(format=1,version=1,installed=str(self.root/'1-old'),data=str(self.data),runtime=str(self.runtime))
        au.atomic_json(self.root/'launch-current.json',self.old)
        (self.final/'update-config.json').write_text(json.dumps(dict(version=2)))
        (self.final/'CCBStudioSuitePreview.exe').write_bytes(b'new synthetic main')
        self.bundled=self.final/'updater/CCBStudioLauncher.exe';self.bundled.write_bytes(b'new synthetic launcher')
        self.stable=self.root/'CCBStudioLauncher.exe';self.stable.write_bytes(b'old synthetic launcher')
        self.link=self.root/'Launch-CCB-Studio.lnk';self.link.write_bytes(b'old synthetic link')
        self.before=self.snapshot()
    def snapshot(self):return {p.name:p.read_bytes() for p in [self.stable,self.link,self.root/'launch-current.json']}
    def prepare(self,args,**kw):
        self.assertEqual(args[0],str(self.bundled));self.assertEqual(args[1],'--prepare-shortcut')
        self.assertEqual(args[3:],list(map(str,[self.stable,self.link])))
        Path(args[2]).write_bytes(b'new synthetic link');return SimpleNamespace(returncode=0)
    def publish(self,admission=None):return au.create_launcher(self.final,self.data,self.runtime,expected_current=self.old,publication_admission=admission)
    def journal(self):return json.loads(next(self.root.glob('.entrypoint-*/journal.json')).read_text())

    def test_shortcut_return_exception_and_no_output_leave_entrypoints_unchanged(self):
        for failure in [lambda *a,**k:SimpleNamespace(returncode=37),OSError('shortcut error'),lambda *a,**k:SimpleNamespace(returncode=0)]:
            with self.subTest(failure=str(failure)),patch('apply_update.subprocess.run',side_effect=failure),self.assertRaises((UpdateRejected,OSError)):
                self.publish()
            self.assertEqual(self.before,self.snapshot())
        self.assertTrue(all(json.loads(p.read_text())['status']=='FAILED_RESTORED' for p in self.root.glob('.entrypoint-*/journal.json')))

    def test_link_write_failure_restores_exe_and_keeps_descriptor(self):
        replace=au.os.replace
        def failing(source,target):
            if Path(target)==self.link:raise OSError('injected link replace')
            return replace(source,target)
        with patch('apply_update.subprocess.run',side_effect=self.prepare),patch('apply_update.os.replace',side_effect=failing),self.assertRaises(OSError):self.publish()
        self.assertEqual(self.before,self.snapshot());self.assertEqual(self.journal()['status'],'FAILED_RESTORED')

    def test_descriptor_failure_restores_both_components(self):
        atomic=au.atomic_json
        def failing(path,value):
            if Path(path)==self.root/'launch-current.json':raise OSError('descriptor replace failure')
            return atomic(path,value)
        with patch('apply_update.subprocess.run',side_effect=self.prepare),patch('apply_update.atomic_json',side_effect=failing),self.assertRaises(OSError):self.publish()
        self.assertEqual(self.before,self.snapshot());self.assertEqual(self.journal()['status'],'FAILED_RESTORED')

    def test_restore_failure_is_explicit_recovery_required(self):
        replace=au.os.replace
        def failing(source,target):
            if Path(target)==self.link or str(source).endswith('.restore'):raise OSError('injected failure')
            return replace(source,target)
        with patch('apply_update.subprocess.run',side_effect=self.prepare),patch('apply_update.os.replace',side_effect=failing),self.assertRaises(au.EntrypointRecoveryRequired):self.publish()
        self.assertEqual(json.loads((self.root/'launch-current.json').read_text()),self.old)
        self.assertEqual(self.journal()['status'],'ENTRYPOINT_RECOVERY_REQUIRED')
        self.assertEqual(self.stable.read_bytes(),b'new synthetic launcher')

    def test_success_commits_descriptor_last_and_retains_recovery_material(self):
        def admit():self.assertEqual(self.before,self.snapshot())
        with patch('apply_update.subprocess.run',side_effect=self.prepare):self.publish(admit)
        self.assertEqual(json.loads((self.root/'launch-current.json').read_text())['installed'],str(self.final))
        self.assertEqual(self.journal()['status'],'COMMITTED');self.assertTrue(list(self.root.glob('.entrypoint-*/CCBStudioLauncher.exe.before')))

    def test_cancel_before_admission_preserves_all_three_files(self):
        def cancel():raise au.UpdateCancelled('cancel accepted')
        with patch('apply_update.subprocess.run',side_effect=self.prepare),self.assertRaises(au.UpdateCancelled):self.publish(cancel)
        self.assertEqual(self.before,self.snapshot())

    def test_newer_descriptor_is_never_blindly_rolled_back(self):
        newer=dict(self.old,version=3)
        def race(args,**kw):
            self.prepare(args,**kw);au.atomic_json(self.root/'launch-current.json',newer);return SimpleNamespace(returncode=0)
        with patch('apply_update.subprocess.run',side_effect=race),self.assertRaises(au.EntrypointRecoveryRequired):self.publish()
        self.assertEqual(json.loads((self.root/'launch-current.json').read_text()),newer)
        self.assertEqual(self.stable.read_bytes(),self.before[self.stable.name])

    def test_commit_with_lost_write_return_is_recognized(self):
        atomic=au.atomic_json
        def after_effect(path,value):
            atomic(path,value)
            if Path(path)==self.root/'launch-current.json':raise OSError('injected lost return')
        with patch('apply_update.subprocess.run',side_effect=self.prepare),patch('apply_update.atomic_json',side_effect=after_effect):self.publish()
        self.assertEqual(self.journal()['status'],'COMMITTED_RECEIPT_RECOVERED')

    def test_interrupted_postcommit_status_is_reconciled_without_republication(self):
        folder=self.root/'handoffs/request';folder.mkdir(parents=True)
        admitted=dict(nonce='a'*64,folder=str(folder))
        with patch('apply_update.subprocess.run',side_effect=self.prepare):self.publish(lambda:admitted)
        before=self.snapshot()
        self.assertEqual(au.interrupted_publication_status(folder,self.root,'a'*64),'COMPLETE_RECEIPT_RECOVERED')
        self.assertEqual(self.snapshot(),before)
        self.assertEqual(au.interrupted_publication_status(folder,self.root,'b'*64),'ENTRYPOINT_RECOVERY_REQUIRED')

    def test_interrupted_partial_publication_is_not_failed_retained(self):
        folder=self.root/'handoffs/request';folder.mkdir(parents=True)
        admitted=dict(nonce='a'*64,folder=str(folder));replace=au.os.replace
        def failing(source,target):
            if Path(target)==self.link or str(source).endswith('.restore'):raise OSError('injected partial')
            return replace(source,target)
        with patch('apply_update.subprocess.run',side_effect=self.prepare),patch('apply_update.os.replace',side_effect=failing),self.assertRaises(au.EntrypointRecoveryRequired):self.publish(lambda:admitted)
        before=self.snapshot()
        self.assertEqual(au.interrupted_publication_status(folder,self.root,'a'*64),'ENTRYPOINT_RECOVERY_REQUIRED')
        self.assertEqual(before,self.snapshot())

class CommittedInputBoundaryTests(unittest.TestCase):
    def test_actual_git_tree_and_untracked_inputs_without_creating_commit(self):
        root=Path(os.environ['CCB_SYNTHETIC_TEST_ROOT'])/('git-'+uuid.uuid4().hex);root.mkdir()
        run=subprocess.check_output
        def git(*args):return run(['git','-c','core.autocrlf=false',*args],cwd=root,stderr=subprocess.STDOUT)
        git('init','-q')
        for name in PACKAGING_REQUIRED:
            p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b'synthetic committed input\n')
        git('add','--',*PACKAGING_REQUIRED);tree=git('write-tree').decode().strip()
        # No commit/ref: only substitute the HEAD tree alias for real git diff.
        head_tree=tree
        def controlled(args,**kw):
            if args[1:3]==['rev-parse','HEAD']:return 'a'*40
            args=[head_tree if x=='HEAD' else x for x in args]
            return run(args,**kw)
        with patch('internal_installer_inputs.subprocess.check_output',side_effect=controlled):
            self.assertEqual(validate_committed_sources(root),'a'*40)
            for name in ['tools/installer_repair_ui.py','tools/update-runtime/repair_install.py','tools/internal_installer_inputs.py','tools/windows-launcher/Launcher.cs']:
                path=root/name;original=path.read_bytes();path.write_bytes(b'dirty')
                with self.subTest(modified=name),self.assertRaises(ValueError):validate_committed_sources(root)
                path.write_bytes(original)
            for name in ['tools/new_generator.py','tools/update-runtime/new_runtime.py','modules/untracked-input.txt']:
                path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(b'untracked')
                with self.subTest(untracked=name),self.assertRaises(ValueError):validate_committed_sources(root)
                path.unlink()
            for name in ['tools/installer_repair_ui.py','tools/update-runtime/repair_install.py']:
                git('update-index','--force-remove','--',name);head_tree=git('write-tree').decode().strip()
                # diff is exactly clean against this tree; rejection must include
                # the actual existing untracked helper, not a modified neighbour.
                self.assertEqual(git('diff',head_tree,'--name-only'),b'')
                with self.subTest(untrackedHelper=name),self.assertRaises(ValueError):validate_committed_sources(root)
                git('add','--',name);head_tree=tree
            self.assertEqual(validate_committed_sources(root),'a'*40)

    def test_default_and_preview_reject_before_output_or_build(self):
        spec=importlib.util.spec_from_file_location('boundary_builder',ROOT/'tools/Build-InternalInstaller.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        for profile in [None,dict(channel='preview',displayVersion='0.0.0-preview.120',githubRepository='MisakaPiano/CCB-Studio-Suite')]:
            dest=ROOT/'.local'/('review-reject-'+uuid.uuid4().hex)
            # Actual current uncommitted Suite tree, read-only Git. The separate
            # isolated-tree test attributes helper-only cases precisely.
            with self.assertRaisesRegex(ValueError,'Commit package inputs'):
                module.build(Path('unused'),Path('unused'),Path('unused'),dest,120,
                    'https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/update-metadata/preview/windows-x86_64/metadata/',
                    'https://github.com/MisakaPiano/CCB-Studio-Suite/releases/download/',Path('unused'),profile=profile)
            self.assertFalse(dest.exists())

class RepairJournalBoundaryTests(unittest.TestCase):
    snapshot=ProgramRepairTests.snapshot
    retained=ProgramRepairTests.retained
    def setUp(self):
        ProgramRepairTests.setUp(self)
        (self.package/'updater').mkdir();(self.package/'updater/CCBStudioLauncher.exe').write_bytes(b'synthetic launcher')
        self.manifest['files']=[dict(path=p.relative_to(self.package).as_posix(),bytes=p.stat().st_size,
            sha256=__import__('hashlib').sha256(p.read_bytes()).hexdigest()) for p in self.package.rglob('*')
            if p.is_file() and p.name not in ['INSTALL_MANIFEST.json','INSTALL_COMPLETE']]
        (self.package/'INSTALL_MANIFEST.json').write_text(json.dumps(self.manifest))
        (self.parent/'CCBStudioLauncher.exe').write_bytes(b'old launcher');(self.parent/'Launch-CCB-Studio.lnk').write_bytes(b'old link')
        self.entry_before=(self.parent/'launch-current.json').read_bytes()
    def result(self):return json.loads(next((self.parent/'.repair-receipts').glob('*.json')).read_text())
    def test_prepare_failure_journal_failed_retained_and_default_unchanged(self):
        from repair_install import repair_program
        with patch('apply_update.subprocess.run',return_value=SimpleNamespace(returncode=37)),self.assertRaises(UpdateRejected):repair_program(self.package,self.old)
        self.assertEqual(self.result()['status'],'FAILED_RETAINED');self.retained()
        self.assertEqual((self.parent/'launch-current.json').read_bytes(),self.entry_before)
        self.assertEqual((self.parent/'CCBStudioLauncher.exe').read_bytes(),b'old launcher')
        self.assertEqual((self.parent/'Launch-CCB-Studio.lnk').read_bytes(),b'old link')
    def test_partial_publication_journal_requires_recovery(self):
        from repair_install import repair_program
        replace=au.os.replace
        def prepare(args,**kw):Path(args[2]).write_bytes(b'new link');return SimpleNamespace(returncode=0)
        def failing(source,target):
            if Path(target)==self.parent/'Launch-CCB-Studio.lnk' or str(source).endswith('.restore'):raise OSError('injected')
            return replace(source,target)
        with patch('apply_update.subprocess.run',side_effect=prepare),patch('apply_update.os.replace',side_effect=failing),self.assertRaises(au.EntrypointRecoveryRequired):repair_program(self.package,self.old)
        self.assertEqual(self.result()['status'],'ENTRYPOINT_RECOVERY_REQUIRED');self.retained()
        self.assertEqual((self.parent/'launch-current.json').read_bytes(),self.entry_before)
