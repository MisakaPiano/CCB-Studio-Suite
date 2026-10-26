from pathlib import Path
import datetime,hashlib,json,os,subprocess,sys,shutil
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/preview-publication-117-20261027';BASE=ROOT/'.local/preview-candidate-116-20261026/candidate/payload';OLDLIB=BASE/'updater/lib';OLDPY=BASE/'updater/python.exe'
FIX=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027');SHA='bec5f78735b6cd4c517c7c31c3d5bd499ed97a0d565e75718bc90c38fb0089c9';SOURCE='b9b240a248d428ec158c341b8c127969fd54cb64'
assert json.loads((OUT/'METADATA_PUBLICATION.json').read_text())['releaseVersion']==117
def sha(p):
 with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def write(n,v):(OUT/n).write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
assert not FIX.exists();assert shutil.disk_usage('C:/').free>1024**3;FIX.mkdir()
(FIX/'OWNER.json').write_text(json.dumps(dict(kind='EXPLICIT_SUITE_117_PUBLIC_CHAIN_FIXTURE',suite=str(ROOT),task='preview-publication-117-20261027',dailyInstallation=False)))
cache=FIX/'r/updates';cache.mkdir(parents=True);data=FIX/'d';data.mkdir();(data/'synthetic-original.bin').write_bytes(b'SYNTHETIC_PUBLIC117_ORIGINAL')
programs=FIX/'p';programs.mkdir();anchor=programs/'116-test-anchor';anchor.mkdir();(anchor/'SYNTHETIC_ANCHOR_ONLY.txt').write_text('Old real116 updater is executed from retained package; this is not a native old116 application.')
profile=FIX/'profile';profile.mkdir()
for n in ('Local','Roaming','Temp'):(profile/n).mkdir()
env={k:v for k,v in os.environ.items() if k.upper() in ('SYSTEMROOT','WINDIR','COMSPEC','PATHEXT','SYSTEMDRIVE','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS','OS')}
env.update(USERPROFILE=str(profile),HOME=str(profile),LOCALAPPDATA=str(profile/'Local'),APPDATA=str(profile/'Roaming'),TEMP=str(profile/'Temp'),TMP=str(profile/'Temp'),PATH='C:/Windows/System32;C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0')
baseline_manifest=json.loads((BASE/'INSTALL_MANIFEST.json').read_text());baseline_entries={x['path']:x for x in baseline_manifest['files']}
for name in ('studio_update.py','github_transport.py','install_ops.py','owned_files.py','apply_update.py','residue_cleanup.py'):
 rel='updater/lib/'+name;p=BASE/rel;assert sha(p)==baseline_entries[rel]['sha256'] and p.stat().st_size==baseline_entries[rel]['bytes']
write('PUBLIC_CHECK_FIXTURE.json',dict(root=str(FIX),baseline=str(BASE),oldVersion=116,oldSource='9839bce20ccdf4460e0aa42befcb070c1a6a235f',oldPythonSha256=sha(OLDPY),scope='Actual retained116 bundled client/helper; public signed117 download; real isolated NSIS install; synthetic parent anchor, not native old116 GUI update',dailyInstallationAccessed=False))
command=[str(OLDPY),'-B',str(OLDLIB/'studio_update.py'),'--package',str(BASE),'--cache',str(cache),'--download'];write('OLD116_PUBLIC_COMMAND.json',dict(arguments=command))
with (OUT/'old116-public.log').open('xb') as log:result=subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=240)
assert result.returncode==0,'Retain old116-public.log on download failure'
download=json.loads((OUT/'old116-public.log').read_text(encoding='utf-8'));assert download['status']=='DOWNLOADED_VERIFIED' and download['version']==117 and download['sha256']==SHA
installer=Path(download['installer']);assert installer.is_relative_to(FIX) and sha(installer)==SHA
write('OLD116_PUBLIC_DOWNLOAD.json',dict(status='PASS',result=download,dailyInstallationAccessed=False,privateCredentialsRead=False))
sys.path.insert(0,str(OLDLIB));import studio_update,install_ops,github_transport
assert all(Path(m.__file__).parent==OLDLIB for m in (studio_update,install_ops,github_transport))
installed=studio_update.Installed.load(BASE);updater,fetcher=studio_update.create_updater(installed,cache/'metadata',cache/'downloads');target,custom=studio_update.inspect(updater,installed);studio_update.authorize_download(fetcher,target,installed)
assert custom['version']==117 and custom['sourceSha']==SOURCE and target.hashes['sha256']==SHA
original=sha(data/'synthetic-original.bin');new,journal,record=install_ops.install_candidate(installer,target,custom,anchor,cache,data)
assert record['status']=='INSTALLED_NOT_STARTED';check=install_ops.verify_install(new,custom)
assert sha(data/'synthetic-original.bin')==original and sha(new/'INSTALL_MANIFEST.json')==sha(ROOT/'.local/relocation-117-20261027/candidate-preview-117/payload/INSTALL_MANIFEST.json')
write('PUBLIC_ISOLATED_INSTALL.json',dict(status='PASS',journal=str(journal),record=record,verification=check,oldClientModules={m.__name__:dict(path=m.__file__,sha256=sha(Path(m.__file__))) for m in (studio_update,install_ops,github_transport)},dataSentinelMatched=True,realOldNativeGui='NOT TESTED IN THIS CASE',newNativeStart='NOT STARTED; native checks are separate candidate evidence',dailyInstallationAccessed=False))
current=FIX/'current-check';command=[str(new/'updater/python.exe'),'-B',str(new/'updater/lib/studio_update.py'),'--package',str(new),'--cache',str(current)]
with (OUT/'current117.log').open('xb') as log:result=subprocess.run(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=100)
assert result.returncode==0
value=json.loads((OUT/'current117.log').read_text(encoding='utf-8'));assert value['status']=='UP_TO_DATE' and value['version']==117 and value['sha256']==SHA
write('PUBLIC117_CURRENT_CHECK.json',dict(status='PASS',result=value,command=command))
raw=OUT/'public-raw';raw.mkdir()
for prefix,folder in (('old116-metadata',cache/'metadata'),('current117-metadata',current/'metadata')):
 dest=raw/prefix;dest.mkdir()
 for p in folder.glob('*.json'):shutil.copyfile(p,dest/p.name)
shutil.copyfile(journal,raw/journal.name);shutil.copyfile(new/'INSTALL_MANIFEST.json',raw/'INSTALL_MANIFEST.json')
write('PUBLIC_CHANNEL_VERIFICATION.json',dict(status='PASS',oldClientVersion=116,newVersion=117,source=SOURCE,installerSha256=SHA,rootUnchanged=True,discovery='SIGNED117',download='REAL_PUBLIC_BYTES_VERIFIED',isolatedInstall='REAL_NSIS_PASS',current117='UP_TO_DATE',data='SYNTHETIC_SENTINEL_UNCHANGED',nativeGui='NOT TESTED IN THIS PUBLIC CASE; fixed candidate native evidence remains separate',fixtureRoot=str(FIX),dailyInstallationModified=False,privateCredentialsRead=False,providerCalls=0,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
print('PUBLIC_CHAIN_116_TO_117_PASS; daily116 unchanged',flush=True)
