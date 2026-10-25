from pathlib import Path
import subprocess,json,os,datetime,hashlib
OUT=Path('H:/CCB-Studio-Suite/.local/candidate-review-20261026');APP=OUT/'isolated-install/CCBStudioSuitePreview.exe'
PAY=OUT/'startup-smoke';PAY.mkdir()
for name in ('tmp','home','home/Local','home/Roaming'):(PAY/name).mkdir(parents=True,exist_ok=True)
system=Path(os.environ['SystemRoot'])
env={'SystemRoot':str(system),'WINDIR':str(system),'SystemDrive':system.drive,'ComSpec':str(system/'System32/cmd.exe'),'PATH':os.pathsep.join(map(str,[system/'System32',system])),'TEMP':str(PAY/'tmp'),'TMP':str(PAY/'tmp'),'USERPROFILE':str(PAY/'home'),'HOME':str(PAY/'home'),'APPDATA':str(PAY/'home/Roaming'),'LOCALAPPDATA':str(PAY/'home/Local')}
args=[str(APP),'--studio-root',str(PAY/'workspace'),'--runtime-root',str(PAY/'runtime'),'--secondary-workspace','--pause-collections']
(PAY/'COMMAND.json').write_text(json.dumps(dict(arguments=args,environment=env,scope='production packaged UI; fresh empty task-owned workspace and caches; no real credentials; no default entrypoint; directory collection paused'),indent=2)+'\n')
with (PAY/'stdout.log').open('xb') as stdout,(PAY/'stderr.log').open('xb') as stderr:
 p=subprocess.Popen(args,cwd=APP.parent,env=env,stdout=stdout,stderr=stderr,creationflags=subprocess.CREATE_NO_WINDOW)
 (PAY/'STARTED.json').write_text(json.dumps(dict(pid=p.pid,atUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),exe=str(APP),exeSha256=hashlib.sha256(APP.read_bytes()).hexdigest(),exit='NOT YET OBSERVED'),indent=2)+'\n')
 print('STARTED_PID='+str(p.pid),flush=True)