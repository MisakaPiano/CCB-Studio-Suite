import tempfile, unittest
from pathlib import Path
from cryptography.exceptions import InvalidTag
from signing_custody import generate, validate, seal, unseal, persist_local, local_signer, ROLES

class CustodyTests(unittest.TestCase):
    def test_backup_recovers_all_roles_and_rejects_wrong_password_and_tampering(self):
        bundle = generate()
        encoded = seal(bundle, 'synthetic-password-only')
        self.assertNotIn(b'PRIVATE KEY', encoded)
        self.assertEqual(validate(bundle), validate(unseal(encoded, 'synthetic-password-only')))
        with self.assertRaises(InvalidTag): unseal(encoded, 'wrong-password')
        with self.assertRaises(InvalidTag): unseal(encoded[:-1]+bytes([encoded[-1]^1]), 'synthetic-password-only')
        with self.assertRaises(ValueError): seal(bundle, 'short')
        bundle['private']['timestamp'] = bundle['private']['root']
        with self.assertRaises(ValueError): validate(bundle)

    def test_windows_protection_and_independent_restore(self):
        bundle = generate()
        with tempfile.TemporaryDirectory() as temp:
            source, restored = Path(temp)/'source', Path(temp)/'restored'
            persist_local(bundle, source)
            persist_local(unseal(seal(bundle, 'synthetic-password-only'), 'synthetic-password-only'), restored)
            for role in ROLES:
                self.assertNotIn(b'PRIVATE KEY', (source/(role+'.dpapi')).read_bytes())
                self.assertEqual(local_signer(source, role).public_key, local_signer(restored, role).public_key)
            with self.assertRaises(FileExistsError): persist_local(bundle, source)

if __name__ == '__main__': unittest.main()
