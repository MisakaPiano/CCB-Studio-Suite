from pathlib import Path
import json,sys,hashlib
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/retire111-workspace-20261027'
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'tools/update-runtime')]
import workspace_artifacts as a
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rows=[]
for name in ['appearance-gear-20261026','appearance-text-actions-20261026','token-candidate-20261026']:
 root=ROOT/'.local'/name;package=root/'candidate';receipt=a.read(package/'BUILD_RECEIPT.json');installer=package/'studio-installer.exe'
 assert sha(installer)==receipt['sha256'];manifest=a.read(package/'payload/INSTALL_MANIFEST.json')
 assert manifest['sourceSha']==receipt['sourceSha']
 for entry in manifest['files']:
  p=package/'payload'/entry['path'];assert p.stat().st_size==entry['bytes'] and sha(p)==entry['sha256']
 item=dict(root=str(root),installerSha256=receipt['sha256'],installerBytes=installer.stat().st_size,source=receipt['sourceSha'],payloadManifestSha256=sha(package/'payload/INSTALL_MANIFEST.json'),targets=[])
 for target in [package/'payload',root/'isolated-install']:
  if not target.exists():continue
  m=a.read(target/'INSTALL_MANIFEST.json');expected={e['path'] for e in m['files']}|{'INSTALL_MANIFEST.json','INSTALL_COMPLETE','Uninstall.exe'}
  allFiles=[(p,p.stat().st_size) for p,isDir in a.walk(target) if not isDir]
  unknown=[p.relative_to(target).as_posix() for p,n in allFiles if p.relative_to(target).as_posix() not in expected]
  item['targets'].append(dict(path=str(target),files=len(allFiles),bytes=sum(n for p,n in allFiles),unknownNames=unknown,manifestMatchesPayload=(target/'INSTALL_MANIFEST.json').read_bytes()==(package/'payload/INSTALL_MANIFEST.json').read_bytes()))
 rows.append(item)
a.write(OUT/'WORKSPACE_PROGRAM_DUPLICATES_PLAN.json',rows)
a.write(OUT/'PREPARATION_BOUNDARY.json',dict(status='RETAINED_PREPARATION_ERROR',reason='verify_install requires INSTALL_COMPLETE; raw packaging payload is not an installed program. Replaced with explicit manifest-byte verification; real NSIS proof remains separate',deletedAtFailure=0,notApplicationFailure=True))
print(json.dumps(rows,indent=2))
