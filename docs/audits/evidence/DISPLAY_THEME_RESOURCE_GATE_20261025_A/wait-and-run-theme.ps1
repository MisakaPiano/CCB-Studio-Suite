$ErrorActionPreference='Stop'
$taskRoot='H:\CCB-Studio-Suite'
$taskRecordRoot=Join-Path $taskRoot '.local\studio-display-theme-20261025'
$taskSource='42146d3705f5cbc9ed392735f9c4727f8a8f67d7'
$taskPhase='display-theme-20261025'
if((Get-Location).Path -ne $taskRoot){throw 'Unexpected workspace'}
. (Join-Path $taskRoot '.local\workspace-file-guard.ps1')
$taskBudget=Get-Content -LiteralPath (Join-Path $taskRecordRoot 'RESOURCE_RECHECK_A.json') -Raw|ConvertFrom-Json
if(-not $taskBudget.budget.boundedScanComplete -or $taskBudget.budget.hBudgetBytes -lt 2194033190 -or $taskBudget.budget.cBudgetBytes -lt 805306368){throw 'Incomplete or reduced resource budget'}
$taskHMinimum=[long]$taskBudget.budget.hBudgetBytes
$taskCMinimum=[long]$taskBudget.budget.cBudgetBytes
$taskStatus=Join-Path $taskRecordRoot 'gate-status.json'
$taskObservations=Join-Path $taskRecordRoot 'gate-observations.jsonl'
$taskRunnerLog=Join-Path $taskRecordRoot 'gate-runner.log'
foreach($taskPath in @($taskStatus,$taskObservations,$taskRunnerLog)){
    Assert-TaskWorkspaceFile -Path $taskPath -New|Out-Null
    $taskHandle=[IO.File]::Open($taskPath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
    $taskHandle.Dispose()
}
$taskOwner=[Guid]::NewGuid().ToString()
$taskInvoked=$false
function Write-ThemeGateState($taskState,$taskExtra){
    $taskIdentity=[ordered]@{owner=$taskOwner;atUtc=[DateTime]::UtcNow.ToString('o');status=$taskState;source=$taskSource;phase=$taskPhase;runnerInvoked=$taskInvoked;details=$taskExtra}
    if((Get-Item -LiteralPath $taskStatus).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Gate status became reparse'}
    $taskIdentity|ConvertTo-Json -Depth 7|Set-Content -LiteralPath $taskStatus -Encoding utf8
}
$taskDeadline=[DateTime]::UtcNow.AddMinutes(30)
$taskQuiet=0
$taskPrevious=''
Write-ThemeGateState 'WAITING_RESOURCE_ADMISSION' @{actualTestsStarted=0;hMinimum=$taskHMinimum;cMinimum=$taskCMinimum}
while([DateTime]::UtcNow -lt $taskDeadline){
    $taskHFree=[IO.DriveInfo]::new('H:\').AvailableFreeSpace
    $taskCFree=[IO.DriveInfo]::new('C:\').AvailableFreeSpace
    $taskOwners=@()
    try {
        $taskOwners=@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction Stop|ForEach-Object{
            [pscustomobject]@{pid=$_.ProcessId;owner=if(-not $_.CommandLine){'UNKNOWN_UNREADABLE'}elseif($_.CommandLine.Contains('ChatChatBar-Desktop')){'DESKTOP'}elseif($_.CommandLine.Contains('CCB-Studio-Suite')){'SUITE_EXISTING_WORK'}else{'UNKNOWN_OTHER_JAVA'}}
        })
    }catch{$taskOwners=@([pscustomobject]@{pid=$null;owner='UNKNOWN_CIM_UNAVAILABLE'})}
    $taskSpace=$taskHFree -ge $taskHMinimum -and $taskCFree -ge $taskCMinimum
    if($taskSpace -and $taskOwners.Count -eq 0){$taskQuiet++}else{$taskQuiet=0}
    $taskSnapshot=[ordered]@{atUtc=[DateTime]::UtcNow.ToString('o');hFree=$taskHFree;cFree=$taskCFree;spaceAdmitted=$taskSpace;owners=$taskOwners;quietObservations=$taskQuiet}
    $taskKey=([string]$taskSpace)+':'+(($taskOwners|ConvertTo-Json -Compress))+':'+[string]$taskQuiet
    if($taskKey -ne $taskPrevious){
        $taskSnapshot|ConvertTo-Json -Depth 5 -Compress|Add-Content -LiteralPath $taskObservations -Encoding utf8
        Write-ThemeGateState 'WAITING_RESOURCE_ADMISSION' $taskSnapshot
        Write-Output ('RESOURCE_OBSERVATION='+($taskSnapshot|ConvertTo-Json -Depth 5 -Compress))
        $taskPrevious=$taskKey
    }
    if($taskQuiet -ge 2){
        if(@(Get-ChildItem -LiteralPath (Join-Path $taskRoot '.local\windows-preview\evidence') -Directory -Filter ('*-'+$taskPhase)).Count -ne 0){throw 'Existing phase; refusing duplicate execution'}
        foreach($taskArguments in @(
            @('diff','--name-only',$taskSource,'HEAD','--','modules','tools'),
            @('diff','--name-only','HEAD','--','modules','tools'),
            @('ls-files','--others','--exclude-standard','--','modules','tools')
        )){
            $taskDrift=@(& git @taskArguments)
            if($LASTEXITCODE -ne 0 -or $taskDrift.Count -ne 0){throw 'Frozen execution inputs differ'}
        }
        # Keep the original conservative budget immediately before invoking the serial runner.
        if([IO.DriveInfo]::new('H:\').AvailableFreeSpace -lt $taskHMinimum -or [IO.DriveInfo]::new('C:\').AvailableFreeSpace -lt $taskCMinimum){$taskQuiet=0;continue}
        $taskInvoked=$true
        Write-ThemeGateState 'RUNNER_INVOKED_RESULT_NOT_OBSERVED' @{actualTests='UNKNOWN_UNTIL_FRESH_RUNNER_SUMMARY';admission=$taskSnapshot}
        Write-Output ('RUNNER_START_UTC='+[DateTime]::UtcNow.ToString('o'))
        & (Join-Path $taskRoot '.local\windows-rc\updater-runtime-115\python.exe') -B -c 'import sys,runpy;sys.path.insert(0,"H:/CCB-Studio-Suite/tools");sys.argv=["tools/Run-PreviewChecks.py","--phase","display-theme-20261025","--",":studio-desktop:test","--tests","*StudioAppearanceTest"];runpy.run_path(sys.argv[0],run_name="__main__")' *> $taskRunnerLog
        $taskExit=$LASTEXITCODE
        Write-ThemeGateState 'RUNNER_EXITED_SUMMARY_TO_READ' @{exitCode=$taskExit;actualTests='READ_FRESH_SUMMARY_NOT_EXIT_CODE'}
        Write-Output ('RUNNER_EXIT_CODE='+$taskExit)
        Get-Content -LiteralPath $taskRunnerLog -Tail 10
        exit $taskExit
    }
    Start-Sleep -Seconds 30
}
Write-ThemeGateState 'RESOURCE_GATE_TIMEOUT_NO_TEST_STARTED' @{actualTestsStarted=0}
Write-Output 'DEFERRED_NO_TEST_STARTED'
