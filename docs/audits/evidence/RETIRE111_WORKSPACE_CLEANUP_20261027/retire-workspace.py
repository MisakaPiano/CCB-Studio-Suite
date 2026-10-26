from pathlib import Path
import sys,json,os,hashlib,shutil,subprocess,time
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/retire111-workspace-20261027'
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
import workspace_artifacts as a
from install_ops import verify_install,immutable_file
from owned_files import delete_matching,prune_empty
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
plan=a.read(OUT/'WORKSPACE_PROGRAM_DUPLICATES_PLAN.json');results=[]
# This user-authorized operation retires exact old program copies, not their evidence or profiles.
for index,row in enumerate(plan):
 root=Path(row['root']);assert root.parent==ROOT/'.local' and root.name in ('appearance-gear-20261026','appearance-text-actions-20261026','token-candidate-20261026')
 package=root/'candidate';installer=package/'studio-installer.exe';assert sha(installer)==row['installerSha256']
 assert all(not x['unknownNames'] and x['manifestMatchesPayload'] for x in row['targets'])
 fixture=a.create('workspace-program-proof');fresh=fixture/'payload/program'
 a.write(OUT/('WORKSPACE_PROOF_FIXTURE_'+str(index)+'.json'),dict(root=str(fixture),source=str(installer),installerSha256=row['installerSha256']))
 with immutable_file(installer),(OUT/('reconstruct-'+str(index)+'.log')).open('xb') as log:
  command=[str(installer),'/S','/D='+str(fresh)]
  process=subprocess.run(command,cwd=fixture,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=90)
 assert process.returncode==0
 config=a.read(fresh/'update-config.json');verification=verify_install(fresh,config)
 assert sha(fresh/'INSTALL_MANIFEST.json')==row['payloadManifestSha256']
 ref={p.relative_to(fresh).as_posix():dict(path=p.relative_to(fresh).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p,isDir in a.walk(fresh) if not isDir}
 inputs=package/'release-inputs';inputs.mkdir(exist_ok=False)
 for name in ('INSTALL_MANIFEST.json','update-config.json','update-root.json'):shutil.copy2(fresh/name,inputs/name)
 proofs=dict(status='MATCHED_REAL_FIXED_INSTALLER_OUTPUT',installerSha256=row['installerSha256'],programManifestSha256=row['payloadManifestSha256'],source=row['source'],originalInstallerPreserved=True,verification=verification,commands=command,targets=[])
 for targetRow in row['targets']:
  target=Path(targetRow['path']);assert target in (package/'payload',root/'isolated-install')
  actual=[];dirs=[]
  for p,isDir in a.walk(target):
   rel=p.relative_to(target).as_posix()
   if isDir:dirs.append(rel)
   else:
    assert rel in ref,'Unknown content retained'
    expected=ref[rel];assert p.stat().st_size==expected['bytes'] and sha(p)==expected['sha256']
    actual.append(expected)
  assert len(actual)==targetRow['files'] and sum(x['bytes'] for x in actual)==targetRow['bytes']
  inventory=dict(root=str(target),files=actual,directories=dirs,proofInstallerSha256=row['installerSha256'],unknownFiles=0)
  inventoryPath=OUT/('PROGRAM_INVENTORY_'+str(index)+'_'+target.name+'.json');a.write(inventoryPath,inventory)
  for entry in actual:assert delete_matching(target/entry['path'],entry)=='REMOVED'
  prune_empty(target,actual,dirs);assert not target.exists()
  result=dict(root=str(target),status='COMPLETE',files=len(actual),bytesFreed=sum(x['bytes'] for x in actual),unknownFilesDeleted=0)
  results.append(result);proofs['targets'].append(result)
 proofFile=OUT/('PROGRAM_PROOF_'+str(index)+'.json');a.write(proofFile,proofs)
 a.write(package/'PAYLOAD_RETIRED.json',dict(status='RETIRED_BY_USER_AUTHORIZED_UNUSED_WORKSPACE_CLEANUP',installerSha256=row['installerSha256'],inputs='release-inputs',evidence=str(proofFile),reconstructFromRetainedInstaller=True,profileAndEvidencePreserved=True))
 a.seal(fixture,[proofFile],'PASS');retired=a.cleanup(fixture);assert retired['status']=='COMPLETE'
 a.write(OUT/('WORKSPACE_PROOF_RETIRED_'+str(index)+'.json'),retired)
 a.write(OUT/'WORKSPACE_RETIREMENT_PROGRESS.json',results)
# Use the existing lifecycle mechanism only for already-sealed successful disposable payloads.
small=[]
for record in a.inventory():
 folder=Path(record['path']);sealed=record['sealed']
 if sealed and sealed.get('status') in ('PASS','COMMAND_SUCCESS') and (folder/'payload').exists():
  result=a.cleanup(folder);small.append(dict(root=str(folder),**result))
a.write(OUT/'EXISTING_PASS_DISPOSABLE_RESULTS.json',small)
summary=dict(status='COMPLETE',oldProgramCopies=results,totalOldProgramFiles=sum(x['files'] for x in results),totalOldProgramBytes=sum(x['bytesFreed'] for x in results),successfulDisposables=small,failedAndUnsealedDisposablesRetained=True,originalInstallersAndEvidenceRetained=True,fixed107114115116BaselinesRetained=True,gradleCachesSigningCustodySourceDataBackupsUntouched=True,newRegressionTests=0,newReleases=0)
a.write(OUT/'WORKSPACE_RETIREMENT_RESULT.json',summary);print(json.dumps(summary,indent=2))
