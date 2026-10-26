$ErrorActionPreference = 'Stop'
$file = Join-Path (Join-Path $env:USERPROFILE '.config') 'ocr-tool/api-key-encryption-secret'; $secret = $env:APP_API_KEY_ENCRYPTION_SECRET
if (-not $secret -and (Test-Path $file)) { $secret = (Get-Content $file -Raw).Trim() }
if (-not $secret) { New-Item -ItemType Directory -Force (Split-Path $file) | Out-Null; $b=New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Fill($b); $secret=[Convert]::ToBase64String($b); Set-Content $file $secret -NoNewline }
try {$b=[Convert]::FromBase64String($secret)} catch {throw 'APP_API_KEY_ENCRYPTION_SECRET must be base64 encoding of 32 bytes'}
if ($b.Length -ne 32) {throw 'APP_API_KEY_ENCRYPTION_SECRET must be base64 encoding of 32 bytes'}; [Console]::WriteLine($secret)
