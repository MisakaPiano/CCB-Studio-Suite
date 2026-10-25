"""One local P2 attempt; never submits, pushes, deletes, or changes global settings."""
from pathlib import Path
import datetime,hashlib,json,os,stat,subprocess,time

ROOT=Path('H:/CCB-Studio-Suite');OUT=Path(__file__).resolve().parent
SOURCE='c39e860252fdd96c9bec5b5d42d1abd988d3ec06'
PHASE='display-theme-selected-p2-lowmem-f-20261026'
assert Path.cwd().resolve()==ROOT.resolve() and OUT.parent==ROOT/'.local'
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def emit(name,value):
    with (OUT/name).open('x',encoding='utf-8',newline='\n') as f:json.dump(value,f,indent=2,ensure_ascii=False);f.write('\n')
def scan(relative):
    todo=[Path(chr(92)*2+'?'+chr(92)+str(ROOT/relative))];size=count=0;start=time.monotonic()
    while todo:
        assert time.monotonic()-start<15 and count<20000,'Incomplete bounded build scan'
        p=todo.pop();s=p.lstat();assert not s.st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT
        if stat.S_ISDIR(s.st_mode):
            with os.scandir(p) as entries:todo.extend(Path(e.path) for e in entries)
        else:assert stat.S_ISREG(s.st_mode);size+=s.st_size;count+=1
    return dict(bytes=size,files=count,complete=True)
assert not subprocess.check_output(['git','diff','--name-only',SOURCE,'HEAD','--','modules','tools']).strip()
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
assert not list((ROOT/'.local/windows-preview/evidence').glob('*-'+PHASE)),'Existing phase; no duplicate attempt'
ps=r'''
$ErrorActionPreference='Stop'
$taskRootPid=TASK_PID
$taskOwned=@();$taskSeen=@{};$taskQueue=[Collections.Generic.Queue[int]]::new()
if($taskRootPid -gt 0){$taskQueue.Enqueue($taskRootPid)}
while($taskQueue.Count -gt 0){
 $taskId=$taskQueue.Dequeue();if($taskSeen.ContainsKey($taskId)){continue};$taskSeen[$taskId]=$true
 $taskNode=Get-CimInstance Win32_Process -Filter "ProcessId=$taskId" -Property ProcessId,ParentProcessId,Name,CreationDate,WorkingSetSize,PeakWorkingSetSize,PrivatePageCount,PageFileUsage,PeakPageFileUsage,CommandLine
 if($taskNode){
  $taskFlags=@([regex]::Matches([string]$taskNode.CommandLine,'(?<!\S)(?:-Xm[xs]\S+|-XX:\S+)')|ForEach-Object{$_.Value})
  $taskOwned+=@{pid=[int]$taskNode.ProcessId;parent=[int]$taskNode.ParentProcessId;name=$taskNode.Name;createdUtc=$taskNode.CreationDate.ToUniversalTime().ToString('o');workingSetBytes=[long]$taskNode.WorkingSetSize;peakWorkingSetBytes=([long]$taskNode.PeakWorkingSetSize*1024);privatePageCountBytes=[long]$taskNode.PrivatePageCount;pageFileUsageBytes=([long]$taskNode.PageFileUsage*1024);peakPageFileUsageBytes=([long]$taskNode.PeakPageFileUsage*1024);jvmFlags=$taskFlags}
  Get-CimInstance Win32_Process -Filter "ParentProcessId=$taskId" -Property ProcessId|ForEach-Object{$taskQueue.Enqueue([int]$_.ProcessId)}
 }
}
$taskForeign=@(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe' OR Name='jpackage.exe' OR Name='jlink.exe' OR Name='makensis.exe' OR Name='7z.exe' OR Name='msiexec.exe' OR Name='light.exe' OR Name='candle.exe' OR Name='dotnet.exe'" -Property ProcessId,Name |Where-Object{-not $taskSeen.ContainsKey([int]$_.ProcessId)}|ForEach-Object{@{pid=[int]$_.ProcessId;name=$_.Name;owner='FOREIGN_OR_UNKNOWN_NO_CONTENT_READ'}})
$taskMemory=Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory -Property AvailableBytes,CommittedBytes,CommitLimit,PageReadsPersec,PagesInputPersec
[ordered]@{atUtc=[DateTime]::UtcNow.ToString('o');availableBytes=[long]$taskMemory.AvailableBytes;committedBytes=[long]$taskMemory.CommittedBytes;commitLimitBytes=[long]$taskMemory.CommitLimit;pageReadsPerSecond=[long]$taskMemory.PageReadsPersec;pagesInputPerSecond=[long]$taskMemory.PagesInputPersec;hFreeBytes=[IO.DriveInfo]::new('H:\').AvailableFreeSpace;cFreeBytes=[IO.DriveInfo]::new('C:\').AvailableFreeSpace;owned=$taskOwned;foreign=$taskForeign}|ConvertTo-Json -Depth 6 -Compress
'''
def snapshot(pid=0):
    return json.loads(subprocess.check_output(['pwsh','-NoProfile','-Command',ps.replace('TASK_PID',str(pid))],encoding='utf-8'))
