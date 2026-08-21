#!/usr/bin/env bash
# =============================================================================
# build-bundle.sh — build self-contained OCR tool bundle for HOST platform
#
# Output: dist/ocr-tool-<version>-<os>-<arch>.(tar.gz|zip) containing:
#   runtime/     jlink-trimmed JRE 17
#   python/      python-build-standalone + site-packages (cv2, pytesseract, numpy)
#   tesseract/   tesseract binary + all shared libs (via micromamba/conda-forge)
#   tessdata/    ind + eng + osd traineddata (tessdata_fast)
#   app/         fat jar + python bridge scripts
#   start.sh / start.bat / README.txt / .env.example
#
# Usage:
#   bash scripts/bundle/build-bundle.sh              # build + archive
#   SKIP_MAVEN=1 bash scripts/bundle/build-bundle.sh # reuse existing target/*.jar
#
# Env overrides:
#   PY_VERSION (3.11.9), PY_RELEASE (20240415), TESSERACT_SPEC ("tesseract"),
#   BUNDLE_NAME, DIST_DIR
#
# Runs natively per platform (no cross-compile):
#   linux-amd64 | linux-arm64 | macos-arm64 | macos-x64 | windows-amd64
#   (windows = git-bash / MSYS2, as used by GitHub Actions windows runners)
# =============================================================================
set -euo pipefail

# --- Config -----------------------------------------------------------------
PY_VERSION="${PY_VERSION:-3.11.9}"
PY_RELEASE="${PY_RELEASE:-20240415}"
TESSERACT_SPEC="${TESSERACT_SPEC:-tesseract}" # conda-forge spec, e.g. "tesseract=5.3"
DIST_DIR="${DIST_DIR:-dist}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

log() { printf '\n[bundle] %s\n' "$*"; }
die() { printf '[bundle][ERROR] %s\n' "$*" >&2; exit 1; }

fetch() { # fetch <url> <out>
    curl -fSL --retry 3 --retry-delay 2 -o "$2" "$1" \
        || die "download failed: $1"
}

# --- Detect host platform ---------------------------------------------------
OS_RAW="$(uname -s)"
ARCH_RAW="$(uname -m)"
case "$OS_RAW" in
    Linux)  OS_ID="linux" ;;
    Darwin) OS_ID="macos" ;;
    MINGW*|MSYS*|CYGWIN*) OS_ID="windows" ;;
    *) die "unsupported OS: $OS_RAW" ;;
esac
case "$ARCH_RAW" in
    x86_64|amd64) ARCH_ID="amd64" ;;
    arm64|aarch64) ARCH_ID="arm64" ;;
    *) die "unsupported arch: $ARCH_RAW" ;;
esac
PLATFORM="${OS_ID}-${ARCH_ID}"
log "host platform: $PLATFORM"

# Per-platform identifiers
case "$PLATFORM" in
    linux-amd64)   PY_TRIPLE="x86_64-unknown-linux-gnu"; MM_SUBDIR="linux-64"   ;;
    linux-arm64)   PY_TRIPLE="aarch64-unknown-linux-gnu"; MM_SUBDIR="aarch64-64" ;;
    macos-arm64)   PY_TRIPLE="aarch64-apple-darwin";      MM_SUBDIR="osx-arm64"  ;;
    macos-amd64)   PY_TRIPLE="x86_64-apple-darwin";       MM_SUBDIR="osx-64"     ;;
    windows-amd64) PY_TRIPLE="x86_64-pc-windows-msvc";    MM_SUBDIR="win-64"     ;;
    *) die "no mapping for $PLATFORM" ;;
esac

# --- Locate JDK (needs jlink + jmods) ---------------------------------------
if [ -z "${JAVA_HOME:-}" ] || [ ! -d "${JAVA_HOME:-}" ]; then
    JBIN="$(command -v java || true)"
    [ -n "$JBIN" ] || die "JAVA_HOME not set and java not in PATH"
    JAVA_HOME="$(cd "$(dirname "$JBIN")/.." && pwd)"
