import hashlib, json, tempfile, unittest
from datetime import datetime,timedelta,timezone
from pathlib import Path
from tuf.api.metadata import Metadata,Targets,Snapshot,Timestamp,MetaFile
from cryptography.hazmat.primitives.serialization import load_pem_private_key
from securesystemslib.signer import CryptoSigner
from signing_custody import generate
from maintain_metadata import maintain

class MaintenanceTests(unittest.TestCase):
    def test_expired_online_metadata_renews_empty_channel_and_rejects_bad_trust(self):
        b=generate();signers={r:CryptoSigner(load_pem_private_key(k.encode(),None)) for r,k in b['private'].items()}
        now=datetime.now(timezone.utc)
        t=Metadata(Targets(expires=now+timedelta(days=90)));t.sign(signers['targets'])
        s=Metadata(Snapshot(expires=now-timedelta(days=1)));s.signed.meta['targets.json']=MetaFile.from_data(1,t.to_bytes(),['sha256']);s.sign(signers['snapshot'])
        ts=Metadata(Timestamp(expires=now-timedelta(days=1)));ts.signed.snapshot_meta=MetaFile.from_data(1,s.to_bytes(),['sha256']);ts.sign(signers['timestamp'])
        with tempfile.TemporaryDirectory() as d:
            f=Path(d);(f/'1.root.json').write_text(b['root']);(f/'targets.json').write_bytes(t.to_bytes());(f/'snapshot.json').write_bytes(s.to_bytes());(f/'timestamp.json').write_bytes(ts.to_bytes())
            pin=hashlib.sha256(b['root'].encode()).hexdigest();online={r:signers[r] for r in ('snapshot','timestamp')}
            with self.assertRaises(ValueError):maintain(f,'0'*64,online)
            with self.assertRaises(ValueError):maintain(f,pin,signers)
            result=maintain(f,pin,online)
            self.assertEqual(result['timestampVersion'],2);self.assertEqual((f/'targets.json').read_bytes(),t.to_bytes())
            before=(f/'timestamp.json').read_bytes()
            with self.assertRaises(ValueError):maintain(f,pin,online,now+timedelta(days=84))
            self.assertEqual(before,(f/'timestamp.json').read_bytes())
