import base64,json,os,subprocess,sys,uuid,unittest
from unittest.mock import patch
from pathlib import Path
from apply_update import ParentProcess,create_launcher,perform,data_destination
from studio_update import UpdateRejected,Installed
ROOT=Path(__file__).resolve().parents[2]

class HandoffTests(unittest.TestCase):
    def setUp(self):
        base=Path(os.environ.get('CCB_SYNTHETIC_TEST_ROOT',ROOT/'.local/windows-rc'))/'handoff-fixtures';base.mkdir(exist_ok=True)
        self.path=base/uuid.uuid4().hex;self.path.mkdir()
    def test_parent_handle_binds_actual_process_image(self):
        parent=ParentProcess(os.getpid(),Path(sys.executable))
        try:self.assertFalse(parent.exited())
        finally:parent.close()
        with self.assertRaises(UpdateRejected):ParentProcess(os.getpid(),self.path/'not-this-process.exe')
    def test_unicode_shortcut_keeps_explicit_roots_without_execution(self):
        final=self.path/'programs/2-test';final.mkdir(parents=True);(final/'CCBStudioSuitePreview.exe').write_bytes(b'not an executable')
        data=self.path/'合成 资料 %test%';runtime=self.path/'运行时';data.mkdir();runtime.mkdir()
        link=create_launcher(final,data,runtime)
        command="[Console]::OutputEncoding=[Text.UTF8Encoding]::new();$s=New-Object -ComObject WScript.Shell;$l=$s.CreateShortcut('"+str(link).replace("'","''")+"');@{target=$l.TargetPath;args=$l.Arguments}|ConvertTo-Json -Compress"
        result=subprocess.run(['powershell.exe','-NoProfile','-NonInteractive','-EncodedCommand',base64.b64encode(command.encode('utf-16-le')).decode()],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW,check=True)
        value=json.loads(result.stdout.decode('utf-8-sig'))
        self.assertEqual(Path(value['target']),final/'CCBStudioSuitePreview.exe');self.assertEqual(value['args'],subprocess.list2cmdline(['--studio-root',str(data),'--runtime-root',str(runtime)]))
    def test_terminal_handoff_replay_does_not_reinstall_or_replace_receipt(self):
        cache=self.path/'cache';folder=cache/'handoffs/test';folder.mkdir(parents=True);data=self.path/'data';data.mkdir();(data/'.owner.lock').touch()
        plan=dict(format=1,nonce='a'*64,installed=str(self.path/'app'),cache=str(cache),dataRoot=str(data),runtimeRoot=str(self.path/'runtime'))
        path=folder/'plan.json';path.write_text(json.dumps(plan));receipt=folder/'result.json'
        receipt.write_text(json.dumps(dict(status='COMPLETE',nonce=plan['nonce'])));before=receipt.read_bytes()
        self.assertEqual(perform(path),0);self.assertEqual(receipt.read_bytes(),before)
        receipt.write_text(json.dumps(dict(status='INSTALLING',nonce=plan['nonce'])))
        self.assertEqual(perform(path),1);self.assertEqual(json.loads(receipt.read_text())['status'],'FAILED_RETAINED')

    def test_independent_root_stable_launcher_advances_twice_without_changing_data(self):
        independent=self.path/'independent-preview';independent.mkdir()
        data=independent/'data/workspace';runtime=independent/'data/runtime'
        data.mkdir(parents=True);runtime.mkdir(parents=True)
        (data/'retained.bin').write_bytes(b'synthetic workspace retained across updates')
        (runtime/'settings.json').write_text('{"synthetic":true}')
        baseline={p:p.read_bytes() for p in (data/'retained.bin',runtime/'settings.json')}
        stable=None
        for version in (105,106,107):
            final=independent/f'{version}-synthetic';final.mkdir()
            (final/'CCBStudioSuitePreview.exe').write_bytes(b'not executable; shortcut only')
            link=create_launcher(final,data,runtime)
            if stable is not None:self.assertEqual(link,stable)
            stable=link
            self.assertEqual(link,independent/'Launch-CCB-Studio.lnk')
            command="[Console]::OutputEncoding=[Text.UTF8Encoding]::new();$s=New-Object -ComObject WScript.Shell;$l=$s.CreateShortcut('"+str(link).replace("'","''")+"');@{target=$l.TargetPath;args=$l.Arguments}|ConvertTo-Json -Compress"
            result=subprocess.run(['powershell.exe','-NoProfile','-NonInteractive','-EncodedCommand',base64.b64encode(command.encode('utf-16-le')).decode()],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW,check=True)
            value=json.loads(result.stdout.decode('utf-8-sig'))
            self.assertEqual(Path(value['target']),final/'CCBStudioSuitePreview.exe')
            self.assertEqual(value['args'],subprocess.list2cmdline(['--studio-root',str(data),'--runtime-root',str(runtime)]))
            self.assertEqual(baseline,{p:p.read_bytes() for p in baseline})

    def test_migration_destination_is_fresh_sibling_and_bound_to_confirmed_versions(self):
        source=self.path/'data';source.mkdir();identity=Installed(1,2,'internal','','',b'')
        nonce='a'*64;target=source.with_name('data-format3-'+nonce[:12])
        plan=dict(nonce=nonce,sourceDataVersion=2,targetDataVersion=3,destinationDataRoot=str(target))
        self.assertEqual(data_destination(plan,source,identity,{'data_version':3}),target)
        for key,value in [('sourceDataVersion',1),('targetDataVersion',4),('destinationDataRoot',str(source)),('destinationDataRoot',str(source/'nested'))]:
            changed=dict(plan);changed[key]=value
            with self.assertRaises(UpdateRejected):data_destination(changed,source,identity,{'data_version':3})
        target.mkdir()
        with self.assertRaises(UpdateRejected):data_destination(plan,source,identity,{'data_version':3})
        self.assertEqual(data_destination({'nonce':nonce},source,identity,{}),source)

    def test_locked_launcher_rejects_delayed_older_or_foreign_root_before_any_write(self):
        root=self.path/'launcher-cas';root.mkdir();data=self.path/'data';runtime=self.path/'runtime'
        data.mkdir();runtime.mkdir()
        final=root/'2-test';(final/'updater').mkdir(parents=True)
        (final/'updater/CCBStudioLauncher.exe').write_bytes(b'unexecuted synthetic launcher')
        (final/'CCBStudioSuitePreview.exe').write_bytes(b'unexecuted synthetic program')
        (final/'update-config.json').write_text('{"version":2}')
        stable=root/'CCBStudioLauncher.exe';link=root/'Launch-CCB-Studio.lnk';descriptor=root/'launch-current.json'
        stable.write_bytes(b'original launcher');link.write_bytes(b'original shortcut')
        def value(version,installed):return dict(format=1,version=version,installed=str(installed),data=str(data),runtime=str(runtime))
        for current,target_data,target_runtime,options in [
            (value(3,root/'3-test'),data,runtime,{}),
            (value(2,final),self.path/'restored',runtime,{}),
            (value(2,final),data,self.path/'other-runtime',{}),
            (value(3,root/'3-test'),data,runtime,dict(expected_current=value(1,root/'1-test'))),
            (value(2,final),data,runtime,dict(expected_absent=True)),
        ]:
            descriptor.write_text(json.dumps(current));before={p:p.read_bytes() for p in (stable,link,descriptor)}
            with patch('apply_update.subprocess.run') as execute:
                with self.assertRaises(UpdateRejected):create_launcher(final,target_data,target_runtime,**options)
                execute.assert_not_called()
            self.assertEqual(before,{p:p.read_bytes() for p in before})

    def test_locked_launcher_accepts_newer_same_profile_and_only_explicit_migration(self):
        root=self.path/'launcher-upgrade';root.mkdir();data=self.path/'data';runtime=self.path/'runtime';data.mkdir();runtime.mkdir()
        first=dict(format=1,version=1,installed=str(root/'1-test'),data=str(data),runtime=str(runtime))
        descriptor=root/'launch-current.json';descriptor.write_text(json.dumps(first))
        for version,destination,migration in [(2,data,None),(3,self.path/'data-format3-confirmed',data)]:
            final=root/f'{version}-test';(final/'updater').mkdir(parents=True)
            (final/'updater/CCBStudioLauncher.exe').write_bytes(b'unexecuted synthetic launcher')
            (final/'CCBStudioSuitePreview.exe').write_bytes(b'unexecuted synthetic program')
            (final/'update-config.json').write_text(json.dumps(dict(version=version)))
            expected=json.loads(descriptor.read_text())
            with patch('apply_update.subprocess.run',return_value=subprocess.CompletedProcess([],0)):
                create_launcher(final,destination,runtime,expected_current=expected,migration_source=migration)
            after=json.loads(descriptor.read_text());self.assertEqual(after['version'],version);self.assertEqual(Path(after['data']),destination)