outputs={f'modules/{m}/build':scan(f'modules/{m}/build') for m in ['studio-desktop','studio-core','image-authority','asset-library']}
hmin=max(2195497728,2*sum(x['bytes'] for x in outputs.values())+1248*1024**2)
before=snapshot()
profile=dict(atUtc=now(),source=SOURCE,executionHead=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),phase=PHASE,
 gradleHeapMiB=384,gradleMetaspaceMiB=256,testHeapMiB=256,testMetaspace='not explicitly capped; native allocations monitored',
 worker=1,testForks=1,kotlin='in-process',priority='low',cpuPerJvm=2,daemon='no persistent daemon; single-use fork may occur',
 oldPhysicalFree2GiBRule='Not used; it was an unmeasured estimate, not user or platform requirement',
 admissionBasis='H/C bounded budget and no foreign owner required. Current commit headroom vs configured 896MiB heap/metaspace ceilings, paging/Available and actual process overlap evaluated; no new fixed physical-free gate.',
 risk='Native/metaspace/launcher are not bounded by heap sum; GC/paging/OOM possible; one scoped attempt and retain failures, no blind retries.',
 pressureAlert='Two consecutive samples: commit>=95% and Available<128MiB, or Available<64MiB and pageReads/sec>1000. Foreign owner or disk below retained floors also alerts for normal cancellation of only this run; no forced kill or OS changes.',
 hBudgetBytes=hmin,cBudgetBytes=805306368,buildOutputs=outputs,before=before,
 preserveFixtures=True,externalSubmission='PROHIBITED_THIS_ROUND',push='PROHIBITED_THIS_ROUND')
emit('PLAN.json',profile)
assert before['hFreeBytes']>=hmin and before['cFreeBytes']>=805306368,'Disk budget not admitted'
assert not before['foreign'],'Foreign owner conflict'
assert before['commitLimitBytes']>before['committedBytes'],'No system commit headroom'
assert before['commitLimitBytes']-before['committedBytes']>896*1024**2,'Insufficient commit headroom for known configured limits'
runtime=ROOT/'.local/windows-rc/updater-runtime-115/python.exe';assert runtime.is_file()
bootstrap="import sys,runpy;sys.path.insert(0,'H:/CCB-Studio-Suite/tools');sys.argv=['tools/Run-PreviewChecks.py']+sys.argv[1:];runpy.run_path(sys.argv[0],run_name='__main__')"
args=[str(runtime),'-B','-c',bootstrap,'--phase',PHASE,'--keep-fixtures','--',
      ':studio-desktop:test','--tests','*StudioSelectedContrastTest',
      '-Dorg.gradle.jvmargs=-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ActiveProcessorCount=2']
emit('COMMAND.json',dict(arguments=args,startedUtc=now(),noProjectSendOrPush=True))
samples=[];failures=[];pressure=0
print('STARTING_LOCAL_ONLY='+json.dumps(dict(source=SOURCE,phase=PHASE,profile='384/256 Gradle,256 test',before=before)),flush=True)
with (OUT/'runner.log').open('xb') as log,(OUT/'memory-samples.jsonl').open('x',encoding='utf-8') as trace:
    child=subprocess.Popen(args,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
    emit('RUNNER_IDENTITY.json',dict(pid=child.pid,startedUtc=now(),source=SOURCE,phase=PHASE))
    while child.poll() is None:
        try:
            s=snapshot(child.pid);samples.append(s);trace.write(json.dumps(s)+'\n');trace.flush()
            severe=(s['committedBytes']>=.95*s['commitLimitBytes'] and s['availableBytes']<128*1024**2) or (s['availableBytes']<64*1024**2 and s['pageReadsPerSecond']>1000)
            pressure=pressure+1 if severe else 0
            if pressure>=2 or s['foreign'] or s['hFreeBytes']<hmin or s['cFreeBytes']<805306368:
                print('INTERVENTION_REQUIRED_NORMAL_CANCEL_OWN_RUN_ONLY='+json.dumps(s),flush=True)
            print('MEMORY_SAMPLE='+json.dumps(dict(atUtc=s['atUtc'],available=s['availableBytes'],commit=s['committedBytes'],ownedPrivate=sum(x['privatePageCountBytes'] for x in s['owned']),jvms=[dict(pid=x['pid'],flags=x['jvmFlags']) for x in s['owned'] if x['name'] in ['java.exe','javaw.exe']],foreign=s['foreign'])),flush=True)
        except Exception as e:
            failures.append(dict(atUtc=now(),type=type(e).__name__,message=str(e)))
            print('MONITOR_FAILURE='+str(e),flush=True)
        time.sleep(3)
    exitcode=child.wait()
after=snapshot()
peaks={}
for s in samples:
 for p in s['owned']:
  key=str(p['pid'])+'@'+p['createdUtc']
  if key not in peaks:peaks[key]=dict(p)
  else:
   for field in ['workingSetBytes','peakWorkingSetBytes','privatePageCountBytes','pageFileUsageBytes','peakPageFileUsageBytes']:peaks[key][field]=max(peaks[key][field],p[field])
emit('MONITOR_RESULT.json',dict(finishedUtc=now(),runnerExitCode=exitcode,samples=len(samples),monitorFailures=failures,
 before=before,after=after,minAvailableBytes=min((s['availableBytes'] for s in samples),default=None),
 maxSystemCommittedBytes=max((s['committedBytes'] for s in samples),default=None),
 sampledMaxOwnedPrivateBytes=max((sum(p['privatePageCountBytes'] for p in s['owned']) for s in samples),default=None),
 sampledMaxOwnedWorkingSetBytes=max((sum(p['workingSetBytes'] for p in s['owned']) for s in samples),default=None),
 perProcessObservedPeaks=list(peaks.values()),limits='Sampled aggregate maxima can miss between-sample peaks; OS per-process lifetime peaks only for processes observed. Working sets overlap/share pages and are not unique physical RAM. Monitor helper overhead excluded from owned group but included in system observations.',
 externalSubmission='NONE; PROHIBITED',push='NONE; PROHIBITED'))
print('RUNNER_EXIT='+str(exitcode),flush=True)
print((OUT/'runner.log').read_text(encoding='utf-8',errors='replace')[-12000:],flush=True)
raise SystemExit(exitcode)
