#!/usr/bin/env sh
set -e

SQL_FILE="/docker-entrypoint-initdb.d/01-init-core-schema.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "SQL init file not found: $SQL_FILE" >&2
  exit 1
fi

ROOT_PASS="${MYSQL_ROOT_PASSWORD:-}"

if [ -n "$ROOT_PASS" ]; then
  if mysql -uroot -p"$ROOT_PASS" -e "SELECT 1;" >/dev/null 2>&1; then
    mysql -uroot -p"$ROOT_PASS" < "$SQL_FILE"
    exit 0
  fi
fi

if mysql -uroot -e "SELECT 1;" >/dev/null 2>&1; then
  mysql -uroot < "$SQL_FILE"
  exit 0
fi

echo "Unable to connect to MySQL as root with or without a password." >&2
exit 1
