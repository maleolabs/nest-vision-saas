# Nestara Vision SaaS

![Build](https://github.com/maleolabs/nest-vision-saas/actions/workflows/ci.yml/badge.svg)
![Release](https://github.com/maleolabs/nest-vision-saas/actions/workflows/release.yml/badge.svg)

OCR SaaS berbasis Spring Boot — ekstraksi teks dari gambar (KTP & dokumen umum) melalui REST API dengan autentikasi API key, kuota harian per klien, dan dashboard manajemen.

Menyediakan preprocessing otomatis untuk **gambar berkualitas rendah**: blur, pencahayaan buruk, kontras rendah, kemiringan hingga 45°, dan distorsi perspektif.

---

## Fitur

### Pipeline OCR

| Kemampuan | Detail |
|---|---|
| Ensemble engine | pyTesseract → native Tesseract (tess4j) → PaddleOCR (opsional). Engine dipilih otomatis berdasarkan confidence score |
| Quality gate | Deteksi blur (Laplacian variance), brightness, contrast — preprocessing diaktifkan otomatis |
| Preprocessing | Upscale 300 DPI, CLAHE, fastNlMeans denoising, deskew Hough 10–45°, koreksi perspektif 4-titik, koreksi orientasi 90/180/270 |
| Multi-PSM retry | 5 strategi page segmentation × 3 varian citra, dipilih via confidence scoring |
| KTP extractor | Fuzzy key matching (Levenshtein), alias field, koreksi typo NIK (`O→0`, `I→1`, `B→8`), regex NIK 16-digit global |
| Parser toleran | Multi-delimiter (`:`, `;`, `|`, spasi ganda) — tidak lagi bergantung pada satu karakter `:` |
| Observability | Confidence, blur score, engine yang dipakai, durasi — tercatat per request |

### Platform SaaS

- Autentikasi API key (terenkripsi, TTL 180 hari) + verifikasi akun via email
- Kuota harian per tipe akun (personal / organisasi)
- Dashboard: pembuatan API key, log request, profil
- WebSocket notification untuk progress OCR

---

## Arsitektur Pipeline

```
Gambar input
     │
     ▼
┌─────────────────────────────────────────────┐
│ Quality Gate (blur / brightness / contrast) │
└──────────────┬──────────────────────────────┘
               ▼
┌─────────────────────────────────────────────┐
│ Preprocessing                               │
│ perspective → OSD → upscale → CLAHE →       │
│ denoise → deskew(Hough) → threshold         │
└──────────────┬──────────────────────────────┘
               ▼
┌─────────────────────────────────────────────┐
│ Ensemble OCR                                │
│ 1. pyTesseract (multi-PSM + confidence)     │
│ 2. native Tesseract/tess4j   [conf < 60]    │
│ 3. PaddleOCR                [opsional]      │
│ 4. LLM Vision              [scaffold]       │
└──────────────┬──────────────────────────────┘
               ▼
┌─────────────────────────────────────────────┐
│ Post-processing                             │
│ NIK typo fix → KTP/generic parser →         │
│ fuzzy key match → filter required keys      │
└─────────────────────────────────────────────┘
```

---

## Quick Start (Development)

Prasyarat: JDK 17, Docker (MySQL), Tesseract binary + tessdata.

```bash
# 1. Environment + MySQL container (idempotent)
source scripts/dev-env.sh

# 2. Build + test
./mvnw verify

# 3. Jalankan
./mvnw spring-boot:run   # http://localhost:8080
```

`scripts/dev-env.sh` menyiapkan `JAVA_HOME`, `TESSERACT_DATAPATH`, kredensial DB, dan men-start container MySQL `ocr-tool-mysql` (port 3307) secara otomatis.

Jika `ind.traineddata` belum tersedia:

```bash
mkdir -p ~/.local/share/tessdata
curl -sL https://github.com/tesseract-ocr/tessdata_fast/raw/main/ind.traineddata \
  -o ~/.local/share/tessdata/ind.traineddata
```

---

## API

Semua endpoint memerlukan header `X-API-KEY`.

| Method | Endpoint | Deskripsi |
|---|---|---|
| `POST` | `/api/ocr/do-ocr` | Submit OCR. Multipart: `image` (file) *atau* `imageUrl`, opsional `requiredKeys` |
| `GET` | `/api/ocr/check-status/{requestId}` | Cek status proses |
| `GET` | `/api/ocr/get-result/{requestId}` | Ambil hasil ekstraksi |

Contoh:

```bash
curl -X POST https://host/api/ocr/do-ocr \
  -H "X-API-KEY: <key>" \
  -F "image=@ktp.jpg" \
  -F "requiredKeys=nik,nama,tempat/tgl lahir"
```

Response berisi `requestId`; hasil akhir berupa pasangan key-value per field yang terdeteksi.

Dashboard web tersedia di `/dashboard` (registrasi, login, buat API key, lihat log).

---

## Deployment

### Opsi 1 — Self-Contained Bundle (tanpa install dependency)

Bundle berisi JRE + Python + Tesseract + tessdata dalam satu folder. Tidak perlu instalasi apa pun selain MySQL.

Unduh dari [GitHub Releases](https://github.com/maleolabs/nest-vision-saas/releases):

| Platform | File |
|---|---|
| Linux x64 | `ocr-tool-<ver>-linux-amd64.tar.gz` |
| Windows x64 | `ocr-tool-<ver>-windows-amd64.zip` |
| macOS Apple Silicon | `ocr-tool-<ver>-macos-arm64.tar.gz` |

```bash
tar -xzf ocr-tool-*.tar.gz && cd ocr-tool-*
cp .env.example .env        # isi kredensial DB
./start.sh                  # Windows: double-click start.bat
```

> macOS first run: `xattr -cr .` (binary unsigned).

### Opsi 2 — Docker Compose

```bash
docker compose up -d --build
docker compose logs -f
```

### Opsi 3 — Jar manual

Butuh JDK 17, Python 3 + `opencv-python-headless pytesseract numpy`, Tesseract binary.

```bash
java -jar ocr-tool-*.jar    # konfigurasi via env var / .env
```

Skrip bantu Windows: `scripts/run.bat` (double-click) atau `scripts/run.ps1`.

---

## Release & Versioning

Versi canonical ada di `anvil.yaml` (`project.version`) dan disinkronkan ke `pom.xml` oleh script bump — nama file bundle mengikuti versi ini.

```bash
scripts/bump-version.sh patch   # 0.5.0 -> 0.5.1
scripts/bump-version.sh minor   # 0.5.0 -> 0.6.0
scripts/bump-version.sh major   # 0.5.0 -> 1.0.0
scripts/bump-version.sh minor --dry-run   # preview tanpa eksekusi
```

Script akan: bump versi → commit `chore(release): vX.Y.Z` → tag → push → workflow Release membangun jar + bundle semua platform dan mem-publish ke GitHub Releases.

---

## Konfigurasi

Semua key punya default (`${VAR:default}` di `application.properties`). Override via environment variable atau file `.env` (lihat [.env.example](.env.example)).

### Inti

| Key | Default | Keterangan |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://127.0.0.1:3307/...` | JDBC URL MySQL |
| `SPRING_DATASOURCE_USERNAME/PASSWORD` | `ocr` / `ocr` | Kredensial DB |
| `TESSERACT_DATAPATH` | `/usr/share/tesseract/tessdata` | Lokasi traineddata |
| `TESSERACT_CMD` | *(auto-detect)* | Path binary tesseract |
| `OCR_PYTHON_PATH` | `python3` | Interpreter Python |
| `OCR_SCRIPT_PATH` | `opt/app/ocr/tesseract_ocr.py` | Skrip bridge OCR |

### Tuning OCR

| Key | Default | Keterangan |
|---|---|---|
| `OCR_PREPROCESSING_ENABLED` | `true` | Auto-preprocess gambar buram |
| `OCR_ENSEMBLE_FALLBACK` | `true` | Fallback antar engine |
| `OCR_CONF_THRESHOLD` | `60` | Ambang confidence untuk fallback |
| `OCR_PERSPECTIVE_ENABLED` | `true` | Koreksi trapesium/perspektif |
| `OCR_OSD_ENABLED` | `true` | Koreksi rotasi 90/180/270 |
| `OCR_DESKEW_MAX_ANGLE` | `45` | Sudut deskew maksimum (derajat) |
| `OCR_PADDLE_ENABLED` | `false` | Fallback PaddleOCR (`pip install paddlepaddle paddleocr`) |
| `OCR_SR_ENABLED` | `false` | Super-resolution blur ekstrem |
| `OCR_LLM_ENABLED` | `false` | LLM vision fallback (butuh API key, berbayar) |

Daftar lengkap (31 keys): `.env.example`.

---

## Anvil Pipeline

```bash
anvil pipeline build          # mvn package → copy jar
anvil pipeline ci             # mvn verify + test report
anvil pipeline bundle         # build self-contained bundle utk OS lokal
```

Definisi: `.anvil/pipelines/{build,ci,bundle}.yaml`.

## GitHub Workflows

| Workflow | Trigger | Fungsi |
|---|---|---|
| `ci.yml` | push `master`/`feat/**` + PR | Build + test (MySQL service) |
| `build.yml` | push `master`, tag `v*` | Package jar + artifact |
| `release.yml` | tag `v*` | Jar + bundle 3 platform → GitHub Release |

## Struktur Proyek

```
├── src/main/java/m2codes/ocr_tool/
│   ├── application/          # service OCR, ensemble, quality assessor, parser
│   ├── domain/               # entity, repository, domain service
│   ├── infrastructure/       # security (API key), python executor, websocket
│   └── interfaces/           # controller, DTO, validator
├── opt/app/ocr/              # skrip Python: tesseract_ocr.py, paddle_ocr.py,
│   └── extractor/            # postprocessor, ktp_extractor
├── scripts/
│   ├── bundle/build-bundle.sh  # builder self-contained bundle
│   ├── bump-version.sh         # release versioning
│   ├── dev-env.sh              # setup dev linux/mac
│   └── run.ps1 / run.bat       # launcher windows
├── .github/workflows/        # ci, build, release
└── anvil.yaml                # project identity + canonical version
```

## Lisensi

Lihat [LICENSE](LICENSE).
