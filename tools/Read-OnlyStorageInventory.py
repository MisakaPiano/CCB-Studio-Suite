"""Offline, read-only v1 storage inventory. No Studio launch, image decode, secrets or exports.

Only hashes image bytes, never decodes/displays them. A candidate is a local finding,
not deletion authorization: unknown/incomplete/changed metadata stays HOLD.
"""
from pathlib import Path
import argparse,hashlib,json,struct,uuid,stat

class Refused(Exception):pass
def safe(path):
    for p in [path,*path.parents]:
        try:s=p.lstat()
        except FileNotFoundError:continue
        if stat.S_ISLNK(s.st_mode) or getattr(s,'st_file_attributes',0)&1024:raise Refused('LINK_OR_REPARSE')
    return path
def digest(path):
    safe(path)
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
class Reader:
    def __init__(self,data):self.data=data;self.pos=0
    def take(self,n):
        if n<0 or self.pos+n>len(self.data):raise Refused('TRUNCATED')
        v=self.data[self.pos:self.pos+n];self.pos+=n;return v
    def number(self,fmt):return struct.unpack(fmt,self.take(struct.calcsize(fmt)))[0]
    def utf(self):return self.take(self.number('>H'))
    def end(self):
        if self.pos!=len(self.data):raise Refused('UNKNOWN_FIELDS')
def envelope(path,kind):
    safe(path)
    if path.stat().st_size>3*1024*1024+128:raise Refused('OVERSIZE')
    raw=path.read_bytes();b=Reader(raw)
    if b.number('>I')!=0x43434241:raise Refused('BAD_MAGIC')
    if b.number('>I')!=1:raise Refused('FUTURE_ENVELOPE')
    if b.utf()!=kind.encode():raise Refused('WRONG_TYPE')
    n=b.number('>i');h=b.take(32);payload=b.take(n);b.end()
    if hashlib.sha256(payload).digest()!=h:raise Refused('BAD_METADATA_HASH')
    return payload
def identity(value):
    s=value.decode('ascii');u=uuid.UUID(s)
    if str(u)!=s:raise Refused('NON_CANONICAL_ID')
    return s
def asset(payload):
    b=Reader(payload);key=identity(b.utf());b.utf() # Never decode/record the private filename.
    kind=b.utf();parent=b.utf()
    if kind not in (b'ORIGINAL',b'DERIVED') or (kind==b'ORIGINAL')!= (not parent):raise Refused('BAD_KIND')
    if parent:identity(parent)
    recipe=b.take(b.number('>i'));project=b.take(b.number('>i'));request=payload[:b.pos]
    size=b.number('>q');sha=b.utf().decode('ascii');b.end()
    if size<0 or size>256*1024*1024 or len(sha)!=64 or any(c not in '0123456789abcdef' for c in sha):raise Refused('BAD_ASSET_IDENTITY')
    return dict(id=key,size=size,sha256=sha,recipe=recipe,project=project,request=request)
