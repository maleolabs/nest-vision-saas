#!/usr/bin/env bash
# =============================================================================
# build-css.sh — compile Tailwind CSS for Nestara Vision
#
# Generated file is committed at src/main/resources/static/css/app.css so
# local `mvn spring-boot:run` works without this script; re-run whenever
# template classes change.
#
# Strategy:
#   1. Node.js + npm available  -> tailwindcss@3.4.17 via npm (fast, reliable)
#   2. otherwise                -> Tailwind standalone CLI binary (no Node)
#
# Usage:
#   scripts/build-css.sh            # build
#   scripts/build-css.sh --check    # CI mode: fail if generated CSS is stale
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

TW_VERSION="3.4.17"
CACHE_DIR="${TW_CACHE_DIR:-$HOME/.cache/tailwindcss}"
INPUT="src/main/resources/static/css/input.css"
OUTPUT="src/main/resources/static/css/app.css"
CHECK_MODE="${1:-}"

die() { echo "[css][ERROR] $*" >&2; exit 1; }

run_tailwind() { "$@" -i "$INPUT" -o "$OUTPUT" --minify; }

if command -v npm >/dev/null 2>&1 && command -v node >/dev/null 2>&1; then
    # --- Path 1: npm ---------------------------------------------------------
    NPM_DIR="$CACHE_DIR/npm"
    TW_BIN="$NPM_DIR/node_modules/.bin/tailwindcss"
    if [ ! -x "$TW_BIN" ]; then
        echo "[css] installing tailwindcss@$TW_VERSION via npm..."
        mkdir -p "$NPM_DIR"
        npm install --prefix "$NPM_DIR" --no-audit --no-fund --silent \
            "tailwindcss@$TW_VERSION"
    fi
    echo "[css] building $OUTPUT (npm)"
    run_tailwind "$TW_BIN"
else
    # --- Path 2: standalone binary -------------------------------------------
    OS_RAW="$(uname -s)"
    ARCH_RAW="$(uname -m)"
    case "$OS_RAW" in
        Linux)  OS_ID="linux" ;;
        Darwin) OS_ID="macos" ;;
        MINGW*|MSYS*|CYGWIN*) OS_ID="windows" ;;
        *) die "unsupported OS: $OS_RAW" ;;
    esac
    case "$ARCH_RAW" in
        x86_64|amd64) ARCH_ID="x64" ;;
        arm64|aarch64) ARCH_ID="arm64" ;;
        *) die "unsupported arch: $ARCH_RAW" ;;
    esac
    case "${OS_ID}-${ARCH_ID}" in
        linux-x64)     ASSET="tailwindcss-linux-x64" ;;
        linux-arm64)   ASSET="tailwindcss-linux-arm64" ;;
        macos-x64)     ASSET="tailwindcss-macos-x64" ;;
        macos-arm64)   ASSET="tailwindcss-macos-arm64" ;;
        windows-x64)   ASSET="tailwindcss-windows-x64.exe" ;;
        *) die "no mapping for ${OS_ID}-${ARCH_ID}" ;;
    esac

    BIN="$CACHE_DIR/tailwindcss-$TW_VERSION-$ASSET"
    if [ ! -f "$BIN" ]; then
        echo "[css] downloading Tailwind standalone $TW_VERSION ($ASSET)..."
        mkdir -p "$CACHE_DIR"
        # atomic: download to temp then move, so a partial download is never used
        curl -fSL --retry 3 --retry-delay 2 -o "$BIN.part" \
            "https://github.com/tailwindlabs/tailwindcss/releases/download/v${TW_VERSION}/${ASSET}"
        mv "$BIN.part" "$BIN"
        chmod +x "$BIN"
    fi
    echo "[css] building $OUTPUT (standalone)"
    run_tailwind "$BIN"
fi

if [ "$CHECK_MODE" = "--check" ]; then
    if [ -n "$(git status --porcelain -- "$OUTPUT")" ]; then
        die "$OUTPUT is stale — re-run scripts/build-css.sh and commit."
    fi
    echo "[css] ok: generated CSS is up to date"
fi
