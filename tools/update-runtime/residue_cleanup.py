"""Best-effort retirement of explicitly enrolled updater files, never a disk sweeper.

Only install-time output of a verified installer or explicitly named download output
can be enrolled. Legacy directories are not adopted by scanning. Receipts/journals
are local integrity records, not a new publisher trust anchor or a hostile-user sandbox.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, time, uuid
from owned_files import relative, held, held_parents, delete_matching, prune_empty
from install_ops import no_reparse, atomic_json, verify_install
from studio_update import UpdateRejected, exclusive

ROLLBACK_SECONDS=0 # User policy: retire immediately after COMPLETE, no time-based rollback copy.
FAILED_SECONDS=0 # Owned failed output may retire after a later verified success.
MAX_RECEIPTS=1000

def read_json(path, limit=4*1024**2):
    path=no_reparse(path)
    if path.stat().st_size>limit:raise UpdateRejected('Oversized cleanup record')
    return json.loads(path.read_text(encoding='utf-8-sig'))

def file_identity(path):
    path=no_reparse(path)
    with path.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    return dict(path=path.name,bytes=path.stat().st_size,sha256=digest)

def identity(expected):
    value={k:expected[k] for k in ('product','platform','channel','version','sourceSha')}
    if (value['product']!='ccb-studio-suite' or value['platform']!='windows-x86_64'
        or value['channel'] not in ('internal','preview','stable') or type(value['version']) is not int or value['version']<1
        or not re.fullmatch('[0-9a-f]{40}',value['sourceSha'])):raise UpdateRejected('Invalid cleanup identity')
    return value

def write_receipt(cache, install_root, area, base, files, expected, proof, now=None):
    cache=no_reparse(cache);install_root=no_reparse(install_root);base=no_reparse(base)
    anchor=install_root if area=='program' else cache/'downloads'
    if base.parent!=anchor or not base.is_dir():raise UpdateRejected('Owned directory is outside its exact parent')
    folder=cache/'cleanup/owned';no_reparse(folder);folder.mkdir(parents=True,exist_ok=True)
    path=folder/(uuid.uuid4().hex+'.json')
    atomic_json(path,dict(format=1,installRoot=str(install_root),cache=str(cache),area=area,base=base.name,
        expected=identity(expected),files=files,proof=proof,createdAt=time.time() if now is None else now))
    return path

def enroll_program(cache, install_root, directory, expected, installer_sha256, journal, now=None,pinned_manifest=None):
    """Called while the authenticated installer handle is still held, after it exits.
    Even on failure only matching manifest members are enrolled; partial/unknown bytes
    remain untouched. No future scan of an arbitrary legacy manifest grants ownership.
    """
    directory=no_reparse(directory);manifest=directory/'INSTALL_MANIFEST.json'
    if not manifest.is_file():return None
    if pinned_manifest is not None and file_identity(manifest)['sha256']!=pinned_manifest:raise UpdateRejected('Previous manifest differs from its verified install')
    value=read_json(manifest);expected=identity(expected)
    if any(value.get(k)!=v for k,v in expected.items()) or value.get('format')!=1:return None
    if not re.fullmatch('[0-9a-f]{64}',installer_sha256):raise UpdateRejected('Missing authenticated installer digest')
    entries=value.get('files')
    if not isinstance(entries,list) or not 1<=len(entries)<=20000:return None
    files=[];seen=set()
    for item in entries:
        parts=relative(item['path']);folded=item['path'].casefold()
        if folded in seen:raise UpdateRejected('Duplicate owned filename')
        seen.add(folded);p=no_reparse(directory.joinpath(*parts))
        if not p.is_file():continue
        actual=file_identity(p)
        if actual['bytes']==item.get('bytes') and actual['sha256']==item.get('sha256'):
            files.append(dict(actual,path=item['path']))
    for name in ('INSTALL_MANIFEST.json','INSTALL_COMPLETE','Uninstall.exe'):
        p=no_reparse(directory/name)
        # Legacy journals pin the manifest, not the generated NSIS uninstaller.
        # Its basename alone cannot prove the bytes still belong to the installer.
        if pinned_manifest is not None:
            if name=='Uninstall.exe':continue
            if name=='INSTALL_COMPLETE' and (not p.is_file() or p.stat().st_size>200 or p.read_text(encoding='utf-8')!=str(expected['version'])+':'+expected['sourceSha']):continue
        if p.is_file() and name.casefold() not in seen:files.append(file_identity(p))
    return write_receipt(cache,install_root,'program',directory,files,expected,
        dict(kind='VERIFIED_PRIOR_INSTALLATION' if pinned_manifest else 'VERIFIED_INSTALLER_OUTPUT',installerSha256=installer_sha256,
             manifestSha256=file_identity(manifest)['sha256'],installJournal=Path(journal).name),now)

def move_program_receipt(path, final):
    if path is not None:
        value=read_json(path);value['base']=Path(final).name;atomic_json(path,value)

def enroll_download(cache, install_root, directory, expected, installer_sha256, verified, now=None):
    directory=no_reparse(directory)
    files=[file_identity(directory/name) for name in ('studio-installer.exe','VERIFIED.json') if (directory/name).is_file()]
    return write_receipt(cache,install_root,'download',directory,files,expected,
        dict(kind='TUF_VERIFIED_DOWNLOAD' if verified else 'OWNED_DOWNLOAD_FAILURE',installerSha256=installer_sha256),now)

def protected_paths(install_root, data, runtime):
    protected=[no_reparse(data),no_reparse(runtime)]
    for name in ('data','plugins','backups','source','.git'):protected.append(install_root/name)
    record=install_root/'ACTIVE_INSTALLATION.json'
    if record.exists():
        value=read_json(record,65536)
        if value.get('doNotCleanReferencedDirectories') is not False:
            for key in ('program','workspace','runtime','retainedBackupWorkspace','retainedBackupRuntime'):
                if key in value:
                    if not isinstance(value[key],str) or not Path(value[key]).is_absolute():raise UpdateRejected('Invalid protected installation record')
                    protected.append(no_reparse(value[key]))
    return protected

def overlaps(path, protected):return any(path==p or path.is_relative_to(p) or p.is_relative_to(path) for p in protected)

def event(cache,kind,**fields):
    folder=no_reparse(cache/'cleanup');folder.mkdir(parents=True,exist_ok=True)
    log=no_reparse(folder/'events.jsonl')
    with log.open('a',encoding='utf-8') as stream:
        stream.write(json.dumps(dict(at=time.time(),event=kind,**fields),ensure_ascii=True)+'\n');stream.flush();os.fsync(stream.fileno())

def completed_updates(cache,install_root):
    """Keep handoff proof in place. Return bounded identity projections, never log snapshots."""
    completed=[];blocked=set();pending_downloads=set()
    folders=cache/'handoffs'
    if not folders.exists():return completed,blocked,pending_downloads
    paths=sorted(folders.iterdir())
    if len(paths)>MAX_RECEIPTS:raise UpdateRejected('Handoff inspection bound reached; retained')
    for folder in paths:
        no_reparse(folder)
        if not folder.is_dir():continue
        plan_path=folder/'plan.json';result_path=folder/'result.json'
        if not plan_path.exists():continue
        plan=read_json(plan_path);result=read_json(result_path) if result_path.exists() else {}
        if plan.get('format')!=1 or not re.fullmatch('[0-9a-f]{64}',plan.get('nonce','')):raise UpdateRejected('Invalid handoff identity')
        if Path(plan.get('cache','')).absolute()!=cache:raise UpdateRejected('Cleanup handoff cache mismatch')
        verified_complete=(result.get('status')=='COMPLETE' and result.get('nonce')==plan.get('nonce') and result.get('verification')=='B_PROGRAM_DATA_AND_CREDENTIAL_SNAPSHOT_MATCH')
        if not verified_complete and result.get('status')!='FAILED_RETAINED':
            installer=plan.get('approved',{}).get('installer')
            if installer:pending_downloads.add(no_reparse(Path(installer)).parent)
        old=no_reparse(plan['installed']);new=no_reparse(result['installed']) if result.get('installed') else None
        if old.parent!=install_root or (new is not None and new.parent!=install_root):continue
        if not verified_complete:
            if result.get('status')!='FAILED_RETAINED':
                blocked.add(old.name)
                if new:blocked.add(new.name)
                approved=plan.get('approved',{})
                if 'version' in approved and 'sourceSha' in approved:blocked.add(str(approved['version'])+'-'+approved['sourceSha'][:12])
            continue
        approved=plan['approved'];journal=no_reparse(result['installJournal'])
        if journal.parent!=cache:raise UpdateRejected('Cleanup journal outside cache')
        installed=read_json(journal)
        if Path(installed.get('target','')).absolute()!=new or installed.get('expected',{}).get('sourceSha')!=approved.get('sourceSha') or installed.get('expected',{}).get('version')!=approved.get('version'):
            raise UpdateRejected('Cleanup completion identity mismatch')
        completed.append(dict(old=old.name,new=new.name,approved=approved,journal=journal,installation=installed,nonce=plan['nonce'],completedAt=result_path.stat().st_mtime))
    return completed,blocked,pending_downloads

def run(package,cache,data,runtime,startup_token=None,now=None,after_delete=lambda _:None):
    now=time.time() if now is None else now
    package=no_reparse(package);cache=no_reparse(cache);data=no_reparse(data);runtime=no_reparse(runtime);root=package.parent
    if cache!=runtime/'updates' or overlaps(cache,[package]) or overlaps(root,[data]) and data==root:
        raise UpdateRejected('Cleanup roots overlap')
    descriptor=read_json(root/'launch-current.json',16384)
    if any(Path(descriptor.get(k,'')).absolute()!=v for k,v in [('installed',package),('data',data),('runtime',runtime)]):
        return dict(status='RETAINED_SECONDARY_WORKSPACE',removed=0)
    protections=protected_paths(root,data,runtime)
    with exclusive(cache):
        completed,blocked,pending_downloads=completed_updates(cache,root)
        current=[c for c in completed if c['new']==package.name]
        if not current:return dict(status='RETAINED_NO_COMPLETE_HANDOFF',removed=0)
        folder=no_reparse(cache/'cleanup');folder.mkdir(exist_ok=True)
        state_path=folder/'state.json';state=read_json(state_path) if state_path.exists() else dict(format=1,current=package.name,starts=[],resources={})
        if state.get('format')!=1:raise UpdateRejected('Unsupported cleanup state')
        if state.get('current')!=package.name:state.update(current=package.name,starts=[])
        if startup_token is not None:
            if not re.fullmatch('[0-9a-f-]{36}',startup_token):raise UpdateRejected('Invalid startup token')
            state['starts']=(state.get('starts',[])+[startup_token] if startup_token not in state.get('starts',[]) else state['starts'])[-2:]
        owned=folder/'owned';owned.mkdir(exist_ok=True);no_reparse(owned)
        paths=sorted(owned.glob('*.json'))
        if len(paths)>MAX_RECEIPTS:raise UpdateRejected('Cleanup receipt bound reached; retained')
        # First upgrade into this feature can use an older helper. Adopt ONLY the running
        # new package, tied to that helper's pinned manifest digest, never its predecessor.
        if not any(read_json(p).get('area')=='program' and read_json(p).get('base')==package.name for p in paths):
            c=current[-1];expected=c['installation']['expected'];check=verify_install(package,expected)
            if check['manifestSha256']!=c['installation'].get('verification',{}).get('manifestSha256'):raise UpdateRejected('Current manifest differs from verified install')
            receipt=enroll_program(cache,root,package,expected,c['approved']['sha256'],c['journal'],now,pinned_manifest=check['manifestSha256'])
            if receipt:paths.append(receipt)
        # Retire only the active predecessor of THIS successful update. Older historical
        # directories are never enrolled by enumeration. This also supports an old A helper.
        predecessor=current[-1]['old']
        if predecessor not in blocked and not overlaps(root/predecessor,protections):
            known=any(read_json(p).get('area')=='program' and read_json(p).get('base')==predecessor for p in paths)
            prior=next((c for c in completed if c['new']==predecessor),None)
            if not known and prior is not None and (root/predecessor).is_dir():
                pinned=prior['installation'].get('verification',{}).get('manifestSha256')
                if pinned:
                    receipt=enroll_program(cache,root,root/predecessor,prior['installation']['expected'],prior['approved']['sha256'],prior['journal'],now,pinned_manifest=pinned)
                    if receipt:paths.append(receipt)
        atomic_json(state_path,state)
        removed=0;deferred=0
        for path in paths[:MAX_RECEIPTS]:
            receipt=read_json(path)
            if receipt.get('format')!=1 or receipt.get('installRoot')!=str(root) or receipt.get('cache')!=str(cache):raise UpdateRejected('Cleanup receipt root mismatch')
            expected=identity(receipt['expected']);area=receipt['area'];name=receipt['base'];parts=relative(name)
            if len(parts)!=1 or area not in ('program','download'):raise UpdateRejected('Invalid cleanup area')
            version_name=str(expected['version'])+'-'+expected['sourceSha'][:12]
            if area=='program' and not (name==version_name or re.fullmatch(re.escape(version_name)+r'\.[0-9a-f]{32}\.installing',name)):raise UpdateRejected('Invalid owned program basename')
            if area=='download' and not re.fullmatch(r'[0-9a-f-]{36}(?:\.incomplete)?',name):raise UpdateRejected('Invalid owned download basename')
            base=no_reparse((root if area=='program' else cache/'downloads')/name)
            files=receipt['files'];seen=set()
            if not isinstance(files,list) or not 0<=len(files)<=20003:raise UpdateRejected('Invalid cleanup file inventory')
            for item in files:
                relative(item['path']);folded=item['path'].casefold()
                if folded in seen or type(item.get('bytes')) is not int or not 0<=item['bytes']<=1024**3 or not re.fullmatch('[0-9a-f]{64}',item.get('sha256','')):raise UpdateRejected('Invalid cleanup file identity')
                seen.add(folded)
                if area=='download' and item['path'] not in ('studio-installer.exe','VERIFIED.json'):raise UpdateRejected('Unowned download member')
            if area=='program' and (base==package or any(name==b or name.startswith(b+'.') for b in blocked) or overlaps(base,protections)):continue
            proof=receipt['proof']
            if proof.get('kind') not in ('VERIFIED_INSTALLER_OUTPUT','VERIFIED_PRIOR_INSTALLATION','TUF_VERIFIED_DOWNLOAD','OWNED_DOWNLOAD_FAILURE'):raise UpdateRejected('Unsupported ownership proof')
            if not re.fullmatch('[0-9a-f]{64}',proof.get('installerSha256','')):raise UpdateRejected('Missing source proof')
            matching=[c for c in completed if all(c['installation']['expected'].get(k)==expected[k] for k in ('product','platform','channel','sourceSha','version')) and c['approved'].get('sha256')==proof['installerSha256']]
            if area=='download':
                if base in pending_downloads:continue
                # Verified candidates awaiting installation are not failed output.
                # Only completion of that exact target releases them.
                if proof['kind']=='TUF_VERIFIED_DOWNLOAD' and not matching:continue
                if proof['kind']=='OWNED_DOWNLOAD_FAILURE' and not matching:
                    # A different success must be newer than the failed attempt;
                    # an old A->B completion must not collect a fresh C attempt.
                    if not any(c['completedAt']>=receipt['createdAt'] for c in completed):continue
            if area=='program' and not any(c['old']==version_name or name.endswith('.installing') for c in completed):continue
            resource=state['resources'].setdefault(path.stem,dict(retiredAt=now))
            failed=name.endswith('.installing') or proof.get('kind')=='OWNED_DOWNLOAD_FAILURE'
            if failed and not any(c['completedAt']>=receipt['createdAt'] for c in completed):continue
            if failed and now-receipt['createdAt']<FAILED_SECONDS:continue
            atomic_json(state_path,state) # Persist retirement before the first deletion.
            if not base.exists():continue
            if area=='program':
                # Keep the whole fallback intact while its executable is in use, rather
                # than removing its unlocked JARs around a running older process.
                busy=False
                for item in files:
                    executable=base.joinpath(*relative(item['path']))
                    if executable.suffix.lower() not in ('.exe','.dll','.pyd') or not executable.exists():continue
                    try:
                        with held_parents(executable),held(executable,True):pass
                    except OSError as error:
                        deferred+=1;event(cache,'RESOURCE_BUSY',receipt=path.name,reason=type(error).__name__);busy=True;break
                if busy:continue
            statuses={};count=0
            for item in files:
                target=no_reparse(base.joinpath(*relative(item['path'])))
                if not target.exists():continue
                if overlaps(target,protections) and area=='program':continue
                # User/plugin/backup areas stay protected even if a future package shipped defaults.
                if area=='program' and any(p.casefold() in ('plugins','backups','userdata','secrets','.git','source') for p in relative(item['path'])):continue
                try:status=delete_matching(target,item)
                except (OSError,ValueError) as error:status='RETRY_'+type(error).__name__;deferred+=1
                statuses[status]=statuses.get(status,0)+1
                if status=='REMOVED':removed+=1;count+=1;after_delete(target)
            prune_empty(base,files)
            event(cache,'RESOURCE_PASS',receipt=path.name,area=area,base=name,counts=statuses,retentionSeconds=FAILED_SECONDS if failed else ROLLBACK_SECONDS if area=='program' else 0)
        atomic_json(state_path,state)
        event(cache,'PASS_COMPLETE',removed=removed,deferred=deferred)
        return dict(status='DEFERRED' if deferred else 'COMPLETE',removed=removed,deferred=deferred)

def main():
    p=argparse.ArgumentParser();p.add_argument('--package',required=True,type=Path);p.add_argument('--cache',required=True,type=Path);p.add_argument('--data',required=True,type=Path);p.add_argument('--runtime',required=True,type=Path);p.add_argument('--startup-token')
    a=p.parse_args()
    try:
        for attempt in range(4):
            try:value=run(a.package,a.cache,a.data,a.runtime,a.startup_token)
            except OSError as error:value=dict(status='DEFERRED',reason=type(error).__name__)
            except UpdateRejected as error:
                if str(error)!='Another update operation owns this cache':raise
                value=dict(status='DEFERRED',reason='CACHE_BUSY')
            if value.get('status')!='DEFERRED':break
            if attempt<3:time.sleep(2**attempt)
    except Exception as error:
        value=dict(status='DEFERRED',reason=type(error).__name__)
        try:event(a.cache,'PASS_DEFERRED',reason=type(error).__name__)
        except Exception:pass
    print(json.dumps(value));return 0 # Cleanup never rewrites update success/failure.

if __name__=='__main__':raise SystemExit(main())
