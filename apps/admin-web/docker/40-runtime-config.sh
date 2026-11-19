#!/bin/sh
set -eu

: "${API_BASE_URL:?Set API_BASE_URL, for example https://api.example.com/api/v1}"

case "$API_BASE_URL" in
  http://*|https://*|/*) ;;
  *) echo "API_BASE_URL must start with http://, https:// or /" >&2; exit 1 ;;
esac
case "$API_BASE_URL" in
  *\"*|*\'*|*\\*|*\<*|*\>*|*" "*) echo "API_BASE_URL contains forbidden characters" >&2; exit 1 ;;
esac

printf 'window.__LUTIANO__ = { apiBase: "%s" };\n' "$API_BASE_URL" > /tmp/config.js
echo "runtime config written: apiBase=$API_BASE_URL"