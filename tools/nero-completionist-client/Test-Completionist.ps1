param([ValidateSet('Prepare','Start','Stop','Status')][string]$Action = 'Start', [switch]$NoClient)
Import-Module (Join-Path $PSHOME 'Modules/Microsoft.PowerShell.Utility/Microsoft.PowerShell.Utility.psd1')
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$package = [IO.Path]::GetFullPath($PSScriptRoot)
$manifest = Get-Content -LiteralPath (Join-Path $package 'test-package.json') -Raw | ConvertFrom-Json
$target = [IO.Path]::GetFullPath([string]$manifest.serverRoot)
$root = Join-Path $package 'completionist-test-server'
$java21 = 'C:\Program Files\BellSoft\LibericaJDK-21\bin\java.exe'
$java11 = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.27.6-hotspot\bin\java.exe'
function Hash([string]$Path) { (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Inside([string]$Path, [string]$Parent) {
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($Parent.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Pad buiten de eigen testmap.' }
    $cursor = $full
    while ($cursor) {
        if ((Test-Path -LiteralPath $cursor) -and ((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Links/junctions zijn niet toegestaan.' }
        $parentDir = [IO.Directory]::GetParent($cursor); if ($null -eq $parentDir) { break }; $cursor = $parentDir.FullName
    }
    return $full
}
Inside $root $package | Out-Null
function OwnedProcess {
    $record = Join-Path $root 'owned-server.json'
    if (-not (Test-Path -LiteralPath $record)) { return $null }
    $state = Get-Content -LiteralPath $record -Raw | ConvertFrom-Json
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($state.pid)" -ErrorAction SilentlyContinue
    if ($null -eq $process) { return $null }
    if ($process.Name -ne 'java.exe' -or -not $process.CommandLine -or $process.CommandLine.IndexOf($root, [StringComparison]::OrdinalIgnoreCase) -lt 0 -or $process.CommandLine -notmatch 'TestServerMain') { throw 'PID hoort niet bij deze testserver.' }
    return $process
}
if ($Action -eq 'Stop') {
    if ($null -ne (OwnedProcess)) { Set-Content -LiteralPath (Join-Path $root 'STOP-TEST') -Value 'stop' -Encoding utf8; Write-Host 'Testserver wordt normaal afgesloten. Sluit het aparte testclientvenster.' }
    else { Write-Host 'Testserver draait niet.' }; return
}
if ($Action -eq 'Status') {
    if ($null -ne (OwnedProcess)) { Write-Host "Testserver draait. Log: $root\boot.log" } else { Write-Host 'Testserver draait niet.' }; return
}
foreach ($file in $manifest.files) {
    $payload = Inside (Join-Path $package ([string]$file.payload)) $package
    if ((Hash $payload) -ne $file.afterSha256) { throw "Pakket beschadigd: $($file.relative)" }
}
if (-not (Test-Path -LiteralPath (Join-Path $root 'ISOLATED-COMPLETIONIST'))) {
    if (Test-Path -LiteralPath $root) { throw 'Onbekende bestaande testmap; gebruik een nieuw pakketpad.' }
    foreach ($file in $manifest.baseFiles) {
        $base = Inside (Join-Path $target ([string]$file.relative)) $target
        if ((Hash $base) -ne $file.sha256) { throw "Baseline is veranderd: $($file.relative)" }
    }
    New-Item -ItemType Directory -Path $root | Out-Null
    foreach ($file in $manifest.baseFiles) {
        $dest = Inside (Join-Path $root ([string]$file.testRelative)) $root
        New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($dest)) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $target ([string]$file.relative)) -Destination $dest
    }
    foreach ($file in $manifest.files) {
        $dest = Inside (Join-Path $root ([string]$file.testRelative)) $root
        New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($dest)) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $package ([string]$file.payload)) -Destination $dest -Force
    }
    [IO.File]::WriteAllText((Join-Path $root 'ISOLATED-COMPLETIONIST'), $root)
}
if (-not (Test-Path -LiteralPath (Join-Path $root 'test-ports.json'))) {
    if ($null -ne (OwnedProcess)) { throw 'De eigen testserver draait al; stop hem voor configuratie.' }
    $listeners = @(); $portNumbers = @()
    try {
        for ($index = 0; $index -lt 6; $index++) {
            $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Any, 0); $listener.Start(); $listeners += $listener; $portNumbers += $listener.LocalEndpoint.Port
        }
    } finally { foreach ($listener in $listeners) { $listener.Stop() } }
    $ports = @{game=$portNumbers[0];http=$portNumbers[1];link=$portNumbers[2];appearance=$portNumbers[3];config=$portNumbers[4];bridge=$portNumbers[5]}
    $ports | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'test-ports.json') -Encoding utf8
    $database = (Join-Path $root '.data\postgres-isolated').Replace('\','/')
    @"
name: "Completionist cape test"
game-port: $($ports.game)
revision: 240
environment: "LIVE"
world: 255
central:
  same-instance: true
  host: "127.0.0.1"
  http-port: $($ports.http)
  link-port: $($ports.link)
  postgres:
    pool-size: 3
    embedded-pgdata-dir: "$database"
"@ | Set-Content -LiteralPath (Join-Path $root 'game.yml') -Encoding utf8
    'appearance.bind=127.0.0.1', ('appearance.port=' + $ports.appearance) | Set-Content -LiteralPath (Join-Path $root 'nero-completionist.properties') -Encoding utf8
    [IO.File]::WriteAllText((Join-Path $root 'ISOLATED-COMPLETIONIST'), $root)
    [IO.File]::WriteAllText((Join-Path $root 'package-server.sha256'), [string]$manifest.serverSha256)
}
$ports = Get-Content -LiteralPath (Join-Path $root 'test-ports.json') -Raw | ConvertFrom-Json
$profile = Join-Path $package ('completionist-client-' + $ports.game)
if ([IO.File]::ReadAllText((Join-Path $root 'ISOLATED-COMPLETIONIST')) -ne $root -or (Hash (Join-Path $root 'server.jar')) -ne $manifest.serverSha256) { throw 'Testmap of testserver wijkt af van dit pakket.' }
if (-not (Test-Path -LiteralPath $java11) -or -not (Test-Path -LiteralPath $java21)) { throw 'De gekwalificeerde Java 11/21-installatie ontbreekt.' }
& python (Join-Path $package 'client\client_profile.py') --java $java11 --server $root --profile $profile --config-port $ports.config --appearance-port $ports.appearance --stage
if ($LASTEXITCODE -ne 0) { throw 'Clientprofiel kon niet worden voorbereid.' }
if ($Action -eq 'Prepare') { Write-Host "Testomgeving voorbereid: $root"; return }
if ($null -eq (OwnedProcess)) {
    foreach ($port in @($ports.game,$ports.http,$ports.link,$ports.appearance,$ports.config,$ports.bridge)) { if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Testpoort $port is al in gebruik." } }
    Remove-Item -LiteralPath (Join-Path $root 'STOP-TEST') -Force -ErrorAction SilentlyContinue
    $classes = Join-Path $root 'test-classes'; New-Item -ItemType Directory -Path $classes -Force | Out-Null
    & ($java21.Replace('java.exe','javac.exe')) -cp (Join-Path $root 'server.jar') -d $classes (Join-Path $package 'client\TestServerMain.java') (Join-Path $package 'client\IsolatedAccessScript.java')
    if ($LASTEXITCODE -ne 0) { throw 'Teststarter kon niet worden gebouwd.' }
    $taskEnvironment = @{}
    $names = @('OPENRUNE_JDBC_URL','OPENRUNE_DB_USER','OPENRUNE_DB_PASSWORD','OPENRUNE_CENTRAL_HOST','OPENRUNE_CENTRAL_PORT','OPENRUNE_WORLD_KEY','JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','NERO_COMPLETIONIST_TEST_ROOT','NERO_STUDIO_BRIDGE_PORT','NERO_STUDIO_SESSION_TOKEN','NERO_LOCAL_DEV_OFFLINE_BADWORDS')
    foreach ($name in $names) { $taskEnvironment[$name] = [Environment]::GetEnvironmentVariable($name,'Process'); [Environment]::SetEnvironmentVariable($name,$null,'Process') }
    try {
        $env:NERO_COMPLETIONIST_TEST_ROOT = $root; $env:NERO_STUDIO_BRIDGE_PORT = [string]$ports.bridge; $env:NERO_STUDIO_SESSION_TOKEN = [Guid]::NewGuid().ToString('N'); $env:NERO_LOCAL_DEV_OFFLINE_BADWORDS = '1'
        $process = Start-Process -FilePath $java21 -ArgumentList @('-Xms256m','-Xmx2g','-cp', ('"' + $classes + ';' + (Join-Path $root 'server.jar') + '"'), 'TestServerMain') -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $root 'boot.log') -RedirectStandardError (Join-Path $root 'boot-error.log') -PassThru
        @{pid=$process.Id;root=$root} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'owned-server.json') -Encoding utf8
    } finally { foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$taskEnvironment[$name],'Process') } }
}
$deadline = [DateTime]::UtcNow.AddSeconds(180)
do {
    if ($null -eq (OwnedProcess)) { throw "Testserver is gestopt. Zie $root\boot.log en boot-error.log." }
    try { $feed = Invoke-RestMethod -Uri ("http://127.0.0.1:" + $ports.appearance + "/appearance") -TimeoutSec 2; $ready = $feed.schema -eq 1 -and [bool](Get-NetTCPConnection -LocalPort $ports.game -State Listen -ErrorAction SilentlyContinue) } catch { $ready = $false }
    if (-not $ready) { Start-Sleep -Seconds 1 }
} while (-not $ready -and [DateTime]::UtcNow -lt $deadline)
if (-not $ready) { throw 'Testserver werd niet op tijd klaar; bekijk het bootlog.' }
Write-Host 'Testserver klaar. Gebruik ::curator en ::compcapes. Deze omgeving heeft een eigen database.'
if (-not $NoClient) { & python (Join-Path $package 'client\client_profile.py') --java $java11 --server $root --profile $profile --config-port $ports.config --appearance-port $ports.appearance }
