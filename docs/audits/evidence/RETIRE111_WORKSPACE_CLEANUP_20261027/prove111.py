from pathlib import Path
import sys,json,hashlib,subprocess,os
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/retire111-workspace-20261027';APP=Path('H:/CCB-Studio-Suite-App');OLD=APP/'111-1c9385afc38c'
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
import workspace_artifacts as a
from install_ops import verify_install,immutable_file,no_reparse
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
candidate=ROOT/'.local/windows-rc/batch-parity-111/candidate';installer=candidate/'studio-installer.exe'
assert sha(installer)=='d20ab5390227f9dbbf054e81a7b1f390fca6019eeba6b0b12578e1fc7e344a1b'
fixture=a.create('retire111-proof');payload=fixture/'payload/program'
a.write(OUT/'PROOF_FIXTURE.json',dict(root=str(fixture),source=str(installer),installerSha256=sha(installer)))
with immutable_file(installer), (OUT/'prove111-install.log').open('xb') as log:
 p=subprocess.run([str(installer),'/S','/D='+str(payload)],cwd=fixture,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=90)
assert p.returncode==0
config=a.read(payload/'update-config.json');verification=verify_install(payload,config)
assert (payload/'INSTALL_MANIFEST.json').read_bytes()==(OLD/'INSTALL_MANIFEST.json').read_bytes()
proof=dict(status='PASS',method='ORIGINAL_FIXED111_NSIS_OUTPUT_RECONSTRUCTION',sourceSha=config['sourceSha'],installerSha256=sha(installer),manifestSha256=sha(payload/'INSTALL_MANIFEST.json'),uninstallSha256=sha(payload/'Uninstall.exe'),oldUninstallMatches=sha(payload/'Uninstall.exe')==sha(OLD/'Uninstall.exe'),oldUninstallBytes=(OLD/'Uninstall.exe').stat().st_size,verification=verification,originalInstallerPreserved=True,fixture=str(fixture),applicationLaunched=False,networkCalls=False)
assert proof['oldUninstallMatches'],'Old uninstaller retained: independent byte proof failed'
a.write(OUT/'UNINSTALL111_PROOF.json',proof);print(json.dumps(proof))
