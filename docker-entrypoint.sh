#!/bin/sh
set -eu
file=${APP_API_KEY_ENCRYPTION_SECRET_FILE:-/run/secrets/app-api-key-encryption-secret}
if [ -z "${APP_API_KEY_ENCRYPTION_SECRET:-}" ]; then
  mkdir -p "${file%/*}"
  if [ -s "$file" ]; then
    APP_API_KEY_ENCRYPTION_SECRET=$(tr -d '\r\n' < "$file")
  else
    umask 077
    APP_API_KEY_ENCRYPTION_SECRET=$(head -c 32 /dev/urandom | base64 | tr -d '\r\n')
    tmp="$file.$$"
    printf %s "$APP_API_KEY_ENCRYPTION_SECRET" > "$tmp" && mv -n "$tmp" "$file" 2>/dev/null || true
    [ -s "$file" ] && APP_API_KEY_ENCRYPTION_SECRET=$(tr -d '\r\n' < "$file")
  fi
fi
decoded=$(printf %s "$APP_API_KEY_ENCRYPTION_SECRET" | base64 -d 2>/dev/null | wc -c | tr -d ' ')
[ "$decoded" = 32 ] || { echo 'APP_API_KEY_ENCRYPTION_SECRET must be base64 encoding of 32 bytes' >&2; exit 1; }
export APP_API_KEY_ENCRYPTION_SECRET
exec "$@"
