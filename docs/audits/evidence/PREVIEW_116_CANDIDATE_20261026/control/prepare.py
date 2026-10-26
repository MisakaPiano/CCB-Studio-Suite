from pathlib import Path
import ctypes,datetime,hashlib,json,re,shutil,subprocess,urllib.request
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-candidate-116-20261026'
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(name,value):(OUT/name).write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def get(path):
 with urllib.request.urlopen(urllib.request.Request('https://api.github.com/'+path,headers={'User-Agent':'CCBStudioSuite-readonly-candidate'}),timeout=25) as response:return json.load(response)
releases=[r for r in get('repos/MisakaPiano/CCB-Studio-Suite/releases?per_page=30') if not r['draft'] and r['prerelease']]
tags=get('repos/MisakaPiano/CCB-Studio-Suite/tags?per_page=100')
parse=lambda tag:int(re.fullmatch(r'v0\.0\.0-preview\.([1-9][0-9]*)',tag).group(1)) if re.fullmatch(r'v0\.0\.0-preview\.([1-9][0-9]*)',tag) else 0
latest=max(releases,key=lambda r:parse(r['tag_name']));version=max([parse(r['tag_name']) for r in releases]+[parse(r['name']) for r in tags])+1
assert version==116,'Remote changed: stop and reselect candidate explicitly'
write('REMOTE_RELEASE_OBSERVATION.json',dict(atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),latestTag=latest['tag_name'],latestUrl=latest['html_url'],latestPreRelease=latest['prerelease'],assets=[dict(name=a['name'],bytes=a['size'],digest=a.get('digest')) for a in latest['assets']],candidateVersion=version,proposedTag='v0.0.0-preview.116',tagCreated=False,mutations=0))
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();assert head=='9839bce20ccdf4460e0aa42befcb070c1a6a235f'
assert not subprocess.check_output(['git','diff','HEAD','--','modules','tools/update-runtime','tools/windows-launcher','tools/Build-InternalInstaller.py','tools/Prepare-UpdaterRuntime.py'],cwd=ROOT)
app=ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview';runtime=ROOT/'.local/candidate-review-20261026/updater-runtime'
assert (app/'CCBStudioSuitePreview.exe').is_file()
for path in (ROOT,OUT,app,runtime):
 for p in (path,*path.parents):assert not p.is_symlink() and not (p.exists() and p.is_junction())
baseline=ROOT/'.local/windows-rc/performance-107/115-ff145d051bae';root=baseline/'update-root.json';assert sha(root)=='408008d80aeb2dd3844325d74f09da9d248733e220dd88c08cc6299c4a24cc7a'
shutil.copyfile(root,OUT/'preview-update-root.json')
profile=dict(channel='preview',displayVersion='0.0.0-preview.116',githubRepository='MisakaPiano/CCB-Studio-Suite',metadataBranch='long-term-metadata');write('release-profile.json',profile)
app_files=[dict(path=p.relative_to(app).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(app.rglob('*')) if p.is_file()]
write('APP_IMAGE_BEFORE.json',dict(path=str(app),source='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0',files=app_files,consume='Existing supported --consume-app-image: exact Suite-owned build output moved into fresh candidate payload; no old candidate or baseline removed'))
manifest=json.loads((runtime/'RUNTIME_MANIFEST.json').read_text());runtime_bytes=sum(x['bytes'] for x in manifest['files']);app_bytes=sum(x['bytes'] for x in app_files)
prior=(ROOT/'.local/token-candidate-20261026/candidate/studio-installer.exe').stat().st_size
budget=runtime_bytes+int(prior*1.2)+(app_bytes+runtime_bytes)+64*1024**2+256*1024**2
write('BUDGET.json',dict(hRequiredAdditionalBytes=budget,appBytes=app_bytes,runtimeBytes=runtime_bytes,previousInstallerBytes=prior,calculation='Supported same-volume consume of disposable app image, runtime copy, installer x1.2, full isolated installation,64MiB packaging buffer and256MiB safety. No Gradle/app-image duplication or native A/B rerun.',cBudgetBytes=256*1024**2,physicalEstimateBytes=256*1024**2,notMeasuredPeak=True))
# Reuse exact published115 notices only when its immutable release digest matches.
notices=ROOT/'.local/windows-rc/organization-session-115/publication/THIRD_PARTY_NOTICES.zip'
asset=next(a for a in latest['assets'] if a['name']=='THIRD_PARTY_NOTICES.zip')
assert asset['digest']=='sha256:'+sha(notices)
assert not subprocess.check_output(['git','diff','ff145d051bae286ab3e27cadd41195f8bd4c4e18','HEAD','--','gradle','tools/update-runtime/artifacts.json'],cwd=ROOT)
shutil.copyfile(notices,OUT/'THIRD_PARTY_NOTICES.zip')
write('PLAN.json',dict(source=head,appBuildSource='0f11fdd6b6d281987dd7c449d623a74b8b74b8f0',version=116,displayVersion=profile['displayVersion'],proposedTag='v0.0.0-preview.116',tagCreated=False,formalRootSha256=sha(root),runtime=str(runtime),app=str(app),buildEvidence=str(ROOT/'.local/windows-preview/evidence/20261026T100534386552Z-token-candidate-app-20261026'),installerName='CCB-Studio-Suite-0.0.0-preview.116-windows-x64.exe',noticesSha256=sha(notices),publication='WAITING_FOR_USER_CONFIRMATION',dailyInstallation='DO_NOT_MODIFY',signingKeysRead=False,scope='Package/install/identity/release-material checks only; reuse fixed regressions; no expanded tests, no new Gradle'))
print(json.dumps(dict(status='PREPARED',version=version,source=head,hBudget=budget,hFree=shutil.disk_usage(ROOT).free)))
