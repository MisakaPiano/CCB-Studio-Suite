import ctypes,datetime,hashlib,json,os,platform,subprocess,sys,time,uuid
from ctypes import wintypes as W
from pathlib import Path
ROOT=Path('H:/CCB-Studio-Suite');sys.path.insert(0,str(ROOT/'tools'))
import workspace_artifacts as artifacts
OUT=ROOT/'.local/native-perf-20261025'/('run-'+uuid.uuid4().hex[:8]);OUT.mkdir(parents=True)
FIX=artifacts.create('native-perf');PAY=FIX/'payload'
for name in ('tmp','home'):(PAY/name).mkdir()
SOURCE=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
def write(p,v):p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def ambient():
    command="@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { ([string]$_.CommandLine).Contains('ChatChatBar-Desktop') } | ForEach-Object { [ordered]@{pid=$_.ProcessId;parent=$_.ParentProcessId;kind='DesktopJavaLauncher'} }) | ConvertTo-Json -Compress"
    data=subprocess.check_output(['powershell','-NoProfile','-NonInteractive','-Command',command],text=True,encoding='utf-8').strip()
    return json.loads(data) if data else []
peer=ambient();write(OUT/'AMBIENT_BEFORE.json',dict(desktopLaunchers=peer))
if peer:write(OUT/'SUMMARY.json',dict(status='DEFERRED_DESKTOP_BUSY',source=SOURCE,fixture=str(FIX)));print(str(OUT),flush=True);sys.exit(75)
java,classpath=(ROOT/'modules/studio-desktop/build/native-performance-20261025/inputs.txt').read_text().splitlines()
entries=[Path(p) for p in classpath.split(os.pathsep)];inputs=[]
for entry in entries:
    if not entry.exists():continue
    files=[entry] if entry.is_file() else sorted(f for f in entry.rglob('*') if f.is_file())
    inputs.extend(dict(path=str(f),bytes=f.stat().st_size,sha256=sha(f)) for f in files)
write(OUT/'RUNTIME_INPUTS.json',dict(source=SOURCE,java=java,javaSha256=sha(Path(java)),files=inputs,classpath=classpath,installerBuilt=False))
kernel=ctypes.WinDLL('kernel32',use_last_error=True);psapi=ctypes.WinDLL('psapi',use_last_error=True)
class IO(ctypes.Structure):_fields_=[(n,ctypes.c_ulonglong) for n in ('readOps','writeOps','otherOps','readBytes','writeBytes','otherBytes')]
class MEM(ctypes.Structure):_fields_=[('cb',W.DWORD),('pageFaults',W.DWORD)]+[(n,ctypes.c_size_t) for n in ('peakWorkingSet','workingSet','quotaPeakPagedPool','quotaPagedPool','quotaPeakNonPagedPool','quotaNonPagedPool','pagefile','peakPagefile','privateBytes')]
kernel.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];kernel.OpenProcess.restype=W.HANDLE
kernel.CloseHandle.argtypes=[W.HANDLE]
kernel.GetProcessTimes.argtypes=[W.HANDLE,*([ctypes.POINTER(W.FILETIME)]*4)]
kernel.GetProcessIoCounters.argtypes=[W.HANDLE,ctypes.POINTER(IO)]
kernel.GetProcessHandleCount.argtypes=[W.HANDLE,ctypes.POINTER(W.DWORD)]
psapi.GetProcessMemoryInfo.argtypes=[W.HANDLE,ctypes.POINTER(MEM),W.DWORD]
def ticks(t):return (t.dwHighDateTime<<32)|t.dwLowDateTime
def sample(handle,elapsed):
    mem=MEM();mem.cb=ctypes.sizeof(mem);io=IO();times=[W.FILETIME() for _ in range(4)];handles=W.DWORD()
    if not psapi.GetProcessMemoryInfo(handle,ctypes.byref(mem),mem.cb):raise ctypes.WinError(ctypes.get_last_error())
    if not kernel.GetProcessIoCounters(handle,ctypes.byref(io)):raise ctypes.WinError(ctypes.get_last_error())
    if not kernel.GetProcessTimes(handle,*[ctypes.byref(t) for t in times]):raise ctypes.WinError(ctypes.get_last_error())
    kernel.GetProcessHandleCount(handle,ctypes.byref(handles))
    return dict(elapsedMs=elapsed,cpuMs=(ticks(times[2])+ticks(times[3]))/10000,workingSetBytes=mem.workingSet,peakWorkingSetBytes=mem.peakWorkingSet,privateBytes=mem.privateBytes,peakCommitBytes=mem.peakPagefile,handles=handles.value,io={n:getattr(io,n) for n,_ in IO._fields_})
def command(mode,out=None):
    args=[java,'-Xmx512m','-XX:ActiveProcessorCount=2','-Djava.io.tmpdir='+str(PAY/'tmp'),'-Duser.home='+str(PAY/'home'),'-cp',classpath,'io.github.misakapiano.ccbstudio.desktop.NativePerformanceProbe',mode,str(PAY)]
    if out:args.append(str(out))
    return args
