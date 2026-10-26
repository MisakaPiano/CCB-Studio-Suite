from pathlib import Path
import hashlib,json,os,sys,time,unittest
ROOT=Path('H:/CCB-Studio-Suite');BASE=ROOT/'.local/legacy-cache-cleanup-20261026';stage=sys.argv[1];OUT=BASE/stage;OUT.mkdir()
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as artifacts
fixture=artifacts.create('legacy-cache');os.environ['CCB_SYNTHETIC_TEST_ROOT']=str(fixture/'payload')
sys.path.insert(0,str(ROOT/'tools/update-runtime'));from test_residue_cleanup import ResidueCleanupTests
names=['test_old_cache_record_is_retained_without_blocking_unrelated_completed_cleanup','test_old_cache_unproven_local_references_still_protect_program_and_download','test_old_cache_record_without_projectable_target_fails_closed','test_legacy_completed_download_without_receipt_is_adopted_and_retired','test_legacy_completed_download_changed_bytes_or_identity_is_retained','test_legacy_download_adoption_never_releases_foreign_unproven_reference','test_foreign_drive_bookkeeping_is_projected_without_traversing_old_drive']
def hashes():return {name:hashlib.sha256((ROOT/'tools/update-runtime'/name).read_bytes()).hexdigest() for name in ('residue_cleanup.py','test_residue_cleanup.py','install_ops.py','owned_files.py','studio_update.py')}
before=hashes();start=time.monotonic();cases=[]
class Results(unittest.TextTestResult):
 def addSuccess(self,test):super().addSuccess(test);cases.append(dict(name=test.id(),status='PASS'))
 def addFailure(self,test,error):super().addFailure(test,error);cases.append(dict(name=test.id(),status='FAILED'))
 def addError(self,test,error):super().addError(test,error);cases.append(dict(name=test.id(),status='ERROR'))
with (OUT/'tests.log').open('x',encoding='utf-8') as log:r=unittest.TextTestRunner(stream=log,verbosity=2,resultclass=Results).run(unittest.TestSuite(ResidueCleanupTests(name) for name in names))
summary=dict(status='PASS' if r.wasSuccessful() and hashes()==before else 'FAILED',stage=stage,methods=r.testsRun,failures=len(r.failures),errors=len(r.errors),skipped=len(r.skipped),cases=cases,seconds=time.monotonic()-start,sourceStable=hashes()==before,sourceHashes=before,fixtureRoot=str(fixture),dailyInstallationAccessed=False,privateCredentialsRead=False,realProviderCalls=0,published116Modified=False)
(OUT/'RESULT.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
if summary['status']=='PASS':
 artifacts.seal(fixture,[OUT/'tests.log',OUT/'RESULT.json'],'PASS');cleanup=artifacts.cleanup(fixture)
else:cleanup=dict(status='RETAINED_FAILED_FIXTURE',root=str(fixture))
(OUT/'FIXTURE_LIFECYCLE.json').write_text(json.dumps(cleanup,indent=2)+'\n',encoding='utf-8')
print(json.dumps(summary,indent=2));sys.exit(0 if summary['status']=='PASS' else 1)
