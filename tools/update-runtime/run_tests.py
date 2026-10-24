"""Preserve each runtime-client run, including failures. No installation claim."""
import hashlib,json,platform,subprocess,sys,time,unittest
from datetime import datetime,timezone
from pathlib import Path
import test_client
import test_install
import test_handoff
import test_github
import test_repository
import test_signing_custody
import test_maintenance
import test_residue_cleanup
root=Path(__file__).resolve().parents[2]
out=root/'.local/windows-rc/evidence'/(datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'-ux-r4-runtime-client');out.mkdir(parents=True)
paths=list(Path(__file__).resolve().parent.glob('*.py'))+[Path(__file__).resolve().parent/'artifacts.json',root/'tools/Prepare-UpdaterRuntime.py',root/'tools/Build-InternalInstaller.py']
hashes=lambda:{p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
before=hashes();start=time.monotonic()
with (out/'tests.log').open('w',encoding='utf-8') as log:
 suite=unittest.TestSuite([unittest.defaultTestLoader.loadTestsFromTestCase(t) for t in [test_client.ClientTests,test_install.InstallTests,test_handoff.HandoffTests,test_github.GitHubTests,test_repository.RepositoryTests,test_signing_custody.CustodyTests,test_maintenance.MaintenanceTests,test_residue_cleanup.ResidueCleanupTests]])
 result=unittest.TextTestRunner(stream=log,verbosity=2).run(suite)
stable=before==hashes()
report={'task':'UX-R4','status':'PASS' if result.wasSuccessful() and stable and not result.skipped else 'FAILED','tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'seconds':time.monotonic()-start,'python':sys.version,'platform':platform.platform(),'sourceStable':stable,'sourceHashes':before,'baseCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'applicationInstallRestart':'NOT TESTED'}
(out/'summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print((out/'tests.log').read_text(encoding='utf-8'));print(json.dumps(report,indent=2));print('EVIDENCE='+str(out))
raise SystemExit(0 if report['status']=='PASS' else 1)