fi
[ -x "$JAVA_HOME/bin/jlink" ] || die "jlink not found at $JAVA_HOME/bin/jlink (need full JDK, not JRE)"
log "JAVA_HOME: $JAVA_HOME"

# --- Build jar --------------------------------------------------------------
JAR=""
if [ "${SKIP_MAVEN:-0}" = "1" ]; then
    JAR="$(ls -t target/ocr-tool-*.jar 2>/dev/null | head -1 || true)"
    [ -n "$JAR" ] || die "SKIP_MAVEN=1 but no target/ocr-tool-*.jar found"
else
    log "building fat jar (mvnw clean package -DskipTests)"
    ./mvnw -B clean package -DskipTests -q
    JAR="$(ls -t target/ocr-tool-*.jar 2>/dev/null | head -1 || true)"
    [ -n "$JAR" ] || die "maven produced no jar"
fi
JAR_BASENAME="$(basename "$JAR")"
VERSION="$(printf '%s' "$JAR_BASENAME" | sed -E 's/.*-([0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?)\.jar/\1/')"
BUNDLE_NAME="${BUNDLE_NAME:-ocr-tool-${VERSION}-${PLATFORM}}"
BUNDLE="$WORK/$BUNDLE_NAME"
mkdir -p "$BUNDLE"
log "bundle: $BUNDLE_NAME (jar: $JAR_BASENAME)"

# --- 1. jlink trimmed JRE ----------------------------------------------------
log "jlink runtime/"
JAVA_MODULES="java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs"
"$JAVA_HOME/bin/jlink" \
    --add-modules "$JAVA_MODULES" \
    --strip-debug --no-man-pages --no-header-files --compress=2 \
    --output "$BUNDLE/runtime"

# --- 2. Python standalone -----------------------------------------------------
log "python/ (cpython $PY_VERSION standalone)"
PY_URL="https://github.com/astral-sh/python-build-standalone/releases/download/${PY_RELEASE}/cpython-${PY_VERSION}+${PY_RELEASE}-${PY_TRIPLE}-install_only.tar.gz"
fetch "$PY_URL" "$WORK/python.tar.gz"
mkdir -p "$BUNDLE/python"
tar -xzf "$WORK/python.tar.gz" -C "$WORK"
# tarball contains top-level python/
cp -R "$WORK/python/." "$BUNDLE/python/"

if [ "$OS_ID" = "windows" ]; then
    PYEXE="$BUNDLE/python/python.exe"
else
    PYEXE="$BUNDLE/python/bin/python3"
fi
[ -x "$PYEXE" ] || die "bundled python not found at $PYEXE"

log "pip install site-packages (opencv-python-headless, pytesseract, numpy)"
"$PYEXE" -m pip --version >/dev/null 2>&1 || "$PYEXE" -m ensurepip --upgrade
"$PYEXE" -m pip install --quiet --no-warn-script-location \
    --target "$BUNDLE/python/site-packages" \
    opencv-python-headless pytesseract numpy

# --- 3. Tesseract via micromamba (conda-forge) -------------------------------
log "tesseract/ (micromamba + conda-forge: $TESSERACT_SPEC)"
MM_URL="https://micro.mamba.pm/api/micromamba/${MM_SUBDIR}/latest"
fetch "$MM_URL" "$WORK/mm.tar.bz2"
mkdir -p "$WORK/mm"
tar -xjf "$WORK/mm.tar.bz2" -C "$WORK/mm"
# Layout differs per platform: unix = bin/micromamba,
# win-64 = Library/bin/micromamba.exe (conda package layout).
# Never match by name prefix: newer tarballs ship info/test fixtures
# like micromamba_windows_allowed_dlls.tsv that would get executed.
if [ "$OS_ID" = "windows" ]; then
    MM_BIN="$WORK/mm/Library/bin/micromamba.exe"
else
    MM_BIN="$WORK/mm/bin/micromamba"
