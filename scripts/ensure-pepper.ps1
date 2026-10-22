# ensure-pepper.ps1 — resolve APP_API_KEY_PEPPER untuk dev di Windows
#
# Dipakai oleh scripts/run.ps1 dan scripts/run.bat supaya aturan prioritasnya
# satu sumber (mirror scripts/dev-env.sh di Linux):
#   1. environment variable APP_API_KEY_PEPPER
#   2. .env.local di root repo
#   3. file persisten %USERPROFILE%\.config\ocr-tool\api-key-pepper
#   4. generate baru (64 hex char) lalu simpan ke file persisten
#
# Output: HANYA nilai pepper ke stdout (satu baris), supaya aman dipakai
# `for /f` di batch. Diagnostik selalu ke stderr.
# Exit code 1 kalau pepper tidak bisa disiapkan atau < 32 karakter.

$ErrorActionPreference = "Stop"

$repoRoot   = Split-Path -Parent $PSScriptRoot
$pepper     = $env:APP_API_KEY_PEPPER
$pepperFile = Join-Path $env:USERPROFILE ".config\ocr-tool\api-key-pepper"
$explicit   = $false

# --- 1/2: env var (sudah) + .env.local ---
if ($pepper) { $explicit = $true }
if (-not $pepper) {
    $envLocal = Join-Path $repoRoot ".env.local"
    if (Test-Path $envLocal) {
        $line = Get-Content $envLocal | Where-Object { $_ -match '^\s*APP_API_KEY_PEPPER\s*=' } | Select-Object -First 1
        if ($line) {
            $pepper = ($line -replace '^\s*APP_API_KEY_PEPPER\s*=\s*', '').Trim().Trim('"').Trim("'")
            $explicit = $true
        }
    }
}

# Nilai yang di-set eksplisit tapi tidak valid: gagal keras, jangan diam-diam
# fallback ke file (biar tidak ada dua pepper berbeda yang bikin bingung).
if ($explicit -and ($pepper.Length -lt 32)) {
    [Console]::Error.WriteLine("[ERROR] APP_API_KEY_PEPPER yang diset hanya $($pepper.Length) karakter, minimal 32.")
    [Console]::Error.WriteLine("        Perbaiki di environment / .env.local, atau hapus barisnya supaya memakai file persisten.")
    exit 1
}

# --- 3: file persisten ---
if (-not $pepper) {
    if (Test-Path $pepperFile) {
        $fromFile = Get-Content $pepperFile -Raw
        if ($fromFile) {
            $fromFile = $fromFile.Trim()
            if ($fromFile.Length -ge 32) { $pepper = $fromFile }
        }
    }
}

# --- 4: generate baru (sekali saja) ---
if (-not $pepper) {
    $pepperDir = Split-Path -Parent $pepperFile
    if (-not (Test-Path $pepperDir)) {
        New-Item -ItemType Directory -Force -Path $pepperDir | Out-Null
    }
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $bytes = New-Object byte[] 32
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $pepper = -join ($bytes | ForEach-Object { $_.ToString("x2") })
    Set-Content -Path $pepperFile -Value $pepper -NoNewline -Encoding ascii
    [Console]::Error.WriteLine("[INFO] APP_API_KEY_PEPPER dibuat otomatis: $pepperFile")
    [Console]::Error.WriteLine("       JANGAN hapus/ubah - api_key tersimpan tidak bisa diverifikasi kalau nilai ini berubah.")
}

if (-not $pepper -or $pepper.Length -lt 32) {
    [Console]::Error.WriteLine("[ERROR] APP_API_KEY_PEPPER minimal 32 karakter (sekarang $(if ($pepper) { $pepper.Length } else { 0 })).")
    [Console]::Error.WriteLine("        Set manual di environment atau .env.local, contoh:")
    [Console]::Error.WriteLine('        $env:APP_API_KEY_PEPPER = -join ([guid]::NewGuid().ToString("N") + [guid]::NewGuid().ToString("N"))')
    exit 1
}

[Console]::Out.WriteLine($pepper)
