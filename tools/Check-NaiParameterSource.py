"""Read-only drift signal for an explicitly supplied public-source snapshot.
No network, credentials, scheduling, code adoption, Git mutation or product PASS.
"""
from pathlib import Path
import argparse,hashlib,json,re

MAX_BYTES=16*1024*1024

def check(manifest,snapshot,index=0):
    if manifest.stat().st_size>1024*1024:raise ValueError('Oversized field manifest')
    value=json.loads(manifest.read_text(encoding='utf-8'))
    if value.get('format')!=1 or not isinstance(value.get('sources'),list):raise ValueError('Invalid field manifest')
    if type(index) is not int or not 0<=index<len(value['sources']):raise ValueError('Invalid source index')
    source=value['sources'][index]
    if not isinstance(source,dict) or not str(source.get('kind','')).startswith('official-'):raise ValueError('Only an explicit public authority snapshot is supported')
    expected=source.get('wholeChunkSha256',source.get('sha256'))
    if not isinstance(expected,str) or not re.fullmatch('[0-9a-f]{64}',expected):raise ValueError('Missing fixed public-source digest')
    snapshot=Path(snapshot).absolute()
    if any(p.is_symlink() or p.is_junction() for p in [snapshot,*snapshot.parents]):raise ValueError('Reparse snapshot refused')
    if not snapshot.is_file() or not 0<snapshot.stat().st_size<=MAX_BYTES:raise ValueError('Invalid public snapshot size')
    total=0;hashed=hashlib.sha256()
    with snapshot.open('rb') as stream:
        while True:
            body=stream.read(128*1024)
            if not body:break
            total+=len(body)
            if total>MAX_BYTES:raise ValueError('Public snapshot grew beyond bound')
            hashed.update(body)
    observed=hashed.hexdigest();same=observed==expected
    return dict(format=1,checkpoint=value.get('checkpoint'),status='SOURCE_UNCHANGED' if same else 'SOURCE_CHANGED_REVIEW_REQUIRED',expectedSha256=expected,observedSha256=observed,bytes=total,productAlignment='NOT_EVALUATED',automaticAdoption=False,networkRequests=0)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--manifest',required=True,type=Path);parser.add_argument('--snapshot',required=True,type=Path);parser.add_argument('--source-index',type=int,default=0);args=parser.parse_args()
    try:
        result=check(args.manifest,args.snapshot,args.source_index);print(json.dumps(result));raise SystemExit(0 if result['status']=='SOURCE_UNCHANGED' else 2)
    except (OSError,ValueError,KeyError,TypeError) as error:
        print(json.dumps(dict(status='INPUT_REJECTED',errorClass=type(error).__name__,automaticAdoption=False)));raise SystemExit(1)
