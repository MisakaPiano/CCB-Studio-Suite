from pathlib import Path
import json,subprocess,shutil,hashlib,sys,time,msvcrt
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';CROSS=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027')
# Reuse only low-level owned-window, hashing and retirement helpers, no prior cases.
source=(OUT/'native-cases-r4.py').read_text().split('results=[]\nfor case in')[0];exec(compile(source,'retained-native-helpers','exec'),globals())
CANDIDATE=OUT/'candidate-preview-117';formal=load(CANDIDATE/'BUILD_RECEIPT.json');evidence=OUT/'formal-package';evidence.mkdir(exist_ok=True)
assert formal['sourceSha']==SOURCE and formal['version']==117 and formal['channel']=='preview'
old=load(OUT/'INTERNAL_VERIFIED_INSTALL_MANIFEST.json');new=load(CANDIDATE/'payload/INSTALL_MANIFEST.json')
old_files={x['path']:x for x in old['files']};new_files={x['path']:x for x in new['files']}
delta=[name for name in old_files if old_files[name]!=new_files[name]]
assert old_files.keys()==new_files.keys() and set(delta)=={'update-root.json','update-config.json','updater/CCBStudioLauncher.exe'},delta
assert formal['nativeLauncherSourceSha256']==receipt['nativeLauncherSourceSha256']
write(evidence/'INTERNAL_TO_PREVIEW_MAPPING.json',dict(source=SOURCE,appBuildSource=formal['appBuildSourceSha'],identicalMembers=len(old_files)-len(delta),differingOnly=delta,nativeCasesNotRecounted=True,formalSigningKeyRead=False,nativeLauncher='Same committed source and compiler; separately compiled native bytes differ, actual Preview entry independently exercised below'))
# Move this new test-only internal binary to the approved C fixture root for H headroom.
temp=OUT/'candidate-v3/studio-installer.exe';saved=CROSS/'retained-preparation/verified-internal-installer.exe';assert not saved.exists() and sha(temp)==receipt['sha256'];shutil.move(temp,saved)
write(evidence/'INTERNAL_BINARY_LOCATION.json',dict(path=str(saved),sha256=receipt['sha256'],testOnly=True))
root=CROSS/'.local/review-update-native/f0';assert not root.exists();root.mkdir(parents=True)
app=root/('117-'+SOURCE[:12])
assert command([CANDIDATE/'studio-installer.exe','/S','/D='+str(app)],evidence/'install.log').returncode==0
config=load(app/'update-config.json');verified=verify_install(app,config)
assert config['displayVersion']=='0.0.0-preview.117' and sha(app/'update-root.json')=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
assert not list((app/'app').glob('*review-update-probe*'))
(root/'data').mkdir();(root/'runtime').mkdir();create_launcher(app,root/'data',root/'runtime')
entries=[]
def entry(root,label):
 app=root/('117-'+SOURCE[:12]);assert command([root/'CCBStudioLauncher.exe'],evidence/(label+'-entry.log')).returncode==0
 shown=wait(lambda:windows(app/'CCBStudioSuitePreview.exe'),45)
 state=wait(lambda:[p for p in (root/'data/documents').rglob('*') if p.is_file()],15)
 with (root/'data/.owner.lock').open('r+b') as stream:
  blocked=False
  try:msvcrt.locking(stream.fileno(),msvcrt.LK_NBLCK,1)
  except OSError:blocked=True
  else:stream.seek(0);msvcrt.locking(stream.fileno(),msvcrt.LK_UNLCK,1)
  assert blocked
 pid=shown[0]['pid'];cmd=['powershell.exe','-NoProfile','-NonInteractive','-Command',f'(Get-CimInstance Win32_Process -Filter "ProcessId={pid}").CommandLine']
 result=subprocess.run(cmd,env=ENV,capture_output=True,text=True,creationflags=subprocess.CREATE_NO_WINDOW,timeout=12)
 line=result.stdout;assert result.returncode==0 and str(root/'data') in line and str(root/'runtime') in line,line
 time.sleep(1)
 shortcut=evidence/(label+'-shortcut.json');assert command([root/'CCBStudioLauncher.exe','--inspect-shortcut',root/'Launch-CCB-Studio.lnk',shortcut],evidence/(label+'-shortcut.log')).returncode==0
 assert load(shortcut)['target']==str(root/'CCBStudioLauncher.exe')
 ended=close(app/'CCBStudioSuitePreview.exe');value=load(root/'launch-current.json')
 assert Path(value['data'])==root/'data' and Path(value['runtime'])==root/'runtime' and value['version']==117
 entries.append(dict(label=label,status='PASS',root=str(root),visibleWindows=shown,commandLine=line.strip(),normalClose=ended,previousRoots=value.get('previousRoots',[]),workspaceLockHeld=True,stateFiles=len(state)))
 write(evidence/(label+'-RESULT.json'),entries[-1])
entry(root,'installed')
(root/'data/exports').mkdir(exist_ok=True);(root/'data/exports/retained-original.bin').write_bytes(b'SYNTHETIC_FORMAL117_ORIGINAL')
holder=ROOT/'.local/disposable'/('proof117-'+uuid.uuid4().hex[:8]);holder.mkdir();destinations=[CROSS/'.local/review-update-native/f1',CROSS/'.local/review-update-native/sub/f2',holder/'payload/f3']
moves=[]
for label,destination in zip(('rename','same-volume-move','cross-volume-C-to-H'),destinations):
 assert not destination.exists() and (destination.is_relative_to(CROSS) or destination.is_relative_to(holder));destination.parent.mkdir(parents=True,exist_ok=True)
 before=inventory(root)
 if root.drive==destination.drive:root.rename(destination)
 else:
  shutil.copytree(root,destination)
  for item in before:
   p=destination/item['path'];assert p.stat().st_size==item['bytes'] and sha(p)==item['sha256']
  dirs=sorted([p for p in root.rglob('*') if p.is_dir()],key=lambda p:len(p.parts),reverse=True)
  for item in before:assert delete_matching(root/item['path'],item)=='REMOVED'
  for p in dirs:p.rmdir()
  root.rmdir()
 for item in before:
  p=destination/item['path'];assert p.stat().st_size==item['bytes'] and sha(p)==item['sha256']
 moves.append(dict(kind=label,source=str(root),destination=str(destination),files=len(before),allBytesMatched=True,sourceGone=not root.exists()))
 root=destination;entry(root,label)
 assert (root/'data/exports/retained-original.bin').read_bytes()==b'SYNTHETIC_FORMAL117_ORIGINAL'
result=dict(status='PASS',version=117,source=SOURCE,installerSha256=formal['sha256'],installation=verified,channel='preview',formalRootUnchanged=True,productionMain=True,testProbePackaged=False,moves=moves,entryChecks=entries,fullUpdateAndRetirement='Mapped to three internal A-to-B cases; 547 exact members match; separate native launcher built from same input, actually exercised here',public117Metadata='NOT PUBLISHED / NOT TESTED',taskbarPin='NOT TESTED',dailyInstallationModified=False)
write(OUT/'FORMAL_CANDIDATE_VERIFICATION.json',result);retire(root,evidence)
print(json.dumps(dict(status='PASS',version=117,actualPackageEntries=len(entries),sequentialMoves=len(moves))),flush=True)
