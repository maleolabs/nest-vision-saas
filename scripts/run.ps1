# run.ps1 — jalankan ocr-tool di Windows (PowerShell)
# Cara pakai:
#   1. Right-click file ini → Run with PowerShell
#   2. Atau double-click run.bat (yang akan panggil script ini)
#   3. Atau buka PowerShell di folder repo: .\scripts\run.ps1
# Edit CONFIG di bawah, save, lalu run lagi.

param(
    [string]$JarPath = "",          # kosong = auto cari target/*.jar
    [switch]$NoPrompt               # skip konfirmasi
)

# ============================================================
# CONFIG — edit sesuai kebutuhan (user friendly)
# ============================================================
$Config = @{
    JAVA_HOME               = "C:\Program Files\Eclipse Adoptium\jdk-25.0.0.0-hotspot"
    SPRING_DATASOURCE_URL   = "jdbc:mysql://127.0.0.1:3306/ocr_tool?createDatabaseIfNotExist=true"
    SPRING_DATASOURCE_USERNAME = "ocr"
    SPRING_DATASOURCE_PASSWORD = "ocr"
    SPRING_JPA_HIBERNATE_DDL_AUTO = "update"
    TESSERACT_DATAPATH      = "C:\tessdata"
    TESSERACT_CMD           = ""  # kosong = auto-detect; atau "C:\Program Files\Tesseract-OCR\tesseract.exe"
    # Python OCR bridge
    OCR_PYTHON_PATH         = "python"  # windows: python/py; linux: python3
    OCR_SCRIPT_PATH         = "opt/app/ocr/tesseract_ocr.py"
    # Preprocessing (blur/gelap/kontras rendah)
    OCR_PREPROCESSING_ENABLED = "true"
    OCR_UPSCALE_THRESHOLD   = "1000"
    OCR_BLUR_THRESHOLD      = "100"
    # Ensemble fallback
    OCR_ENSEMBLE_FALLBACK   = "true"
    OCR_ENSEMBLE_PARALLEL   = "true"
    OCR_CONF_THRESHOLD      = "55"
    OCR_TIME_BUDGET         = "120"
    OCR_PYTHON_TIMEOUT_MILLIS = "180000"
    # PaddleOCR fallback (legacy, prefer Rapid)
    OCR_PADDLE_ENABLED      = "false"
    OCR_PADDLE_SCRIPT       = "opt/app/ocr/paddle_ocr.py"
    # RapidOCR ONNX fallback (preferred untuk KTP buram)
    OCR_RAPID_ENABLED       = "true"
    OCR_RAPID_SCRIPT        = "opt/app/ocr/rapid_ocr.py"
    # Super-resolution (capped 2200px, cubic+sharpen jika tanpa model)
    OCR_SR_ENABLED          = "true"
    ESRGAN_MODEL            = ""
    # Koreksi miring / perspective / orientasi
    OCR_PERSPECTIVE_ENABLED = "true"
    OCR_OSD_ENABLED         = "true"
    OCR_DESKEW_MAX_ANGLE    = "45"
    # LLM Vision fallback (opsional, berbayar)
    OCR_LLM_ENABLED         = "false"
    OCR_LLM_API_KEY         = ""
    OCR_LLM_BASE_URL        = "https://api.openai.com/v1"
    OCR_LLM_MODEL           = "gpt-4o-mini"
    KEYSTORE_PASSWORD       = "changeit"
    KEYSTORE_ALIAS          = "ocr"
    # API key pepper (HMAC untuk api_key hash). Kosong = auto-load/generate.
    # Nilai HARUS stabil antar-restart: kalau berubah, API key tersimpan tidak
    # bisa diverifikasi lagi (client harus rotate/reissue).
    APP_API_KEY_PEPPER      = ""
    SPRING_MAIL_HOST        = "smtp.gmail.com"
    SPRING_MAIL_PORT        = "587"
    SPRING_MAIL_USERNAME    = "dev@example.com"
    SPRING_MAIL_PASSWORD    = "dev"
    SERVER_PORT             = "8080"
}
# ============================================================
# END CONFIG
# ============================================================

