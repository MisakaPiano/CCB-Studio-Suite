"""Generate a separate recovery-code encrypted backup; display only in local console."""
import argparse, base64, getpass, hashlib, json, os, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from signing_custody import ROLES, dpapi, seal, unseal, validate, persist_local

def normalize(value):
    return value.replace('-', '').replace(' ', '').strip().upper()

def main():
    p=argparse.ArgumentParser();p.add_argument('--keys',type=Path,required=True);args=p.parse_args()
    folder=args.keys.resolve()
    backup=folder/'CCB-Suite-recovery-code-backup-v1.enc'
    code_file=folder/'recovery-code.dpapi'
    if backup.exists() or code_file.exists():
        if not backup.exists() or not code_file.exists(): raise ValueError('Incomplete recovery setup retained')
        code=dpapi(code_file.read_bytes(),True).decode()
        bundle=unseal(backup.read_bytes(),code)
    else:
        bundle={'format':1,'root':(folder/'root.json').read_text(encoding='utf-8'),
                'private':{r:dpapi((folder/(r+'.dpapi')).read_bytes(),True).decode() for r in ROLES}}
        validate(bundle)
        code=base64.b32encode(os.urandom(32)).decode().rstrip('=')
        encrypted=seal(bundle,code)
        assert validate(unseal(encrypted,code))==validate(bundle)
        with code_file.open('xb') as f:f.write(dpapi(code.encode()))
        with backup.open('xb') as f:f.write(encrypted)
    print('\nRECOVERY CODE - keep separately from Google Drive and backup password.\nDo not send screenshots of this window to the chat.\n')
    print('-'.join(code[i:i+4] for i in range(0,len(code),4)))
    print('\nSave or print this code. It can restore signing keys without the backup password.')
    while normalize(getpass.getpass('Re-enter saved recovery code to confirm (hidden): '))!=code:
        print('Code does not match. Please try again.')
    check=folder/'recovery-code-restore-check'
    if not check.exists():persist_local(unseal(backup.read_bytes(),code),check)
    receipt={'status':'RECOVERY_CODE_SAVED_AND_RESTORE_VERIFIED','backupSha256':hashlib.sha256(backup.read_bytes()).hexdigest(),'keyIds':validate(bundle)}
    (folder/'RECOVERY_PUBLIC_RECEIPT.json').write_text(json.dumps(receipt,indent=2))
    print('Recovery verified. Code is not stored in plaintext. Close this window when saved.')

if __name__=='__main__':
    try:main()
    except Exception as e:
        print('Stopped; existing files retained. Error type: '+type(e).__name__)
        raise SystemExit(1)
