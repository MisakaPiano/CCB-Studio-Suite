$ErrorActionPreference='Stop'
$entryTestRoot=Join-Path (Resolve-Path "$PSScriptRoot/..").Path ('.local/windows-rc/entrypoint-115-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $entryTestRoot | Out-Null
$entryCompiler=Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
$entryRoot=Join-Path $entryTestRoot '组织与空格 App'
New-Item -ItemType Directory -Path $entryRoot | Out-Null
$entryLauncher=Join-Path $entryRoot 'CCBStudioLauncher.exe'
& $entryCompiler /nologo /target:winexe /platform:x64 /optimize+ /reference:System.Windows.Forms.dll /reference:System.Web.Extensions.dll ("/win32icon:"+(Resolve-Path "$PSScriptRoot/../modules/studio-desktop/src/main/resources/brand/ccb-blue.ico").Path) "/out:$entryLauncher" (Resolve-Path "$PSScriptRoot/windows-launcher/Launcher.cs").Path
if($LASTEXITCODE){throw 'Launcher compile failed'}
$entryFixtureSource=Join-Path $entryTestRoot 'Marker.cs'
@'
using System;using System.IO;class Marker{static void Main(string[] args){File.WriteAllLines(Path.Combine(AppDomain.CurrentDomain.BaseDirectory,"arguments.txt"),args);}}
'@ | Set-Content -LiteralPath $entryFixtureSource -Encoding UTF8
$entryData=Join-Path $entryRoot '手动资料'
$entryRuntime=Join-Path $entryRoot '运行配置'
$entryResults=@()
foreach($entryVersion in 114,115){
    $entryVersionRoot=Join-Path $entryRoot "version-$entryVersion"
    New-Item -ItemType Directory -Path $entryVersionRoot | Out-Null
    $entryTarget=Join-Path $entryVersionRoot 'CCBStudioSuitePreview.exe'
    & $entryCompiler /nologo /target:winexe "/out:$entryTarget" $entryFixtureSource
    if($LASTEXITCODE){throw 'Marker compile failed'}
    @{format=1;version=$entryVersion;installed=$entryVersionRoot;data=$entryData;runtime=$entryRuntime;exeSha256=(Get-FileHash -LiteralPath $entryTarget -Algorithm SHA256).Hash.ToLowerInvariant()} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $entryRoot 'launch-current.json') -Encoding UTF8
    $entryReport=Join-Path $entryTestRoot "resolve-$entryVersion.json"
    $entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--resolve-report',('"'+$entryReport+'"')) -WindowStyle Hidden -PassThru -Wait
    if($entryProcess.ExitCode){throw 'Stable target selection failed'}
    $entryResolved=Get-Content -LiteralPath $entryReport -Raw | ConvertFrom-Json
    if($entryResolved.version -ne $entryVersion -or $entryResolved.installed -ne $entryVersionRoot){throw 'Wrong selected version'}
    $entryProcess=Start-Process -FilePath $entryLauncher -WindowStyle Hidden -PassThru -Wait
    if($entryProcess.ExitCode){throw 'Launch failed'}
    $entryArgumentFile=Join-Path $entryVersionRoot 'arguments.txt'
    for($entryWait=0;$entryWait -lt 50 -and !(Test-Path -LiteralPath $entryArgumentFile);$entryWait++){Start-Sleep -Milliseconds 50}
    $entryArguments=Get-Content -LiteralPath $entryArgumentFile -Encoding UTF8
    if($entryArguments.Count -ne 4 -or $entryArguments[1] -ne $entryData -or $entryArguments[3] -ne $entryRuntime){throw 'Unicode or quoting damaged profile paths'}
    $entryResults+=@{version=$entryVersion;selected=$entryResolved.installed;profilePathsPreserved=$true}
}
$entryShortcut=Join-Path $entryRoot 'CCB Studio Suite.lnk'
$entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--shortcut',('"'+$entryShortcut+'"')) -WindowStyle Hidden -PassThru -Wait
if($entryProcess.ExitCode){throw 'Shortcut failed'}
$entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--shortcut',('"'+$entryShortcut+'"')) -WindowStyle Hidden -PassThru -Wait
if($entryProcess.ExitCode){throw 'Own shortcut replacement failed'}
$entryLinkReport=Join-Path $entryTestRoot 'shortcut.json'
$entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--inspect-shortcut',('"'+$entryShortcut+'"'),('"'+$entryLinkReport+'"')) -WindowStyle Hidden -PassThru -Wait
if($entryProcess.ExitCode){throw 'Shortcut inspection failed'}
$entryLink=Get-Content -LiteralPath $entryLinkReport -Raw | ConvertFrom-Json
if($entryLink.target -ne $entryLauncher -or $entryLink.appId -ne 'MisakaPiano.CCBStudioSuite.Preview'){throw 'Shortcut identity or fixed target mismatch'}
$entryForeignLink=Join-Path $entryRoot 'unrelated.lnk'
'keep this unrelated file' | Set-Content -LiteralPath $entryForeignLink
$entryForeignHash=(Get-FileHash -LiteralPath $entryForeignLink).Hash
$entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--shortcut',('"'+$entryForeignLink+'"')) -WindowStyle Hidden -PassThru -Wait
if($entryProcess.ExitCode -eq 0 -or (Get-FileHash -LiteralPath $entryForeignLink).Hash -ne $entryForeignHash){throw 'Unrelated shortcut file changed'}
Add-Type -AssemblyName System.Windows.Forms
$entryForm=New-Object System.Windows.Forms.Form
try{
    $entryHandle=$entryForm.Handle.ToInt64().ToString()
    $entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--window',$entryHandle,('"'+$entryLauncher+'"')) -WindowStyle Hidden -PassThru -Wait
    if($entryProcess.ExitCode){throw 'Window relaunch properties failed'}
    $entryWindowReport=Join-Path $entryTestRoot 'window.json'
    $entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--inspect-window',$entryHandle,('"'+$entryWindowReport+'"')) -WindowStyle Hidden -PassThru -Wait
    if($entryProcess.ExitCode){throw 'Window inspection failed'}
    $entryWindow=Get-Content -LiteralPath $entryWindowReport -Raw | ConvertFrom-Json
    if($entryWindow.appId -ne $entryLink.appId -or $entryWindow.command -ne ('"'+$entryLauncher+'"')){throw 'Window and shortcut differ'}
}finally{$entryForm.Dispose()}
Add-Content -LiteralPath $entryTarget -Value 'tampered'
$entryProcess=Start-Process -FilePath $entryLauncher -ArgumentList @('--resolve-report',('"'+(Join-Path $entryTestRoot 'tamper.json')+'"')) -WindowStyle Hidden -PassThru -Wait
if($entryProcess.ExitCode -eq 0){throw 'Changed executable was accepted'}
@{status='PASS';scope='Synthetic marker executables and hidden owned native window; actual user taskbar pin not tested';versions=$entryResults;shortcut=$entryLink;window=$entryWindow;tamperRejected=$true;desktopOrStartMenuChanged=$false} | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $entryTestRoot 'RESULT.json') -Encoding UTF8
Write-Output "EVIDENCE=$entryTestRoot"
