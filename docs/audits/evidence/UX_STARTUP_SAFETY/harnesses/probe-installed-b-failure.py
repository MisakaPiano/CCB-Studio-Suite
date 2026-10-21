"""Real NSIS + real B failure. Harness fault/parent termination explicitly separated."""
from pathlib import Path
import hashlib,json,os,shutil,subprocess,time,uuid
r=Path.cwd();fixed=r/'.local/windows-rc/ab-applications-3/3-251dc3495099';source=r/'.local/windows-rc/ab-data-3'
out=r/'.local/windows-rc'/('bf-'+uuid.uuid4().hex[:8]);out.mkdir()
a=out/'applications/3-251dc3495099';shutil.copytree(fixed,a)
manifest=json.loads((a/'INSTALL_MANIFEST.json').read_text(encoding='utf-8'))
def verify_a():
    for item in manifest['files']:
        with (a/item['path']).open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==item['sha256']
verify_a();data=out/'synthetic-data';shutil.copytree(source,data,ignore=shutil.ignore_patterns('backups','exports'))
runtime=out/'runtime';cache=runtime/'updates';cache.mkdir(parents=True)
launcher=a.parent/'Launch-CCB-Studio.lnk';launcher.write_bytes(b'SYNTHETIC OLD LAUNCHER SENTINEL - NEVER EXECUTED');launcher_before=hashlib.sha256(launcher.read_bytes()).hexdigest()
def inventory():
    values={}
    for p in data.rglob('*'):
        if p.is_file() and p.name!='.owner.lock':
            with p.open('rb') as f:values[p.relative_to(data).as_posix()]=hashlib.file_digest(f,'sha256').hexdigest()
    return values
print('EVIDENCE='+str(out),flush=True)
python=r/'.local/windows-rc/updater-runtime-v7/python.exe'
download=subprocess.run([str(python),str(r/'tools/update-runtime/studio_update.py'),'--package',str(a),'--cache',str(cache),'--download'],capture_output=True,timeout=45,creationflags=subprocess.CREATE_NO_WINDOW)
approved=json.loads(download.stdout);(out/'download.json').write_text(json.dumps(approved,indent=2),encoding='utf-8');assert approved['status']=='DOWNLOADED_VERIFIED'
nonce=uuid.uuid4().hex+uuid.uuid4().hex;folder=cache/'handoffs'/nonce;folder.mkdir(parents=True)
prior=json.loads((r/'.local/windows-rc/ab-runtime-3/updates/handoffs/6df798a8-8c60-4e50-959a-9480299eaab6/plan.json').read_text(encoding='utf-8'))
child=None;helper=None;report=dict(status='FAILED',kind='ACTUAL_NSIS_INSTALL_AND_B_START_FAILURE',aSourceSha=manifest['sourceSha'],bSourceSha=approved['sourceSha'],installerSha256=approved['sha256'],guiConfirmation='NOT TESTED; harness supplies approved plan',parentClose='HARNESS CRASH INJECTION; not normal application drain')
try:
    with (out/'a-stdout.log').open('wb') as stdout,(out/'a-stderr.log').open('wb') as stderr:
        child=subprocess.Popen([str(a/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime)],stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    plan=dict(prior,nonce=nonce,parentPid=child.pid,installed=str(a),cache=str(cache),runtimeRoot=str(runtime),dataRoot=str(data),destinationDataRoot=str(data.with_name(data.name+'-format3-'+nonce[:12])),approved=approved)
    path=folder/'plan.json';path.write_text(json.dumps(plan),encoding='utf-8')
    with (out/'helper-stdout.log').open('wb') as stdout,(out/'helper-stderr.log').open('wb') as stderr:
        helper=subprocess.Popen([str(python),str(r/'tools/update-runtime/apply_update.py'),'--handoff',str(path)],stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    deadline=time.monotonic()+30
    while time.monotonic()<deadline:
        if (folder/'result.json').exists() and json.loads((folder/'result.json').read_text(encoding='utf-8'))['status']=='READY_TO_DRAIN':break
        assert helper.poll() is None,'Helper ended before drain';time.sleep(.1)
    else:raise AssertionError('No drain boundary')
    # Deterministic isolated fault: future format appears between A and B. No real root.
    original_marker=(data/'preview-root.txt').read_bytes();(data/'preview-root.txt').write_bytes(b'CCBStudioSuite/WindowsStudio/v99')
    report['fault']=dict(file='preview-root.txt',beforeSha256=hashlib.sha256(original_marker).hexdigest(),afterSha256=hashlib.sha256((data/'preview-root.txt').read_bytes()).hexdigest(),injectedBy='test harness before A process exit')
    before=inventory();child.terminate();child.wait(10)
    start=time.monotonic();helper.wait(90)
    result=json.loads((folder/'result.json').read_text(encoding='utf-8'));failure=json.loads((folder/'B_FAILED.json').read_text(encoding='utf-8'))
    final=Path(result['installed']);after=inventory();verify_a()
    journal=json.loads(Path(result['installJournal']).read_text(encoding='utf-8'))
    report.update(seconds=time.monotonic()-start,result=result,bFailure=failure,helperExit=helper.returncode,installJournalStatus=journal['status'],aFilesVerified=len(manifest['files']),before=before,after=after,oldLauncherUnchanged=hashlib.sha256(launcher.read_bytes()).hexdigest()==launcher_before,bInstalledRoot=str(final))
    assert result['status']=='FAILED_RETAINED' and journal['status']=='INSTALLED_NOT_STARTED'
    assert failure['status']=='FAILED' and before==after and report['oldLauncherUnchanged']
    assert not (folder/'B_VERIFIED.json').exists() and not Path(plan['destinationDataRoot']).exists()
    report['status']='PASS'
finally:
    if helper and helper.poll() is None:helper.terminate();helper.wait(10)
    if child and child.poll() is None:child.terminate();child.wait(10)
    (out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k not in ['before','after']},indent=2),flush=True)
