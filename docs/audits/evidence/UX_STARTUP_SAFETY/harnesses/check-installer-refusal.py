from pathlib import Path
import hashlib,json,subprocess,time,uuid
r=Path.cwd();build=r/'.local/windows-rc/ab-build-b-3';exe=build/'studio-installer.exe'
receipt=json.loads((build/'BUILD_RECEIPT.json').read_text(encoding='utf-8'))
with exe.open('rb') as f:assert hashlib.file_digest(f,'sha256').hexdigest()==receipt['sha256']
out=r/'.local/windows-rc/evidence'/('installer-refusal-'+uuid.uuid4().hex);out.mkdir()
target=out/'existing';target.mkdir();(target/'unique-original.bin').write_bytes(b'SYNTHETIC UNIQUE DATA MUST REMAIN')
before={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in target.iterdir()}
start=time.monotonic();result=subprocess.run([str(exe),'/S','/D='+str(target)],capture_output=True,timeout=30,creationflags=subprocess.CREATE_NO_WINDOW)
after={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in target.iterdir()}
report=dict(test='ACTUAL_NSIS_EXISTING_DESTINATION_REFUSAL',status='PASS' if result.returncode==17 and before==after else 'FAILED',exitCode=result.returncode,seconds=time.monotonic()-start,installerSha256=receipt['sha256'],sourceSha=receipt['sourceSha'],before=before,after=after,applicationUpdate='NOT TESTED',partialWriteFailure='NOT TESTED')
(out/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(str(out));print(json.dumps(report,indent=2))
raise SystemExit(0 if report['status']=='PASS' else 1)
