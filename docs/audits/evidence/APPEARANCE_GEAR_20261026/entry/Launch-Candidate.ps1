$ErrorActionPreference='Stop'
$taskBase='H:\CCB-Studio-Suite\.local\appearance-gear-20261026'
$taskEntry=Join-Path $taskBase 'entry-correction'
$taskShell=[Environment]::ProcessPath
function Assert-LocalCandidatePath([string]$taskPath){
 $taskAbs=[IO.Path]::GetFullPath($taskPath)
 if(-not $taskAbs.StartsWith($taskBase+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Candidate path outside fixed root'}
 $taskCursor=$taskAbs
 for($taskDepth=0;$taskDepth -lt 32;$taskDepth++){
  if(Test-Path -LiteralPath $taskCursor){$taskItem=Get-Item -LiteralPath $taskCursor;if($taskItem.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Candidate path has reparse point'}}
  if($taskCursor -eq $taskBase){return $taskAbs}
  $taskCursor=[IO.Path]::GetDirectoryName($taskCursor)
 }
 throw 'Candidate path validation did not reach fixed root'
}
$taskMode=(Get-Content -LiteralPath (Assert-LocalCandidatePath (Join-Path $taskEntry 'MODE.txt')) -Raw).Trim()
if($taskMode -notin @('PROBE','APP','REVIEW')){throw 'Entry disabled until validated'}
$taskExe=Assert-LocalCandidatePath (Join-Path $taskBase 'isolated-install/CCBStudioSuitePreview.exe')
$taskJar=Assert-LocalCandidatePath (Join-Path $taskBase 'isolated-install/app/studio-desktop-863147116b8b314d5fbb4bae898951f.jar')
if((Get-FileHash -LiteralPath $taskJar).Hash.ToLowerInvariant() -ne '7aa4793d26b090cffa9c5aeaffecd8f94b75a22a86af7f4eec8c7edfc966b378'){throw 'Candidate app identity changed'}
$taskProfile=Assert-LocalCandidatePath (Join-Path $taskBase 'entry-review/profile')
$taskWorkspace=Assert-LocalCandidatePath (Join-Path $taskBase 'entry-review/workspace')
$taskRuntime=Assert-LocalCandidatePath (Join-Path $taskBase 'entry-review/runtime')
foreach($taskSub in @('Local','Roaming','tmp')){[IO.Directory]::CreateDirectory((Join-Path $taskProfile $taskSub))|Out-Null}
$taskEnv=[ordered]@{SystemRoot=$env:SystemRoot;WINDIR=$env:SystemRoot;SystemDrive=[IO.Path]::GetPathRoot($env:SystemRoot).TrimEnd('\');ComSpec=(Join-Path $env:SystemRoot 'System32/cmd.exe');PATH=((Join-Path $env:SystemRoot 'System32')+';'+$env:SystemRoot);LOCALAPPDATA=(Join-Path $taskProfile 'Local');APPDATA=(Join-Path $taskProfile 'Roaming');USERPROFILE=$taskProfile;HOME=$taskProfile;TEMP=(Join-Path $taskProfile 'tmp');TMP=(Join-Path $taskProfile 'tmp')}
$taskArgs=@('--studio-root',$taskWorkspace,'--runtime-root',$taskRuntime,'--secondary-workspace','--pause-collections')
$taskInfo=[Diagnostics.ProcessStartInfo]::new();$taskInfo.UseShellExecute=$false;$taskInfo.CreateNoWindow=$true;$taskInfo.WorkingDirectory=Join-Path $taskBase 'isolated-install';$taskInfo.Environment.Clear()
foreach($taskKey in $taskEnv.Keys){$taskInfo.Environment[$taskKey]=$taskEnv[$taskKey]}
if($taskMode -eq 'PROBE'){$taskInfo.FileName=$taskShell;foreach($taskArg in @('-NoProfile','-File',(Join-Path $taskEntry 'Probe.ps1'))){$taskInfo.ArgumentList.Add($taskArg)}}else{
 if(-not(Test-Path -LiteralPath (Join-Path $taskEntry 'PROBE_VERIFIED.json'))){throw 'Real shortcut chain probe not verified'}
 if((Get-FileHash -LiteralPath $taskExe).Hash.ToLowerInvariant() -ne '494f137e9fa8cb07b7f1d19ed6cc3e6dfac4afd3df16397390d30f1aee58541f'){throw 'Candidate native launcher changed'}
 if($taskMode -eq 'REVIEW' -and -not(Test-Path -LiteralPath (Join-Path $taskEntry 'APP_VERIFIED.json'))){throw 'Candidate app entry has not been verified'}
 $taskInfo.FileName=$taskExe
}
foreach($taskArg in $taskArgs){$taskInfo.ArgumentList.Add($taskArg)}
$taskReceipt=Join-Path $taskEntry ('LAUNCH_'+$taskMode+$(if($taskMode -eq 'REVIEW'){'_'+[Guid]::NewGuid().ToString('N')}else{''})+'.json');if(Test-Path -LiteralPath $taskReceipt){throw 'Single-attempt receipt exists; refuse blind rerun'}
$taskPlan=[ordered]@{mode=$taskMode;exe=$taskInfo.FileName;arguments=@($taskInfo.ArgumentList);environment=$taskEnv;startedUtc=[DateTime]::UtcNow.ToString('o');expectedWorkspace=$taskWorkspace;expectedRuntime=$taskRuntime;privateDefaultFallbackPrevented='Child LOCALAPPDATA redirected even if app arguments are ignored';sourceJarSha256=(Get-FileHash -LiteralPath $taskJar).Hash.ToLowerInvariant()}
$taskChild=[Diagnostics.Process]::Start($taskInfo);$taskPlan.pid=$taskChild.Id
[IO.File]::WriteAllText($taskReceipt,($taskPlan|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))