import tempfile, unittest, subprocess, sys, json
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

    def test_recovery_cli_in_new_process_password_code_and_existing_directory(self):
        # Synthetic material only: exercise the actual CLI without parent DPAPI files.
        bundle=generate()
        script=Path(__file__).with_name('restore_signing_backup.py')
        with tempfile.TemporaryDirectory() as temp:
            folder=Path(temp);backup=folder/'backup.enc'
            secret='SYNTHETICRECOVERYCODEONLY23456789'
            backup.write_bytes(seal(bundle,secret))
            def run(destination,value,code=False):
                # Windows getpass reads the console, not redirected stdin. Replace only
                # the prompt boundary in this child; execute the unmodified CLI body.
                entry="import sys,runpy,getpass;getpass.getpass=lambda prompt:sys.stdin.readline().rstrip('\\n');sys.argv=sys.argv[1:];runpy.run_path(sys.argv[0],run_name='__main__')"
                args=[sys.executable,'-B','-c',entry,str(script),'--backup',str(backup),'--destination',str(destination)]
                if code:args+=['--recovery-code']
                return subprocess.run(args,input=value+'\n',capture_output=True,text=True,timeout=45)
            wrong=folder/'wrong'
            self.assertNotEqual(run(wrong,'wrong-password').returncode,0)
            self.assertFalse(wrong.exists())
            restored=folder/'password'
            self.assertEqual(run(restored,secret).returncode,0)
            by_code=folder/'code'
            self.assertEqual(run(by_code,'synthetic-recovery-code-only23456789',True).returncode,0)
            for destination in (restored,by_code):
                receipt=json.loads((destination/'RESTORED_PUBLIC_RECEIPT.json').read_text())
                self.assertEqual(receipt['keyIds'],validate(bundle))
                for role in ROLES:
                    self.assertEqual(local_signer(destination,role).public_key.keyid,validate(bundle)[role])
            before={p.name:p.read_bytes() for p in restored.iterdir()}
            self.assertNotEqual(run(restored,secret).returncode,0)
            self.assertEqual(before,{p.name:p.read_bytes() for p in restored.iterdir()})

if __name__ == '__main__': unittest.main()
