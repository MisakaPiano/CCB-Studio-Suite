$ErrorActionPreference='Stop'
$taskProbeRoot='H:\CCB-Studio-Suite\.local\appearance-text-actions-20261026\entry-correction'
$taskPayload=[ordered]@{arguments=@($args);localAppData=$env:LOCALAPPDATA;appData=$env:APPDATA;userProfile=$env:USERPROFILE;home=$env:HOME;temp=$env:TEMP;tmp=$env:TMP;observedUtc=[DateTime]::UtcNow.ToString('o');studioStarted=$false}
[IO.File]::WriteAllText((Join-Path $taskProbeRoot 'PROBE_RESULT.json'),($taskPayload|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))