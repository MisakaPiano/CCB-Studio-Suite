"""Prepare deterministic synthetic review images and audit fixed package provenance.

No application launch, provider request, clipboard operation or private data access.
"""
import argparse, hashlib, json, struct, subprocess, sys, zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from install_ops import verify_install
from studio_update import Installed

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def png(variant):
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    width,height=320,240
    rows=b''.join(b'\0'+b''.join(bytes(((x+variant*70)%256,(y*2)%256,60 if variant==1 else 190)) for x in range(width)) for y in range(height))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',width,height,8,2,0,0,0))+chunk(b'tEXt',b'Description\0Synthetic acceptance fixture; no provider call; '+str(variant).encode())+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')

def main():
    p=argparse.ArgumentParser();p.add_argument('--destination',type=Path,required=True);p.add_argument('--package',type=Path,required=True);p.add_argument('--build-evidence',type=Path,required=True);a=p.parse_args()
    dest=a.destination.resolve();package=a.package.resolve()
    if not dest.is_relative_to(ROOT/'.local') or dest.exists():raise ValueError('New Suite-local destination required')
    if not package.is_relative_to(ROOT/'.local'):raise ValueError('Suite-local synthetic package only')
    config=json.loads((package/'update-config.json').read_text())
    installed=Installed.load(package);verified=verify_install(package,config)
    before=json.loads((a.build_evidence/'source-before.json').read_text())
    modules={name:value for name,value in before.items() if name.startswith('modules/')}
    assert modules and all(sha(ROOT/name)==value for name,value in modules.items())
    prohibited=[p.relative_to(package).as_posix() for p in package.rglob('*') if p.is_file() and (p.suffix.lower() in ('.dpapi','.enc','.pfx','.p12','.key') or p.name in ('PUBLIC_RECEIPT.json','RECOVERY_PUBLIC_RECEIPT.json'))]
    assert not prohibited
    dest.mkdir();(dest/'inputs').mkdir();(dest/'paste-receiver').mkdir();(dest/'exports').mkdir()
    for i in (1,2):(dest/'inputs'/f'synthetic-{i}.png').write_bytes(png(i))
    report={'status':'AUTOMATED_PREPARATION_AND_PROVENANCE_PASS','head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'packageVersion':installed.version,'packageSourceSha':installed.source_sha,'metadataBranch':installed.metadata_branch,'packageRootSha256':hashlib.sha256(installed.bootstrap).hexdigest(),'installation':verified,'moduleSourceFilesUnchanged':len(modules),'unexpectedSigningFiles':prohibited,'inputs':{p.name:{'bytes':p.stat().st_size,'sha256':sha(p)} for p in (dest/'inputs').iterdir()},'humanAcceptance':'NOT TESTED','realProviderCalls':0,'privateDataAccess':False}
    (dest/'PREPARATION.json').write_text(json.dumps(report,indent=2));print(json.dumps(report))

if __name__=='__main__':main()
