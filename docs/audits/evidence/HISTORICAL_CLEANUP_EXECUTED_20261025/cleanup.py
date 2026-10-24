"""Exact user-authorized manual cleanup, separate from the updater mechanism.
Only listed roots; no recursive deletion, no secret/backup contents discovery.
"""
import argparse,ctypes,gzip,hashlib,json,msvcrt,os,re,sys,time
from contextlib import ExitStack
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from owned_files import relative,io_path,held,held_parents,dispose
from install_ops import verify_install
APP=Path('H:/CCB-Studio-Suite-App');HISTORY=ROOT/'.local/review-fixes-cleanup-20261024'
OBS=ROOT/'docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/observations'
PROGRAMS=[Path(x) for x in (
 'H:/CCB-Studio-Suite/.local/windows-rc/performance-106/smoke-106',
 'H:/CCB-Studio-Suite/.local/windows-rc/batch-109/smoke-109',
 'H:/CCB-Studio-Suite/.local/windows-rc/update-repair-110/smoke-110',
 'H:/CCB-Studio-Suite/.local/windows-rc/batch-parity-111/smoke-111',
 'H:/CCB-Studio-Suite/.local/windows-rc/batch-history-112/smoke-112')]
SHARED=[ROOT/'.local/windows-preview/tests',ROOT/'.local/windows-preview/desktop-tests']
OLD_APP=[APP/x for x in ('105-a416a820955a','107-5907779fad12','109-2093ae5cb858','112-acf71fc1acb2','114-f59b0c25a2eb')]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,value):
    p.write_text(json.dumps(value,ensure_ascii=True,indent=2)+'\n')
def digest(p):
    with io_path(p).open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def safe(p):
    p=Path(os.path.abspath(p))
    for here in (p,*p.parents):
        physical=io_path(here)
        if physical.is_symlink() or physical.is_junction():raise ValueError('Reparse path refused: '+str(here))
    return p
