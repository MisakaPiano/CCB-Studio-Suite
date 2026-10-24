"""Lifecycle for NEW explicitly disposable Suite test artifacts, not a legacy sweeper.

No source, daily profile, backup, signing custody or old evidence directory is eligible.
Only roots created exclusively by create() under .local/disposable can be sealed and
retired, after evidence has been durably retained outside that root. Unknown/changed
files and failed-run fixtures are retained. All removals use Windows handle guards.
"""
import argparse,gzip,hashlib,json,os,re,sys,time,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent/'update-runtime'))
from owned_files import relative,delete_matching,prune_empty

ROOT=Path(__file__).resolve().parents[1]

def safe(path):
    path=Path(os.path.abspath(path))
    for p in [path,*path.parents]:
        if p.is_symlink() or (p.exists() and p.is_junction()):raise ValueError('Reparse path refused')
    return path

def digest(path):
    with safe(path).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()

def atomic(path,data):
    path=safe(path);stage=path.with_name(path.name+'.'+uuid.uuid4().hex+'.tmp')
    with stage.open('xb') as f:f.write(data);f.flush();os.fsync(f.fileno())
    os.replace(stage,path)

def write(path,value):atomic(path,(json.dumps(value,ensure_ascii=True,indent=2)+'\n').encode())

def read(path,limit=1024*1024):
    path=safe(path)
    if path.stat().st_size>limit:raise ValueError('Artifact record oversized')
    return json.loads(path.read_text(encoding='utf-8-sig'))

def create(task):
    if not re.fullmatch('[a-zA-Z0-9_-]{1,64}',task):raise ValueError('Invalid task label')
    parent=safe(ROOT/'.local/disposable');parent.mkdir(parents=True,exist_ok=True)
    folder=parent/(task+'-'+uuid.uuid4().hex);folder.mkdir()
    payload=folder/'payload';payload.mkdir()
    write(folder/'OWNER.json',dict(format=1,kind='EXPLICIT_SYNTHETIC_DISPOSABLE',root=str(folder),task=task,nonce=uuid.uuid4().hex,createdAt=time.time(),sourceCommit='recorded by retained test evidence'))
    return folder

