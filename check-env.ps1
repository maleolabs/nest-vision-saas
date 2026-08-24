# check-env.ps1 — Diagnostik konektivitas Nestara di WINDOWS
# Jalankan:  powershell -ExecutionPolicy Bypass -File tool/check-env.ps1
# Atau:      .\tool\check-env.ps1 -FixFirewall
#
# Mengecek: Docker, port 8000/8080, firewall, Tailscale, adb, LAN IP, dan saran run_dev mode
param(
  [switch]$FixFirewall
)

$ErrorActionPreference = "Continue"
function Ok($m){ Write-Host "[OK] $m" -ForegroundColor Green }
function Warn($m){ Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Fail($m){ Write-Host "[FAIL] $m" -ForegroundColor Red }
function Info($m){ Write-Host "[INFO] $m" -ForegroundColor Cyan }

Info "=== Nestara Workspace — Windows Env Check ==="
Write-Host ""

# 1. Docker
try {
  $dockerInfo = docker info 2>&1
  if ($LASTEXITCODE -eq 0) { Ok "Docker running" } else { Fail "Docker tidak running — start Docker Desktop dulu" }
} catch { Fail "Docker CLI tidak ditemukan di PATH" }

# 2. Port check
foreach ($port in @(8000,8080,3306,3307)) {
  $conn = Test-NetConnection -ComputerName "127.0.0.1" -Port $port -WarningAction SilentlyContinue -ErrorAction SilentlyContinue
  if ($conn.TcpTestSucceeded) { Ok "Port $port LISTEN (127.0.0.1:$port)" } else { Warn "Port $port tidak listen — service belum jalan?" }
}

# 3. Firewall
Info "Cek firewall inbound 8000/8080..."
$rules = netsh advfirewall firewall show rule name=all 2>$null | Out-String
$has8000 = $rules -match "Nestara Dev 8000" -or $rules -match "localport.*8000"
$has8080 = $rules -match "Nestara Vision 8080" -or $rules -match "localport.*8080"
if ($has8000) { Ok "Firewall rule 8000 ada" } else { Warn "Firewall rule 8000 BELUM ada — HP fisik bakal timeout" }
if ($has8080) { Ok "Firewall rule 8080 ada" } else { Warn "Firewall rule 8080 BELUM ada" }

if ($FixFirewall) {
  Info "Menambahkan rule firewall (butuh Administrator)..."
  try {
    netsh advfirewall firewall add rule name="Nestara Dev 8000" dir=in action=allow protocol=TCP localport=8000 | Out-Null
    netsh advfirewall firewall add rule name="Nestara Vision 8080" dir=in action=allow protocol=TCP localport=8080 | Out-Null
    Ok "Rule firewall ditambahkan"
  } catch { Fail "Gagal tambah rule — jalankan PowerShell as Administrator" }
} else {
  if (-not $has8000 -or -not $has8080) {
    Warn "Fix: powershell -ExecutionPolicy Bypass -File tool/check-env.ps1 -FixFirewall  (Run as Administrator)"
  }
}

# 4. IP detection — LAN vs Tailscale vs vEthernet
Info "Network interfaces (filter vEthernet/Docker):"
Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | ForEach-Object {
  $iface = $_
  $alias = (Get-NetAdapter -InterfaceIndex $iface.InterfaceIndex -ErrorAction SilentlyContinue).Name
  $isVirtual = $alias -match "vEthernet|WSL|Docker|tailscale|Tailscale|Hyper-V"
  $tag = if ($isVirtual) { " (virtual - diabaikan run_dev)" } else { "" }
  $color = if ($isVirtual) { "DarkGray" } else { "White" }
  Write-Host ("  {0,-18} {1,-30} {2}" -f $iface.IPAddress, "[$alias]", $tag) -ForegroundColor $color
}

# Tailscale
try {
  $ts = tailscale ip -4 2>$null
  if ($LASTEXITCODE -eq 0 -and $ts) { Ok "Tailscale IP: $ts (100.x)" } else { Warn "Tailscale tidak aktif / tidak terinstall — mode --tailscale tidak bisa" }
} catch { Warn "tailscale CLI tidak ditemukan — install dari tailscale.com/download" }

# 5. ADB
try {
  $adbVer = adb version 2>&1 | Select-Object -First 1
  if ($LASTEXITCODE -eq 0 -or $adbVer) {
    Ok "ADB: $adbVer"
    $devices = adb devices 2>&1 | Out-String
    if ($devices -match "device`$") { Ok "ADB device terdeteksi" } else { Warn "ADB device tidak ada — colok HP + enable USB debugging untuk --reverse" }
  } else { Warn "ADB tidak ditemukan — install via Android Studio / platform-tools" }
} catch { Warn "ADB tidak di PATH — download platform-tools dan tambah ke PATH" }

# 6. LAN IP trick (sama dengan run_dev.dart RawDatagramSocket)
Info "Deteksi LAN IP (simulasi run_dev.dart):"
try {
  $sock = New-Object System.Net.Sockets.UdpClient
  $sock.Connect("10.254.254.254", 1)
  $localIp = $sock.Client.LocalEndPoint.Address.ToString()
  $sock.Close()
  if ($localIp -and $localIp -ne "0.0.0.0" -and $localIp -notlike "100.64.*") {
    Ok "LAN IP terdeteksi: $localIp"
    Info "  -> HP fisik WiFi: dart run tool/run_dev.dart --device  (auto $localIp)"
    Info "  -> Atau manual:   dart run tool/run_dev.dart --base=http://$localIp`:8000"
  } else { Warn "Deteksi UDP trick gagal / CGNAT ($localIp) — fallback ke interface enumeration" }
} catch { Warn "Gagal deteksi LAN IP via UDP trick: $_" }

# 7. Vision health (kalau jalan)
try {
  $r = Invoke-WebRequest -Uri "http://localhost:8080/up" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
  if ($r.StatusCode -eq 200) { Ok "Vision /up OK" } else { Warn "Vision /up status $($r.StatusCode)" }
} catch {
  try {
    $r2 = Invoke-WebRequest -Uri "http://localhost:8080/api/ocr/check-status/test" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
    Warn "Vision tidak ada /up, tapi 8080 listen — coba docker compose ps"
  } catch { Warn "Vision tidak reachable di :8080 — docker compose up -d ?" }
}
try {
  $r = Invoke-WebRequest -Uri "http://localhost:8000/up" -TimeoutSec 3 -UseBasicParsing -ErrorAction SilentlyContinue
  if ($r.StatusCode -eq 200) { Ok "Web /up OK (8000)" } else { Warn "Web /up status $($r.StatusCode)" }
} catch { Warn "Web tidak reachable di :8000 — php artisan serve --host=0.0.0.0 --port=8000 ?" }

Write-Host ""
Info "=== Rekomendasi mode run_dev (Windows) ==="
Write-Host "  1) USB paling stabil : dart run tool/run_dev.dart --reverse" -ForegroundColor Green
Write-Host "  2) Tailscale remote  : dart run tool/run_dev.dart --tailscale" -ForegroundColor Green
Write-Host "  3) WiFi kantor       : dart run tool/run_dev.dart --device" -ForegroundColor Yellow
Write-Host "  4) Emulator          : dart run tool/run_dev.dart" -ForegroundColor White
Write-Host ""
Info "Selesai. Jika FAIL/WARN, ikuti saran di atas lalu re-run script ini."
