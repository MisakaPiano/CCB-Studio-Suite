"""Standalone program-only repair, run from a newly extracted trusted installer.
Never executes/imports the damaged program, opens its vault, rewrites data, or
claims data verification. A repaired program is published only after full
package comparison and an entrypoint CAS. The original directory is retained.
"""
from pathlib import Path
from contextlib import ExitStack,contextmanager
import argparse,hashlib,json,os,shutil,uuid,msvcrt
from install_ops import no_reparse,verify_install,atomic_json
from apply_update import read_entrypoint,create_launcher,EntrypointRecoveryRequired
from studio_update import Installed,UpdateRejected

@contextmanager
def workspace_idle(data):
    with ExitStack() as stack:
        for relative in ('.owner.lock','assets/.owner.lock'):
            path=no_reparse(data/relative)
            stream=stack.enter_context(path.open('r+b'))
            try:msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
            except OSError as error:raise UpdateRejected('Close Studio before program repair; no process was terminated') from error
            stack.callback(lambda f=stream:(f.seek(0),msvcrt.locking(f.fileno(),msvcrt.LK_UNLCK,1)))
        yield

def repair_program(package,target,launcher=create_launcher,disk_usage=shutil.disk_usage):
    package=no_reparse(package);target=no_reparse(target);parent=no_reparse(target.parent)
    if not target.is_dir():raise UpdateRejected('Select the existing active program directory')
    config=json.loads((package/'update-config.json').read_text(encoding='utf-8'))
    identity=Installed.load(package)
    expected={k:config[k] for k in ['product','platform','channel','version','sourceSha']}
    reference=verify_install(package,expected)
    entry=read_entrypoint(parent)
    if entry is None or no_reparse(Path(entry['installed']))!=target:
        raise UpdateRejected('Selected program is not the registered default installation')
    if config['version']<entry['version']:raise UpdateRejected('Repair package cannot downgrade the selected program')
    data=no_reparse(Path(entry['data']));runtime=no_reparse(Path(entry['runtime']))
    final=parent/(str(config['version'])+'-'+config['sourceSha'][:12]+'.repair-'+uuid.uuid4().hex)
    for program in (package,target,final):
        for protected in (data,runtime):
            if protected==program or protected.is_relative_to(program) or program.is_relative_to(protected):
                raise UpdateRejected('Program repair cannot overlap data/runtime')
    marker=no_reparse(data/'preview-root.txt')
    # Do not migrate/initialize or open a workspace from program-repair mode.
    if not marker.is_file() or marker.stat().st_size>1024 or marker.read_bytes()!=('CCBStudioSuite/WindowsStudio/v'+str(identity.reader)).encode():
        raise UpdateRejected('Repair package must read the existing data format; use a checked migration separately')
    manifest=json.loads((package/'INSTALL_MANIFEST.json').read_text(encoding='utf-8'))
    units=lambda value:len(str(value).encode('utf-16-le'))//2
    if any(units(final/item['path'])>259 or units((final/item['path']).parent)>247 for item in manifest['files']):
        raise UpdateRejected('Choose a shorter program location for repair')
    if disk_usage(parent).free<reference['bytes']+64*1024**2:raise UpdateRejected('Insufficient space for retained-original program repair')
    records=parent/'.repair-receipts';no_reparse(records);records.mkdir(exist_ok=True)
    journal=records/(uuid.uuid4().hex+'.json')
    record=dict(format=1,status='PREPARING',mode='PROGRAM_ONLY_REPAIR',package=str(package),original=str(target),replacement=str(final),
        originalRetained=True,dataTouched=False,credentialsOpened=False,dataVerified=False,referenceManifestSha256=reference['manifestSha256'])
    atomic_json(journal,record)
    published=False
    try:
        with workspace_idle(data):
            # Do not copy unknown/damaged original files. Keep them at their
            # original path; populate only a fresh sibling from package bytes.
            shutil.copytree(package,final)
            output=verify_install(final,expected)
            if output!=reference:raise UpdateRejected('Repair copy differs from package')
            # Recheck source after copy to reject a concurrently changed reference.
            if verify_install(package,expected)!=reference:raise UpdateRejected('Repair package changed during copy')
            record.update(status='PROGRAM_VERIFIED',verification=output);atomic_json(journal,record)
            backup=records/(journal.stem+'-entrypoint-before');backup.mkdir()
            for name in ('launch-current.json','CCBStudioLauncher.exe','Launch-CCB-Studio.lnk'):
                if (parent/name).is_file():shutil.copyfile(parent/name,backup/name)
            link=launcher(final,data,runtime,expected_current=entry,repair_manifest_sha256=reference['manifestSha256'])
            published=True
            record.update(status='PROGRAM_REPAIRED_DATA_NOT_OPENED',launcher=str(link))
            try:atomic_json(journal,record)
            except OSError:record['status']='PROGRAM_REPAIRED_RECEIPT_INCOMPLETE'
        return final,journal,record
    except Exception as error:
        status='PROGRAM_REPAIRED_RECEIPT_INCOMPLETE' if published else 'ENTRYPOINT_RECOVERY_REQUIRED' if isinstance(error,EntrypointRecoveryRequired) else 'FAILED_RETAINED'
        record.update(status=status,reason=type(error).__name__)
        error.repair_status=status
        atomic_json(journal,record);raise

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--package',required=True,type=Path);parser.add_argument('--target',required=True,type=Path)
    args=parser.parse_args()
    # The external installer runs its own freshly extracted repair tool.
    if args.package.resolve()!=Path(__file__).resolve().parents[2]:
        raise SystemExit('Repair tool must run from the selected installer package')
    result=args.package.with_name(args.package.name+'.repair-result.json')
    try:
        final,journal,record=repair_program(args.package,args.target)
        atomic_json(result,dict(status=record['status'],journal=str(journal),program=str(final),dataTouched=False,dataVerified=False))
    except Exception as error:
        atomic_json(result,dict(status=getattr(error,'repair_status','FAILED_RETAINED'),errorClass=type(error).__name__,reason=str(error)[:240],dataTouched=False))
        raise SystemExit(1)

