# cloud-gui

## 1) Service Purpose
The JavaFX GUI provides the user-facing interface for authentication, file
operations, sharing, logs, and terminal emulation. It also supports offline use
through a local SQLite cache that syncs back to MySQL when online.

## 2) Runtime & Interfaces
- Container name: `ntu-vm-soft40051` (when using Docker Compose)
- Internal ports: 3389 (RDP), 22 (SSH)
- Protocols: JavaFX UI, HTTP (to Load Balancer), JDBC (MySQL), SQLite (local),
  SSH (remote terminal)
- Incoming: RDP/SSH access to the VM container
- Outgoing: HTTP to Load Balancer, JDBC to MySQL, SSH to storage nodes

## 3) End-to-End Flow (service-specific)
User action -> GUI -> Load Balancer (6869) -> Aggregator (9000) -> SFTP storage nodes  
Offline mode: GUI -> SQLite cache -> SyncService -> MySQL (when online)

## 4) Code Flow (key execution path)
- Startup: `MainApp` initializes databases -> starts `SyncService` -> loads login UI
- Request handling: Controller -> `AuthService`/`FileService` -> `LoadBalancerClient`
- Error/logging: `LoggingService` -> local SQLite log + `SystemLogRepository`

## 5) Class Summary (short notes)
- `MainApp` - JavaFX entry point, DB init, starts sync loop.
- `LoginController` - handles login and scene transition.
- `DashboardController` - main navigation and status display.
- `FilesController` - file CRUD, upload/download, share.
- `AuthService` - authentication and user management.
- `FileService` - file metadata ops and LB client calls.
- `SyncService` - SQLite <-> MySQL sync and conflict handling.
- `LoggingService` - audit logging to SQLite/MySQL.
- `LocalTerminalService` - whitelisted ProcessBuilder commands.
- `RemoteTerminalService` - SSH command execution via JSch.

## 6) Directory Tree (depth 4)
```
cloud-gui
|-- src
|   `-- main
|       |-- java
|       |   `-- com
|       `-- resources
|           `-- com
`-- pom.xml
```

## 7) Environment Variables
Used by `LoadBalancerClient` + MySQL connection utilities:
- `LB_BASE_URL` (optional) default: unset
- `LB_HOST` default: `load-balancer`
- `LB_PORT` default: `6869`
- `MYSQL_HOST` / `DB_HOST` default: `lamp-server`
- `MYSQL_PORT` / `DB_PORT` default: `3306`
- `MYSQL_DATABASE` / `DB_NAME` default: `dbtutorial`
- `MYSQL_USER` / `DB_USER` default: unset
- `MYSQL_PASSWORD` / `DB_PASS` default: unset

## 8) How to Run / Verify
- Docker Compose:
  - `docker compose up -d --build`
  - Connect via RDP `localhost:3390` and log in with `admin/admin`
- Local run:
  - `mvn -f cloud-gui/pom.xml javafx:run`
- Verify:
  - Login succeeds and dashboard loads
  - File list loads (online) or shows cached items (offline)

## 9) Known Issues / Troubleshooting
- Login fails: verify MySQL is running and `admin/admin` is seeded.
- Offline mode: if MySQL is unreachable, actions are queued in SQLite until sync.
- Remote terminal fails: confirm SSH to storage nodes is reachable and credentials match.
- RDP not reachable: confirm the `ntu-vm-soft40051` container is running and port 3390 is free.
