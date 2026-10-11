$running = @(Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -and
    $_.CommandLine.IndexOf([string]$manifest.serverRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0
})
$targetConfig = Join-Path ([string]$manifest.serverRoot) 'game.yml'
if (Test-Path -LiteralPath $targetConfig) {
    $gamePortLine = [regex]::Match([IO.File]::ReadAllText($targetConfig), '(?m)^game-port:\s*(\d+)')
    if ($gamePortLine.Success -and (Get-NetTCPConnection -LocalPort ([int]$gamePortLine.Groups[1].Value) -State Listen -ErrorAction SilentlyContinue)) { throw 'De server van dit doel draait nog; stop hem voor installatie of rollback.' }
}
if ($running.Count -gt 0) { throw 'De server van dit doel draait nog; stop hem voor installatie of rollback.' }
