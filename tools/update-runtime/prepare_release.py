"""Operator CLI: encrypted signing inputs -> local reviewed public generation.

Does not create keys, upload assets, change Git refs or publish metadata.
Passwords are read from the console, never from command-line arguments.
"""
import argparse
import getpass
import json
from pathlib import Path
from cryptography.hazmat.primitives.serialization import load_pem_private_key
from securesystemslib.signer import CryptoSigner
from release_repository import prepare,target_from_build


def signer_from_encrypted_pem(data,password):
    if not data.startswith(b'-----BEGIN ENCRYPTED PRIVATE KEY-----'):
        raise ValueError('Only encrypted PKCS8 signing inputs are accepted')
    return CryptoSigner(load_pem_private_key(data,password=password))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('store','bootstrap','key-directory'):parser.add_argument('--'+name,required=True,type=Path)
    parser.add_argument('--repository',required=True)
    parser.add_argument('--channel',choices=['preview'],default='preview')
    parser.add_argument('--build',type=Path)
    parser.add_argument('--notes-file',type=Path)
    parser.add_argument('--tag');parser.add_argument('--asset')
    parser.add_argument('--rotated-root',type=Path)
    parser.add_argument('--dry-run',action='store_true')
    args=parser.parse_args()
    if args.build and not all((args.notes_file,args.tag,args.asset)):
        parser.error('New release requires notes-file, tag and asset')
    if not args.build and any((args.notes_file,args.tag,args.asset)):
        parser.error('Renewal must not change release identity')
    target=target_from_build(args.build,args.notes_file.read_text(encoding='utf-8'),args.tag,args.asset) if args.build else None
    roles=['snapshot','timestamp']+(['targets'] if target else [])
    password=getpass.getpass('Signing input password: ').encode('utf-8')
    signers={role:signer_from_encrypted_pem((args.key_directory/(role+'.pem')).read_bytes(),password) for role in roles}
    del password
    result=prepare(args.store,args.bootstrap.read_bytes(),args.repository,args.channel,signers,target=target,
                   dry_run=args.dry_run,rotated_root=args.rotated_root.read_bytes() if args.rotated_root else None)
    print(json.dumps(result,indent=2))


if __name__=='__main__':main()
