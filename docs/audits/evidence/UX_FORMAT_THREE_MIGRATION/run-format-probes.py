import subprocess,json,hashlib,datetime
from pathlib import Path
root=Path(__file__).resolve().parents[1]
out=root/'.local/windows-rc/evidence'/('format-probe-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ'));out.mkdir(parents=True)
java=Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin')
a='.local/windows-rc/ab-build-a-3/payload/app/*'
cp='.local/windows-rc/probes;modules/studio-core/build/classes/kotlin/main;'+a
source=root/'.local/windows-rc/ab-data-3';target=root/'.local/windows-rc/ab-data-3-probe-copy'
def inventory(p):return {f.relative_to(p).as_posix():hashlib.sha256(f.read_bytes()).hexdigest() for f in p.rglob('*') if f.is_file()}
before=inventory(source);runs=[]
for name,args in [('compile',['javac.exe','-encoding','UTF-8','-classpath',cp,'-d','.local/windows-rc/probes','.local/FormatMigrationProbe.java']),('migrate',['java.exe','-Xmx128m','-classpath',cp,'FormatMigrationProbe',str(source),str(target)])]:
 args[0]=str(java/args[0]);p=subprocess.run(args,cwd=root,capture_output=True);(out/(name+'.log')).write_bytes(p.stdout+p.stderr);runs.append(dict(name=name,exitCode=p.returncode))
 if p.returncode:raise SystemExit(f'{name} failed, evidence={out}')
newbefore=inventory(target)
p=subprocess.run([str(java/'java.exe'),'-Xmx128m','-classpath','.local/windows-rc/probes;'+a,'FormatCompatibilityProbe','reject',str(target)],cwd=root,capture_output=True)
(out/'old-reader.log').write_bytes(p.stdout+p.stderr)
expected=json.loads((root/'.local/windows-rc/ab-data-3-fixture.json').read_text())['credentialSha256']
result=dict(status='PASS' if p.returncode==0 and before==inventory(source) and newbefore==inventory(target) and expected in (out/'migrate.log').read_text() else 'FAILED',sourceA='251dc34950990b0878e1910d16ed289e9400db8d',sourceUnchanged=before==inventory(source),oldReaderRefuses=p.returncode==0,newRootUnchangedByOldReader=newbefore==inventory(target),syntheticDpapiHashMatches=expected in (out/'migrate.log').read_text(),runs=runs,actualInstalledUpdate='NOT TESTED')
(out/'summary.json').write_text(json.dumps(result,indent=2));(out/'source-inventory.json').write_text(json.dumps(before,indent=2));(out/'destination-inventory.json').write_text(json.dumps(newbefore,indent=2));print(out);print(json.dumps(result,indent=2))
assert result['status']=='PASS'
