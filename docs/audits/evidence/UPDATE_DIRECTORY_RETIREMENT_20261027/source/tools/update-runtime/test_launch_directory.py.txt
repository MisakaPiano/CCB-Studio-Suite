"""Scoped Windows directory-retirement regressions; fresh synthetic roots only."""
import ctypes, json, os, subprocess, sys, time, unittest
from pathlib import Path
from unittest.mock import patch
from test_residue_cleanup import ResidueCleanupTests
from owned_files import prune_empty


class DirectoryRetirementTests(unittest.TestCase):
    setUp=ResidueCleanupTests.setUp
    program=ResidueCleanupTests.program
    cleanup=ResidueCleanupTests.cleanup

    def test_empty_cwd_is_deferred_then_retried_without_changing_complete(self):
        before=(self.folder/'result.json').read_bytes()
        # A real Windows process pins its cwd; it exits normally on stdin EOF.
        child=subprocess.Popen([sys.executable,'-B','-c','import sys;print("READY",flush=True);sys.stdin.read()'],
            cwd=self.old,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,
            creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            self.assertEqual(child.stdout.readline().strip(),b'READY')
            result=self.cleanup()
            self.assertEqual(list(self.old.iterdir()),[])
            self.assertEqual(result['status'],'DEFERRED')
            self.assertGreater(result['deferred'],0)
            self.assertEqual((self.folder/'result.json').read_bytes(),before)
        finally:
            child.stdin.close();child.wait(timeout=15);child.stdout.close();child.stderr.close()
        self.assertEqual(self.cleanup()['status'],'COMPLETE');self.assertFalse(self.old.exists())
        self.assertEqual(self.cleanup()['removed'],0)

    def test_both_update_children_choose_new_program_directory(self):
        import ast
        source=Path(__file__).with_name('apply_update.py').read_text(encoding='utf-8')
        calls=[n for n in ast.walk(ast.parse(source)) if isinstance(n,ast.Call)
               and isinstance(n.func,ast.Attribute) and n.func.attr=='Popen']
        self.assertEqual(len(calls),2)
        for call in calls:
            self.assertTrue(any(k.arg=='cwd' and isinstance(k.value,ast.Name) and k.value.id=='final' for k in call.keywords))

    def test_unknown_nonempty_directory_retained_without_endless_retry(self):
        note=self.old/'user-note.txt';note.write_bytes(b'unknown original stays')
        result=self.cleanup();self.assertEqual(result['status'],'COMPLETE')
        self.assertEqual(note.read_bytes(),b'unknown original stays')
        self.assertFalse((self.old/'app').exists())
        self.assertEqual(self.cleanup()['removed'],0)

    def test_empty_directory_access_denied_is_reported_and_can_retry(self):
        import owned_files
        original=owned_files.dispose
        def deny_directory(kernel,handle):
            info=owned_files.FileInfo()
            self.assertTrue(kernel.GetFileInformationByHandle(handle,ctypes.byref(info)))
            if info.attributes&0x10:raise ctypes.WinError(5)
            return original(kernel,handle)
        with patch('owned_files.dispose',side_effect=deny_directory):
            self.assertEqual(self.cleanup()['status'],'DEFERRED')
        self.assertEqual(self.cleanup()['status'],'COMPLETE');self.assertFalse(self.old.exists())
        self.assertEqual((self.data/'original.bin').read_bytes(),b'unique synthetic data')


if __name__=='__main__':unittest.main(verbosity=2)
