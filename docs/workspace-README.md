# Nestara — Local Integration Guide

Tiga project dalam workspace ini:

| Project | Stack | Port dev | Peran |
|---|---|---|---|
| `nestara-web` | Laravel + Livewire | 8000 | Backend API + proxy OCR |
| `nestara-vision-saas` | Spring Boot (Docker) | 8080 | OCR SaaS (KTP → text) |
| `nestara-mobile` | Flutter | — | Client mobile |

Alur dependensi: **mobile → web → vision**. Mobile tidak pernah memanggil vision langsung.

## 0. Cek Environment (Windows)

Sebelum run, diagnosa 1 klik:

```powershell
# Windows — jalankan di PowerShell
powershell -ExecutionPolicy Bypass -File tool/check-env.ps1
# auto-fix firewall (Run as Administrator):
powershell -ExecutionPolicy Bypass -File tool/check-env.ps1 -FixFirewall

# Linux/macOS
bash tool/check-env.sh
```

Script cek: Docker jalan, port 8000/8080 listen, firewall inbound, Tailscale IP, ADB device, dan saran `run_dev` mode.

## 1. Jalankan Vision SaaS

Opsi A — via root compose (recommended, web+vision sekaligus):
```bash
docker compose up -d
docker compose ps
```

Opsi B — cuma vision (seperti dulu):
```bash
cd nestara-vision-saas
docker compose up -d
```

API tersedia di `http://localhost:8080`. Perilaku identik di Windows/Linux.

## 2. Jalankan Web (Laravel)

Pilih salah satu:

**Native (paling umum di dev):**
```bash
cd nestara-web
composer install
php artisan key:generate        # jika belum ada .env
php artisan migrate             # jika perlu
php artisan serve --host=0.0.0.0 --port=8000
```

**Docker (lebih stabil di Windows — auto-restart, tanpa urus php.exe di Defender):**
```bash
# dari root workspace
docker compose up -d web
docker compose logs -f web
```

`--host=0.0.0.0` wajib agar HP fisik/emulator bisa menjangkau backend.
Pastikan `.env` mengarah ke vision:

```
OCR_API_BASE_URL="http://localhost:8080/api/"
```

> Jika `web` jalan via `docker compose up -d web` (container), set di DB juga:
> `http://ocr-api:8080/api/` via halaman **Admin > Pengaturan > OCR** (karena `OcrAPIServiceImpl2` prioritas baca dari DB `get_pengaturan(OCR_BASE_URL)`).

> Jika `web` native tapi vision di Docker, tetap `http://localhost:8080/api/` — Docker Desktop Windows forward `localhost` otomatis.

## 3. Jalankan Mobile

Dari `nestara-mobile`, pilih mode sesuai target:

```bash
# Emulator Android (default)
dart run tool/run_dev.dart

# HP fisik via Wi-Fi (auto-detect LAN IP komputer)
dart run tool/run_dev.dart --device

# HP fisik via Tailscale — paling robust, anti AP-isolation & ganti-ganti jaringan
dart run tool/run_dev.dart --tailscale

# HP fisik via USB — tanpa Wi-Fi & tanpa firewall (REKOMENDASI #1 di Windows)
dart run tool/run_dev.dart --reverse

# Manual override
dart run tool/run_dev.dart --base=http://192.168.1.50:8000
```

Script meng-inject `--dart-define=API_BASE_URL=...` otomatis + pre-flight check (warn kalau `apiclient.g.dart` stale atau backend `/up` tidak reachable).

Referensi URL per target:

| Target | Base URL |
|---|---|
| Emulator Android | `http://10.0.2.2:8000/api` |
| iOS simulator / desktop | `http://localhost:8000/api` |
| HP fisik (Wi-Fi) | `http://<LAN-IP-host>:8000/api` |
| HP fisik (Tailscale) | `http://<IP-tailscale-host>:8000/api` (100.x.x.x) |
| HP fisik (`adb reverse`) | `http://localhost:8000/api` |

### Ranking mode di Windows (paling proper → paling rapuh)

