from pathlib import Path
import sys,importlib.util,json,hashlib
from datetime import datetime,timedelta,timezone
root=Path('H:/CCB-Studio-Suite')
sys.path.insert(0,str(root/'tools/update-runtime'))
from securesystemslib.signer import CryptoSigner
from tuf.api.metadata import Metadata,Root
from release_repository import prepare,target_from_build
from github_transport import endpoints
spec=importlib.util.spec_from_file_location('builder',root/'tools/Build-InternalInstaller.py')
builder=importlib.util.module_from_spec(spec);spec.loader.exec_module(builder)
folder=root/'.local/windows-rc/github-ab-prepared'
assert not folder.exists()
folder.mkdir()
repository='MisakaPiano/CCB-Studio-Suite-Releases'
trust=Metadata(Root(expires=datetime.now(timezone.utc)+timedelta(days=365),consistent_snapshot=True))
signers={r:CryptoSigner.generate_ed25519() for r in ['root','targets','snapshot','timestamp']}
for role,signer in signers.items():trust.signed.add_key(signer.public_key,role)
trust.sign(signers['root']);bootstrap=trust.to_bytes();(folder/'root.json').write_bytes(bootstrap)
metadata,targets=endpoints(repository,'preview')
report=dict(scope='SYNTHETIC A/B CHANNEL VALIDATION ONLY',repository=repository,privateKeys='MEMORY_ONLY_DISCARDED_ON_EXIT',published=False,builds=[])
for version in (100,101):
    profile=dict(channel='preview',displayVersion='0.0.0-preview.'+str(version),githubRepository=repository)
    build=folder/('build-'+str(version))
    builder.build(root/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview',root/'.local/windows-rc/updater-runtime-github-v1',folder/'root.json',build,version,metadata,targets,root/'docs/audits/evidence/UX_FIXED_TWELVE/runs/20261022T090552192998Z-fixed-twelve-current-package-retry',profile)
    tag='v'+profile['displayVersion'];asset='CCB-Studio-Suite-channel-test-'+str(version)+'.exe'
    target=target_from_build(build,'Synthetic channel validation only. Not a daily-use release. Test workspace only.',tag,asset)
    prepared=prepare(folder/'metadata-store',bootstrap,repository,'preview',signers,target=target)
    report['builds'].append(dict(version=version,tag=tag,asset=asset,receipt=json.loads((build/'BUILD_RECEIPT.json').read_text()),metadata=prepared))
    (folder/'PREPARED.json').write_text(json.dumps(report,indent=2))
print('Prepared two signed generations locally; no publication, no persistent private keys.')
