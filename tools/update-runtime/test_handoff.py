import base64,json,os,subprocess,sys,tempfile,unittest
from pathlib import Path
from apply_update import ParentProcess,create_launcher,perform
from studio_update import UpdateRejected
ROOT=Path(__file__).resolve().parents[2]

class HandoffTests(unittest.TestCase):
    def setUp(self):
        base=ROOT/'.local/windows-rc/handoff-fixtures';base.mkdir(exist_ok=True)
        self.path=Path(tempfile.mkdtemp(dir=base))
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
