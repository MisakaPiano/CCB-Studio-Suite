"""Online-only maintenance. Cannot sign root or targets, or select a new package."""
import argparse, base64, json, os
from pathlib import Path
from cryptography.hazmat.primitives.serialization import load_pem_private_key
from securesystemslib.signer import CryptoSigner
from release_repository import current, prepare
from signing_custody import local_signer

def renew(store, bootstrap, repository, signers, dry_run=False):
    if set(signers) != {'snapshot', 'timestamp'}:
        raise ValueError('Online maintenance accepts exactly snapshot and timestamp signers')
    before, files = current(store)
    if not before: raise ValueError('Reviewed initial target publication required first')
    result = prepare(store, bootstrap, repository, 'preview', signers, dry_run=dry_run)
    if not dry_run:
        after, renewed = current(store)
        assert renewed['targets.json'] == files['targets.json']
        assert after['releaseVersion'] == before['releaseVersion']
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--store', type=Path, required=True)
    parser.add_argument('--bootstrap', type=Path, required=True)
    parser.add_argument('--repository', required=True)
    parser.add_argument('--local-keys', type=Path)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    if args.local_keys:
        signers = {r: local_signer(args.local_keys, r) for r in ('snapshot', 'timestamp')}
    else:
        signers = {}
        for role in ('snapshot', 'timestamp'):
            # GitHub Secrets inject only these maintenance keys. Never log values.
            encoded = os.environ.pop('SUITE_'+role.upper()+'_KEY_B64')
            signers[role] = CryptoSigner(load_pem_private_key(base64.b64decode(encoded, validate=True), None))
    print(json.dumps(renew(args.store, args.bootstrap.read_bytes(), args.repository, signers, args.dry_run), indent=2))

if __name__ == '__main__':
    try: main()
    except Exception as error:
        print(json.dumps({'status': 'FAILED_RETAINED', 'errorType': type(error).__name__}))
        raise SystemExit(1)
