#!/bin/sh
set -eu
if [ -z "${APP_API_KEY_ENCRYPTION_SECRET:-}" ]; then
  file=/run/secrets/app-api-key-encryption-secret; mkdir -p /run/secrets
  if [ -s "$file" ]; then APP_API_KEY_ENCRYPTION_SECRET=$(cat "$file"); else umask 077; APP_API_KEY_ENCRYPTION_SECRET=$(head -c 32 /dev/urandom | base64 | tr -d '\n'); printf %s "$APP_API_KEY_ENCRYPTION_SECRET" > "$file"; fi
fi
export APP_API_KEY_ENCRYPTION_SECRET
exec "$@"
