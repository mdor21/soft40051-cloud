# CI/CD Evidence Pack (SOFT40051)

This document captures the CI/CD and orchestration evidence for the project.
All commands below were run on the host machine unless stated otherwise.

## 1) Stack Composition and Persistence

### docker-compose.yml (key services)
- Jenkins: `jenkins-soft40051` on `http://localhost:8081`
- Gitea: `gitea` on `http://localhost:3000` and SSH `localhost:2222`
- NetBeans VM: `ntu-vm-soft40051` (RDP `3390`, SSH `2022`)
- Load Balancer: `load-balancer` on `6869`
- Aggregator: `aggregator` on `9000`
- MQTT: `mqtt-broker` on `1883`
- MySQL: `lamp-server` on `3306`
- Storage nodes: `soft40051-files-container1..4` on `4848..4851`

### Persistent volumes (survive restarts)
- `jenkins_data` (jobs, plugins, workspaces)
- `gitea_data` (repos, users, mirror config)
- `docker_soft40051` (NetBeansProjects)
- `mysql_data`, `mqtt_data`, `sqlite_data`

Important:
- `docker compose down` keeps data.
- `docker compose down -v` removes volumes and deletes Jenkins/Gitea data.

## 2) Repo and Remote Setup

Remotes on host:
```
olympus https://olympus.ntu.ac.uk/N1411795/soft40051-cloud.git (fetch)
olympus https://olympus.ntu.ac.uk/N1411795/soft40051-cloud.git (push)
origin  https://github.com/mdor21/soft40051-cloud (fetch)
origin  https://github.com/mdor21/soft40051-cloud (push)
```

Policy:
- Olympus is the official submission repo.
- Gitea is a mirror of Olympus for CI.

## 3) CI/CD Flow (Verified)

### Step A: Commit and push to Olympus
```
git push olympus main
```
Observed:
```
To https://olympus.ntu.ac.uk/N1411795/soft40051-cloud.git
   c31a365..7a627c5  main -> main
```

### Step B: Mirror sync (Gitea)
```
export GITEA_TOKEN="your_token_here"
curl -s -X POST -H "Authorization: token $GITEA_TOKEN" \
  http://localhost:3000/api/v1/repos/admin/soft40051-cloud/mirror-sync
```
Observed in Gitea logs:
```
POST /api/v1/repos/admin/soft40051-cloud/mirror-sync ... 200 OK
```

### Step C: Trigger Jenkins build
```
java -jar /tmp/jenkins-cli.jar -s "$JENKINS_URL" -auth "$JENKINS_AUTH" \
  build soft40051-ci -s
```
Observed:
```
Started soft40051-ci #9
Completed soft40051-ci #9 : SUCCESS
```

### Step D: Jenkins console evidence (build #9)
Jenkins checked out commit:
```
Checking out Revision 7a627c5743d240e53617a1c99bab5c574fa2ad0c (origin/main)
Commit message: "Add JUnit tests for GUI and Load Balancer"
```

JUnit tests executed for all modules:
- AggService:
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
- cloud-gui:
```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```
- cloudlb:
```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```
- hostmanager:
```
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
```

Final pipeline status:
```
Finished: SUCCESS
```

## 4) Runtime Orchestration (Non-CI)

CI responsibilities:
- Jenkins only builds and runs tests.
- Jenkins does not control live containers.

Runtime scaling responsibilities:
- Host Manager listens to MQTT.
- Host Manager uses ProcessBuilder to run Docker commands.
- Load Balancer publishes scaling events via MQTT.

Flow:
```
Load Balancer -> MQTT Broker -> Host Manager -> Docker (scale containers)
```

## 5) Test Coverage Fixes (Ensuring "All Modules")

Added minimal tests:
- `cloud-gui/src/test/java/com/ntu/cloudgui/app/session/InMemoryStoreTest.java`
- `cloudlb/src/test/java/com/ntu/cloudgui/cloudlb/RequestTest.java`
- `cloudlb/src/test/java/com/ntu/cloudgui/cloudlb/RequestQueueTest.java`

JUnit dependency added in `cloud-gui/pom.xml`.

## 6) What to Show in a Demo

1) Push to Olympus.
2) Mirror sync Gitea.
3) Trigger Jenkins build.
4) Show Jenkins console with:
   - commit SHA
   - test counts for all modules
   - final SUCCESS
5) State: Jenkins is CI only; Host Manager handles runtime scaling.
