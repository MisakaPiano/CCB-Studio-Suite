param(
    [Parameter(Mandatory=$true)][string]$Phase,
    [Parameter(Mandatory=$true)][string]$ApplicationSource,
    [Parameter(Mandatory=$true)][string]$LocalRecordRoot,
    [Parameter(Mandatory=$true)][string[]]$CheckArguments
)
$ErrorActionPreference='Stop'
if ((Get-Location).Path -ne 'H:\CCB-Studio-Suite') { throw 'Unexpected workspace' }
if ($Phase -notmatch '^[a-z0-9-]+$' -or $ApplicationSource -notmatch '^[a-f0-9]{40}$') { throw 'Invalid phase/source' }
$recordPath=[IO.Path]::GetFullPath($LocalRecordRoot)
if (-not $recordPath.StartsWith('H:\CCB-Studio-Suite\.local\',[StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $recordPath -PathType Container)) { throw 'Unexpected record root' }
if ($CheckArguments.Count -eq 0 -or $CheckArguments[0] -notmatch '^:[a-z-]+:') { throw 'Explicit scoped task arguments required' }
$deadline=[DateTime]::UtcNow.AddMinutes(30);$quietChecks=0
while ([DateTime]::UtcNow -lt $deadline) {
    if (Test-Path -LiteralPath (Join-Path $recordPath 'CANCEL_GATE')) { Write-Output 'CANCELLED_NO_TEST_STARTED';exit 2 }
    $desktopWorkers=@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { ([string]$_.CommandLine).Contains('ChatChatBar-Desktop') })
    if ($desktopWorkers.Count -eq 0) { $quietChecks++ } else { $quietChecks=0 }
    if ($quietChecks -ge 2) {
        if (@(Get-ChildItem .local/windows-preview/evidence -Directory -Filter ('*'+$Phase)).Count -gt 0) { throw 'Existing phase; refusing duplicate execution' }
        $moduleDiff=@(git diff --name-only $ApplicationSource HEAD -- modules tools)
        if ($LASTEXITCODE -ne 0 -or $moduleDiff.Count -ne 0) { throw 'Execution inputs differ from frozen application' }
        $pendingDiff=@(git diff --name-only HEAD -- modules tools)
        if ($LASTEXITCODE -ne 0 -or $pendingDiff.Count -ne 0) { throw 'Uncommitted module/tool changes' }
        $untrackedInputs=@(git ls-files --others --exclude-standard -- modules tools)
        if ($LASTEXITCODE -ne 0 -or $untrackedInputs.Count -ne 0) { throw 'Untracked module/tool inputs' }
        Write-Output ('START_UTC='+[DateTime]::UtcNow.ToString('o'))
        & .local/windows-rc/updater-runtime-115/python.exe -B -c 'import sys,runpy;sys.path.insert(0,"H:/CCB-Studio-Suite/tools");phase,args=sys.argv[1],sys.argv[2:];sys.argv=["tools/Run-PreviewChecks.py","--phase",phase,"--"]+args;runpy.run_path(sys.argv[0],run_name="__main__")' $Phase @CheckArguments
        exit $LASTEXITCODE
    }
    Start-Sleep -Seconds 30
}
$record=@{observedUtc=[DateTime]::UtcNow.ToString('o');phase=$Phase;applicationSource=$ApplicationSource;outcome='RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED';testExecutions=0}
($record|ConvertTo-Json -Compress)|Add-Content -LiteralPath (Join-Path $recordPath 'resource-deferrals.jsonl') -Encoding utf8
Write-Output 'DEFERRED_NO_TEST_STARTED'
