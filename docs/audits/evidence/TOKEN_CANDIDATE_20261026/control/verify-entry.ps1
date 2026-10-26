$ErrorActionPreference='Stop'
. H:\CCB-Studio-Suite\.local\workspace-file-guard.ps1
$taskBase='H:\CCB-Studio-Suite\.local\token-candidate-20261026'
$taskEntry=Join-Path $taskBase 'entry-correction'
$taskInstall=Join-Path $taskBase 'isolated-install'
$taskProfile=Join-Path $taskBase 'entry-review/profile'
$taskWorkspace=Join-Path $taskBase 'entry-review/workspace'
$taskRuntime=Join-Path $taskBase 'entry-review/runtime'
$taskJdk='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin'
$taskUtf8=[Text.UTF8Encoding]::new($false)
if((Get-Content -LiteralPath (Join-Path $taskEntry 'MODE.txt') -Raw) -cne 'PROBE'){throw 'Fresh PROBE mode required'}
if(Test-Path -LiteralPath $taskWorkspace){throw 'Synthetic workspace already exists'}
foreach($taskSub in @('Local','Roaming','tmp')){[IO.Directory]::CreateDirectory((Join-Path $taskProfile $taskSub))|Out-Null}
$taskSeedClasses=Join-Path $taskBase 'seed-classes'
if(Test-Path -LiteralPath $taskSeedClasses){throw 'Fresh seed class directory required'}
[IO.Directory]::CreateDirectory($taskSeedClasses)|Out-Null
$taskEnv=[ordered]@{SystemRoot=$env:SystemRoot;WINDIR=$env:SystemRoot;SystemDrive=[IO.Path]::GetPathRoot($env:SystemRoot).TrimEnd('\');ComSpec=(Join-Path $env:SystemRoot 'System32/cmd.exe');PATH=($taskJdk+';'+(Join-Path $env:SystemRoot 'System32')+';'+$env:SystemRoot);LOCALAPPDATA=(Join-Path $taskProfile 'Local');APPDATA=(Join-Path $taskProfile 'Roaming');USERPROFILE=$taskProfile;HOME=$taskProfile;TEMP=(Join-Path $taskProfile 'tmp');TMP=(Join-Path $taskProfile 'tmp')}
function Invoke-IsolatedSeedTool([string]$taskFile,[string[]]$taskArguments){
 $taskInfo=[Diagnostics.ProcessStartInfo]::new();$taskInfo.FileName=$taskFile;$taskInfo.WorkingDirectory=$taskBase;$taskInfo.UseShellExecute=$false;$taskInfo.CreateNoWindow=$true;$taskInfo.RedirectStandardOutput=$true;$taskInfo.RedirectStandardError=$true;$taskInfo.Environment.Clear()
 foreach($taskKey in $taskEnv.Keys){$taskInfo.Environment[$taskKey]=$taskEnv[$taskKey]}
 foreach($taskArg in $taskArguments){$taskInfo.ArgumentList.Add($taskArg)}
 $taskChild=[Diagnostics.Process]::Start($taskInfo);$taskOut=$taskChild.StandardOutput.ReadToEndAsync();$taskErr=$taskChild.StandardError.ReadToEndAsync();$taskChild.WaitForExit()
 return [ordered]@{exe=$taskFile;arguments=$taskArguments;environment=$taskEnv;exitCode=$taskChild.ExitCode;stdout=$taskOut.Result;stderr=$taskErr.Result}
}
$taskCompile=Invoke-IsolatedSeedTool (Join-Path $taskJdk 'javac.exe') @('-J-Xmx128m','-cp',(Join-Path $taskInstall 'app/*'),'-d',$taskSeedClasses,(Join-Path $taskBase 'SeedTheme.java'))
$taskCompilePath=Assert-TaskWorkspaceFile -Path (Join-Path $taskBase 'SEED_COMPILE.json') -New
[IO.File]::WriteAllText($taskCompilePath,($taskCompile|ConvertTo-Json -Depth 6),$taskUtf8)
if($taskCompile.exitCode -ne 0){throw 'Seed helper compilation failed; retain record'}
$taskSeed=Invoke-IsolatedSeedTool (Join-Path $taskJdk 'java.exe') @('-Xmx128m','-XX:ActiveProcessorCount=2',('-Duser.home='+$taskProfile),('-Djava.io.tmpdir='+(Join-Path $taskProfile 'tmp')),'-cp',($taskSeedClasses+';'+(Join-Path $taskInstall 'app/*')),'SeedTheme',$taskWorkspace)
$taskSeedPath=Assert-TaskWorkspaceFile -Path (Join-Path $taskBase 'SEED_RESULT.json') -New
[IO.File]::WriteAllText($taskSeedPath,($taskSeed|ConvertTo-Json -Depth 6),$taskUtf8)
if($taskSeed.exitCode -ne 0){throw 'Synthetic seed failed; retain record'}
$taskLinkPath=Assert-TaskWorkspaceFile -Path (Join-Path $taskBase 'Start-Studio-Tokens-Fix-26ef.lnk') -New
$taskShell='C:\Users\1\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\powershell\pwsh.exe'
$taskLinkArgs='-NoProfile -WindowStyle Hidden -File "'+(Join-Path $taskEntry 'Launch-Candidate.ps1')+'"'
$taskCom=New-Object -ComObject WScript.Shell
$taskLink=$taskCom.CreateShortcut($taskLinkPath);$taskLink.TargetPath=$taskShell;$taskLink.Arguments=$taskLinkArgs;$taskLink.WorkingDirectory=$taskEntry;$taskLink.IconLocation=(Join-Path $taskInstall 'CCBStudioSuitePreview.exe')+',0';$taskLink.Description='26ef Tokens修复；独立隔离候选，不是日常115或旧48eed';$taskLink.Save()
$taskRead=$taskCom.CreateShortcut($taskLinkPath)
if($taskRead.TargetPath -ne $taskShell -or $taskRead.Arguments -ne $taskLinkArgs -or $taskRead.WorkingDirectory -ne $taskEntry){throw 'Persisted shortcut identity mismatch'}
$taskLinkRecord=[ordered]@{path=$taskLinkPath;target=$taskRead.TargetPath;arguments=$taskRead.Arguments;workingDirectory=$taskRead.WorkingDirectory;sha256=(Get-FileHash -LiteralPath $taskLinkPath).Hash.ToLowerInvariant();scope='Task-local link only; no Desktop/StartMenu/global registration';nativeAppStarts=0}
$taskLinkRecordPath=Assert-TaskWorkspaceFile -Path (Join-Path $taskEntry 'SHORTCUT_IDENTITY.json') -New
[IO.File]::WriteAllText($taskLinkRecordPath,($taskLinkRecord|ConvertTo-Json -Depth 4),$taskUtf8)
Start-Process -FilePath $taskLinkPath -WindowStyle Hidden
$taskProbePath=Join-Path $taskEntry 'PROBE_RESULT.json'
for($taskTry=0;$taskTry -lt 30 -and -not(Test-Path -LiteralPath $taskProbePath);$taskTry++){Start-Sleep -Milliseconds 500}
if(-not(Test-Path -LiteralPath $taskProbePath)){throw 'Probe receipt absent; do not launch native app'}
$taskProbe=Get-Content -LiteralPath $taskProbePath -Raw|ConvertFrom-Json
$taskExpectedArgs=@('--studio-root',$taskWorkspace,'--runtime-root',$taskRuntime,'--secondary-workspace','--pause-collections')
if((ConvertTo-Json -InputObject @($taskProbe.arguments) -Compress) -cne (ConvertTo-Json -InputObject $taskExpectedArgs -Compress)){throw 'Actual shortcut arguments mismatch'}
if($taskProbe.studioStarted -ne $false){throw 'Probe unexpectedly started app'}
foreach($taskPair in @(@('localAppData','Local'),@('appData','Roaming'),@('userProfile',''),@('home',''),@('temp','tmp'),@('tmp','tmp'))){$taskExpected=if($taskPair[1]){Join-Path $taskProfile $taskPair[1]}else{$taskProfile};if($taskProbe.($taskPair[0]) -cne $taskExpected){throw ('Probe environment mismatch: '+$taskPair[0])}}
$taskFallback=Join-Path $taskProfile 'Local/CCBStudioSuite/workspace-v3'
if((Test-Path -LiteralPath $taskFallback) -or (Test-Path -LiteralPath (Join-Path $taskBase 'launch-current.json'))){throw 'Unexpected fallback/default pointer created'}
$taskProbeVerified=[ordered]@{status='REAL_SHORTCUT_PROBE_PASS';observedUtc=[DateTime]::UtcNow.ToString('o');arguments=$taskExpectedArgs;childEnvironmentIsolated=$true;nativeAppStarts=0;syntheticSeed='DARK / CUSTOM_ACCENT / 000000, no accounts/provider';fallbackCreated=$false;defaultPointerCreated=$false;dailyInstallationAccessed=$false;privateGalleryAccessed=$false;packageLock='PACKAGE_LOCK.json';nativeStartup='NOT TESTED';humanAcceptance='NOT TESTED';entryMeaning='READY permits user launch through verified explicit roots and isolated child environment; not a native or user PASS'}
$taskProbeVerifiedPath=Assert-TaskWorkspaceFile -Path (Join-Path $taskEntry 'PROBE_VERIFIED.json') -New
[IO.File]::WriteAllText($taskProbeVerifiedPath,($taskProbeVerified|ConvertTo-Json -Depth 5),$taskUtf8)
$taskModePath=Join-Path $taskEntry 'MODE.txt'
$taskModeChecked=Assert-TaskWorkspaceFile -Path $taskModePath -ExpectedSha256 (Get-FileHash -LiteralPath $taskModePath).Hash
[IO.File]::WriteAllText($taskModeChecked,'READY',$taskUtf8)
$taskProbeVerified|ConvertTo-Json -Depth 5
