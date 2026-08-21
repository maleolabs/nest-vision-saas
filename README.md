# perizinan-ocr-tool

Proyek ini menyediakan layanan pengenalan teks dari gambar menggunakan teknologi Optical Character Recognition (OCR) berbasis API. API ini memungkinkan pengguna untuk mengunggah gambar dan menerima teks yang dihasilkan dari proses OCR, memungkinkan integrasi mudah ke dalam aplikasi atau sistem yang membutuhkan kemampuan pengenalan teks dari gambar.

## Prasyarat

- **JDK 17** — Temurin 17 (`curl -sL https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse | tar xz -C ~/.local/opt/jdk-17 --strip-components=1`)
- **Docker** — untuk MySQL dev container dan `docker compose up`
- **Maven Wrapper** — `./mvnw` (sudah included, tidak perlu install Maven global)

> `application.properties` sudah punya default value untuk semua env var, jadi `mvn verify` dan `spring-boot:run` bisa jalan tanpa export manual — cukup pastikan MySQL up dan `JAVA_HOME` mengarah ke JDK 17.

## Setup Development

```bash
# 1. Set environment + start MySQL container (idempotent)
source scripts/dev-env.sh

# 2. Verifikasi
./mvnw verify          # build + test (butuh MySQL di 127.0.0.1:3307)
./mvnw spring-boot:run # start app di http://localhost:8080
```

`scripts/dev-env.sh` otomatis:
- Set `JAVA_HOME` ke `~/.local/opt/jdk-17` (fallback `/opt/phpstorm/jbr`)
- Set `TESSERACT_DATAPATH` ke `~/.local/share/tessdata` (butuh `ind.traineddata` + `eng.traineddata`)
- Set `SPRING_DATASOURCE_*` ke MySQL container `ocr-tool-mysql` (127.0.0.1:3307, db `ocr_tool`, user `ocr`/`ocr`)
- Start container MySQL jika belum jalan

Jika `ind.traineddata` belum ada:

```bash
mkdir -p ~/.local/share/tessdata
curl -sL https://github.com/tesseract-ocr/tessdata_fast/raw/main/ind.traineddata -o ~/.local/share/tessdata/ind.traineddata
cp /usr/share/tesseract/tessdata/eng.traineddata ~/.local/share/tessdata/ 2>/dev/null || true
```

## Build & Test

```bash
./mvnw clean package -DskipTests   # package saja (jar di target/*.jar, ~171MB)
./mvnw verify                       # package + test (butuh MySQL)
./mvnw test                         # test saja
```

## Docker Compose

```bash
docker compose up -d        # start db + ocr-api (build dari Dockerfile)
docker compose logs -f      # lihat log
docker compose down         # stop
docker compose config       # validasi compose file
```

`docker-compose.yml` berisi service `db` (mysql:8, healthcheck, volume `ocr-tool-db-data`) dan `ocr-api` (build `.`, depends_on `db` healthy, env lengkap).

## Anvil Pipeline

Project ini sudah di-init sebagai Anvil project (`anvil.yaml`, `anvil init ocr-tool`).

```bash
anvil config validate        # validasi anvil.yaml
anvil pipeline build         # build pipeline: mvn dependency:go-offline → package → copy jar
anvil pipeline build -o dist # build + copy jar ke dist/
anvil pipeline ci            # CI pipeline: mvn verify (butuh MySQL di 3307) → test report
```

Pipeline definitions: `.anvil/pipelines/build.yaml` dan `.anvil/pipelines/ci.yaml`.
State lokal `.anvil/state/` di-ignore (tidak di-commit).

## GitHub Workflows

| Workflow | Trigger | Apa yang dilakukan |
|----------|---------|-------------------|
| `ci.yml` | push `master`/`feat/**`/`security/**` + PR ke `master` | `setup-java` Temurin 17 + MySQL service + `./mvnw verify` |
| `build.yml` | push `master` + tag `v*` | `./mvnw clean package -DskipTests` + upload artifact `ocr-tool-jar` |
| `release.yml` | push tag `v*` | package + hitung `sha256` + `softprops/action-gh-release` → GitHub Release dengan `*.jar` + `*.jar.sha256` |