def inventory(workspace):
    w=safe(workspace);root=w/'assets';out=dict(workspace=str(w),mode='READ_ONLY',privateContentsRecorded=False,candidates=[],holds=[],externalSourcePathsScanned=False)
    if not root.is_dir():return dict(out,status='MISSING_ROOT')
    if envelope(root/'suite-root.rec','root')!=b'CCBStudioSuite/assets/a1-001':raise Refused('FOREIGN_ROOT')
    metadata={};records={};library=[]
    def checked(p,kind):
        value=envelope(p,kind);metadata[p]=digest(p);return value
    for p in sorted((root/'records').iterdir()):
        try:
            key=identity(p.stem.encode());a=asset(checked(p,'asset'))
            if p.name!=key+'.rec' or a['id']!=key:raise Refused('RECORD_ID_MISMATCH')
            records[key]=(a,p)
            if a['project']==b'CCB_LIBRARY_STATE_V1':library.append((a,json.loads(a['recipe'])))
        except Exception as e:out['holds'].append(dict(area='record',reason=type(e).__name__ if not isinstance(e,Refused) else str(e)))
    unique={a['sha256']:a for a,_ in records.values()};verified={}
    def canonical(a):
        sha=a['sha256'];p=safe(root/'objects'/(sha+'.blob'))
        if sha not in verified:verified[sha]=p.is_file() and p.stat().st_size==a['size'] and digest(p)==sha
        return verified[sha]
    state=None
    if library:
        try:
            library.sort(key=lambda v:v[1]['sequence']);previous=None
            for i,(a,h) in enumerate(library):
                if h.get('version')!=1 or h['sequence']!=i+1 or h.get('previous')!=previous:raise Refused('LIBRARY_CHAIN')
                previous=a['id']
            latest=library[-1][0]
            if latest['size']>4*1024*1024 or not canonical(latest):raise Refused('LIBRARY_PAYLOAD')
            state=json.loads((root/'objects'/(latest['sha256']+'.blob')).read_bytes())
            if state.get('version',1)!=1:raise Refused('FUTURE_LIBRARY_STATE')
            observations=state.get('observations',{});albums=state.get('virtualAlbums',[])
            out['references']=dict(observations=len(observations),albums=len(albums),albumEdges=sum(len(a.get('images',[])) for a in albums),unresolvedAlbumEdges=sum(i not in records for a in albums for i in a.get('images',[])))
            for folder in state.get('folders',[]):
                if folder['category']!='imports' or Path(folder['path']).absolute()!=w.with_name(w.name+'-collections')/'imports':continue
                for key,obs in observations.items():
                    if not key.startswith(folder['id']+':'):continue
                    relative=Path(key[len(folder['id'])+1:]);aid=obs['assetId']
                    if len(relative.parts)!=1 or not relative.name.startswith(aid+'-'):continue
                    p=safe(Path(folder['path'])/relative)
                    if not p.exists():continue
                    a=records[aid][0]
                    if obs['sha256']!=a['sha256'] or obs['size']!=a['size'] or not canonical(a) or p.stat().st_size!=a['size'] or digest(p)!=a['sha256']:raise Refused('IMPORT_COPY_CHANGED')
                    out['candidates'].append(dict(area='legacy-import-copy',path=str(p),assetId=aid,sha256=a['sha256'],bytes=a['size'],action='HOLD_FOR_REFERENCE_MIGRATION',proof='Exact managed imports observation + verified committed canonical object + equal file bytes'))
        except Exception as e:out['holds'].append(dict(area='library',reason=str(e) if isinstance(e,Refused) else type(e).__name__))
    for tx in sorted((root/'transactions').iterdir()):
        p=tx/'payload.part'
        if not p.exists():continue
        try:
            key=identity(tx.name.encode());a,record=records[key]
            if checked(tx/'seal.rec','asset')!=checked(record,'asset') or checked(tx/'request.rec','request')!=a['request'] or checked(tx/'complete.rec','complete')!=a['sha256'].encode():raise Refused('INCOMPLETE_OR_CONFLICTING_TRANSACTION')
            safe(p)
            if not canonical(a) or p.stat().st_size!=a['size'] or digest(p)!=a['sha256']:raise Refused('STAGING_BYTES_CHANGED')
            out['candidates'].append(dict(area='committed-staging',path=str(p),assetId=key,sha256=a['sha256'],bytes=a['size'],action='ELIGIBLE_AFTER_EXCLUSIVE_RECHECK',proof='Exact intent/seal/commit/complete and both payload hashes verified'))
        except Exception as e:out['holds'].append(dict(area='staging',transaction=tx.name,reason=str(e) if isinstance(e,Refused) else type(e).__name__))
    if any(not p.is_file() or digest(p)!=sha for p,sha in metadata.items()):out['holds'].append(dict(area='metadata',reason='CHANGED_DURING_INVENTORY'))
    out.update(status='PLAN_ONLY_HOLD' if out['holds'] else 'PLAN_ONLY',committedRecords=len(records),uniqueReferencedObjects=len(unique),referencedBytes=sum(a['size'] for a in unique.values()),candidateBytes=sum(a['bytes'] for a in out['candidates']),filesModified=0)
    return out
def main():
    p=argparse.ArgumentParser();p.add_argument('--workspace',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();w=a.workspace.absolute();o=a.output.absolute()
    if o.is_relative_to(w) or o.is_relative_to(w.with_name(w.name+'-collections')):p.error('Output must be outside the audited data/collections roots')
    try:result=inventory(w)
    except Exception as e:result=dict(status='HOLD',workspace=str(w),reason=str(e) if isinstance(e,Refused) else type(e).__name__,filesModified=0)
    o.parent.mkdir(parents=True,exist_ok=True)
    with o.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)
    print(json.dumps({k:v for k,v in result.items() if k not in ('candidates','holds','workspace') } | dict(candidates=len(result.get('candidates',[])),holds=len(result.get('holds',[])))))
if __name__=='__main__':main()
