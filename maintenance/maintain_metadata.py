"""Renew an existing signed metadata directory, including an empty bootstrap channel.

Root and targets are immutable here. Only online snapshot/timestamp keys are read.
"""
import argparse, base64, hashlib, json, os
from datetime import datetime, timedelta, timezone
from pathlib import Path
from cryptography.hazmat.primitives.serialization import load_pem_private_key
from securesystemslib.signer import CryptoSigner
from tuf.api.metadata import Metadata, Snapshot, Timestamp, MetaFile

def maintain(folder, pinned_hash, signers, now=None):
    if set(signers)!={'snapshot','timestamp'}:raise ValueError('Only maintenance signers allowed')
    root_bytes=(folder/'1.root.json').read_bytes()
    if hashlib.sha256(root_bytes).hexdigest()!=pinned_hash:raise ValueError('Pinned root changed')
    root=Metadata.from_bytes(root_bytes)
    root.signed.verify_delegate('root',root.signed_bytes,root.signatures)
    now=now or datetime.now(timezone.utc)
    if root.signed.is_expired(now):raise ValueError('Root expired; offline renewal required')
    blobs={r:(folder/(r+'.json')).read_bytes() for r in ('targets','snapshot','timestamp')}
    old={r:Metadata.from_bytes(v) for r,v in blobs.items()}
    for role,m in old.items():root.signed.verify_delegate(role,m.signed_bytes,m.signatures)
    old['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(blobs['targets'])
    old['timestamp'].signed.snapshot_meta.verify_length_and_hashes(blobs['snapshot'])
    if old['snapshot'].signed.meta['targets.json'].version!=old['targets'].signed.version:raise ValueError('Targets version mismatch')
    if old['timestamp'].signed.snapshot_meta.version!=old['snapshot'].signed.version:raise ValueError('Snapshot version mismatch')
    # With consistent snapshots clients fetch these versioned objects, not the
    # unversioned working copies. Do not renew a repository they cannot consume.
    if root.signed.consistent_snapshot:
        for role in ('targets','snapshot'):
            versioned=folder/(str(old[role].signed.version)+'.'+role+'.json')
            if versioned.read_bytes()!=blobs[role]:raise ValueError('Versioned metadata differs from signed working copy')
    expires=min(root.signed.expires,old['targets'].signed.expires)
    if expires<=now+timedelta(days=7):raise ValueError('Offline root or targets renewal due')
    snapshot=Metadata(Snapshot(version=old['snapshot'].signed.version+1,expires=min(now+timedelta(days=14),expires)))
    snapshot.signed.meta['targets.json']=MetaFile.from_data(old['targets'].signed.version,blobs['targets'],['sha256'])
    snapshot.sign(signers['snapshot'])
    timestamp=Metadata(Timestamp(version=old['timestamp'].signed.version+1,expires=min(now+timedelta(days=7),snapshot.signed.expires)))
    timestamp.signed.snapshot_meta=MetaFile.from_data(snapshot.signed.version,snapshot.to_bytes(),['sha256'])
    timestamp.sign(signers['timestamp'])
    for role,m in [('snapshot',snapshot),('timestamp',timestamp)]:root.signed.verify_delegate(role,m.signed_bytes,m.signatures)
    # The caller publishes the complete directory in one commit, without force push.
    versioned=folder/(str(snapshot.signed.version)+'.snapshot.json')
    with versioned.open('xb') as f:f.write(snapshot.to_bytes())
    for role,m in [('snapshot',snapshot),('timestamp',timestamp)]:
        temporary=folder/(role+'.json.stage')
        with temporary.open('xb') as f:f.write(m.to_bytes())
        os.replace(temporary,folder/(role+'.json'))
    assert (folder/'targets.json').read_bytes()==blobs['targets']
    return {'status':'RENEWED','snapshotVersion':snapshot.signed.version,'timestampVersion':timestamp.signed.version,'targetsUnchanged':True,'rootUnchanged':True,'targetsExpires':old['targets'].signed.expires.isoformat(),'timestampExpires':timestamp.signed.expires.isoformat()}

def main():
    p=argparse.ArgumentParser();p.add_argument('--metadata',type=Path,required=True);p.add_argument('--root-sha256',required=True);a=p.parse_args()
    signers={r:CryptoSigner(load_pem_private_key(base64.b64decode(os.environ.pop('SUITE_'+r.upper()+'_KEY_B64'),validate=True),None)) for r in ('snapshot','timestamp')}
    print(json.dumps(maintain(a.metadata,a.root_sha256,signers)))

if __name__=='__main__':
    try:main()
    except Exception as e:
        print(json.dumps({'status':'FAILED_NO_PUBLICATION','errorType':type(e).__name__}));raise SystemExit(1)
