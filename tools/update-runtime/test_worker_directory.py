"""Old-helper inherited cwd compatibility for the packaged cleanup worker."""
import os,subprocess,sys,unittest
from unittest.mock import patch
from pathlib import Path
import residue_cleanup
from studio_update import UpdateRejected
from test_residue_cleanup import ResidueCleanupTests


class WorkerDirectoryTests(unittest.TestCase):
    setUp=ResidueCleanupTests.setUp
    program=ResidueCleanupTests.program

    def test_packaged_worker_releases_inherited_empty_root_while_alive(self):
        old=self.base/'inherited';old.mkdir();(self.current/'updater/lib').mkdir(parents=True)
        code=('import sys,pathlib;sys.path.insert(0,sys.argv[1]);import residue_cleanup as r;'
              'r.__file__=str(pathlib.Path(sys.argv[2])/"updater/lib/residue_cleanup.py");'
              'sys.executable=str(pathlib.Path(sys.argv[2])/"updater/pythonw.exe");'
              'r.anchor_packaged_worker(pathlib.Path(sys.argv[2]));'
              'print(str(pathlib.Path.cwd()),flush=True);sys.stdin.read()')
        child=subprocess.Popen([sys.executable,'-B','-c',code,str(Path(__file__).parent),str(self.current)],
            cwd=old,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            self.assertEqual(Path(child.stdout.readline().decode().strip()),self.current)
            old.rmdir();self.assertFalse(old.exists());self.assertIsNone(child.poll())
        finally:
            child.stdin.close();self.assertEqual(child.wait(timeout=15),0);child.stdout.close();child.stderr.close()

    def test_source_or_foreign_interpreter_does_not_reanchor(self):
        (self.current/'updater/lib').mkdir(parents=True)
        for script,interpreter in [(str(Path(__file__).with_name('residue_cleanup.py')),str(self.current/'updater/pythonw.exe')),
                                  (str(self.current/'updater/lib/residue_cleanup.py'),sys.executable)]:
            with self.subTest(script=script),patch.object(residue_cleanup,'__file__',script),patch.object(sys,'executable',interpreter),patch('residue_cleanup.os.chdir') as chdir:
                residue_cleanup.anchor_packaged_worker(self.current);chdir.assert_not_called()

    def test_junction_package_refused_without_changing_directory(self):
        junction=self.base/'alias'
        result=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(junction),str(self.current)],capture_output=True)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        try:
            with patch('residue_cleanup.os.chdir') as chdir:
                with self.assertRaises(UpdateRejected):residue_cleanup.anchor_packaged_worker(junction)
                chdir.assert_not_called()
        finally:os.rmdir(junction) # exact new synthetic junction, never recursive


if __name__=='__main__':unittest.main(verbosity=2)
