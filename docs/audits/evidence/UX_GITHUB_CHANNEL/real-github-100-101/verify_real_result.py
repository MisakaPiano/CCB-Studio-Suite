import hashlib,json,shutil,sys
from pathlib import Path
root=Path('H:/CCB-Studio-Suite')
sys.path.insert(0,str(root/'tools/update-runtime'))
from install_ops import verify_install
run=root/'.local/windows-rc/github-real-100-101'
handoffs=list((run/'runtime/updates/handoffs').iterdir())
assert len(handoffs)==1
h=handoffs[0]
plan=json.loads((h/'plan.json').read_text())
b=json.loads((h/'B_VERIFIED.json').read_text())
result=json.loads((h/'result.json').read_text())
assert result['status']=='COMPLETE' and not result['dataModifiedByInstaller']
assert plan['nonce']==b['nonce']==result['nonce']
assert plan['snapshot']==b['snapshot']
assert plan['dataRoot']==b['dataRoot']==result['dataRoot']==result['sourceDataRoot']
assert b['version']==101 and b['sourceSha']==plan['approved']['sourceSha']
assert b['actualStateSha256']==plan['snapshot']['stateSha256']
verified={}
for label,directory in [('A',Path(plan['installed'])),('B',Path(result['installed']))]:
    config=json.loads((directory/'update-config.json').read_text())
    verified[label]=verify_install(directory,config)
installer=Path(plan['approved']['installer'])
assert hashlib.sha256(installer.read_bytes()).hexdigest()==plan['approved']['sha256']
for asset in b['snapshot']['assets']:
    blob=Path(b['dataRoot'])/'assets/objects'/(asset['sha256']+'.blob')
    assert blob.stat().st_size==asset['size']
    assert hashlib.sha256(blob.read_bytes()).hexdigest()==asset['sha256']
setup=json.loads((run/'SETUP.json').read_text())
source=Path(setup['fixtureSource'])
assert {p.relative_to(source).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in source.rglob('*') if p.is_file()}==setup['fixtureFileHashes']
assert list((Path(b['dataRoot'])/'secrets').glob('*'))==[]
out=root/'docs/audits/evidence/UX_GITHUB_CHANNEL/real-github-100-101'
for name in ['plan.json','B_VERIFIED.json','result.json','events.jsonl']:
    if (h/name).exists():shutil.copy2(h/name,out/name)
shutil.copy2(result['installJournal'],out/'INSTALL_JOURNAL.json')
report={'status':'PASS','scope':'REAL_PUBLIC_GITHUB_APPLICATION_100_TO_101_SYNTHETIC_WORKSPACE','handoff':h.name,'sourceSha':b['sourceSha'],'snapshotEqual':True,'assetsVerified':len(b['snapshot']['assets']),'stateSha256':b['snapshot']['stateSha256'],'credentials':b['snapshot']['credentials'],'installations':verified,'installerSha256':plan['approved']['sha256'],'originalBackupUnchanged':True,'dataModifiedByInstaller':False,'dataRoot':b['dataRoot'],'userEvidence':'Seven screenshots: candidate, downloading, verified, confirmation, B startup success, check, UP_TO_DATE 101','longTermChannel':'NOT_DEPLOYED: ephemeral test keys discarded','overallProduct':'PRODUCT_HOLD / NOT RC'}
(out/'ACTUAL_RESULT.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