def owner(folder):
    folder=safe(folder);parent=safe(ROOT/'.local/disposable')
    if folder.parent!=parent or not re.fullmatch('[a-zA-Z0-9_-]{1,64}-[0-9a-f]{32}',folder.name):raise ValueError('Not a registered disposable root')
    record=read(folder/'OWNER.json')
    if record.get('format')!=1 or record.get('kind')!='EXPLICIT_SYNTHETIC_DISPOSABLE' or record.get('root')!=str(folder) or not re.fullmatch('[0-9a-f]{32}',record.get('nonce','')):raise ValueError('Invalid artifact owner')
    # Existing activity/backup records override disposable registration.
    registrations=[ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json',ROOT/'.local/PROTECTED_ARTIFACTS.json']
    for p in registrations:
        if not p.exists():continue
        value=read(p)
        def paths(v):
            if isinstance(v,dict):return [s for x in v.values() for s in paths(x)]
            if isinstance(v,list):return [s for x in v for s in paths(x)]
            return [safe(v)] if isinstance(v,str) and Path(v).is_absolute() else []
        for protected in paths(value):
            if protected==folder or protected.is_relative_to(folder) or folder.is_relative_to(protected):raise ValueError('Protected activity or backup overlaps disposable root')
    return folder,record

def seal(folder,evidence_files,status='PASS'):
    folder,record=owner(folder);payload=safe(folder/'payload');items=[];dirs=[];total=0
    if (folder/'MANIFEST.json.gz').exists():raise ValueError('Already sealed; do not replace old evidence')
    proofs=[]
    for p in evidence_files:
        p=safe(p)
        if p.is_relative_to(folder) or not p.is_relative_to(ROOT/'.local'):raise ValueError('Evidence must be retained separately in Suite .local')
        proofs.append(dict(path=str(p),bytes=p.stat().st_size,sha256=digest(p)))
    if not proofs:raise ValueError('No retained evidence')
    for here,subdirs,names in os.walk(payload,followlinks=False):
        safe(here)
        for name in subdirs:safe(Path(here)/name);dirs.append((Path(here)/name).relative_to(payload).as_posix())
        for name in names:
            p=safe(Path(here)/name);size=p.stat().st_size;total+=size
            if len(items)>=200000 or total>8*1024**3:raise ValueError('Disposable inventory bound exceeded; retained')
            items.append(dict(path=p.relative_to(payload).as_posix(),bytes=size,sha256=digest(p)))
    manifest=dict(format=1,nonce=record['nonce'],root=str(folder),status=status,files=items,directories=dirs,evidence=proofs,totalBytes=total)
    atomic(folder/'MANIFEST.json.gz',gzip.compress(json.dumps(manifest,ensure_ascii=True).encode(),mtime=0))
    write(folder/'SEALED.json',dict(format=1,status=status,manifestSha256=digest(folder/'MANIFEST.json.gz'),files=len(items),bytes=total))
    return manifest

def cleanup(folder):
    folder,record=owner(folder);sealed=read(folder/'SEALED.json');archive=folder/'MANIFEST.json.gz'
    if digest(archive)!=sealed['manifestSha256']:raise ValueError('Disposable inventory changed')
    with gzip.open(archive,'rb') as f:
        raw=f.read(64*1024**2+1)
    if len(raw)>64*1024**2:raise ValueError('Disposable inventory exceeds bound')
    manifest=json.loads(raw)
    if manifest.get('root')!=str(folder) or manifest.get('nonce')!=record['nonce']:raise ValueError('Disposable identity mismatch')
    if manifest.get('status') not in ('PASS','COMMAND_SUCCESS'):return dict(status='RETAINED_FAILED_FIXTURES',removed=0)
    for proof in manifest['evidence']:
        p=safe(proof['path'])
        if p.is_relative_to(folder) or not p.is_relative_to(ROOT/'.local') or p.stat().st_size!=proof['bytes'] or digest(p)!=proof['sha256']:raise ValueError('Retained evidence missing or changed')
    payload=safe(folder/'payload');items=manifest['files'];seen=set();dirs=manifest.get('directories',[])
    for item in items:
        relative(item['path']);key=item['path'].casefold()
        if key in seen or type(item['bytes']) is not int or not 0<=item['bytes']<=8*1024**3 or not re.fullmatch('[0-9a-f]{64}',item.get('sha256','')):raise ValueError('Invalid disposable file identity')
        seen.add(key)
    for directory in dirs:relative(directory)
    removed=0;freed=0;retained=[]
    write(folder/'CLEANUP.json',dict(status='IN_PROGRESS',sourceManifestSha256=sealed['manifestSha256']))
    for item in items:
        p=safe(payload.joinpath(*relative(item['path'])))
        if not p.exists():continue
        try:status=delete_matching(p,item)
        except (OSError,ValueError) as e:status='RETRY_'+type(e).__name__
        if status=='REMOVED':removed+=1;freed+=item['bytes']
        else:retained.append(dict(path=item['path'],status=status))
    # Empty directories are explicitly inventoried, and removal is still nonrecursive.
    prune_empty(payload,items,dirs)
    if payload.exists():
        for here,subdirs,names in os.walk(payload,followlinks=False):
            safe(here)
            for name in names:
                p=safe(Path(here)/name);key=p.relative_to(payload).as_posix()
                if key.casefold() not in seen:retained.append(dict(path=key,status='RETAIN_UNKNOWN'))
    result=dict(status='COMPLETE' if not retained else 'DEFERRED_OR_UNKNOWN_RETAINED',removed=removed,bytesFreed=freed,retained=retained,sourceManifestSha256=sealed['manifestSha256'])
    write(folder/'CLEANUP.json',result);return result

def inventory():
    parent=safe(ROOT/'.local/disposable')
    if not parent.exists():return []
    return [dict(path=str(p),owner=read(p/'OWNER.json'),sealed=read(p/'SEALED.json') if (p/'SEALED.json').exists() else None,cleanup=read(p/'CLEANUP.json') if (p/'CLEANUP.json').exists() else None) for p in parent.iterdir() if p.is_dir() and (p/'OWNER.json').exists()]

if __name__=='__main__':
    p=argparse.ArgumentParser();g=p.add_mutually_exclusive_group(required=True);g.add_argument('--create');g.add_argument('--cleanup',type=Path);g.add_argument('--inventory',action='store_true');a=p.parse_args()
    value=str(create(a.create)) if a.create else cleanup(a.cleanup) if a.cleanup else inventory();print(json.dumps(value,ensure_ascii=True,indent=2))
