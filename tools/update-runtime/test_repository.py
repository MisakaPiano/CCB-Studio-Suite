from datetime import datetime, timedelta, timezone
import unittest
from tuf.api.metadata import Metadata, TargetFile, Root
from securesystemslib.signer import CryptoSigner
from studio_update import execute, exclusive, UpdateRejected
from release_repository import prepare,current
from test_github import GitHubTests
from prepare_release import signer_from_encrypted_pem
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding,PrivateFormat,BestAvailableEncryption,NoEncryption


class RepositoryTests(unittest.TestCase):
    def test_encrypted_signing_input_and_password_failure(self):
        key=Ed25519PrivateKey.generate() # Synthetic; private bytes never written to disk.
        encoded=key.private_bytes(Encoding.PEM,PrivateFormat.PKCS8,BestAvailableEncryption(b'synthetic-test-password'))
        signer=signer_from_encrypted_pem(encoded,b'synthetic-test-password')
        self.assertTrue(signer.sign(b'synthetic').signature)
        with self.assertRaises(ValueError):signer_from_encrypted_pem(encoded,b'wrong')
        with self.assertRaises(ValueError):signer_from_encrypted_pem(key.private_bytes(Encoding.PEM,PrivateFormat.PKCS8,NoEncryption()),b'')

    def setUp(self):
        self.fixture=GitHubTests();self.fixture.setUp()
        self.store=self.fixture.path/'publisher'
        self.root=self.fixture.identity.bootstrap
        self.signers=self.fixture.fixture.repo.signers
        self.target=Metadata.from_bytes(self.fixture.fixture.repo.files['metadata/targets.json']).signed.targets['studio-installer.exe']
        self.target.unrecognized_fields['custom']['maximum_reader']=3

    def prepare(self,**kwargs):
        return prepare(self.store,self.root,self.fixture.repository,'preview',kwargs.pop('signers',self.signers),**kwargs)

    def serve(self):
        _,files=current(self.store)
        for name,data in files.items():self.fixture.pool.routes[self.fixture.identity.metadata_url+name]=(data,200,None)

    def test_durable_two_versions_and_online_renewal_without_offline_signer(self):
        first=self.prepare(target=self.target);self.serve()
        self.assertEqual(execute(self.fixture.identity,self.fixture.path/'client',True,self.fixture.fetcher())['version'],2)
        self.target.unrecognized_fields['custom']['version']=3
        second=self.prepare(target=self.target);self.serve()
        self.assertEqual(execute(self.fixture.identity,self.fixture.path/'client',True,self.fixture.fetcher())['version'],3)
        self.assertNotEqual(first['generation'],second['generation'])
        _,before=current(self.store)
        renewed=self.prepare(signers={k:self.signers[k] for k in ('snapshot','timestamp')});self.serve()
        _,after=current(self.store)
        self.assertEqual(before['targets.json'],after['targets.json'])
        self.assertGreater(renewed['timestampVersion'],second['timestampVersion'])
        self.assertEqual(execute(self.fixture.identity,self.fixture.path/'client',fetcher=self.fixture.fetcher())['status'],'AVAILABLE')

    def test_expired_timestamp_recovers_after_renewal(self):
        self.prepare(target=self.target,now=datetime.now(timezone.utc)-timedelta(days=8));self.serve()
        with self.assertRaises(Exception):execute(self.fixture.identity,self.fixture.path/'client',fetcher=self.fixture.fetcher())
        self.prepare(signers={k:self.signers[k] for k in ('snapshot','timestamp')});self.serve()
        self.assertEqual(execute(self.fixture.identity,self.fixture.path/'client',fetcher=self.fixture.fetcher())['status'],'AVAILABLE')

    def test_root_rotation_needs_both_signatures_and_preserves_chain(self):
        self.prepare(target=self.target)
        new=CryptoSigner.generate_ed25519()
        rotated=Metadata(Root(version=2,expires=datetime.now(timezone.utc)+timedelta(days=365),consistent_snapshot=False))
        for role,signer in self.signers.items():rotated.signed.add_key(new.public_key if role=='root' else signer.public_key,role)
        rotated.sign(self.signers['root'])
        with self.assertRaises(Exception):self.prepare(rotated_root=rotated.to_bytes())
        self.assertEqual(current(self.store)[0]['rootVersion'],1)
        rotated.sign(new,append=True)
        self.prepare(rotated_root=rotated.to_bytes());self.serve()
        execute(self.fixture.identity,self.fixture.path/'client',fetcher=self.fixture.fetcher())
        root=Metadata.from_file(str(self.fixture.path/'client/metadata/root.json'))
        self.assertEqual(root.signed.version,2)

    def test_dry_run_and_failed_signing_leave_committed_state_unchanged(self):
        self.prepare(target=self.target)
        before=(self.store/'CURRENT.json').read_bytes()
        self.prepare(dry_run=True)
        self.assertEqual(before,(self.store/'CURRENT.json').read_bytes())
        bad=dict(self.signers,timestamp=CryptoSigner.generate_ed25519())
        with self.assertRaises(Exception):self.prepare(signers=bad)
        self.assertEqual(before,(self.store/'CURRENT.json').read_bytes())
        with exclusive(self.store):
            with self.assertRaises(UpdateRejected):self.prepare()

    def test_reused_release_and_tampered_generation_rejected(self):
        self.prepare(target=self.target)
        self.target.unrecognized_fields['custom']['sourceSha']='b'*40
        with self.assertRaises(ValueError):self.prepare(target=self.target)
        receipt,_=current(self.store)
        path=self.store/'generations'/receipt['generation']/'targets.json'
        path.write_bytes(b'corrupted')
        with self.assertRaises(ValueError):self.prepare()

    def test_offline_target_renewal_required_before_expiry(self):
        self.prepare(target=self.target,now=datetime.now(timezone.utc)-timedelta(days=84))
        with self.assertRaisesRegex(ValueError,'Offline target renewal'):self.prepare()
        self.prepare(target=self.target);self.serve()
        self.assertEqual(execute(self.fixture.identity,self.fixture.path/'client',fetcher=self.fixture.fetcher())['status'],'AVAILABLE')
