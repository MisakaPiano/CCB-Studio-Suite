"""Serial isolated Gradle runner. Preserve every attempt; count only fresh JUnit XML."""
import argparse, datetime, hashlib, json, os, shutil, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser(); p.add_argument('--phase',required=True);p.add_argument('--online',action='store_true');p.add_argument('--bootstrap-locks',action='store_true');p.add_argument('tasks',nargs='+');a=p.parse_args()
root=Path(__file__).resolve().parents[1];local=root/'.local/windows-preview'
stamp=datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
out=local/'evidence'/(stamp+'-'+a.phase);out.mkdir(parents=True)
def hashes():
 files=[f for d in ['modules','tools'] for f in (root/d).rglob('*') if f.is_file() and (f.suffix in ['.kt','.java','.kts','.py','.ps1','.cjs','.png','.ico','.hex'] or 'resources' in f.parts) and not any(x in f.parts for x in ['build','__pycache__'])]
 files += [root/f for f in ['settings.gradle.kts','build.gradle.kts','gradle.properties']]
 return {f.relative_to(root).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted(files)}
before=hashes();(out/'source-before.json').write_text(json.dumps(before,indent=2))
gradles=list((Path.home()/'.gradle/wrapper/dists/gradle-9.1.0-bin').glob('*/gradle-9.1.0/bin/gradle.bat'));assert len(gradles)==1
args=[str(gradles[0]),'--no-daemon','--max-workers=1','--console=plain','-Pkotlin.compiler.execution.strategy=in-process']
if not a.online: args+=['--offline']
args+=['--write-verification-metadata','sha256','--write-locks'] if a.bootstrap_locks else ['--dependency-verification=strict']
args+=a.tasks
env=os.environ.copy();env['GRADLE_USER_HOME']=str(local/'gradle-home')
start=time.time()
with (out/'gradle.log').open('wb') as log:
 result=subprocess.run(args,cwd=root,env=env,stdout=log,stderr=subprocess.STDOUT)
total=failed=errors=skipped=0;cases=[]
for f in (root/'modules').glob('*/build/test-results/test/TEST-*.xml'):
 if f.stat().st_mtime < start: continue
 target=out/(f.parts[-5]+'-'+f.name);shutil.copyfile(f,target)
 suite=ET.parse(f).getroot()
 for key in ['tests','failures','errors','skipped']:
  value=int(suite.get(key,'0'))
  if key=='tests':total+=value
  elif key=='failures':failed+=value
  elif key=='errors':errors+=value
  else:skipped+=value
 for c in suite.findall('testcase'):
  status='FAILED' if c.find('failure') is not None or c.find('error') is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASS'
  cases.append(dict(suite=suite.get('name'),name=c.get('name'),seconds=c.get('time'),status=status))
stable=before==hashes()
summary=dict(phase=a.phase,startedUtc=datetime.datetime.fromtimestamp(start,datetime.timezone.utc).isoformat(),seconds=round(time.time()-start,3),exitCode=result.returncode,tests=total,failures=failed,errors=errors,skipped=skipped,sourceStable=stable,status=('PASS' if total else 'COMMAND_SUCCESS') if result.returncode==0 and stable and not failed and not errors else 'FAILED',testEvidence='EXECUTED' if total else 'NOT TESTED (build only or failed before execution)',arguments=args,baseCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),realProvider='NOT TESTED',humanAcceptance='NOT TESTED')
(out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');(out/'test-cases.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('EVIDENCE='+str(out));print(json.dumps(summary,indent=2));print((out/'gradle.log').read_text(errors='replace')[-10000:])
raise SystemExit(0 if summary['status'] in ['PASS','COMMAND_SUCCESS'] else 1)