def overlap(a,b):return a==b or a.is_relative_to(b) or b.is_relative_to(a)
def protections():
    active=read(APP/'launch-current.json')
    keep=[Path(active[k]) for k in ('installed','data','runtime')]
    for p in (ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json',APP/'ACTIVE_INSTALLATION.json'):
        record=read(p)
        keep += [Path(record[k]) for k in ('program','workspace','runtime','retainedBackupWorkspace','retainedBackupRuntime') if k in record]
    keep += [ROOT/'docs',ROOT/'handoff',ROOT/'.git',ROOT/'.local/publish-entry-review-20261024',ROOT/'.local/review-fixes-cleanup-20261024']
    keep += [ROOT/'.local/windows-rc/performance-107'/n for n in ('smoke-107','114-f59b0c25a2eb','115-ff145d051bae')]
    return active,[safe(p) for p in keep]
def named_protection(name,is_dir):
    lower=name.casefold()
    if 'backup' in lower or '备份' in name:return 'BACKUP_RETAINED'
    if is_dir and (lower=='evidence' or lower.startswith('.studio-format-migration')):return 'EVIDENCE_OR_MIGRATION_RETAINED'
    if not is_dir and (lower.endswith(('.log','.xml')) or lower in ('result.json','summary.json','test-cases.json','command.json','commands.txt')):return 'POTENTIAL_EVIDENCE_RETAINED'
    return None
def scan(root,kind):
    files=[];directories=[];retained=[];stack=[root]
    while stack:
        here=stack.pop()
        with os.scandir(io_path(here)) as entries:
            for entry in entries:
                p=here/entry.name;name=p.relative_to(root).as_posix()
                if entry.is_symlink() or entry.is_junction():retained.append(dict(path=name,reason='REPARSE_RETAINED'));continue
                is_dir=entry.is_dir(follow_symlinks=False)
                if kind=='shared':
                    reason=named_protection(entry.name,is_dir)
                    if reason:retained.append(dict(path=name,reason=reason));continue
                try:relative(name)
                except ValueError:retained.append(dict(path=name,reason='UNSAFE_NAME_RETAINED'));continue
                if is_dir:directories.append(name);stack.append(p);continue
                if not entry.is_file(follow_symlinks=False):retained.append(dict(path=name,reason='UNKNOWN_TYPE_RETAINED'));continue
                s=entry.stat(follow_symlinks=False)
                files.append(dict(path=name,bytes=s.st_size,modified100=s.st_mtime_ns//100+116444736000000000))
    return files,directories,retained
def anchors():
    paths=[APP/n for n in ('launch-current.json','ACTIVE_INSTALLATION.json','CCBStudioLauncher.exe','Launch-CCB-Studio.lnk','MIGRATION_COMPLETE.json','MIGRATION_DATA_VERIFIED.json')]
    active=read(APP/'launch-current.json')
    paths += [Path(active['installed'])/n for n in ('INSTALL_MANIFEST.json','update-config.json','CCBStudioSuitePreview.exe')]
    paths += [ROOT/'.local/ACTIVE_STUDIO_INSTALLATION.json',ROOT/'docs/audits/evidence/PROJECT_PHASE_20261024/RAW_EVIDENCE.zip',ROOT/'docs/audits/evidence/REVIEW_FIXES_CLEANUP_20261025/RAW_EVIDENCE.zip']
    return [dict(path=str(p),bytes=p.stat().st_size,sha256=digest(p)) for p in paths]
def plan():
    active,protected=protections();rows=[];skipped=[]
    pinned=read(OBS/'HISTORICAL_RESIDUE_READ_ONLY.json');old={p['path']:p for p in pinned['programs']}
    confirmed={p['path']:p for p in read(OBS/'CONFIRMED_WORKSPACE_PROGRAM_CANDIDATES.json') if p['verification'].startswith('EXACT')}
    for root in PROGRAMS+SHARED+OLD_APP:
        root=safe(root)
        assert root in PROGRAMS+SHARED+OLD_APP and (root.is_relative_to(ROOT/'.local') or root.parent==APP)
        assert not any(overlap(root,k) for k in protected),'Protected target: '+str(root)
        if not root.exists():skipped.append(dict(path=str(root),reason='ALREADY_ABSENT'));continue
        kind='shared' if root in SHARED else 'program'
        try:
            expected=None
            if kind=='program':
                proof=(confirmed if root in PROGRAMS else old)[str(root)]
                assert digest(root/'INSTALL_MANIFEST.json')==proof['manifestSha256'],'Pinned manifest changed'
                config=read(root/'update-config.json');expected={k:config[k] for k in ('product','platform','channel','version','sourceSha')}
                verify_install(root,expected)
            files,dirs,retained=scan(root,kind)
            if kind=='program':
                manifest=read(root/'INSTALL_MANIFEST.json');members={x['path']:x for x in manifest['files']}
                for item in files:
                    item['sha256']=members[item['path']]['sha256'] if item['path'] in members else digest(root/item['path'])
            rows.append(dict(root=str(root),kind=kind,files=files,directories=dirs,retained=retained,expected=expected))
        except (OSError,ValueError,AssertionError) as e:skipped.append(dict(path=str(root),reason=str(e)))
    cache=APP/'data/runtime/updates';success=[];busy=False
    for folder in (cache/'handoffs').iterdir():
        if not folder.is_dir():continue
        result=read(folder/'result.json') if (folder/'result.json').exists() else {}
        if result.get('status')=='COMPLETE' and result.get('installed')==active['installed']:success.append(result)
        elif result.get('status') not in ('COMPLETE','FAILED_RETAINED'):busy=True
    if not success or busy:skipped.append(dict(path=str(cache/'downloads'),reason='NO_CURRENT_COMPLETE_OR_BUSY_HANDOFF'))
    else:
        for proof in pinned['downloads']:
            root=safe(Path(proof['path']));assert root.parent==cache/'downloads'
            if not root.exists():continue
            try:
                value=read(root/'VERIFIED.json')
                assert value['sha256']==proof['sha256'] and value['version']<=active['version']
                assert {p.name for p in root.iterdir()}=={'VERIFIED.json','studio-installer.exe'}
                assert digest(root/'studio-installer.exe')==value['sha256']
                files,dirs,retained=scan(root,'download');assert not retained
                for item in files:item['sha256']=digest(root/item['path'])
                rows.append(dict(root=str(root),kind='download',files=files,directories=dirs,retained=[],expected=None))
            except (OSError,ValueError,AssertionError) as e:skipped.append(dict(path=str(root),reason=str(e)))
    document=dict(format=1,authorization='User 2026-10-25: confirmed old test programs/shared contents and unneeded daily program residues; preserve review evidence, fixed baselines, daily current installation, all backups and unknowns',sourceCommit='1d2f4ff93dc4b7b68f6f60be403f4cf0dcd319f0',active=active,anchors=anchors(),protectedPaths=[str(p) for p in protected],roots=rows,skipped=skipped)
    with gzip.open(OUT/'PLAN.json.gz','wt',encoding='utf-8') as f:json.dump(document,f)
    write(OUT/'PLAN_SUMMARY.json',dict(roots=[dict(path=r['root'],kind=r['kind'],files=len(r['files']),bytes=sum(x['bytes'] for x in r['files']),retainedEntries=len(r['retained'])) for r in rows],skipped=skipped,planSha256=digest(OUT/'PLAN.json.gz')))
    print((OUT/'PLAN_SUMMARY.json').read_text(),flush=True)
def delete_file(path,item):
    with held(path,True) as (k,h,info):
        if info.attributes&0x10:return 'RETAIN_CHANGED_TYPE'
        size=info.sizeHigh<<32|info.sizeLow;modified=info.written.dwHighDateTime<<32|info.written.dwLowDateTime
        if size!=item['bytes'] or modified!=item['modified100']:return 'RETAIN_CHANGED_SINCE_PLAN'
        if 'sha256' in item:
            import _winapi
            copy=_winapi.DuplicateHandle(_winapi.GetCurrentProcess(),h,_winapi.GetCurrentProcess(),0,False,2)
            with os.fdopen(msvcrt.open_osfhandle(copy,os.O_RDONLY|os.O_BINARY),'rb') as f:
                if hashlib.file_digest(f,'sha256').hexdigest()!=item['sha256']:return 'RETAIN_CHANGED_BYTES'
        dispose(k,h)
    return 'REMOVED'
def apply():
    summary=read(OUT/'PLAN_SUMMARY.json');assert digest(OUT/'PLAN.json.gz')==summary['planSha256']
    with gzip.open(OUT/'PLAN.json.gz','rt',encoding='utf-8') as f:document=json.load(f)
    active,protected=protections();assert active==document['active']
    for a in document['anchors']:assert digest(Path(a['path']))==a['sha256'],'Protected anchor changed'
    results=[];global_count=0
    with ExitStack() as locks,gzip.open(OUT/'REMOVALS.jsonl.gz','at',encoding='utf-8') as log:
        daily_locked=False
        for entry in document['roots']:
            root=safe(entry['root']);kind=entry['kind']
            if kind=='download':assert root.parent==APP/'data/runtime/updates/downloads'
            else:assert root in PROGRAMS+SHARED+OLD_APP and not any(overlap(root,k) for k in protected)
            if root.is_relative_to(APP) and not daily_locked:
                from studio_update import exclusive
                locks.enter_context(exclusive(APP/'data/runtime/updates'));daily_locked=True
                assert read(APP/'launch-current.json')==active,'Active install changed; abort daily cleanup'
            if not root.exists():continue
            files={};children={}
            for item in entry['files']:parts=relative(item['path']);parent=Path(*parts).parent.as_posix();files.setdefault(parent,[]).append(item)
            for directory in entry['directories']:parts=relative(directory);parent=Path(*parts).parent.as_posix();children.setdefault(parent,[]).append(directory)
            count=freed=0;deferred=[]
            print('CLEANING '+str(root),flush=True)
            def visit(name):
                nonlocal count,freed
                here=root if name=='.' else root.joinpath(*relative(name))
                if not io_path(here).exists():return
                try:
                    with held(here):
                        for item in files.get(name,[]):
                            p=root.joinpath(*relative(item['path']))
                            try:status=delete_file(p,item)
                            except FileNotFoundError:status='ALREADY_ABSENT'
                            except (OSError,ValueError) as e:status='RETAIN_'+type(e).__name__
                            if status=='REMOVED':count+=1;freed+=item['bytes']
                            elif status!='ALREADY_ABSENT':deferred.append(dict(path=item['path'],status=status))
                            log.write(json.dumps(dict(root=str(root),path=item['path'],status=status,bytes=item['bytes']))+'\n')
                            if count and count%10000==0:print('REMOVED '+str(count)+' files from '+root.name,flush=True);log.flush()
                        for child in children.get(name,[]):visit(child)
                    try:
                        with held(here,True) as (k,h,info):
                            assert info.attributes&0x10;dispose(k,h)
                    except OSError:pass # nonempty directory and all unplanned files stay
                except (OSError,ValueError) as e:deferred.append(dict(path=name,status='RETAIN_DIRECTORY_'+type(e).__name__))
            with held_parents(root):visit('.')
            result=dict(root=str(root),kind=kind,removed=count,bytesFreed=freed,deferred=deferred,protectedEntries=entry['retained'],rootRetained=io_path(root).exists())
            results.append(result);write(OUT/'RESULT_IN_PROGRESS.json',dict(results=results));log.flush()
            print(json.dumps(dict(root=str(root),removed=count,bytesFreed=freed,deferred=len(deferred),protectedEntries=len(entry['retained']))),flush=True)
    for a in document['anchors']:assert digest(Path(a['path']))==a['sha256'],'Protected anchor changed after cleanup'
    write(OUT/'RESULT.json',dict(status='COMPLETE_WITH_EXPLICIT_RETAINS',results=results,filesRemoved=sum(x['removed'] for x in results),bytesFreed=sum(x['bytesFreed'] for x in results),protectedAnchorsUnchanged=True,backupsNeverTraversed=True,dailyCurrentInstallationUnchanged=True,skipped=document['skipped']))
    print('RESULT='+str(OUT/'RESULT.json'),flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['plan','apply']);a=p.parse_args()
    plan() if a.action=='plan' else apply()
