from pathlib import Path
import hashlib,json,subprocess,shutil,time
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';FIX=OUT/'launcher-fixtures';FIX.mkdir()
CSC=Path('C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe');NEW=OUT/'CCBStudioLauncher.exe'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dump(p,v):p.write_text(json.dumps(v),encoding='utf-8')
def run(args):return subprocess.run(list(map(str,args)),capture_output=True,creationflags=subprocess.CREATE_NO_WINDOW,timeout=25)
marker=FIX/'Marker.cs';marker.write_text('using System;using System.IO;class Marker{static void Main(string[] a){File.WriteAllLines(Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"arguments.txt"),a);}}')
results=[]
for case in ('rename','external-profile','missing-profile','tampered-exe','junction-profile'):
 root=FIX/(case+'-before');app=root/('2-'+'b'*12);(app/'updater').mkdir(parents=True)
 data=root/'data/workspace';runtime=root/'data/runtime';data.mkdir(parents=True);runtime.mkdir()
 shutil.copyfile(NEW,root/'CCBStudioLauncher.exe');shutil.copyfile(NEW,app/'updater/CCBStudioLauncher.exe')
 result=run([CSC,'/nologo','/target:winexe','/out:'+str(app/'CCBStudioSuitePreview.exe'),marker]);assert result.returncode==0,result.stderr
 cfg=dict(product='ccb-studio-suite',platform='windows-x86_64',channel='internal',version=2,sourceSha='b'*40)
 dump(app/'update-config.json',cfg);dump(app/'INSTALL_MANIFEST.json',dict(cfg,format=1,files=[]));(app/'INSTALL_COMPLETE').write_text('2:'+'b'*40)
 if case=='external-profile':data=FIX/'external-data';data.mkdir()
 descriptor=dict(format=1,version=2,installed=str(app),data=str(data),runtime=str(runtime),exeSha256=sha(app/'CCBStudioSuitePreview.exe'))
 dump(root/'launch-current.json',descriptor);old_body=(root/'launch-current.json').read_bytes()
 if case=='rename':
  old_src=FIX/'Baseline.cs';old_src.write_bytes(subprocess.check_output(['git','show','71bac6f:tools/windows-launcher/Launcher.cs'],cwd=ROOT));old_exe=FIX/'OldLauncher.exe'
  assert run([CSC,'/nologo','/target:winexe','/reference:System.Windows.Forms.dll','/reference:System.Web.Extensions.dll','/out:'+str(old_exe),old_src]).returncode==0
 moved=root.with_name(case+'-after');shutil.move(root,moved);app=moved/app.name
 if case=='missing-profile':shutil.rmtree(moved/'data/workspace')
 if case=='tampered-exe':(app/'CCBStudioSuitePreview.exe').write_bytes(b'not original target')
 if case=='junction-profile':
  (moved/'data/workspace').rmdir();target=FIX/'junction-target';target.mkdir()
  assert run(['cmd.exe','/c','mklink','/J',str(moved/'data/workspace'),str(target)]).returncode==0
 if case=='rename':
  # The old entry must run at the moved root, not alongside the fixture parent.
  shutil.copyfile(old_exe,moved/'BaselineLauncher.exe')
  baseline=run([moved/'BaselineLauncher.exe','--resolve-report',OUT/'launcher-baseline.json']);assert baseline.returncode!=0
 report=OUT/('launcher-'+case+'.json');p=run([moved/'CCBStudioLauncher.exe','--resolve-report',report]);value=json.loads(report.read_text(encoding='utf-8-sig'))
 good=case in ('rename','external-profile')
 assert (p.returncode==0)==good,(case,value)
 if good:
  assert value['installed']==str(app) and value['runtime']==str(moved/'data/runtime')
  assert value['data']==str(data if case=='external-profile' else moved/'data/workspace')
  p=run([moved/'CCBStudioLauncher.exe']);assert p.returncode==0
  args=app/'arguments.txt'
  for i in range(100):
   if args.exists():break
   time.sleep(.05)
  assert args.read_text().splitlines()==['--studio-root',value['data'],'--runtime-root',value['runtime']]
 else:assert (moved/'launch-current.json').read_bytes()==old_body
 if case=='junction-profile':(moved/'data/workspace').rmdir() # remove link only, preserve target
 results.append(dict(case=case,status='PASS',accepted=good))
dump(OUT/'LAUNCHER_SAFETY_RESULT.json',dict(status='PASS',cases=results,oldLauncherRelocation='REJECTED_AS_EXPECTED',nativeStudio='NOT TESTED HERE'))
print(json.dumps(results))
