#!/usr/bin/env sh
set -eu

SQL_FILE="/docker-entrypoint-initdb.d/01-init-core-schema.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "SQL init file not found: $SQL_FILE" >&2
  exit 1
fi

ROOT_PASS="${MYSQL_ROOT_PASSWORD:-}"
DB_NAME="${MYSQL_DATABASE:-${MYSQL_DB:-dbtutorial}}"
APP_USER="${MYSQL_USER:-admin}"
APP_PASS="${MYSQL_PASSWORD:-admin}"

ROOT_MODE=""
if [ -n "$ROOT_PASS" ] && mysql -uroot -p"$ROOT_PASS" -e "SELECT 1;" >/dev/null 2>&1; then
  ROOT_MODE="password"
elif mysql -uroot -e "SELECT 1;" >/dev/null 2>&1; then
  ROOT_MODE="nopass"
else
  echo "Unable to connect to MySQL as root with or without a password." >&2
  exit 1
fi

run_root() {
  if [ "$ROOT_MODE" = "password" ]; then
    mysql -uroot -p"$ROOT_PASS" "$@"
  else
    mysql -uroot "$@"
  fi
}

# Ensure database exists and schema is present.
run_root -e "CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`;"
TABLE_EXISTS="$(run_root -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}' AND table_name='User_Profiles';")"
if [ "$TABLE_EXISTS" = "0" ]; then
  run_root < "$SQL_FILE"
fi

# Enforce app user credentials to match the container env.
run_root -e "CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASS}';"
run_root -e "ALTER USER '${APP_USER}'@'%' IDENTIFIED BY '${APP_PASS}';"
run_root -e "GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${APP_USER}'@'%';"
run_root -e "FLUSH PRIVILEGES;"

# Verify app user can log in to the target database.
mysql -u"$APP_USER" -p"$APP_PASS" -e "USE \`${DB_NAME}\`; SELECT 1;" >/dev/null 2>&1
