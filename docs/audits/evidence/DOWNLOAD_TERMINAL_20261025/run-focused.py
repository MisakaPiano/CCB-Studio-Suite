"""Only synthetic cleanup decisions; no GUI, package, service or daily data access."""
import argparse,datetime,hashlib,json,os,subprocess,sys,tempfile,time,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as artifacts
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
p=argparse.ArgumentParser();p.add_argument('mode',choices=['baseline','final']);a=p.parse_args()
stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
out=ROOT/'.local/review-terminal-20261025'/(a.mode+'-'+stamp);out.mkdir()
folder=artifacts.create('terminal-test');payload=folder/'payload'
os.environ['CCB_SYNTHETIC_TEST_ROOT']=str(payload);tempfile.tempdir=str(payload/'tmp');Path(tempfile.tempdir).mkdir()
import test_residue_cleanup
names=['test_cross_root_complete_markers_without_installed_or_journal_keep_download','test_cross_root_complete_with_nonexistent_journal_keeps_download','test_cross_root_failed_receipt_with_wrong_or_missing_nonce_keeps_download','test_cross_root_journal_identity_and_verification_must_match_before_releasing_reference','test_cross_root_valid_complete_and_matching_failed_nonce_release_download_immediately']
cls=test_residue_cleanup.ResidueCleanupTests
suite=unittest.TestSuite(cls(name) for name in names) if a.mode=='baseline' else unittest.defaultTestLoader.loadTestsFromTestCase(cls)
paths=[ROOT/'tools/update-runtime'/n for n in ('residue_cleanup.py','test_residue_cleanup.py','owned_files.py','install_ops.py','studio_update.py')]
hashes=lambda:{x.relative_to(ROOT).as_posix():hashlib.sha256(x.read_bytes()).hexdigest() for x in paths}
before=hashes();cases=[];subcases=[]
class Results(unittest.TextTestResult):
    def startTest(self,t):self.started=time.monotonic();self.status='PASS';super().startTest(t)
    def addFailure(self,t,e):self.status='FAILED';super().addFailure(t,e)
    def addError(self,t,e):self.status='ERROR';super().addError(t,e)
    def addSkip(self,t,r):self.status='SKIPPED';super().addSkip(t,r)
    def addSubTest(self,t,s,e):
        status='PASS' if e is None else 'FAILED' if issubclass(e[0],t.failureException) else 'ERROR'
        if e is not None:self.status=status
        subcases.append(dict(name=s.id(),status=status));super().addSubTest(t,s,e)
    def stopTest(self,t):
        sentinel=t.base/'data/original.bin'
        cases.append(dict(name=t.id(),status=self.status,seconds=time.monotonic()-self.started,dataSentinelRetained=sentinel.read_bytes()==b'unique synthetic data',fixtureRoot=str(t.base)))
        super().stopTest(t)
with (out/'tests.log').open('w',encoding='utf-8') as log:
    result=unittest.TextTestRunner(stream=log,verbosity=2,resultclass=Results).run(suite)
stable=before==hashes();passed=result.wasSuccessful() and stable and not result.skipped
summary=dict(mode=a.mode,status='PASS' if passed else 'FAILED',tests=result.testsRun,casePass=sum(x['status']=='PASS' for x in cases),caseFail=sum(x['status'] in ('FAILED','ERROR') for x in cases),failures=len(result.failures),errors=len(result.errors),skipped=len(result.skipped),subcases=subcases,sourceStable=stable,sourceHashes=before,baseCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),command=['verified-updater-python','-B','.local/review-terminal-20261025/run-focused.py',a.mode],windowsLockAndDeletionPrimitives='ACTUAL',realProviderCalls=0,dailyInstallationModified=False,packagesBuilt=0)
(out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(out/'test-cases.json').write_text(json.dumps(cases,indent=2)+'\n')
artifacts.seal(folder,list(out.iterdir()),summary['status']);cleanup=artifacts.cleanup(folder)
(out/'fixture-cleanup.json').write_text(json.dumps(dict(root=str(folder),**cleanup),indent=2)+'\n')
print(json.dumps(dict(evidence=str(out),status=summary['status'],tests=summary['tests'],casePass=summary['casePass'],caseFail=summary['caseFail'],failures=summary['failures'],errors=summary['errors'],sourceStable=stable,cleanup=cleanup),indent=2))
raise SystemExit(0 if passed else 1)
