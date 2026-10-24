from pathlib import Path
import hashlib,json,subprocess,shutil
ROOT=Path(__file__).resolve().parents[2];lab=Path(json.loads((ROOT/'.local/review-fixes-cleanup-20261024/native-lab.json').read_text())['lab'])
folder=lab/'entrypoint-probe';folder.mkdir();programs=folder/'programs';programs.mkdir();data=folder/'data';runtime=folder/'runtime';data.mkdir();runtime.mkdir();restored=folder/'restored';restored.mkdir()
source=ROOT/'.local/review-fixes-cleanup-20261024/EntrypointConcurrencyProbe.java';shutil.copyfile(source,folder/source.name)
classpath=str(lab/'candidate/payload/app/*')+';'+str(folder)
jdk=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin')
with (folder/'compiler.log').open('wb') as f:subprocess.run([str(jdk/'javac.exe'),'-encoding','UTF-8','-cp',classpath,'-d',str(folder),str(source)],stdout=f,stderr=subprocess.STDOUT,check=True)
for name,version in [('low',1),('high',2)]:
 app=programs/name;(app/'updater').mkdir(parents=True);files=[]
 for name in ['CCBStudioSuitePreview.exe','updater/CCBStudioLauncher.exe']:
  p=app/name;shutil.copyfile(lab/'candidate/payload'/name,p);b=p.read_bytes();files.append(dict(path=name,bytes=len(b),sha256=hashlib.sha256(b).hexdigest()))
 (app/'INSTALL_MANIFEST.json').write_text(json.dumps(dict(files=files)));(app/'update-config.json').write_text(json.dumps(dict(version=version)))
def command(name,out,profile=data):return [str(jdk/'java.exe'),'-cp',classpath,'EntrypointConcurrencyProbe',str(programs/name),str(profile),str(runtime),str(folder/out)]
processes=[]
for name in ['low','high']:
 log=(folder/(name+'.log')).open('wb');p=subprocess.Popen(command(name,name+'.json'),stdout=log,stderr=subprocess.STDOUT);processes.append((p,log))
for p,log in processes:assert p.wait(30)==0;log.close()
descriptor=programs/'launch-current.json';before=descriptor.read_bytes();assert json.loads(before)['version']==2
for name,out,profile in [('low','late-low.json',data),('high','restore.json',restored)]:subprocess.run(command(name,out,profile),check=True,capture_output=True)
assert json.loads((folder/'late-low.json').read_text())['status']=='REJECTED';assert json.loads((folder/'restore.json').read_text())['accepted'] is False;assert descriptor.read_bytes()==before
report=dict(status='PASS',sourceSha='9dde7daacd6526e2e252408310df4a594a8e39c0',concurrentProcesses='HIGHEST_VERSION_RETAINED',lateOlderProcess='REJECTED_WITHOUT_DESCRIPTOR_CHANGE',restoredProfile='REJECTED_WITHOUT_DESCRIPTOR_CHANGE',javaProbeSha256=hashlib.sha256(source.read_bytes()).hexdigest(),descriptorSha256=hashlib.sha256(before).hexdigest(),productionJarSha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (lab/'candidate/payload/app').glob('studio-desktop-*.jar')},syntheticOnly=True)
(folder/'summary.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
