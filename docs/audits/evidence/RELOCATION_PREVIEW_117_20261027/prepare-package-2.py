from pathlib import Path
import json,subprocess,os,time,sys,shutil
ROOT=Path('H:/CCB-Studio-Suite');OUT=ROOT/'.local/relocation-117-20261027';PY=ROOT/'.local/windows-rc/updater-runtime-115/python.exe'
CROSS=Path('C:/Users/1/AppData/Local/Temp/CCB-Studio-Suite-Relocation-20261027')
def write(n,v):(OUT/n).write_text(json.dumps(v,indent=2)+'\n')
profile=CROSS/'profile';profile.mkdir(exist_ok=True)
for name in ('Local','Roaming','Temp'):(profile/name).mkdir(exist_ok=True)
env={k:v for k,v in os.environ.items() if k.upper() in ('SYSTEMROOT','WINDIR','COMSPEC','PATHEXT','SYSTEMDRIVE','PROCESSOR_ARCHITECTURE','NUMBER_OF_PROCESSORS','OS')}
env.update(PATH='C:/Windows/System32;C:/Windows;C:/Windows/System32/WindowsPowerShell/v1.0;C:/Program Files/Git/cmd',USERPROFILE=str(profile),HOME=str(profile),APPDATA=str(profile/'Roaming'),LOCALAPPDATA=str(profile/'Local'),TEMP=str(profile/'Temp'),TMP=str(profile/'Temp'),JAVA_TOOL_OPTIONS='-Duser.home='+str(profile))
write('SYNTHETIC_ENVIRONMENT_2.json',env)
server=subprocess.Popen([str(PY),'-B',str(ROOT/'tools/update-runtime/internal_repository.py'),'--folder',str(OUT/'repository-v2')],cwd=ROOT,env=env,stdout=(OUT/'server-2.log').open('xb'),stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
write('SERVER_PROCESS_2.json',dict(pid=server.pid,image=str(PY),loopbackOnly=True,privateKeys='MEMORY_ONLY'))
deadline=time.monotonic()+15
while not (OUT/'repository-v2/server.json').exists():
 if server.poll() is not None or time.monotonic()>deadline:raise RuntimeError('Internal repository did not start')
 time.sleep(.1)
port=json.loads((OUT/'repository-v2/server.json').read_text())['port']
build=list((ROOT/'.local/windows-preview/evidence').glob('*-relocation-117-native-inputs-20261027'));assert len(build)==1
cmd=[str(PY),'-B',str(ROOT/'tools/Build-InternalInstaller.py'),'--app',str(ROOT/'modules/studio-desktop/build/compose/binaries/main/app/CCBStudioSuitePreview'),'--runtime',str(OUT/'updater-runtime'),'--bootstrap',str(OUT/'repository-v2/root.json'),'--destination',str(OUT/'candidate-v2'),'--version','2','--metadata-url',f'http://127.0.0.1:{port}/metadata/','--targets-url',f'http://127.0.0.1:{port}/targets/','--build-evidence',str(build[0]),'--consume-app-image']
write('INTERNAL_PACKAGE_COMMAND_2.json',dict(arguments=cmd,appBuild=str(build[0]),formalTrustChanged=False))
with (OUT/'internal-package-2.log').open('xb') as log:p=subprocess.run(cmd,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW,timeout=180)
write('INTERNAL_PACKAGE_RESULT_2.json',dict(exitCode=p.returncode,hFree=shutil.disk_usage(ROOT).free))
if p.returncode:raise RuntimeError('Internal packaging failed; server kept with evidence')
(OUT/'repository-v2/publish.json').write_text(json.dumps(dict(build=str(OUT/'candidate-v2'))))
deadline=time.monotonic()+15
while not (OUT/'repository-v2/PUBLISHED.json').exists():
 if server.poll() is not None or time.monotonic()>deadline:raise RuntimeError('Internal target not published')
 time.sleep(.1)
print(json.dumps(dict(status='INTERNAL_PACKAGE_READY',serverPid=server.pid,port=port,source=json.loads((OUT/'candidate-v2/BUILD_RECEIPT.json').read_text())['sourceSha'])),flush=True)
