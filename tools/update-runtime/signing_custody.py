"""Password-encrypted portable backup; Windows-current-user local protection.

No plaintext private key is written, logged, or placed in process arguments.
The backup password is never persisted. Publishing keys is a separate operation.
"""
import argparse, base64, ctypes, getpass, hashlib, json, os, sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding, PrivateFormat, NoEncryption, load_pem_private_key
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from securesystemslib.signer import CryptoSigner
from tuf.api.metadata import Metadata, Root

ROLES = ('root', 'targets', 'snapshot', 'timestamp')
AAD = b'CCB-Suite-signing-backup-v1'

def dpapi(data, decrypt=False):
    if sys.platform != 'win32': raise RuntimeError('Windows current-user protection required')
    from ctypes import wintypes
    class Blob(ctypes.Structure):
        _fields_ = [('size', wintypes.DWORD), ('data', ctypes.POINTER(ctypes.c_byte))]
    buffer = ctypes.create_string_buffer(data)
    source = Blob(len(data), ctypes.cast(buffer, ctypes.POINTER(ctypes.c_byte)))
    target = Blob()
    crypt = ctypes.WinDLL('crypt32', use_last_error=True)
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.LocalFree.argtypes = [ctypes.c_void_p]
    kernel.LocalFree.restype = ctypes.c_void_p
    fn = crypt.CryptUnprotectData if decrypt else crypt.CryptProtectData
    fn.argtypes = [ctypes.POINTER(Blob), ctypes.c_void_p, ctypes.c_void_p, ctypes.c_void_p, ctypes.c_void_p, wintypes.DWORD, ctypes.POINTER(Blob)]
    fn.restype = wintypes.BOOL
    if not fn(ctypes.byref(source), None, None, None, None, 1, ctypes.byref(target)):
        raise RuntimeError('Windows protection failed')
    try: return ctypes.string_at(target.data, target.size)
    finally:
        ctypes.memset(target.data, 0, target.size)
        kernel.LocalFree(target.data)

def generate():
    private = {}
    root = Metadata(Root(expires=datetime.now(timezone.utc)+timedelta(days=365), consistent_snapshot=True))
    signers = {}
    for role in ROLES:
        key = Ed25519PrivateKey.generate()
        private[role] = key.private_bytes(Encoding.PEM, PrivateFormat.PKCS8, NoEncryption()).decode('ascii')
        signers[role] = CryptoSigner(key)
        root.signed.add_key(signers[role].public_key, role)
    root.sign(signers['root'])
    return {'format': 1, 'root': root.to_bytes().decode(), 'private': private}

def validate(bundle):
    if set(bundle) != {'format', 'root', 'private'} or bundle['format'] != 1 or set(bundle['private']) != set(ROLES):
        raise ValueError('Invalid signing bundle')
    root = Metadata.from_bytes(bundle['root'].encode())
    root.signed.verify_delegate('root', root.signed_bytes, root.signatures)
    ids = {}
    for role in ROLES:
        key = load_pem_private_key(bundle['private'][role].encode(), None)
        if not isinstance(key, Ed25519PrivateKey): raise ValueError('Unsupported key')
        signer = CryptoSigner(key)
        if root.signed.roles[role].keyids != [signer.public_key.keyid] or root.signed.roles[role].threshold != 1:
            raise ValueError('Backup key identity mismatch')
        challenge = os.urandom(32)
        signer.public_key.verify_signature(signer.sign(challenge), challenge)
        ids[role] = signer.public_key.keyid
    return ids

def seal(bundle, password):
    validate(bundle)
    if len(password) < 12: raise ValueError('Use at least 12 characters for backup password')
    salt, nonce = os.urandom(16), os.urandom(12)
    key = Scrypt(salt=salt, length=32, n=2**17, r=8, p=1).derive(password.encode('utf-8'))
    ciphertext = AESGCM(key).encrypt(nonce, json.dumps(bundle).encode(), AAD)
    return AAD + salt + nonce + ciphertext

def unseal(data, password):
    if not data.startswith(AAD) or not 128 <= len(data) <= 65536: raise ValueError('Invalid backup envelope')
    offset = len(AAD)
    salt, nonce = data[offset:offset+16], data[offset+16:offset+28]
    key = Scrypt(salt=salt, length=32, n=2**17, r=8, p=1).derive(password.encode('utf-8'))
    bundle = json.loads(AESGCM(key).decrypt(nonce, data[offset+28:], AAD))
    validate(bundle)
    return bundle

def persist_local(bundle, destination):
    validate(bundle)
    destination.mkdir(parents=True, exist_ok=False)
    for role in ROLES:
        (destination/(role+'.dpapi')).write_bytes(dpapi(bundle['private'][role].encode()))
    (destination/'root.json').write_text(bundle['root'], encoding='utf-8')

def local_signer(destination, role):
    if role not in ROLES: raise ValueError('Unknown signing role')
    root = Metadata.from_bytes((destination/'root.json').read_bytes())
    signer = CryptoSigner(load_pem_private_key(dpapi((destination/(role+'.dpapi')).read_bytes(), True), None))
    if signer.public_key.keyid not in root.signed.roles[role].keyids: raise ValueError('Local key mismatch')
    return signer

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--destination', required=True, type=Path)
    args = parser.parse_args()
    destination = args.destination.resolve()
    if destination.exists(): raise ValueError('Existing destination retained; choose a new directory')
    print('Create long-term Suite signing keys and an encrypted backup.\nKeep this password separately from Google Drive. It will not be saved.')
    while True:
        password = getpass.getpass('Backup password (12+ characters; input is hidden): ')
        if len(password) < 12:
            print('Password is too short. Please enter at least 12 characters. Try again.')
            continue
        if any(ord(char) < 32 or ord(char) == 127 for char in password):
            print('Input contained a control key. Please type the password again.')
            continue
        if password != getpass.getpass('Repeat the SAME password (input is hidden): '):
            print('The two passwords do not match. Please enter both again.')
            continue
        break
    bundle = generate()
    encrypted = seal(bundle, password)
    restored = unseal(encrypted, password)
    assert validate(restored) == validate(bundle)
    persist_local(bundle, destination)
    backup = destination/'CCB-Suite-signing-backup-v1.enc'
    backup.write_bytes(encrypted)
    # Read back the actual backup bytes and recover to a separate protected directory.
    recovered = unseal(backup.read_bytes(), password)
    persist_local(recovered, destination/'restore-check')
    for role in ROLES:
        assert local_signer(destination, role).public_key == local_signer(destination/'restore-check', role).public_key
    receipt = {'status': 'CREATED_LOCAL_BACKUP_RESTORE_VERIFIED', 'keyIds': validate(bundle), 'backupSha256': hashlib.sha256(encrypted).hexdigest(), 'rootSha256': hashlib.sha256(bundle['root'].encode()).hexdigest(), 'driveUpload': 'NOT_YET_DONE', 'automaticRenewal': 'NOT_YET_DEPLOYED'}
    (destination/'PUBLIC_RECEIPT.json').write_text(json.dumps(receipt, indent=2), encoding='utf-8')
    print('Created and recovery verified. Password was not saved. You may close this window.')

if __name__ == '__main__':
    try: main()
    except Exception as e:
        print('Operation stopped; existing files retained. Error type: '+type(e).__name__)
        sys.exit(1)
