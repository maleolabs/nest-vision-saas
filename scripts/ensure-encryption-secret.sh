#!/usr/bin/env bash
set -euo pipefail
file="${XDG_CONFIG_HOME:-$HOME/.config}/ocr-tool/api-key-encryption-secret"
secret="${APP_API_KEY_ENCRYPTION_SECRET:-}"
[ -n "$secret" ] || [ ! -s "$file" ] || secret="$(tr -d '\r\n' < "$file")"
if [ -z "$secret" ]; then mkdir -p "${file%/*}"; chmod 700 "${file%/*}"; secret="$(openssl rand -base64 32 | tr -d '\r\n')"; (umask 077; printf %s "$secret" > "$file"); fi
[ "$(printf %s "$secret" | base64 -d 2>/dev/null | wc -c | tr -d ' ')" = 32 ] || { echo 'APP_API_KEY_ENCRYPTION_SECRET must be base64 encoding of 32 bytes' >&2; exit 1; }
printf '%s\n' "$secret"
