[CmdletBinding()]
param([string]$Root = '', [string]$GradleCommand = '', [string]$TestArtifactCache = '')
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Root)) { $Root = Split-Path -Parent $PSScriptRoot }
$suiteRoot = (Resolve-Path -LiteralPath $Root).Path
if (-not (Test-Path -LiteralPath (Join-Path $suiteRoot 'modules/asset-library/build.gradle.kts'))) { throw 'Not the Suite asset-library workspace' }
if ((Split-Path $suiteRoot -Leaf) -eq 'ChatChatBar-Desktop') { throw 'Desktop workspace is forbidden' }
$runRoot = Join-Path $suiteRoot '.local/a1-001'
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
if ([string]::IsNullOrWhiteSpace($GradleCommand)) {
    $gradleCandidates = @(Get-ChildItem -Path (Join-Path $env:USERPROFILE '.gradle/wrapper/dists/gradle-9.1.0-bin/*/gradle-9.1.0/bin/gradle.bat') -File -ErrorAction SilentlyContinue)
    if ($gradleCandidates.Count -ne 1) { throw 'Pass -GradleCommand with an installed Gradle 9.1.0; this offline runner does not download a distribution' }
    $GradleCommand = $gradleCandidates[0].FullName
}
if ([string]::IsNullOrWhiteSpace($TestArtifactCache)) { $TestArtifactCache = Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1' }
$artifacts = @(
    @{ Group='junit'; Name='junit'; Version='4.13.2'; Sha='8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3' },
    @{ Group='org.hamcrest'; Name='hamcrest-core'; Version='1.3'; Sha='66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9' }
)
$repository = Join-Path $runRoot 'test-maven'
foreach ($artifact in $artifacts) {
    $name = '{0}-{1}.jar' -f $artifact.Name,$artifact.Version
    $source = @(Get-ChildItem -LiteralPath (Join-Path $TestArtifactCache ($artifact.Group+'/'+$artifact.Name+'/'+$artifact.Version)) -Recurse -File -Filter $name)
    if ($source.Count -ne 1) { throw "Expected one cached $name" }
    if ((Get-FileHash -LiteralPath $source[0].FullName -Algorithm SHA256).Hash.ToLowerInvariant() -ne $artifact.Sha) { throw "Checksum mismatch: $name" }
    $target = Join-Path $repository ($artifact.Group.Replace('.','/')+'/'+$artifact.Name+'/'+$artifact.Version+'/'+$name)
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
    Copy-Item -LiteralPath $source[0].FullName -Destination $target
}
$stamp = [DateTimeOffset]::UtcNow.ToString('yyyyMMddTHHmmssfffZ')
$evidence = Join-Path $suiteRoot ('docs/audits/evidence/A1_001/'+$stamp)
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
$gradleArgs = @('-g',(Join-Path $runRoot 'gradle-home'),'--offline','--no-daemon','--max-workers=1','--console=plain','--dependency-verification=strict',
    ('-PassetOfflineTestRepository='+$repository.Replace('\','/')),':asset-library:test','--rerun-tasks')
Push-Location $suiteRoot
try {
    $before = @{}
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $suiteRoot 'modules/asset-library/src') -Recurse -File) { $before[$file.FullName] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash }
    $watch = [Diagnostics.Stopwatch]::StartNew()
    & $GradleCommand @gradleArgs *> (Join-Path $evidence 'gradle.log')
    $exitCode = $LASTEXITCODE
    $watch.Stop()
    foreach ($file in $before.Keys) { if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $before[$file]) { throw 'Source changed during test execution' } }
    $xmlFiles = @(Get-ChildItem -LiteralPath (Join-Path $suiteRoot 'modules/asset-library/build/test-results/test') -File -Filter '*.xml' -ErrorAction SilentlyContinue)
    $total = 0; $failures = 0; $errors = 0; $skipped = 0
    foreach ($xml in $xmlFiles) {
        Copy-Item -LiteralPath $xml.FullName -Destination (Join-Path $evidence $xml.Name)
        [xml]$data = Get-Content -LiteralPath $xml.FullName -Raw
        $total += [int]$data.testsuite.tests; $failures += [int]$data.testsuite.failures
        $errors += [int]$data.testsuite.errors; $skipped += [int]$data.testsuite.skipped
    }
    $status = if ($exitCode -eq 0 -and $total -gt 0 -and $failures -eq 0 -and $errors -eq 0) { 'PASS' } else { 'FAILED' }
    $summary = [ordered]@{ status=$status; gradleExitCode=$exitCode; elapsedSeconds=$watch.Elapsed.TotalSeconds; tests=$total; failures=$failures; errors=$errors; skipped=$skipped;
        gradleCommand=$GradleCommand; arguments=$gradleArgs; dependencyNetworkDownloadBytes=0; dependencySource='Explicit local copy of two checksum-verified cached test JARs; production has no external dependency';
        sourceSha256=$before; baseCommit=(& git rev-parse HEAD); branch=(& git branch --show-current); peakMemory='NOT MEASURED'; productAcceptance='NOT TESTED'; evidence=$evidence }
    $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $evidence 'summary.json') -Encoding utf8
    Get-Content -LiteralPath (Join-Path $evidence 'gradle.log') -Tail 65
    $summary | ConvertTo-Json -Depth 3
    if ($status -ne 'PASS') { throw "Asset durability tests failed; preserve evidence at $evidence" }
} finally { Pop-Location }