1. **`--reverse` (USB)** — bypass total firewall, AP isolation, ganti WiFi, vEthernet WSL. Butuh kabel + `adb`.
2. **`--tailscale`** — IP permanen `100.x`, survive ganti jaringan, enkripsi. Tetap butuh firewall allow 8000.
3. **`--device` (LAN)** — native, tapi di Windows rapuh karena `vEthernet (WSL)` sering kedetect duluan; IP berubah tiap ganti WiFi.
4. **Emulator `10.0.2.2`** — cuma untuk emulator.

### Mode Tailscale (rekomendasi untuk HP fisik remote)

Kenapa Tailscale lebih stabil daripada LAN Wi-Fi:

1. Tidak terpengaruh AP isolation / guest network / beda subnet
2. IP host (`100.x.x.x`) permanen — tidak berubah saat ganti Wi-Fi
3. Terenkripsi end-to-end, jalan di atas jaringan apapun

Prasyarat:

1. Komputer: Tailscale aktif (`tailscale status` harus menunjukkan node online)
2. HP: install aplikasi Tailscale, login ke **tailnet yang sama**, dan pastikan
   status Connected
3. Backend tetap harus listen `0.0.0.0`

Lalu:

```bash
dart run tool/run_dev.dart --tailscale
```

Script otomatis mendeteksi IP Tailscale host (interface `tailscale0` /
rentang 100.64.0.0/10). Bila deteksi gagal, pakai manual:
`--base=http://<ip-tailscale>:8000`.

> Catatan Windows: rule firewall untuk port 8000 tetap diperlukan — trafik
> Tailscale tetap melewati Windows Defender Firewall.

### Catatan Windows

Firewall Defender memblokir inbound dari HP. Izinkan sekali saja (PowerShell **as Administrator**):

```powershell
netsh advfirewall firewall add rule name="Nestara Dev 8000" dir=in action=allow protocol=TCP localport=8000
netsh advfirewall firewall add rule name="Nestara Vision 8080" dir=in action=allow protocol=TCP localport=8080
# atau otomatis:
powershell -ExecutionPolicy Bypass -File tool/check-env.ps1 -FixFirewall
```

HTTP plaintext hanya diizinkan untuk **build debug** via
`android/app/src/debug/AndroidManifest.xml` (`usesCleartextTraffic="true"`).
Build release tetap HTTPS-only.

### Build Release (APK)

Jangan pakai `flutter build apk --dart-define` manual. Pakai Anvil variant:

```bash
cd nestara-mobile
# alpha = emulator (10.0.2.2), beta = staging, stable = prod (lihat anvil.yaml)
anvil pipeline build --target apk --output dist/apk --env production
# atau via GitHub Actions: push ke develop/main auto-build per variant
```

`anvil.yaml` `distribution.variants` adalah single source of truth untuk `API_BASE_URL` release.

## Troubleshooting cepat

| Gejala | Penyebab umum | Solusi |
|---|---|---|
| HP fisik tidak konek, emulator konek | Firewall / beda subnet / AP isolation | `tool/check-env.ps1` → pakai `--tailscale` atau `--reverse`; atau buka port 8000 & pastikan satu Wi-Fi |
| `Connection refused` di emulator | Laravel cuma listen loopback | `php artisan serve --host=0.0.0.0` atau `docker compose up -d web` |
| Gagal setelah ganti Wi-Fi | IP host berubah | Pakai `--device` (auto-detect) atau `--tailscale` (IP permanen) |
| Timeout saat upload KTP | Vision belum jalan | `docker compose ps` di root atau `nestara-vision-saas` |
| `--tailscale` gagal padahal PC online | Tailscale di HP belum connected / beda tailnet | Cek aplikasi Tailscale di HP; pastikan status Connected |
| `apiclient.g.dart` warn 192.168.1.17 | Generated file stale | `dart run build_runner build --delete-conflicting-outputs` di `nestara-mobile` |
| Web container tidak konek ke vision | `OCR_API_BASE_URL` masih localhost di DB | Set di DB via Admin > Pengaturan > OCR jadi `http://ocr-api:8080/api/` saat web via Docker |
