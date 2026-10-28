from pathlib import Path
import sys,json,subprocess,importlib.util,hashlib
r=Path('H:/CCB-Studio-Suite');out=Path(__file__).parent;sys.path.insert(0,str(r/'tools'));import workspace_artifacts as art
def module(name,file):
 s=importlib.util.spec_from_file_location(name,r/'tools'/file);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m
owned=art.create('preview119-package');payload=owned/'payload'
(out/'PACKAGE_OWNER.json').write_text(json.dumps(dict(root=str(owned)),indent=2))
module('prepare','Prepare-UpdaterRuntime.py').build(payload/'runtime',[r/'.local/windows-rc/updater-downloads',r/'.local/windows-rc/wheels'])
profile=out/'release-profile.json';profile.write_text(json.dumps(dict(channel='preview',displayVersion='0.0.0-preview.119',githubRepository='MisakaPiano/CCB-Studio-Suite',metadataBranch='long-term-metadata'),indent=2))
phase=sorted((r/'.local/windows-preview/evidence').glob('*preview119-app-image-20261028'))[-1]
bootstrap=r/'.local/preview-candidate-118-20261027/candidate-r3/release-inputs/update-root.json';assert hashlib.sha256(bootstrap.read_bytes()).hexdigest()=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
installer=module('installer','Build-InternalInstaller.py').build(r/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview',payload/'runtime',bootstrap,payload/'candidate',119,'https://raw.githubusercontent.com/MisakaPiano/CCB-Studio-Suite/long-term-metadata/preview/windows-x86_64/metadata/','https://github.com/MisakaPiano/CCB-Studio-Suite/releases/download/',phase,json.loads(profile.read_text()),consume_app_image=True)
value=dict(owner=str(owned),build=str(installer.parent),installer=str(installer),phase=str(phase),source=subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip())
(out/'PACKAGE.json').write_text(json.dumps(value,indent=2));print(json.dumps(value))
