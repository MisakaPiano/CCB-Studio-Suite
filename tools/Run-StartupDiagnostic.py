"""Two synthetic native test-host startup sessions. No product/package/private data.

Requires previously compiled :studio-desktop:nativePerformanceInputs. Retains every
attempt outside its OWNER fixture. No automatic retries or process termination.
JFR uses a minimum event and child-environment whitelist, never stock profile.jfc.
"""
import datetime, gzip, hashlib, json, os, platform, re, subprocess, sys, time, uuid
from pathlib import Path
import workspace_artifacts as artifacts

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'.local/startup-diagnostic-20261025'/('run-'+uuid.uuid4().hex[:8])
OUT.mkdir(parents=True)
SOURCE=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
FIX=None
attempts=[]

def write(p,v):p.write_text(json.dumps(v,ensure_ascii=True,indent=2)+'\n',encoding='utf-8')
def sha(p):
    with p.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def utc():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def ambient():
    command="@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { ([string]$_.CommandLine).Contains('ChatChatBar-Desktop') } | ForEach-Object { [ordered]@{pid=$_.ProcessId;parent=$_.ParentProcessId;kind='DesktopJavaLauncher'} }) | ConvertTo-Json -Compress"
    data=subprocess.check_output(['powershell','-NoProfile','-NonInteractive','-Command',command],text=True,encoding='utf-8').strip()
    return json.loads(data) if data else []
def check_ambient(path):
    peer=ambient();write(path,dict(utc=utc(),desktopLaunchers=peer))
    if peer:raise RuntimeError('DEFERRED_DESKTOP_BUSY: no new diagnostic JVM launched at this boundary')