fi
if [ ! -f "$MM_BIN" ]; then
    # fallback: exact-name search only
    MM_BIN="$(find "$WORK/mm" -type f \( -name 'micromamba.exe' -o -name 'micromamba' \) | head -1)"
fi
[ -n "$MM_BIN" ] && [ -f "$MM_BIN" ] || die "micromamba binary not found in extracted tarball"
chmod +x "$MM_BIN"

export MAMBA_ROOT_PREFIX="$WORK/mamba-root"
"$MM_BIN" create -y -q -p "$BUNDLE/tesseract" -c conda-forge "$TESSERACT_SPEC"

# slim: drop docs/cmake leftovers
rm -rf "$BUNDLE/tesseract/share/man" "$BUNDLE/tesseract/share/doc" \
       "$BUNDLE/tesseract/share/cmake" 2>/dev/null || true

TESS_BIN="$BUNDLE/tesseract/bin/tesseract"
[ "$OS_ID" = "windows" ] && TESS_BIN="$BUNDLE/tesseract/bin/tesseract.exe"
[ -x "$TESS_BIN" ] || die "tesseract binary not found at $TESS_BIN"

# --- 4. tessdata ---------------------------------------------------------------
log "tessdata/ (ind, eng, osd from tessdata_fast)"
mkdir -p "$BUNDLE/tessdata"
for LANG_CODE in ind eng osd; do
    fetch "https://github.com/tesseract-ocr/tessdata_fast/raw/main/${LANG_CODE}.traineddata" \
        "$BUNDLE/tessdata/${LANG_CODE}.traineddata"
done

