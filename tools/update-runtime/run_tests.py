"""Preserve each runtime-client run, including failures. No installation claim."""
import hashlib,json,platform,subprocess,sys,time,unittest
from datetime import datetime,timezone
from pathlib import Path
import test_client
root=Path(__file__).resolve().parents[2]
out=root/'.local/windows-rc/evidence'/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'-ux-r4-runtime-client');out.mkdir(parents=True)
paths=list(Path(__file__).parent.glob('*.py'))+[Path(__file__).parent/'artifacts.json',root/'tools/Prepare-UpdaterRuntime.py']
hashes=lambda:{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
before=hashes();start=time.monotonic()
with (out/'tests.log').open('w',encoding='utf-8') as log:
 result=unittest.TextTestRunner(stream=log,verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(test_client.ClientTests))
stable=before==hashes()
report={'task':'UX-R4','status':'PASS' if result.wasSuccessful() and stable and not result.skipped else 'FAILED','tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'seconds':time.monotonic()-start,'python':sys.version,'platform':platform.platform(),'sourceStable':stable,'sourceHashes':before,'baseCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'applicationInstallRestart':'NOT TESTED'}
(out/'summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print((out/'tests.log').read_text(encoding='utf-8'));print(json.dumps(report,indent=2));print('EVIDENCE='+str(out))
raise SystemExit(0 if report['status']=='PASS' else 1)