try:
    check_ambient(OUT/'AMBIENT_BEFORE.json')
    FIX=artifacts.create('startup-diag');PAY=FIX/'payload'
    for name in ('tmp','home','home/Roaming','home/Local'):(PAY/name).mkdir(parents=True,exist_ok=True)
    java,classpath=(ROOT/'modules/studio-desktop/build/native-performance-20261025/inputs.txt').read_text().splitlines()
    jfr=Path(java).with_name('jfr.exe');settings=ROOT/'tools/startup-minimal.jfc'
    system=Path(os.environ['SystemRoot'])
    env={'SystemRoot':str(system),'WINDIR':str(system),'SystemDrive':system.drive,
         'ComSpec':str(system/'System32/cmd.exe'),
         'PATH':os.pathsep.join(map(str,[Path(java).parent,system/'System32',system])),
         'TEMP':str(PAY/'tmp'),'TMP':str(PAY/'tmp'),'USERPROFILE':str(PAY/'home'),
         'HOME':str(PAY/'home'),'APPDATA':str(PAY/'home/Roaming'),'LOCALAPPDATA':str(PAY/'home/Local')}
    write(OUT/'CHILD_ENVIRONMENT_WHITELIST.json',env)
    write(OUT/'ENVIRONMENT.json',dict(platform=platform.platform(),python=platform.python_version(),source=SOURCE,utc=utc(),osCache='UNKNOWN; no flush; fixture preparation warms caches',fixture=str(FIX)))
    inputs=[]
    for entry in [Path(p) for p in classpath.split(os.pathsep)]:
        if not entry.exists():continue
        files=[entry] if entry.is_file() else sorted(f for f in entry.rglob('*') if f.is_file())
        inputs.extend(dict(path=str(f),bytes=f.stat().st_size,sha256=sha(f)) for f in files)
    write(OUT/'RUNTIME_INPUTS.json',dict(source=SOURCE,java=java,javaSha256=sha(Path(java)),jfrSha256=sha(jfr),classpath=classpath,files=inputs,installerBuilt=False))
    tracked=['modules/studio-desktop/src/test/kotlin/io/github/misakapiano/ccbstudio/desktop/NativePerformanceProbe.kt','tools/Run-StartupDiagnostic.py','tools/startup-minimal.jfc','tools/workspace_artifacts.py','modules/studio-desktop/build.gradle.kts']
    write(OUT/'SOURCE_INPUTS.json',[dict(path=p,sha256=sha(ROOT/p),gitSha256=hashlib.sha256(subprocess.check_output(['git','show',SOURCE+':'+p],cwd=ROOT)).hexdigest()) for p in tracked])
    assert all(x['sha256']==x['gitSha256'] for x in json.loads((OUT/'SOURCE_INPUTS.json').read_text()))
    def command(mode,out=None,profile=False):
        cmd=[java,'-Xmx512m','-XX:ActiveProcessorCount=2','-Djava.io.tmpdir='+str(PAY/'tmp'),'-Duser.home='+str(PAY/'home')]
        if profile:cmd.append('-XX:StartFlightRecording=settings='+str(settings)+',filename='+str(out/'startup.jfr')+',disk=true,maxsize=32m,dumponexit=true')
        cmd+=['-cp',classpath,'io.github.misakapiano.ccbstudio.desktop.NativePerformanceProbe',mode,str(PAY)]
        if out:cmd.append(str(out))
        return cmd
    def launch(cmd,folder,timeout):
        write(folder/'COMMAND.json',cmd);begin=time.monotonic();epoch=time.time()
        with (folder/'process.log').open('wb') as log:
            child=subprocess.Popen(cmd,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            write(folder/'STARTED.json',dict(pid=child.pid,utc=utc(),epochSeconds=epoch,scope='new synthetic JVM test host; not packaged application'))
            # wait(timeout) does not terminate. A deadline failure retains PID and fixture.
            child.wait(timeout=timeout)
        exit_record=dict(pid=child.pid,exitCode=child.returncode,elapsedMs=(time.monotonic()-begin)*1000,observedExited=True,utc=utc())
        write(folder/'EXIT_OBSERVATION.json',exit_record)
        assert child.returncode==0,'Child failed; inspect retained log; cause not presumed'
        return exit_record
    prepare=OUT/'prepare';prepare.mkdir();launch(command('prepare'),prepare,120)
    originals=json.loads((PAY/'originals.json').read_text());write(OUT/'ORIGINAL_INPUTS.json',originals)
    assert len(originals)==48
    write(OUT/'FIXTURE_LIMITS.json',dict(assetIds=len(originals),uniquePngSha256=len(set(originals.values())),dimensions=[832,1216],privateAssets=False,accountsEmpty=True))
    for kind in ('timeline-only','minimum-jfr'):
        phase=OUT/kind;phase.mkdir();check_ambient(phase/'AMBIENT_START.json')
        result=launch(command('startup',phase,kind=='minimum-jfr'),phase,120)
        events=[json.loads(line) for line in (phase/'events.jsonl').read_text().splitlines()]
        assert events[-1]['phase']=='PASS'
        by={event['phase']:event for event in events}
        assert not any('gallery' in e['phase'] for e in events),'Startup-only session must not repeat gallery baseline'
        assert by['controller-closed']['pendingThumbnails']==by['controller-closed']['retainedThumbnailBytes']==0
        assert by['originals-verified']['images']==48 and by['originals-verified']['providerCalls']==0
        assert by['first-native-draw']['showing'] is True
        required=['main-enter','brand-start','brand-ready','workspace-open-start','workspace-opened','application-first-composition','http-construction-start','http-construction-ready','controller-construction-start','controller-opened','window-call-start','native-window-first-composition','workbench-call-start','workbench-first-composition-committed','first-native-draw','startup-diagnostic-settled','originals-verified','controller-closed','PASS']
        assert all(name in by for name in required)
        started=json.loads((phase/'STARTED.json').read_text())
        boundaries=list(zip(required[:14],required[1:14]))
        stages=[dict(start=a,end=b,wallMs=by[b]['elapsedMs']-by[a]['elapsedMs'],processCpuMs=(by[b]['processCpuNs']-by[a]['processCpuNs'])/1e6) for a,b in boundaries]
        assert all(s['wallMs']>=0 for s in stages)
        timeline=dict(source=SOURCE,profiled=kind=='minimum-jfr',processStartToMainMs=by['main-enter']['epochMillis']-started['epochSeconds']*1000,processStartToReadyMs=by['first-native-draw']['epochMillis']-started['epochSeconds']*1000,firstContentDrawFromProbeMs=by['first-native-draw']['firstContentDrawMs'],stages=stages,limits=['Parent-to-main includes JVM/test class loading/management instrumentation and (profiled only) JFR bootstrap, not pure JVM initialization','Phase note file IO and management reads perturb timing; process CPU includes background/JIT threads','Composition commit is not raster/DWM presentation; first completed content draw is separate','No cold OS cache or pre/post product comparison; one session per observation mode'])
        write(phase/'TIMELINE.json',timeline)
        if kind=='minimum-jfr':
            raw=phase/'startup.jfr'
            with (phase/'JFR_SUMMARY.txt').open('wb') as stream:
                completed=subprocess.run([str(jfr),'summary',str(raw)],cwd=ROOT,env=env,stdout=stream,stderr=subprocess.STDOUT)
            assert completed.returncode==0
            summary=(phase/'JFR_SUMMARY.txt').read_text()
            counts={m[0]:int(m[1]) for m in re.findall(r'^\s+(jdk\.\S+)\s+(\d+)\s+\d+\s*$',summary,re.M)}
            sensitive=['jdk.InitialEnvironmentVariable','jdk.InitialSystemProperty','jdk.InitialSecurityProperty','jdk.JVMInformation','jdk.SystemProcess']
            privacy={event:counts.get(event,0) for event in sensitive}
            write(phase/'JFR_PRIVACY_COUNTS.json',privacy)
            assert all(v==0 for v in privacy.values()),'Sensitive events present; retain locally and do not export'
            wanted='jdk.ExecutionSample,jdk.NativeMethodSample,jdk.ClassLoad,jdk.Compilation,jdk.GarbageCollection'
            export=PAY/'jfr-events.json'
            cmd=[str(jfr),'print','--json','--stack-depth','48','--events',wanted,str(raw)]
            write(phase/'JFR_EXPORT_COMMAND.json',cmd)
            with export.open('wb') as output, (phase/'jfr-export.log').open('wb') as errors:
                completed=subprocess.run(cmd,cwd=ROOT,env=env,stdout=output,stderr=errors)
            assert completed.returncode==0
            with export.open('rb') as source,gzip.open(phase/'JFR_EVENTS.json.gz','wb') as target:
                import shutil
                shutil.copyfileobj(source,target)
        result.update(status='PASS',phase=kind,stages=len(stages),originalsVerified=48,providerCalls=0,desktopAfter=ambient())
        write(phase/'RESULT.json',result);attempts.append(result);print(kind+' PASS',flush=True)
    write(OUT/'SUMMARY.json',dict(status='PASS',source=SOURCE,attempts=attempts,measuredStartupSessions=2,junitExecuted=0,productChanges=False,installerBuilt=False,realProviderCalls=0,independentReview='NOT REQUESTED',humanAcceptance='NOT TESTED',measurement='Startup phase localization only, not causal optimization proof'))
    artifacts.seal(FIX,[p for p in OUT.rglob('*') if p.is_file()],'PASS')
    write(OUT/'FIXTURE_CLEANUP.json',artifacts.cleanup(FIX))
except BaseException as error:
    write(OUT/'FAILURE.json',dict(status='FAILED_OR_DEFERRED',source=SOURCE,utc=utc(),error=repr(error),fixture=str(FIX) if FIX else None,attempts=attempts,forcedTermination=False,cause='UNKNOWN unless retained error/log demonstrates cause'))
    print('EVIDENCE='+str(OUT),flush=True);raise
print('EVIDENCE='+str(OUT),flush=True)