# --- 5. app/ -------------------------------------------------------------------
log "app/"
mkdir -p "$BUNDLE/app/ocr/extractor"
cp "$JAR" "$BUNDLE/app/ocr-tool.jar"
cp opt/app/ocr/*.py "$BUNDLE/app/ocr/"
cp opt/app/ocr/extractor/*.py "$BUNDLE/app/ocr/extractor/"
find "$BUNDLE/app" -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true
cp .env.example "$BUNDLE/.env.example"

# --- 6. Launchers ----------------------------------------------------------------
log "launchers (start.sh, start.bat)"

cat > "$BUNDLE/start.sh" <<'LAUNCHER_SH'
#!/usr/bin/env bash
# Self-contained launcher — expects sibling folders: runtime/, python/,
# tesseract/, tessdata/, app/. Optional .env overrides application.properties.
set -euo pipefail
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export TESSERACT_CMD="$DIR/tesseract/bin/tesseract"
export TESSERACT_DATAPATH="$DIR/tessdata"
export OCR_PYTHON_PATH="$DIR/python/bin/python3"
export OCR_SCRIPT_PATH="$DIR/app/ocr/tesseract_ocr.py"
export PYTHONPATH="$DIR/python/site-packages${PYTHONPATH:+:$PYTHONPATH}"
export LD_LIBRARY_PATH="$DIR/tesseract/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export DYLD_FALLBACK_LIBRARY_PATH="$DIR/tesseract/lib${DYLD_FALLBACK_LIBRARY_PATH:+:$DYLD_FALLBACK_LIBRARY_PATH}"

if [ -f "$DIR/.env" ]; then
    set -a; source "$DIR/.env"; set +a
fi

exec "$DIR/runtime/bin/java" \
    -Djna.library.path="$DIR/tesseract/lib" \
    -jar "$DIR/app/ocr-tool.jar"
LAUNCHER_SH
chmod +x "$BUNDLE/start.sh"

cat > "$BUNDLE/start.bat" <<'LAUNCHER_BAT'
@echo off
REM Self-contained launcher - expects sibling folders: runtime\, python\,
REM tesseract\, tessdata\, app\. Optional .env overrides application.properties.
setlocal EnableDelayedExpansion
set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

set "TESSERACT_CMD=%DIR%\tesseract\bin\tesseract.exe"
set "TESSERACT_DATAPATH=%DIR%\tessdata"
set "OCR_PYTHON_PATH=%DIR%\python\python.exe"
set "OCR_SCRIPT_PATH=%DIR%\app\ocr\tesseract_ocr.py"
set "PYTHONPATH=%DIR%\python\site-packages"
set "PATH=%DIR%\tesseract\bin;%DIR%\tesseract\lib;%PATH%"

if exist "%DIR%\.env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%DIR%\.env") do (
        if not "%%a"=="" set "%%a=%%b"
    )
)

"%DIR%\runtime\bin\java.exe" ^
    -Djna.library.path="%DIR%\tesseract\lib" ^
    -jar "%DIR%\app\ocr-tool.jar"
pause
LAUNCHER_BAT

cat > "$BUNDLE/README.txt" <<README_TXT
OCR Tool ${VERSION} — ${PLATFORM}
====================================

Self-contained bundle: JRE + Python + Tesseract included. No installation needed.

Requirements:
  - MySQL database reachable (configure via .env, see .env.example)
  - ~500 MB free disk space

Run:
  Linux/macOS : ./start.sh
  Windows     : double-click start.bat  (or run from cmd)

First time:
  1. cp .env.example .env
  2. edit SPRING_DATASOURCE_URL / USERNAME / PASSWORD
  3. run start.sh / start.bat
  4. open http://localhost:8080

Notes:
  - macOS first run: right-click start.sh -> Open, or run:
      xattr -cr . && ./start.sh
  - All OCR_* tuning knobs are documented in .env.example
  - Logs print to this console; Ctrl+C stops the app
README_TXT

# --- 7. Validate -----------------------------------------------------------------
log "validating bundle"
export LD_LIBRARY_PATH="$BUNDLE/tesseract/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
export DYLD_FALLBACK_LIBRARY_PATH="$BUNDLE/tesseract/lib${DYLD_FALLBACK_LIBRARY_PATH:+:$DYLD_FALLBACK_LIBRARY_PATH}"
"$TESS_BIN" --version | head -1 || die "tesseract smoke test failed"
PYTHONPATH="$BUNDLE/python/site-packages" "$PYEXE" -c "import cv2, pytesseract, numpy; print('py deps ok:', cv2.__version__)" \
    || die "python deps smoke test failed"
"$BUNDLE/runtime/bin/java" -version 2>&1 | head -1 || die "bundled java smoke test failed"

# --- 8. Archive ------------------------------------------------------------------
log "archive -> $DIST_DIR/"
mkdir -p "$DIST_DIR"
STAGE="$REPO_ROOT/$DIST_DIR"
mv "$BUNDLE" "$STAGE/$BUNDLE_NAME"

SHASUM="sha256sum"
command -v sha256sum >/dev/null 2>&1 || SHASUM="shasum -a 256"

if [ "$OS_ID" = "windows" ]; then
    ARCHIVE="$DIST_DIR/$BUNDLE_NAME.zip"
    if command -v 7z >/dev/null 2>&1; then
        (cd "$STAGE" && 7z a -tzip "../$ARCHIVE" "$BUNDLE_NAME" >/dev/null)
    elif command -v zip >/dev/null 2>&1; then
        (cd "$STAGE" && zip -qr "../$ARCHIVE" "$BUNDLE_NAME")
    else
        powershell.exe -NoProfile -Command "Compress-Archive -Path '$STAGE\\$BUNDLE_NAME' -DestinationPath '$REPO_ROOT\\$ARCHIVE' -Force" \
            || die "no zip tool found (tried 7z, zip, powershell)"
    fi
else
    ARCHIVE="$DIST_DIR/$BUNDLE_NAME.tar.gz"
    (cd "$STAGE" && tar -czf "../$ARCHIVE" "$BUNDLE_NAME")
fi

$SHASUM "$ARCHIVE" > "$ARCHIVE.sha256"

log "done:"
ls -lh "$DIST_DIR/$BUNDLE_NAME"* | awk '{print "  " $5 "\t" $9}'
