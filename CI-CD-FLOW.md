# CI/CD Flow (Current Configuration)

This document describes the end-to-end CI/CD flow as implemented in the current setup, aligned with the SOFT40051 requirements and the existing Docker/Gitea/Jenkins configuration.

## 1) Developer Environment (NetBeans in ntu-vm-soft40051)
- All code changes are made inside NetBeans running in the `ntu-vm-soft40051` container.
- The project is cloned from the **Olympus** repository (official submission repo).
- Developers commit and push to **Olympus** (not directly to Gitea in this setup).

## 2) Source Control Topology
- **Olympus**: official private submission/backup repository.
- **Gitea**: local self-hosted Git server container that mirrors Olympus.
- **Jenkins**: pulls and builds from the **Gitea** mirror only.

## 3) CI Trigger (Automatic Pull Latest Code)
Jenkins is configured to poll the **Gitea mirror** on a schedule:
```
H/15 * * * *
```
Because Gitea is a **mirror**, Jenkins sees new commits only after the mirror syncs from Olympus.

### Mirror sync requirement
After you push to Olympus, trigger a mirror sync so Jenkins can see the latest commit:
```
curl -s -X POST -H "Authorization: token $GITEA_TOKEN" \
  http://localhost:3000/api/v1/repos/admin/soft40051-cloud/mirror-sync
```
This command is run on the host terminal (macOS), where `localhost:3000` maps to the Gitea container.

## 4) Jenkins Pipeline Responsibilities (CI)
Jenkins runs the pipeline defined in `Jenkinsfile` and performs **build + test** for all modules.

### Modules covered
- Main GUI (`cloud-gui`)
- Load Balancer (`cloudlb`)
- File Aggregator (`AggService`)
- Host Manager (`hostmanager`)

### Build/test command
Jenkins runs this for each discovered `pom.xml`:
```
mvn clean verify
```
This:
- Compiles all modules
- Executes **JUnit tests**
- Fails the pipeline immediately if any tests fail

JUnit reports are collected from:
```
**/target/surefire-reports/*.xml
```

## 5) Runtime Orchestration (Not Jenkins)
Jenkins does **not** control runtime scaling or container orchestration.
That responsibility sits with **Host Manager**, which:
- Subscribes to MQTT
- Interprets scaling requests
- Uses `ProcessBuilder` to run Docker commands
- Starts/stops storage containers at runtime

This separation of concerns is a required distinction in the assessment.

## 6) Docker Runtime Environment (soft40051_network)
All services run on the shared Docker network:
```
soft40051_network
```
Key runtime services include:
- `ntu-vm-soft40051` (NetBeans GUI)
- `load-balancer` (6869)
- `aggregator` (9000)
- `lamp-server` (MySQL)
- `mqtt-broker` (1883)
- File storage containers (SFTP on ports 4848–4851)
- `jenkins-soft40051`
- `gitea`

Database initialization is handled automatically via mounted init scripts and healthchecks.

## 7) Expected Demonstration Flow
1. Make a code change in NetBeans (ntu-vm-soft40051).
2. Commit + push to Olympus.
3. Trigger Gitea mirror sync.
4. Jenkins detects the new commit (SCM polling) and starts the build.
5. Jenkins runs `mvn clean verify` for all modules.
6. Jenkins displays Green/Red status in the UI.
7. Explain that Host Manager handles runtime scaling via MQTT + Docker commands, not Jenkins.