Buat release:

```bash
git tag v0.1.0 && git push origin v0.1.0  # trigger build + release workflows
# atau via GitHub UI: Releases → Draft new release → Create tag v0.1.0
```

## Windows — Run tanpa Docker (double-click)

Butuh **JDK 17** + **MySQL** (via installer, bukan Docker). Tidak perlu set env manual — script sudah handle.

**Cara 1: Double-click (paling gampang)**
1. Edit `scripts\run.bat` pakai Notepad — ubah bagian `CONFIG` di atas (DB URL, password, `TESSERACT_DATAPATH`, dll) jika perlu
2. Atau copy `.env.example` → `.env` lalu edit `.env` (dipakai otomatis oleh `run.ps1`)
3. Double-click `scripts\run.bat` — window akan kebuka, log Spring Boot tampil. Buka `http://localhost:8080` setelah `Started OcrToolApplication`

**Cara 2: PowerShell**
```powershell
# Right-click scripts\run.ps1 → Run with PowerShell
# Atau dari terminal:
.\scripts\run.ps1
# Dengan jar custom:
.\scripts\run.ps1 -JarPath "C:\path\to\ocr-tool-0.0.1-SNAPSHOT.jar"
```

**Cara 3: Manual CMD/PowerShell**
```cmd
:: CMD
set SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/ocr_tool?createDatabaseIfNotExist=true
set SPRING_DATASOURCE_USERNAME=ocr
java -jar target\ocr-tool-0.0.1-SNAPSHOT.jar
```
```powershell
# PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/ocr_tool?createDatabaseIfNotExist=true"
java -jar target\ocr-tool-0.0.1-SNAPSHOT.jar
```

> `TESSERACT_DATAPATH` di Windows tidak ada default Linux — set ke `C:\tessdata` yang berisi `ind.traineddata` + `eng.traineddata` (download dari https://github.com/tesseract-ocr/tessdata_fast). Kalau tidak butuh OCR, biarkan — app tetap start.

## Environment Variables

Semua key di `application.properties` punya default (`${VAR:default}`), jadi env var opsional untuk dev. Untuk override, export sebelum run atau set di `docker-compose.yml`:

| Key | Default | Keterangan |
|-----|---------|------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://127.0.0.1:3307/ocr_tool?createDatabaseIfNotExist=true` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `ocr` / `ocr` | DB credentials |
| `TESSERACT_DATAPATH` | `/usr/share/tesseract/tessdata` | tessdata dir (override ke `~/.local/share/tessdata` untuk dev) |
| `OCR_SCRIPT_PATH` | `opt/app/ocr/tesseract_ocr.py` | path script Python OCR |
| `KEYSTORE_PASSWORD` / `ALIAS` | `changeit` / `ocr` | JCEKS keystore (`keystore.jks`) |
| `PERIZINAN_DPMTPST_API_BASE_URL` | `http://localhost:8080` | base URL untuk test `perizinan-dpmptsp` |

Lihat `application.properties` dan `scripts/dev-env.sh` untuk daftar lengkap (28 keys).

## Catatan

- `keystore.jks` di-regenerate dengan password `changeit`, alias `ocr` (AES 256, JCEKS). File lama dengan password tidak diketahui sudah diganti.
- `maven-shade-plugin` sudah dihapus dari `pom.xml` (konflik dengan `spring-boot-maven-plugin` repackage → jar hybrid 360MB). Jar sekarang murni Spring Boot fat jar (~171MB).
- `docker-compose.yml.example` sudah diganti `docker-compose.yml` yang valid (env pakai underscore, hapus `SPRING_CONFIG_ADDITIONAL_LOCATION=/lib/x86_64-linux-gnu` yang salah, TESSERACT path untuk Debian image).
- `tessdata` untuk bahasa Indonesia (`ind.traineddata`) perlu di-download manual (lihat Setup).
