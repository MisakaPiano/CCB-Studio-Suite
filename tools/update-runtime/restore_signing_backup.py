"""Restore a downloaded encrypted backup on Windows without the original machine."""
import argparse,getpass,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from signing_custody import unseal,validate,persist_local
from recovery_code import normalize

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--backup',type=Path,required=True)
    p.add_argument('--destination',type=Path,required=True)
    p.add_argument('--recovery-code',action='store_true')
    a=p.parse_args()
    if a.destination.exists():raise ValueError('Existing destination retained')
    value=getpass.getpass('Recovery code (hidden): ' if a.recovery_code else 'Backup password (hidden): ')
    bundle=unseal(a.backup.read_bytes(),normalize(value) if a.recovery_code else value)
    persist_local(bundle,a.destination)
    (a.destination/'RESTORED_PUBLIC_RECEIPT.json').write_text(json.dumps({'status':'RESTORED','keyIds':validate(bundle)},indent=2))
    print('Restore verified. Private keys protected by this Windows account.')

if __name__=='__main__':
    try:main()
    except Exception as e:
        print('Restore stopped; existing files retained. Error type: '+type(e).__name__);raise SystemExit(1)
