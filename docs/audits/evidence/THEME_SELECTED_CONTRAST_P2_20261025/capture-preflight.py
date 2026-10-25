"""Read-only resource/input observation; NEVER launches tests, builds or cleanup."""
from pathlib import Path
import datetime, hashlib, json, os, stat, subprocess, time

root=Path('H:/CCB-Studio-Suite')
assert Path.cwd().resolve()==root.resolve()
out=root/'.local/theme-selected-contrast-20261025'
source='c39e860252fdd96c9bec5b5d42d1abd988d3ec06'
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==source
assert not subprocess.check_output(['git','status','--porcelain','--','modules','tools']).strip()
def stamp():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def emit(name,value):
    p=out/name
    assert not p.exists() and not p.is_symlink()
    with p.open('x',encoding='utf-8',newline='\n') as f:json.dump(value,f,indent=2,ensure_ascii=False);f.write('\n')
def scan(relative):
    path=Path(chr(92)*2+'?'+chr(92)+str(root/relative));todo=[path];total=files=0;begin=time.monotonic()
    while todo:
        if time.monotonic()-begin>15 or files>20000:raise RuntimeError('Bounded build scan incomplete')
        item=todo.pop();s=item.lstat()
        if s.st_file_attributes & stat.FILE_ATTRIBUTE_REPARSE_POINT:raise RuntimeError('Reparse build input')
        if stat.S_ISDIR(s.st_mode):
            with os.scandir(item) as entries:todo.extend(Path(e.path) for e in entries)
        elif stat.S_ISREG(s.st_mode):total+=s.st_size;files+=1
        else:raise RuntimeError('Unknown build input kind')
    return dict(bytes=total,files=files,complete=True)
started=stamp()
outputs={f'modules/{module}/build':scan(f'modules/{module}/build') for module in
         ['studio-desktop','studio-core','image-authority','asset-library']}
hminimum=max(2194033190,2*sum(x['bytes'] for x in outputs.values())+1248*1024**2)
cmd=r"""$ErrorActionPreference='Stop'
$owners=@(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe' OR Name='jpackage.exe' OR Name='jlink.exe' OR Name='makensis.exe' OR Name='7z.exe' OR Name='msiexec.exe' OR Name='light.exe' OR Name='candle.exe' OR Name='dotnet.exe'" | ForEach-Object {
    [pscustomobject]@{pid=$_.ProcessId;name=$_.Name;owner=if(-not $_.CommandLine){'UNKNOWN_UNREADABLE'}elseif($_.CommandLine.Contains('ChatChatBar-Desktop')){'DESKTOP'}elseif($_.CommandLine.Contains('CCB-Studio-Suite')){'SUITE_EXISTING_WORK'}else{'UNKNOWN_OTHER'}}
})
[ordered]@{atUtc=[DateTime]::UtcNow.ToString('o');hFreeBytes=[IO.DriveInfo]::new('H:\').AvailableFreeSpace;cFreeBytes=[IO.DriveInfo]::new('C:\').AvailableFreeSpace;freeMemoryBytes=([long](Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory*1024);owners=$owners}|ConvertTo-Json -Depth 5 -Compress
"""
snap=json.loads(subprocess.check_output(['pwsh','-NoProfile','-Command',cmd],encoding='utf-8'))
admit=snap['hFreeBytes']>=hminimum and snap['cFreeBytes']>=805306368 and snap['freeMemoryBytes']>=2147483648 and not snap['owners']
record=dict(observedStartUtc=started,observedEndUtc=stamp(),source=source,
    status='SINGLE_OBSERVATION_ONLY' if admit else 'RESOURCE_NOT_ADMITTED_NO_TEST_STARTED',
    budget=dict(outputs=outputs,boundedScanComplete=True,hBudgetBytes=hminimum,cBudgetBytes=805306368,
                memoryFreeMinimumBytes=2147483648,basis='Original floors unchanged or increased; 2x current build bytes + 1248 MiB planning allowance, not measured peak'),
    snapshot=snap,hShortfallBytes=max(0,hminimum-snap['hFreeBytes']),runnerInvocations=0,actualJUnitMethods=0,
    scope='New three methods only; accepted StudioAppearanceTest four methods not rerun; --keep-fixtures; no package/install/release/cleanup')
name='preflight-'+datetime.datetime.now(datetime.timezone.utc).strftime('%H%M%S%f')+'.json'
emit(name,record)
print(json.dumps(dict(file=name,**record),ensure_ascii=False))