attempts=[];startUtc=datetime.datetime.now(datetime.timezone.utc).isoformat()
try:
    prepare=command('prepare');write(OUT/'PREPARE_COMMAND.json',prepare)
    begin=time.monotonic()
    with (OUT/'prepare.log').open('wb') as log:
        result=subprocess.Popen(prepare,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        write(OUT/'PREPARE_STARTED.json',dict(pid=result.pid,utc=datetime.datetime.now(datetime.timezone.utc).isoformat()))
        result.wait(timeout=120)
    attempts.append(dict(kind='prepare',exitCode=result.returncode,seconds=time.monotonic()-begin));assert result.returncode==0
    originals=json.loads((PAY/'originals.json').read_text());write(OUT/'ORIGINAL_INPUTS.json',originals)
    for kind in ('process-cold','warm-reopen'):
        phase=OUT/kind;phase.mkdir();cmd=command('measure',phase);write(phase/'COMMAND.json',cmd)
        peer=ambient();write(phase/'AMBIENT_START.json',peer)
        if peer:raise RuntimeError('Desktop busy before measured launch; no new native measurement launched')
        started=time.monotonic();epoch=time.time();samples=[]
        with (phase/'process.log').open('wb') as log:
            child=subprocess.Popen(cmd,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
            write(phase/'STARTED.json',dict(pid=child.pid,utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),epochSeconds=epoch,scope='new JVM native Workbench test host; not packaged launcher; system file cache not flushed'))
            handle=kernel.OpenProcess(0x410,False,child.pid)
            if not handle:raise ctypes.WinError(ctypes.get_last_error())
            try:
                while child.poll() is None:
                    elapsed=(time.monotonic()-started)*1000
                    if elapsed>240000:raise TimeoutError('Native probe deadline; no process force-kill authorized or performed; preserve fixture and exact PID')
                    try:samples.append(sample(handle,elapsed))
                    except OSError:
                        if child.poll() is None:raise
                    time.sleep(.1)
            finally:kernel.CloseHandle(handle)
        write(phase/'PROCESS_SAMPLES.json',samples)
        write(phase/'EXIT_OBSERVATION.json',dict(pid=child.pid,exitCode=child.returncode,elapsedMs=(time.monotonic()-started)*1000,observedExited=True))
        events=[json.loads(line) for line in (phase/'events.jsonl').read_text().splitlines()] if (phase/'events.jsonl').exists() else []
        assert child.returncode==0, 'Native process failed; see exact process.log, do not assume protection interference'
        assert events and events[-1]['phase']=='PASS'
        closed=next(e for e in events if e['phase']=='controller-closed');assert closed['pendingThumbnails']==closed['retainedThumbnailBytes']==0
        assert len([e for e in events if e['phase'].startswith('gallery-cycle-')])==6
        peak=dict(workingSetBytes=max(s['peakWorkingSetBytes'] for s in samples),privateBytes=max(s['privateBytes'] for s in samples),heapUsedBytes=max(e['heapUsedBytes'] for e in events))
        summary=dict(status='PASS',pid=child.pid,exitCode=child.returncode,seconds=time.monotonic()-started,samples=len(samples),peaks=peak,firstDrawFromParentMs=next(e['epochMillis'] for e in events if e['phase']=='first-native-draw')-epoch*1000,normalExitConfirmed=True,resourceRelease='OS process exited, controller thumbnail requests/pixels zero; OS/global graphics cache not measured',desktopAfter=ambient())
        write(phase/'RESULT.json',summary);attempts.append(dict(kind=kind,**summary));print(kind+' PASS '+str(round(summary['seconds'],2))+'s',flush=True)
    write(OUT/'SUMMARY.json',dict(status='PASS',source=SOURCE,startedUtc=startUtc,fixture=str(FIX),attempts=attempts,measuredNativeSessions=2,junitTestsExecuted=0,priorPerf003TestsNotRerun=True,realProviderCalls=0,scope='48 synthetic 832x1216 originals; two new JVM native Workbench hosts; six page cycles per process; no packaged startup/full Main/update discovery/long-duration acceptance',coldCache='OS_CACHE_UNKNOWN; prepare populated cache; no flush',causeAttribution='No prevention/permission failure inferred; log evidence required',humanAcceptance='NOT TESTED',packageIntegration='NOT TESTED',physicalDiskIO='NOT MEASURED; GetProcessIoCounters logical process IO only'))
    artifacts.seal(FIX,[p for p in OUT.rglob('*') if p.is_file()],'PASS')
    write(OUT/'FIXTURE_CLEANUP.json',artifacts.cleanup(FIX))
except BaseException as error:
    write(OUT/'FAILURE.json',dict(status='FAILED_OR_INCOMPLETE',source=SOURCE,startedUtc=startUtc,failedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),error=repr(error),fixture=str(FIX),attempts=attempts,nativeExitCode=child.returncode if 'child' in locals() else None,cause='UNKNOWN unless exact error/log demonstrates source; not automatically protection software',originalInputsRetained=True,forcedTermination=False))
    print('EVIDENCE='+str(OUT),flush=True);raise
print('EVIDENCE='+str(OUT),flush=True)
