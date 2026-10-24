import hashlib,json,shutil,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];LOCAL=ROOT/'.local/review-fixes-cleanup-20261024';OUT=ROOT/'docs/audits/evidence/REVIEW_RESIDUALS_20261025'
sys.path.insert(0,str(ROOT/'tools'));import workspace_artifacts as w
from tuf.api.metadata import Metadata
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def digest(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=True,indent=2)+'\n',encoding='utf-8')
mapping=load(OUT/'FIXED_SOURCE_PACKAGE_MAP.json');installer=Path(mapping['installerLocalPath'])
assert digest(installer)==mapping['installerSha256'] and installer.stat().st_size==mapping['installerBytes']
raw={k:(OUT/'native-final/repository'/f'{k}.json').read_bytes() for k in ('root','targets','snapshot','timestamp')}
md={k:Metadata.from_bytes(b) for k,b in raw.items()};md['root'].verify_delegate('root',md['root'])
for role in ('targets','snapshot','timestamp'):md['root'].verify_delegate(role,md[role])
md['timestamp'].signed.snapshot_meta.verify_length_and_hashes(raw['snapshot']);md['snapshot'].signed.meta['targets.json'].verify_length_and_hashes(raw['targets'])
target=md['targets'].signed.targets['studio-installer.exe'];assert target.hashes['sha256']==mapping['installerSha256'] and target.length==mapping['installerBytes']
assert target.unrecognized_fields['custom']['sourceSha']==mapping['sourceCommit']
config=load(OUT/'native-final/candidate/payload/update-config.json');assert hashlib.sha256(raw['root']).hexdigest()==config['rootSha256']
index=load(OUT/'RUN_INDEX.json')
for key,expected in [('canonicalJvm',18),('canonicalPython',56)]:
    summary=load(OUT/index[key]/'summary.json');cases=load(OUT/index[key]/'test-cases.json')
    assert summary['status']=='PASS' and summary['tests']==expected and len(cases)==expected
    assert all(c['status']=='PASS' for c in cases)
    names=[(c.get('suite',''),c.get('name','')) for c in cases];assert len(set(names))==expected
assert load(OUT/'native-final/NATIVE_UPDATE_SUMMARY.json')['status']=='PASS'
assert load(OUT/'native-first-failed/DRIVER_FAILURE_FROM_ORIGINAL_TMP.json')['status']=='FAILED'
assert load(OUT/'native-final/B_EXIT_OBSERVATION.json')['exits']
assert all(x.get('alreadyExited') or x.get('waitResult')==0 for x in load(OUT/'native-final/B_EXIT_OBSERVATION.json')['exits'])
write(OUT/'VERIFICATION.json',dict(status='PASS',source=mapping['sourceCommit'],installerHashIndependentlyRecomputedInThisLocalRun=True,internalTufFourRoleSignaturesAndLengthHashLinks=True,internalRootMatchesPackage=True,uniqueJvmCases=18,uniquePythonCases=56,rawFailedAttemptsPreserved=True,oldEvidenceHashesUnchanged=load(OUT/'FROZEN_PRESERVATION.json')['status']=='PASS',independentProjectReview='PENDING',human115='PENDING',privateUserDataRead=False))
# Retain proof outside the managed payload, then retire only this successful run.
record=load(LOCAL/'native-lab.json');folder=Path(record['artifactRoot'])
proof=LOCAL/'retained-residual-success-evidence';proof.mkdir(exist_ok=True)
proofs=[]
for p in (OUT/'native-final/NATIVE_UPDATE_SUMMARY.json',OUT/'native-final/CLEANUP_ASSERTIONS.json',OUT/'FIXED_SOURCE_PACKAGE_MAP.json',OUT/'VERIFICATION.json'):
    q=proof/p.name;assert not q.exists();shutil.copyfile(p,q);assert digest(q)==digest(p);proofs.append(q)
w.seal(folder,proofs,'PASS');result=w.cleanup(folder)
write(OUT/'SUCCESSFUL_LAB_RETIREMENT.json',dict(root=str(folder),failedLabRetained=load(LOCAL/'failed-residual-native-lab.json')['artifactRoot'],**result))
assert result['status']=='COMPLETE',result
print(json.dumps(result,indent=2))
