#!/usr/bin/env sh
set -e

SQLITE_DIR="/home/ntu-user/.local/share/comp20081"
USER_HOME="/home/ntu-user"
USER_ENV_FILE="${USER_HOME}/.cloud-env"

mkdir -p "$SQLITE_DIR"
chown -R 1000:1000 "$SQLITE_DIR" 2>/dev/null || true
chmod 775 "$SQLITE_DIR" 2>/dev/null || true

if [ -z "${MYSQL_USER:-}" ] && [ -n "${DB_USER:-}" ]; then
  MYSQL_USER="$DB_USER"
fi
if [ -z "${MYSQL_PASSWORD:-}" ] && [ -n "${DB_PASS:-}" ]; then
  MYSQL_PASSWORD="$DB_PASS"
fi
if [ -z "${DB_USER:-}" ] && [ -n "${MYSQL_USER:-}" ]; then
  DB_USER="$MYSQL_USER"
fi
if [ -z "${DB_PASS:-}" ] && [ -n "${MYSQL_PASSWORD:-}" ]; then
  DB_PASS="$MYSQL_PASSWORD"
fi

PROFILE_ENV="/etc/profile.d/cloud-env.sh"
cat > "$PROFILE_ENV" <<EOF
export MYSQL_HOST="${MYSQL_HOST:-}"
export MYSQL_PORT="${MYSQL_PORT:-}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-}"
export MYSQL_USER="${MYSQL_USER:-}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
export DB_HOST="${DB_HOST:-}"
export DB_PORT="${DB_PORT:-}"
export DB_NAME="${DB_NAME:-}"
export DB_USER="${DB_USER:-}"
export DB_PASS="${DB_PASS:-}"
EOF
chmod 644 "$PROFILE_ENV" 2>/dev/null || true

cat > "$USER_ENV_FILE" <<EOF
export MYSQL_HOST="${MYSQL_HOST:-}"
export MYSQL_PORT="${MYSQL_PORT:-}"
export MYSQL_DATABASE="${MYSQL_DATABASE:-}"
export MYSQL_USER="${MYSQL_USER:-}"
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
export DB_HOST="${DB_HOST:-}"
export DB_PORT="${DB_PORT:-}"
export DB_NAME="${DB_NAME:-}"
export DB_USER="${DB_USER:-}"
export DB_PASS="${DB_PASS:-}"
EOF
chown 1000:1000 "$USER_ENV_FILE" 2>/dev/null || true
chmod 644 "$USER_ENV_FILE" 2>/dev/null || true

for shell_rc in "${USER_HOME}/.profile" "${USER_HOME}/.bash_profile" "${USER_HOME}/.bashrc"; do
  if [ -f "$shell_rc" ]; then
    if ! grep -q ".cloud-env" "$shell_rc"; then
      echo ". \"${USER_ENV_FILE}\"" >> "$shell_rc"
    fi
  else
    echo ". \"${USER_ENV_FILE}\"" > "$shell_rc"
    chown 1000:1000 "$shell_rc" 2>/dev/null || true
    chmod 644 "$shell_rc" 2>/dev/null || true
  fi
done

ENV_SYSTEM="/etc/environment"
TMP_ENV="$(mktemp)"
grep -vE '^(MYSQL_HOST|MYSQL_PORT|MYSQL_DATABASE|MYSQL_USER|MYSQL_PASSWORD|DB_HOST|DB_PORT|DB_NAME|DB_USER|DB_PASS)=' "$ENV_SYSTEM" > "$TMP_ENV" 2>/dev/null || true
cat >> "$TMP_ENV" <<EOF
MYSQL_HOST=${MYSQL_HOST:-}
MYSQL_PORT=${MYSQL_PORT:-}
MYSQL_DATABASE=${MYSQL_DATABASE:-}
MYSQL_USER=${MYSQL_USER:-}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-}
DB_HOST=${DB_HOST:-}
DB_PORT=${DB_PORT:-}
DB_NAME=${DB_NAME:-}
DB_USER=${DB_USER:-}
DB_PASS=${DB_PASS:-}
EOF
mv "$TMP_ENV" "$ENV_SYSTEM"

exec /docker-entrypoint "$@"
