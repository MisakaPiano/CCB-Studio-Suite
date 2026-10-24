"""Focused update/cleanup tests with durable per-case evidence and owned fixtures.
No signing-custody, real credentials, daily installation or external service access.
"""
import datetime,hashlib,json,os,subprocess,sys,tempfile,time,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
import workspace_artifacts as artifacts
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
folder=artifacts.create('update-tests');payload=folder/'payload'
os.environ['CCB_SYNTHETIC_TEST_ROOT']=str(payload)
tempfile.tempdir=str(payload/'tmp');Path(tempfile.tempdir).mkdir()
import test_client,test_install,test_handoff,test_residue_cleanup,test_workspace_artifacts
test_workspace_artifacts.TEST_ROOT=payload/'workspace-lifecycle';test_workspace_artifacts.TEST_ROOT.mkdir()
out=ROOT/'.local/review-fixes-cleanup-20261024'/('runtime-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'));out.mkdir()
paths=list((ROOT/'tools/update-runtime').glob('*.py'))+[Path(__file__),ROOT/'tools/workspace_artifacts.py',ROOT/'tools/test_workspace_artifacts.py',ROOT/'tools/Prepare-UpdaterRuntime.py',ROOT/'tools/Build-InternalInstaller.py']
hashes=lambda:{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
before=hashes();cases=[]
class Results(unittest.TextTestResult):
    def startTest(self,test):self.started=time.monotonic();super().startTest(test)
    def record(self,test,status):cases.append(dict(name=test.id(),status=status,seconds=time.monotonic()-self.started))
    def addSuccess(self,test):super().addSuccess(test);self.record(test,'PASS')
    def addFailure(self,test,err):super().addFailure(test,err);self.record(test,'FAILED')
    def addError(self,test,err):super().addError(test,err);self.record(test,'ERROR')
    def addSkip(self,test,reason):super().addSkip(test,reason);self.record(test,'SKIPPED')
with (out/'tests.log').open('w',encoding='utf-8') as log:
    suite=unittest.TestSuite(unittest.defaultTestLoader.loadTestsFromModule(m) for m in (test_client,test_install,test_handoff,test_residue_cleanup,test_workspace_artifacts))
    result=unittest.TextTestRunner(stream=log,verbosity=2,resultclass=Results).run(suite)
stable=before==hashes()
summary=dict(status='PASS' if result.wasSuccessful() and stable and not result.skipped else 'FAILED',tests=result.testsRun,failures=len(result.failures),errors=len(result.errors),skipped=len(result.skipped),sourceStable=stable,sourceHashes=before,baseCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),realProviderCalls=0,dailyInstallationModified=False)
(out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(out/'test-cases.json').write_text(json.dumps(cases,indent=2)+'\n')
try:
    artifacts.seal(folder,list(out.iterdir()),summary['status']);cleanup=artifacts.cleanup(folder)
except (OSError,ValueError) as e:cleanup=dict(status='RETAINED_CLEANUP_ERROR',reason=str(e))
(out/'fixture-cleanup.json').write_text(json.dumps(dict(root=str(folder),**cleanup),indent=2)+'\n')
print('EVIDENCE='+str(out));print((out/'tests.log').read_text());print(json.dumps(summary,indent=2));print(json.dumps(cleanup))
raise SystemExit(0 if summary['status']=='PASS' else 1)
