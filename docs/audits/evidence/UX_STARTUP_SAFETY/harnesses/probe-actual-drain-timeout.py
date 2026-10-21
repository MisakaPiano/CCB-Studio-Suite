"""Negative real helper/A-process test. Never dispatch a request or force-drain a product writer."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,time,uuid
r=Path.cwd();a=r/'.local/windows-rc/ab-applications-3/3-251dc3495099'
source=r/'.local/windows-rc/ab-data-3'
out=r/'.local/windows-rc/evidence'/('actual-drain-timeout-'+uuid.uuid4().hex);out.mkdir()
data=out/'synthetic-data';runtime=out/'runtime';cache=runtime/'updates';cache.mkdir(parents=True)
for item in json.loads((a/'INSTALL_MANIFEST.json').read_text(encoding='utf-8'))['files']:
    with (a/item['path']).open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==item['sha256']
assert (source/'preview-root.txt').read_text()=='CCBStudioSuite/WindowsStudio/v2'
shutil.copytree(source,data,ignore=shutil.ignore_patterns('backups','exports'))
def inventory():
    values={}
    for p in data.rglob('*'):
        if p.is_file() and p.name!='.owner.lock':
            with p.open('rb') as f:values[p.relative_to(data).as_posix()]=hashlib.file_digest(f,'sha256').hexdigest()
    return values
before=inventory();print('EVIDENCE='+str(out),flush=True)
download=subprocess.run([os.fspath(r/'.local/windows-rc/updater-runtime-v7/python.exe'),str(r/'tools/update-runtime/studio_update.py'),'--package',str(a),'--cache',str(cache),'--download'],capture_output=True,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
approved=json.loads(download.stdout);(out/'download.json').write_text(json.dumps(approved,indent=2),encoding='utf-8');assert approved['status']=='DOWNLOADED_VERIFIED'
nonce=uuid.uuid4().hex+uuid.uuid4().hex;folder=cache/'handoffs'/nonce;folder.mkdir(parents=True)
prior=json.loads((r/'.local/windows-rc/ab-runtime-3/updates/handoffs/6df798a8-8c60-4e50-959a-9480299eaab6/plan.json').read_text(encoding='utf-8'))
child=None;helper=None;report=dict(status='FAILED',kind='ACTUAL_A_PROCESS_AND_HELPER_DRAIN_TIMEOUT',sourceSha=prior['approved']['sourceSha'],guiConfirmation='NOT TESTED; harness supplies isolated approved plan',before=before)
try:
    with (out/'a-stdout.log').open('wb') as stdout,(out/'a-stderr.log').open('wb') as stderr:
        child=subprocess.Popen([str(a/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime)],stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    plan=dict(prior,nonce=nonce,parentPid=child.pid,installed=str(a),cache=str(cache),runtimeRoot=str(runtime),dataRoot=str(data),destinationDataRoot=str(data.with_name(data.name+'-format3-'+nonce[:12])),approved=approved)
    path=folder/'plan.json';path.write_text(json.dumps(plan),encoding='utf-8')
    with (out/'helper-stdout.log').open('wb') as stdout,(out/'helper-stderr.log').open('wb') as stderr:
        helper=subprocess.Popen([os.fspath(r/'.local/windows-rc/updater-runtime-v7/python.exe'),str(r/'tools/update-runtime/apply_update.py'),'--handoff',str(path)],stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    start=time.monotonic();deadline=start+145;last=None
    while helper.poll() is None and time.monotonic()<deadline:
        if (folder/'result.json').exists():
            status=json.loads((folder/'result.json').read_text(encoding='utf-8'))['status']
            if status!=last:print('STATUS='+status,flush=True);last=status
        time.sleep(.25)
    assert helper.poll() is not None,'Helper did not respect bounded drain timeout'
    result=json.loads((folder/'result.json').read_text(encoding='utf-8'));after=inventory()
    report.update(seconds=time.monotonic()-start,result=result,parentStillRunning=child.poll() is None,helperExit=helper.returncode,after=after,installJournals=len(list(cache.glob('install-*.json'))))
    assert result['status']=='FAILED_RETAINED' and 'drain timed out' in result['reason']
    assert child.poll() is None and before==after and not list(cache.glob('install-*.json')) and not Path(plan['destinationDataRoot']).exists()
    import msvcrt
    with (data/'.owner.lock').open('r+b') as lock:
        try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        except OSError:report['parentRootLockStillHeld']=True
        else:msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1);raise AssertionError('Parent root lock not held after timeout')
    report['status']='PASS'
finally:
    # Only the Popen handles created by this harness, with isolated synthetic roots.
    # This cleanup is not product updater behavior and not a successful app close.
    if helper and helper.poll() is None:helper.terminate();helper.wait(10)
    if child and child.poll() is None:child.terminate();child.wait(10);report['harnessCleanup']='Owned idle A fixture terminated after assertions; product helper never killed parent'
    (out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k not in ['before','after']},indent=2),flush=True)
