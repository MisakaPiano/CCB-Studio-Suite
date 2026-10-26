from pathlib import Path
import sys,json,hashlib,shutil,subprocess,os,datetime
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-118-20261027';CANDIDATE=ROOT/'.local/preview-candidate-118-20261027/candidate-r3';CROSS=Path('C:/CCB-Studio-Suite-Test-118-20261027')
sys.path[:0]=[str(ROOT/'.local/preview-publication-116-20261026'),str(ROOT/'tools/update-runtime')]
from github_session import Session,REPO
from install_ops import verify_install,immutable_file,atomic_json
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
SHA='3540be44ca7a1fcfd583899b325abad7630e10b120d288e9f2e7dc7a1e8bd60d';SOURCE='a1202e24f7a0cba8a53f43bf035b67e2134ca2aa'
s=Session();releases=s.api('GET','/repos/'+REPO+'/releases?per_page=100');assert not any(x['tag_name']=='v0.0.0-preview.118' for x in releases)
public=[x for x in releases if not x['draft']];assert public[0]['tag_name']=='v0.0.0-preview.117';repo=dict(default_branch='main',verifiedVia='Connected GitHub repository read')
receipt=json.loads((CANDIDATE/'BUILD_RECEIPT.json').read_text());assert receipt['sourceSha']==SOURCE and receipt['sha256']==SHA and sha(CANDIDATE/'studio-installer.exe')==SHA
assert not CROSS.exists();CROSS.mkdir();atomic_json(CROSS/'OWNER.json',dict(kind='AUTHORIZED_SUITE_118_PUBLICATION_FIXTURE',root=str(CROSS),suite=str(ROOT),fixedSource=SOURCE,syntheticOnly=True,dailyInstallation=False))
view=CROSS/'b';view.mkdir();installer=view/'studio-installer.exe'
assert (CANDIDATE/'studio-installer.exe').resolve().is_relative_to(ROOT/'.local') and installer.resolve().is_relative_to(CROSS)
shutil.move(CANDIDATE/'studio-installer.exe',installer);assert sha(installer)==SHA
atomic_json(OUT/'FIXED_INSTALLER_LOCATION.json',dict(status='TEMPORARILY_MOVED_FOR_DISK_HEADROOM',original=str(CANDIDATE/'studio-installer.exe'),current=str(installer),sha256=SHA,bytes=installer.stat().st_size,returnOrRetainBeforeFixtureRetirement=True))
shutil.copy2(CANDIDATE/'BUILD_RECEIPT.json',view/'BUILD_RECEIPT.json')
env=dict(os.environ);env['TEMP']=env['TMP']=str(CROSS/'temp');Path(env['TEMP']).mkdir()
with immutable_file(installer),(OUT/'restore-fixed-payload.log').open('xb') as log:
 r=subprocess.run([str(installer),'/S','/D='+str(view/'payload')],env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=60)
assert r.returncode==0
config=json.loads((view/'payload/update-config.json').read_text());verified=verify_install(view/'payload',config);assert config['sourceSha']==SOURCE and config['version']==118
assert sha(view/'payload/INSTALL_MANIFEST.json')==sha(CANDIDATE/'release-inputs/INSTALL_MANIFEST.json')
assert sha(view/'payload/update-root.json')=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
atomic_json(OUT/'RESTORED_FIXED_PAYLOAD.json',dict(status='PASS',view=str(view),verification=verified,fixedInstallerSha256=SHA,publicRelease=False,applicationRebuilt=False))
atomic_json(OUT/'READ_ONLY_PREFLIGHT.json',dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),userConfirmed=True,scope='Publish fixed118; verify real public117-to118; only then update daily installation; preserve pending acceptance',candidateSource=SOURCE,evidenceCommit='ee46f20bccf34fa5268cb6b39e0d180793d9ebb4',latestPublic=public[0]['tag_name'],public117InstallerSha256=next(a['digest'] for a in public[0]['assets'] if a['name'].endswith('.exe')),defaultBranch=repo['default_branch'],buildView=str(view),hFree=shutil.disk_usage(ROOT).free,cFree=shutil.disk_usage(CROSS).free,privateWorkingDocsNotPublished=True))
print(json.dumps(dict(status='PREFLIGHT_AND_FIXED_PACKAGE_RESTORE_PASS',version=118,source=SOURCE,sha256=SHA,hFree=shutil.disk_usage(ROOT).free)))
