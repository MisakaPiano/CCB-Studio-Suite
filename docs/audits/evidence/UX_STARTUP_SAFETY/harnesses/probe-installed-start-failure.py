from pathlib import Path
import hashlib,json,subprocess,time,uuid
r=Path.cwd();app=r/'.local/windows-rc/ab-applications-3/4-0ae42d9b5e6d'
manifest=json.loads((app/'INSTALL_MANIFEST.json').read_text(encoding='utf-8'))
for item in manifest['files']:
    p=app/item['path']
    with p.open('rb') as f:assert p.stat().st_size==item['bytes'] and hashlib.file_digest(f,'sha256').hexdigest()==item['sha256']
out=r/'.local/windows-rc/evidence'/('installed-start-failure-'+uuid.uuid4().hex);out.mkdir()
data=out/'future-data';data.mkdir();(data/'preview-root.txt').write_bytes(b'CCBStudioSuite/WindowsStudio/v99');(data/'unique-original.bin').write_bytes(b'SYNTHETIC ORIGINAL')
runtime=out/'runtime';handoff=runtime/'updates/handoffs/probe';handoff.mkdir(parents=True)
config=json.loads((app/'update-config.json').read_text(encoding='utf-8'))
plan=dict(format=1,nonce='d'*64,dataRoot=str(data),destinationDataRoot=str(data),sourceDataVersion=3,targetDataVersion=3,approved=dict(version=config['version'],sourceSha=config['sourceSha']))
path=handoff/'plan.json';path.write_text(json.dumps(plan),encoding='utf-8')
inventory=lambda:{p.relative_to(data).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in data.rglob('*') if p.is_file()}
before=inventory();start=time.monotonic()
with (out/'stdout.log').open('wb') as stdout,(out/'stderr.log').open('wb') as stderr:
    child=subprocess.Popen([str(app/'CCBStudioSuitePreview.exe'),'--studio-root',str(data),'--runtime-root',str(runtime),'--update-handoff',str(path)],stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
    report=dict(test='ACTUAL_INSTALLED_B_UNSUPPORTED_ROOT_START_FAILURE',pid=child.pid,app=str(app),dataRoot=str(data),sourceSha=config['sourceSha'],manifestFiles=len(manifest['files']),before=before,status='PENDING')
    (out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print('EVIDENCE='+str(out),flush=True)
    deadline=time.monotonic()+35
    while not (handoff/'B_FAILED.json').exists() and child.poll() is None and time.monotonic()<deadline:time.sleep(.1)
    failed=json.loads((handoff/'B_FAILED.json').read_text(encoding='utf-8')) if (handoff/'B_FAILED.json').exists() else None
    after=inventory();report.update(seconds=time.monotonic()-start,after=after,failure=failed,exitCode=child.poll(),status='PASS' if failed and failed.get('reason')=='UNSUPPORTED_VERSION' and before==after and not (handoff/'B_VERIFIED.json').exists() else 'FAILED',applicationUpdate='NOT TESTED; installed B entry-point only',window='Failure dialog may remain; no forced termination')
    (out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report,indent=2))
raise SystemExit(0 if report['status']=='PASS' else 1)
