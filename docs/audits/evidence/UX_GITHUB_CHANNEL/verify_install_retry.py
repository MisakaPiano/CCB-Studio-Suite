import sys,json,subprocess,hashlib,time
from pathlib import Path
root=Path('H:/CCB-Studio-Suite')
sys.path.insert(0,str(root/'tools/update-runtime'))
from install_ops import verify_install
from studio_update import Installed
build=root/'.local/windows-rc/github-preview-fixture-build'
destination=root/'.local/windows-rc/github-fixture-apps/100-test-retry'
out=root/'.local/windows-rc/evidence/github-preview-fixture-install-retry'
assert not destination.exists() and not out.exists()
out.mkdir(parents=True)
receipt=json.loads((build/'BUILD_RECEIPT.json').read_text())
installer=build/'studio-installer.exe'
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert digest(installer)==receipt['sha256']
config=json.loads((build/'payload/update-config.json').read_text())
report=dict(build=receipt,applicationLaunched=False,realGitHubUpdate='NOT TESTED',formalKeysCreated=False,tests=[])
def check(name,path,code):
    assert path.resolve().is_relative_to(root/'.local/windows-rc')
    start=time.monotonic()
    with (out/(name+'.log')).open('wb') as log:
        result=subprocess.run([str(installer),'/S','/D='+str(path)],stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=120)
    report['tests'].append(dict(name=name,exitCode=result.returncode,expected=code,seconds=time.monotonic()-start))
    (out/'receipt.json').write_text(json.dumps(report,indent=2))
    assert result.returncode==code
long_parent=root/'.local/windows-rc/github-fixture-long-path'
assert not long_parent.exists()
check('long-path',long_parent/('a'*170)/('b'*50),19)
assert not long_parent.exists()
check('fresh-install',destination,0)
report['verification']=verify_install(destination,config)
identity=Installed.load(destination)
assert identity.github_repository=='SyntheticOwner/Release-Fixture' and identity.channel=='preview'
probe=subprocess.run([str(destination/'updater/python.exe'),'-B','-c','from github_transport import GitHubFetcher;from studio_update import Installed;print("IMPORT_OK")'],capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW,check=True)
assert b'IMPORT_OK' in probe.stdout
before={p.relative_to(destination).as_posix():digest(p) for p in destination.rglob('*') if p.is_file()}
check('occupied-refusal',destination,17)
after={p.relative_to(destination).as_posix():digest(p) for p in destination.rglob('*') if p.is_file()}
assert before==after
report['occupiedFilesUnchanged']=len(before)
report['postImportVerification']=verify_install(destination,config)
report['isolatedRuntimeImports']='PASS'
report['status']='PASS'
(out/'receipt.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
