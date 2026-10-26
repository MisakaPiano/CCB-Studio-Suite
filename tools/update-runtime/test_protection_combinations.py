"""Packaged updater protection combinations; fresh synthetic Windows fixtures only.

Run explicitly with --lib pointing at an isolated installed updater, --out at a
new directory below this Suite workspace. No discovery of real installations,
credentials or historical test roots. Fixtures and raw logs remain for review.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import unittest
from unittest.mock import patch


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


class ProtectionCombinations(unittest.TestCase):
    def setUp(self):
        self.base = OUT / self._testMethodName
        self.programs = self.base / 'programs'
        self.programs.mkdir(parents=True)
        self.data = self.base / 'data'
        self.data.mkdir()
        (self.data / 'original.bin').write_bytes(b'synthetic original; never user data')
        self.runtime = self.base / 'runtime'
        self.cache = self.runtime / 'updates'
        self.cache.mkdir(parents=True)
        self.backup = self.base / 'backup'
        self.backup.mkdir()
        (self.backup / 'keep.bin').write_bytes(b'synthetic backup')
        self.digest = hashlib.sha256(b'synthetic installer identity').hexdigest()
        self.program = {}
        self.identities = {}
        self.receipts = {}
        for version, letter in ((111, 'a'), (115, 'b'), (116, 'c')):
            expected = dict(product='ccb-studio-suite', platform='windows-x86_64',
                            channel='internal', version=version, sourceSha=letter * 40)
            root = self.programs / (str(version) + '-' + letter * 12)
            (root / 'app').mkdir(parents=True)
            entries = []
            for name in ('CCBStudioSuitePreview.exe', 'app/main.jar', 'retired.dll'):
                target = root / name
                target.write_bytes((str(version) + ':' + name).encode('ascii'))
                entries.append(dict(path=name, bytes=target.stat().st_size, sha256=sha(target)))
            atomic_json(root / 'INSTALL_MANIFEST.json', dict(format=1, **expected, files=entries))
            (root / 'INSTALL_COMPLETE').write_text(str(version) + ':' + letter * 40, encoding='ascii')
            (root / 'Uninstall.exe').write_bytes(b'synthetic uninstaller')
            self.receipts[version] = enroll_program(
                self.cache, self.programs, root, expected, self.digest,
                self.cache / ('install-' + str(version) + '.json'))
            self.program[version] = root
            self.identities[version] = expected
        self.protection = self.programs / 'ACTIVE_INSTALLATION.json'
        atomic_json(self.protection, dict(doNotCleanReferencedDirectories=True,
                    program=str(self.program[111]), workspace=str(self.data),
                    runtime=str(self.runtime), retainedBackupWorkspace=str(self.backup)))
        # Protect 111 even though it also has valid ownership and completion proofs.
        self.complete(111, 115, 'd' * 64)
        self.result_path = self.complete(115, 116, 'e' * 64)
        atomic_json(self.programs / 'launch-current.json', dict(
            installed=str(self.program[116]), data=str(self.data), runtime=str(self.runtime)))
        (self.program[115] / 'unknown-note.txt').write_bytes(b'unknown retained')
        self.before = self.protected_hashes()
        self.steps = []

    def complete(self, old, new, nonce):
        folder = self.cache / 'handoffs' / str(new)
        folder.mkdir(parents=True)
        journal = self.cache / ('install-' + str(new) + '.json')
        atomic_json(journal, dict(status='INSTALLED_NOT_STARTED', expected=self.identities[new],
                    target=str(self.program[new]), verification=verify_install(self.program[new], self.identities[new])))
        atomic_json(folder / 'plan.json', dict(format=1, nonce=nonce,
                    installed=str(self.program[old]), cache=str(self.cache),
                    approved=dict(**self.identities[new], sha256=self.digest)))
        result = folder / 'result.json'
        atomic_json(result, dict(status='COMPLETE', nonce=nonce, installed=str(self.program[new]),
                    installJournal=str(journal), verification='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH'))
        return result

    def protected_hashes(self):
        files = [self.protection, self.data / 'original.bin', self.backup / 'keep.bin']
        files += [p for p in self.program[111].rglob('*') if p.is_file()]
        files += [p for p in self.program[116].rglob('*') if p.is_file()]
        return {str(p.relative_to(self.base)): sha(p) for p in files}

    def cleanup(self, **kwargs):
        result = run(self.program[116], self.cache, self.data, self.runtime, **kwargs)
        self.steps.append(result)
        return result

    def assert_retained(self):
        self.assertEqual(self.protected_hashes(), self.before)
        self.assertEqual((self.program[115] / 'unknown-note.txt').read_bytes(), b'unknown retained')

    def assert_finished(self, proof):
        self.assertFalse((self.program[115] / 'app/main.jar').exists())
        self.assertEqual(self.result_path.read_bytes(), proof)
        self.assertEqual(self.cleanup()['removed'], 0)
        self.assert_retained()

    def tearDown(self):
        # Preserve all fixtures; only production cleanup touches enrolled members.
        atomic_json(self.base / 'case-observation.json', dict(
            steps=self.steps, fixture=str(self.base), protectedBefore=self.before,
            protectedAfter=self.protected_hashes()))

    def test_protected_111_and_busy_predecessor_then_retry(self):
        proof = self.result_path.read_bytes()
        with immutable_file(self.program[115] / 'CCBStudioSuitePreview.exe'):
            result = self.cleanup()
            self.assertEqual(result['status'], 'DEFERRED')
            self.assertEqual(result['removed'], 0)
            self.assertTrue((self.program[115] / 'app/main.jar').is_file())
            self.assert_retained()
        self.assertGreater(self.cleanup()['removed'], 0)
        self.assert_finished(proof)

    def test_protected_111_permission_denial_interruption_and_replay(self):
        proof = self.result_path.read_bytes()
        # Deterministic denial at the filesystem boundary, no system ACL change.
        with patch.object(cleanup_module, 'delete_matching', side_effect=PermissionError('synthetic denial')):
            result = self.cleanup()
            self.assertEqual(result['status'], 'DEFERRED')
            self.assertEqual(result['removed'], 0)
            self.assert_retained()

        def interrupt(_):
            raise SystemExit('synthetic interruption after first owned removal')

        with self.assertRaises(SystemExit):
            self.cleanup(after_delete=interrupt)
        self.assertEqual(self.result_path.read_bytes(), proof)
        self.assert_retained()
        self.assertGreater(self.cleanup()['removed'], 0)
        self.assert_finished(proof)

    def test_protected_111_malformed_record_traversal_and_native_junction(self):
        old_file = self.program[115] / 'app/main.jar'
        old_sha = sha(old_file)
        protection = self.protection.read_bytes()
        atomic_json(self.protection, dict(program='../outside'))
        with self.assertRaises(ValueError):
            self.cleanup()
        self.protection.write_bytes(protection)
        receipt = self.receipts[115]
        receipt_bytes = receipt.read_bytes()
        value = json.loads(receipt_bytes)
        value['files'][0]['path'] = '../../data/original.bin'
        atomic_json(receipt, value)
        with self.assertRaises(ValueError):
            self.cleanup()
        receipt.write_bytes(receipt_bytes)
        self.assertEqual(sha(old_file), old_sha)
        self.assert_retained()
        outside = self.base / 'outside'
        outside.mkdir()
        (outside / 'main.jar').write_bytes(b'outside synthetic sentinel')
        before = sha(outside / 'main.jar')
        retained = self.program[115] / 'retained-app'
        old_file.parent.rename(retained)
        junction = self.program[115] / 'app'
        # Exact, newly-created fixture paths. No recursive shell operation.
        proc = subprocess.run(['cmd.exe', '/d', '/c', 'mklink', '/J', str(junction), str(outside)], capture_output=True)
        self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
        try:
            with self.assertRaises(ValueError):
                self.cleanup()
            self.assertEqual(sha(outside / 'main.jar'), before)
            self.assertEqual(sha(retained / 'main.jar'), old_sha)
            self.assert_retained()
        finally:
            os.rmdir(junction)  # exact synthetic link only; never recursive
        self.assertEqual(json.loads(self.result_path.read_bytes())['status'], 'COMPLETE')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--lib', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args = parser.parse_args()
    workspace = Path(__file__).resolve().parents[2]
    LIB = args.lib.absolute()
    OUT = args.out.absolute()
    for path in (LIB, OUT):
        if not path.is_relative_to(workspace / '.local'):
            raise ValueError('Only explicitly isolated Suite-local roots are allowed')
        for parent in (path, *path.parents):
            if parent.exists() and (parent.is_symlink() or parent.is_junction()):
                raise ValueError('Reparse ancestor rejected')
    OUT.mkdir()  # refuse reuse and accidental overwrite
    sys.path.insert(0, str(LIB))
    import residue_cleanup as cleanup_module
    from residue_cleanup import enroll_program, run
    from install_ops import atomic_json, immutable_file, verify_install
    inputs = {}
    for name in ('residue_cleanup.py', 'owned_files.py', 'install_ops.py', 'studio_update.py', 'apply_update.py', 'github_transport.py'):
        source = workspace / 'tools/update-runtime' / name
        installed = LIB / name
        if sha(source) != sha(installed):
            raise ValueError('Package/source mismatch: ' + name)
        inputs[name] = dict(path=str(installed), sha256=sha(installed))
    start = time.monotonic()
    result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(ProtectionCombinations))
    atomic_json(OUT / 'RESULT.json', dict(
        status='PASS' if result.wasSuccessful() else 'FAILED', methods=result.testsRun,
        failures=len(result.failures), errors=len(result.errors), skipped=len(result.skipped),
        seconds=time.monotonic() - start, inputs=inputs, testSha256=sha(Path(__file__)),
        scope='Packaged production decisions and native Windows handles/junction; ACL denial and interruption injected. Synthetic records and executable bytes, no native application upgrade.',
        fixtures='RETAINED', realInstallationAccessed=False, privateCredentialsRead=False,
        independentReview='NOT REQUESTED'))
    sys.exit(0 if result.wasSuccessful() else 1)
