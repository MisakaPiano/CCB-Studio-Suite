"""User-approved empty-directory boundary in eligible retired program roots only."""
import ctypes,os,subprocess,unittest
from unittest.mock import patch
from test_residue_cleanup import ResidueCleanupTests
from owned_files import prune_empty


class EmptyProgramDirectoryTests(unittest.TestCase):
    setUp=ResidueCleanupTests.setUp
    program=ResidueCleanupTests.program
    cleanup=ResidueCleanupTests.cleanup

    def test_unlisted_empty_driver_directories_retire_with_program(self):
        (self.old/'NVIDIA Corporation/umdlogs').mkdir(parents=True)
        self.assertEqual(self.cleanup()['status'],'COMPLETE');self.assertFalse(self.old.exists())
        self.assertEqual(self.cleanup()['removed'],0)

    def test_unknown_file_preserves_bytes_and_unknown_structure(self):
        empty=self.old/'unlisted/empty';empty.mkdir(parents=True)
        note=self.old/'manual.txt';note.write_bytes(b'unknown user bytes')
        self.assertEqual(self.cleanup()['status'],'COMPLETE')
        self.assertEqual(note.read_bytes(),b'unknown user bytes');self.assertTrue(empty.is_dir())

    def test_protected_empty_directories_are_not_traversed_or_removed(self):
        for name in ('plugins','backups','source','config','secrets','data'):(self.old/name/'empty').mkdir(parents=True)
        driver=self.old/'NVIDIA Corporation/umdlogs';driver.mkdir(parents=True)
        self.assertEqual(self.cleanup()['status'],'COMPLETE');self.assertFalse(driver.parent.exists())
        for name in ('plugins','backups','source','config','secrets','data'):self.assertTrue((self.old/name/'empty').is_dir())

    def test_unknown_junction_refused_and_outside_unchanged(self):
        outside=self.base/'outside';outside.mkdir();keep=outside/'keep.bin';keep.write_bytes(b'outside retained')
        junction=self.old/'unlisted-junction'
        proc=subprocess.run(['cmd.exe','/d','/c','mklink','/J',str(junction),str(outside)],capture_output=True)
        self.assertEqual(proc.returncode,0,proc.stdout+proc.stderr)
        try:
            with self.assertRaises(ValueError):self.cleanup()
            self.assertEqual(keep.read_bytes(),b'outside retained')
        finally:os.rmdir(junction) # exact newly created synthetic junction only

    def test_file_created_during_directory_disposition_is_retained(self):
        import owned_files
        root=self.base/'raced';root.mkdir();note=root/'new.txt';original=owned_files.dispose
        def appeared(kernel,handle):
            note.write_bytes(b'concurrent unknown file');return original(kernel,handle)
        with patch('owned_files.dispose',side_effect=appeared):
            result=prune_empty(root,[],discover_empty=True)
        self.assertEqual(note.read_bytes(),b'concurrent unknown file');self.assertTrue(root.exists())
        self.assertTrue(any(x['status']=='RETAIN_NONEMPTY' for x in result))

    def test_directory_bound_defers_without_walking_indefinitely(self):
        (self.old/'one/two/three').mkdir(parents=True)
        with patch('owned_files.EMPTY_DIRECTORY_LIMIT',2):self.assertEqual(self.cleanup()['status'],'DEFERRED')
        self.assertTrue((self.old/'one').exists())
        self.assertEqual(self.cleanup()['status'],'COMPLETE');self.assertFalse(self.old.exists())


if __name__=='__main__':unittest.main(verbosity=2)
