from pathlib import Path
import json,subprocess,shutil,sys,hashlib
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe';CROSS=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027')
sys.path.insert(0,str(ROOT/'tools/update-runtime'))
from owned_files import delete_matching,prune_empty
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(n,v):(OUT/n).write_text(json.dumps(v,indent=2)+'\n')
assert json.loads((OUT/'NATIVE_CASES_RESULT_FINAL.json').read_text())['uniqueRealCases']==3
(OUT/'repository-v2/STOP').write_text('All three real native migration cases complete; stop test transport normally')
p=OUT/'candidate-v3/payload';image=ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'
assert not image.exists();manifest=json.loads((p/'INSTALL_MANIFEST.json').read_text());raw=(p/'INSTALL_MANIFEST.json').read_bytes()
for name in ('INSTALL_MANIFEST.json','update-config.json','update-root.json'):shutil.copyfile(p/name,OUT/('INTERNAL_VERIFIED_'+name))
removed=[]
for x in manifest['files']:
 if x['path'].startswith('updater/') or x['path'] in ('update-config.json','update-root.json'):
  assert delete_matching(p/x['path'],x)=='REMOVED';removed.append(x)
prune_empty(p,removed);assert delete_matching(p/'INSTALL_MANIFEST.json',dict(bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest()))=='REMOVED'
assert not (p/'updater').exists();shutil.move(p,image)
profile=dict(channel='preview',displayVersion='0.0.0-preview.117',githubRepository='MisakaPiano/CCB-Studio-Suite',metadataBranch='long-term-metadata');write('release-profile-117.json',profile)
bootstrap=ROOT/'.local/preview-candidate-116-20261026/preview-update-root.json';assert sha(bootstrap)=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
build=list((ROOT/'.local/windows-preview/evidence').glob('*-relocation-117-native-inputs-20261027'));assert len(build)==1
cmd=[str(PY),'-B',str(ROOT/'tools/Build-InternalInstaller.py'),'--app',str(image),'--runtime',str(OUT/'updater-runtime'),'--bootstrap',str(bootstrap),'--destination',str(OUT/'candidate-preview-117'),'--version','117','--metadata-url','https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/long-term-metadata/preview/windows-x86_64/metadata/','--targets-url','https://github.com/MisakaPiano/CCB-Studio-Suite/releases/download/','--build-evidence',str(build[0]),'--release-profile',str(OUT/'release-profile-117.json'),'--consume-app-image']
write('PREVIEW_PACKAGE_COMMAND.json',dict(arguments=cmd,source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),formalRootSha256=sha(bootstrap),formalKeysRead=False,publish=False))
env=json.loads((OUT/'SYNTHETIC_ENVIRONMENT_2.json').read_text())
with (OUT/'preview-package.log').open('xb') as log:p=subprocess.run(cmd,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
write('PREVIEW_PACKAGE_EXIT.json',dict(exitCode=p.returncode));assert p.returncode==0
receipt=json.loads((OUT/'candidate-preview-117/BUILD_RECEIPT.json').read_text());assert sha(OUT/'candidate-preview-117/studio-installer.exe')==receipt['sha256']
write('PREVIEW_INPUT_MAPPING.json',dict(source=receipt['sourceSha'],appBuildSource=receipt['appBuildSourceSha'],nativeLauncherSourceSha256=receipt['nativeLauncherSourceSha256'],version=117,displayVersion='0.0.0-preview.117',installerSha256=receipt['sha256'],bytes=receipt['bytes'],tests='3 unique real relocation scenarios on internal identity of same production input; source regression results separately listed',formalRootUnchanged=True,notPublished=True,dailyInstallationModified=False))
print(json.dumps(receipt),flush=True)
