# MySQL Init Scripts

## 1) Service Purpose
Defines the MySQL schema and seed data for the platform. The init SQL creates
user profiles, file metadata, chunk metadata, ACL, and audit log tables.

## 2) Runtime & Interfaces
- Container name: `lamp-server`
- Internal port: `3306`
- Protocols: JDBC/MySQL
- Incoming: connections from GUI and Aggregator
- Outgoing: none

## 3) End-to-End Flow (service-specific)
Docker starts MySQL -> init scripts run on fresh volume -> schema is created -> services connect

## 4) Code Flow (key execution path)
- `01-init-core-schema.sql` creates tables and seeds the admin user.
- `scripts/mysql-init.sh` validates schema and enforces app user credentials.

## 5) Class Summary (short notes)
- Not applicable (SQL + shell scripts).

## 6) Directory Tree (depth 4)
```
config/mysql
`-- 01-init-core-schema.sql
```

## 7) Environment Variables
Used by `lamp-server` and `mysql-init.sh`:
- `MYSQL_ROOT_PASSWORD` default: `admin`
- `MYSQL_DATABASE` or `MYSQL_DB` default: `dbtutorial`
- `MYSQL_USER` default: `admin`
- `MYSQL_PASSWORD` default: `admin`

## 8) How to Run / Verify
- Start with Docker Compose: `docker compose up -d --build`
- Verify tables:
```bash
docker exec -it lamp-server mysql -uadmin -padmin -e "USE dbtutorial; SHOW TABLES;"
```

## 9) Known Issues / Troubleshooting
- Schema does not re-run if volume exists. Use `docker compose down -v`.
- Wrong credentials: update `.env` and restart `lamp-server`.