# Load .env file kalau ada (override CONFIG)
$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Write-Host "  [INFO] Load .env dari $envFile" -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
        if ($_ -match '^\s*([^=]+)=(.*)\s*$') {
            $k = $matches[1].Trim()
            $v = $matches[2].Trim().Trim('"').Trim("'")
            $Config[$k] = $v
        }
    }
}

$Host.UI.RawUI.WindowTitle = "OCR Tool - Spring Boot"

# --- API key pepper (WAJIB, harus stabil antar-restart) ---
# Resolusi nilai dipusatkan di scripts/ensure-pepper.ps1 (dipakai juga oleh
# run.bat): env var > .env/.env.local > %USERPROFILE%\.config\ocr-tool\api-key-pepper.
# JANGAN ganti nilai tanpa prosedur rotasi: api_key tersimpan di-hash dengan pepper ini.
$pepper = $env:APP_API_KEY_PEPPER
if (-not $pepper -and $Config.ContainsKey("APP_API_KEY_PEPPER")) { $pepper = $Config["APP_API_KEY_PEPPER"] }
if (-not $pepper) {
    $ensurePepper = Join-Path $PSScriptRoot "ensure-pepper.ps1"
    if (Test-Path $ensurePepper) {
        $pepper = (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $ensurePepper | Select-Object -Last 1)
        if ($pepper) { $pepper = $pepper.Trim() }
    }
}
if (-not $pepper -or $pepper.Length -lt 32) {
    Write-Host "  [ERROR] APP_API_KEY_PEPPER minimal 32 karakter (sekarang $(if ($pepper) { $pepper.Length } else { 0 }))." -ForegroundColor Red
    Write-Host "          Set di environment, .env, atau .env.local. Contoh generate:" -ForegroundColor Red
    Write-Host '          $env:APP_API_KEY_PEPPER = -join ([guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N"))' -ForegroundColor DarkGray
    Read-Host "  Tekan Enter untuk exit"
    exit 1
}
$Config["APP_API_KEY_PEPPER"] = $pepper
$secretScript = Join-Path $PSScriptRoot "ensure-encryption-secret.ps1"
$Config["APP_API_KEY_ENCRYPTION_SECRET"] = if ($env:APP_API_KEY_ENCRYPTION_SECRET) { $env:APP_API_KEY_ENCRYPTION_SECRET } else { (& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $secretScript | Select-Object -Last 1).Trim() }

Write-Host ""
Write-Host "  ========================================" -ForegroundColor Green
Write-Host "   OCR Tool - Starting..." -ForegroundColor Green
Write-Host "  ========================================" -ForegroundColor Green
Write-Host ""

# --- Cari JAVA ---
$javaExe = $null
if ($Config.JAVA_HOME -and (Test-Path "$($Config.JAVA_HOME)\bin\java.exe")) {
    $javaExe = "$($Config.JAVA_HOME)\bin\java.exe"
    $env:JAVA_HOME = $Config.JAVA_HOME
    Write-Host "  [OK] JAVA_HOME: $($Config.JAVA_HOME)" -ForegroundColor Green
    & $javaExe -version 2>&1 | Select-String "version" | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkGray }
} elseif (Get-Command java -ErrorAction SilentlyContinue) {
    $javaExe = "java"
    Write-Host "  [INFO] JAVA_HOME tidak ditemukan, pakai java di PATH" -ForegroundColor Yellow
    java -version 2>&1 | Select-String "version" | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkGray }
} else {
    Write-Host "  [ERROR] Java tidak ditemukan!" -ForegroundColor Red
    Write-Host "          Install JDK 25: https://adoptium.net/temurin/releases/?version=25" -ForegroundColor Red
    Write-Host "          Atau edit JAVA_HOME di bagian CONFIG atas file ini." -ForegroundColor Red
    Read-Host "  Tekan Enter untuk exit"
    exit 1
}
Write-Host ""

