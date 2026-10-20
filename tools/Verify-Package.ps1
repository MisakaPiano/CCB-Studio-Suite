# Read-only delivery verification. Does not initialize Git, download or modify files.
[CmdletBinding()]
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [switch]$AllowExtraFiles
)
$ErrorActionPreference = 'Stop'
$resolved = (Resolve-Path -LiteralPath $Root).Path
$manifest = Get-Content -LiteralPath (Join-Path $resolved 'BUNDLE_MANIFEST.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$expected = @{}
$errors = New-Object 'System.Collections.Generic.List[string]'
$sep = [IO.Path]::DirectorySeparatorChar
$prefix = $resolved.TrimEnd([char[]]@([char]'\', [char]'/')) + $sep
foreach ($item in $manifest.files) {
    $rel = [string]$item.path
    if ([IO.Path]::IsPathRooted($rel) -or $rel -match '(^|/)\.\.(/|$)' -or $rel.Contains('\')) {
        throw "Unsafe manifest path: $rel"
    }
    if ($expected.ContainsKey($rel)) { throw "Duplicate manifest path: $rel" }
    $expected[$rel] = $true
    $full = [IO.Path]::GetFullPath((Join-Path $resolved $rel))
    if (-not $full.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { throw "Outside root: $rel" }
    if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { $errors.Add("MISSING: $rel"); continue }
    $f = Get-Item -LiteralPath $full -Force
    $ancestor = $f
    $unsafe = $false
    while ($null -ne $ancestor -and $ancestor.FullName -ne $resolved) {
        if (($ancestor.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { $unsafe = $true; break }
        if ($ancestor -is [IO.FileInfo]) { $ancestor = $ancestor.Directory } else { $ancestor = $ancestor.Parent }
    }
    if ($unsafe) { $errors.Add("REPARSE POINT: $rel"); continue }
    $hash = (Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($f.Length -ne [long]$item.sizeBytes -or $hash -ne [string]$item.sha256) { $errors.Add("MISMATCH: $rel") }
}
if (-not $AllowExtraFiles) {
    foreach ($f in Get-ChildItem -LiteralPath $resolved -File -Recurse -Force) {
        $rel = $f.FullName.Substring($prefix.Length).Replace('\', '/')
        if (-not $expected.ContainsKey($rel) -and $manifest.excludedFromManifest -notcontains $rel) { $errors.Add("EXTRA: $rel") }
    }
}
if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Output $_ }
    throw 'Original delivery package differs. Do not overwrite later project edits to make this check pass.'
}
Write-Output ("PASS: {0} files match SHA-256 and size. This is not product acceptance." -f $expected.Count)
