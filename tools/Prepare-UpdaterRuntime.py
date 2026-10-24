"""Build an isolated app-local Python/TUF runtime from hash-locked local artifacts.

No pip execution, system installation, network or trust-store changes. The destination
must be new; a failed partial directory is retained for diagnosis, never replaced.
"""
import argparse, hashlib, json, shutil, stat, zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / 'tools/update-runtime/artifacts.json'

def build(destination, sources):
    destination = destination.resolve()
    if not destination.is_relative_to(ROOT / '.local'):
        raise ValueError('Build only inside Suite .local; packaging copies the verified result')
    if destination.exists():
        raise FileExistsError('Destination must be new')
    artifacts=json.loads(LOCK.read_text(encoding='utf-8'))['artifacts']
    verified=[]
    for item in artifacts:
        found=[p/item['fileName'] for p in sources if (p/item['fileName']).is_file()]
        if not found:raise FileNotFoundError(item['fileName'])
        source=found[0]
        if source.stat().st_size!=item['sizeBytes'] or hashlib.sha256(source.read_bytes()).hexdigest()!=item['sha256']:
            raise ValueError('Artifact digest mismatch: '+item['fileName'])
        verified.append((item,source))
    destination.mkdir(parents=True)
    for item,source in verified:
        target=destination if item['kind']=='runtime' else destination/'lib'
        with zipfile.ZipFile(source) as archive:
            for info in archive.infolist():
                path=PurePosixPath(info.filename)
                if (path.is_absolute() or '..' in path.parts or '\\' in info.filename or ':' in info.filename
                    or stat.S_ISLNK(info.external_attr >> 16) or any(x.endswith('.data') for x in path.parts)):
                    raise ValueError('Unsupported archive member: '+info.filename)
                out=target.joinpath(*path.parts)
                if info.is_dir():out.mkdir(parents=True,exist_ok=True);continue
                out.parent.mkdir(parents=True,exist_ok=True)
                with out.open('xb') as stream:stream.write(archive.read(info))
    # ._pth isolates registry/environment/current-directory import paths; site remains off.
    (destination/'python313._pth').write_text('python313.zip\n.\nlib\n',encoding='utf-8')
    shutil.copyfile(ROOT/'tools/update-runtime/studio_update.py',destination/'lib/studio_update.py')
    shutil.copyfile(ROOT/'tools/update-runtime/github_transport.py',destination/'lib/github_transport.py')
    shutil.copyfile(ROOT/'tools/update-runtime/install_ops.py',destination/'lib/install_ops.py')
    shutil.copyfile(ROOT/'tools/update-runtime/apply_update.py',destination/'lib/apply_update.py')
    shutil.copyfile(ROOT/'tools/update-runtime/residue_cleanup.py',destination/'lib/residue_cleanup.py')
    shutil.copyfile(ROOT/'tools/update-runtime/owned_files.py',destination/'lib/owned_files.py')
    receipt={'task':'UX-R4','status':'ASSEMBLED_NOT_APPLICATION_UPDATE_VALIDATED','artifacts':artifacts,
             'systemEnvironmentChanged':False,'networkDownloads':0,'files':[]}
    for p in sorted(destination.rglob('*')):
        if p.is_file():receipt['files'].append({'path':p.relative_to(destination).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
    (destination/'RUNTIME_MANIFEST.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'destination':str(destination),'files':len(receipt['files']),'bytes':sum(x['bytes'] for x in receipt['files'])}))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--destination',required=True);p.add_argument('--source',action='append',required=True);a=p.parse_args()
    build(Path(a.destination),[Path(x) for x in a.source])