# --- Cari JAR ---
if (-not $JarPath) {
    # Ambil JAR TERBARU (jangan asal satu) supaya release lama di target tidak kepakai.
    $jar = Get-ChildItem -Path (Join-Path $PSScriptRoot "..\target\*.jar") -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { $jar = Get-ChildItem -Path ".\*.jar" -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1 }
    if (-not $jar) { $jar = Get-ChildItem -Path (Join-Path $PSScriptRoot "..\*.jar") -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1 }
    if ($jar) { $JarPath = $jar.FullName }
}
if (-not $JarPath -or -not (Test-Path $JarPath)) {
    Write-Host "  [ERROR] File *.jar tidak ditemukan!" -ForegroundColor Red
    Write-Host "          Jalankan: .\mvnw.cmd clean package -DskipTests" -ForegroundColor Red
    Write-Host "          Atau download dari GitHub Releases." -ForegroundColor Red
    Read-Host "  Tekan Enter untuk exit"
    exit 1
}
Write-Host "  [OK] JAR: $JarPath" -ForegroundColor Green
Write-Host "  [OK] DB: $($Config.SPRING_DATASOURCE_URL) ($($Config.SPRING_DATASOURCE_USERNAME))" -ForegroundColor Green
Write-Host "  [OK] PEPPER: $($Config.APP_API_KEY_PEPPER.Substring(0,6))... (len=$($Config.APP_API_KEY_PEPPER.Length))" -ForegroundColor Green
Write-Host "  [OK] TESSERACT: $($Config.TESSERACT_DATAPATH)" -ForegroundColor Green
if ($Config.TESSERACT_CMD) { Write-Host "  [OK] TESSERACT_CMD: $($Config.TESSERACT_CMD)" -ForegroundColor Green }
Write-Host "  [OK] OCR preprocess: $($Config.OCR_PREPROCESSING_ENABLED) | ensemble: $($Config.OCR_ENSEMBLE_FALLBACK) | perspective: $($Config.OCR_PERSPECTIVE_ENABLED) | OSD: $($Config.OCR_OSD_ENABLED)" -ForegroundColor Green
Write-Host "  [OK] PORT: $($Config.SERVER_PORT)" -ForegroundColor Green
Write-Host ""

# --- Cek tesseract binary ---
$tessFound = $false
if ($Config.TESSERACT_CMD -and (Test-Path $Config.TESSERACT_CMD)) {
    Write-Host "  [OK] tesseract binary: $($Config.TESSERACT_CMD)" -ForegroundColor Green
    $tessFound = $true
} elseif (Get-Command tesseract -ErrorAction SilentlyContinue) {
    Write-Host "  [OK] tesseract binary di PATH" -ForegroundColor Green
    try { tesseract --version 2>&1 | Select-Object -First 1 | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkGray } } catch {}
    $tessFound = $true
}
if (-not $tessFound) {
    Write-Host "  [WARN] tesseract.exe tidak ditemukan!" -ForegroundColor Yellow
    Write-Host "         Install: https://github.com/UB-Mannheim/tesseract/wiki" -ForegroundColor Yellow
    Write-Host "         Atau: choco install tesseract / scoop install tesseract" -ForegroundColor Yellow
    Write-Host "         Atau set TESSERACT_CMD di CONFIG." -ForegroundColor Yellow
    Write-Host "         OCR via Python akan gagal sampai tesseract terinstall." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "  [INFO] Pastikan MySQL running. App auto-create DB jika belum ada." -ForegroundColor Cyan
Write-Host ""

# --- Export env untuk proses Java ---
foreach ($k in $Config.Keys) {
    if ($k -eq "JAVA_HOME" -or $k -eq "SERVER_PORT") { continue }
    Set-Item -Path "env:$k" -Value $Config[$k]
}
# SERVER_PORT mapping ke server.port (Spring Boot)
$env:SERVER_PORT = $Config.SERVER_PORT

Write-Host "  ========================================" -ForegroundColor Green
Write-Host "   Starting Spring Boot... (Ctrl+C untuk stop)" -ForegroundColor Green
Write-Host "   Buka http://localhost:$($Config.SERVER_PORT) setelah 'Started OcrToolApplication'" -ForegroundColor Green
Write-Host "  ========================================" -ForegroundColor Green
Write-Host ""

# --- Jalankan (log tampil di window ini) ---
try {
    & $javaExe -jar $JarPath
} finally {
    Write-Host ""
    Write-Host "  ----------------------------------------" -ForegroundColor Yellow
    Write-Host "   Aplikasi berhenti (exit code $LASTEXITCODE)" -ForegroundColor Yellow
    Write-Host "  ----------------------------------------" -ForegroundColor Yellow
    Read-Host "  Tekan Enter untuk close window"
}
