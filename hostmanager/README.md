# hostmanager

## 1) Service Purpose
The Host Manager bridges the load balancer and the host Docker engine. It
listens to MQTT scaling requests and translates them into Docker CLI commands
to start/stop storage containers.

## 2) Runtime & Interfaces
- Container name: none (runs on host machine)
- Internal ports: none
- Protocols: MQTT, Docker CLI (ProcessBuilder)
- Incoming: MQTT scaling requests from Load Balancer
- Outgoing: Docker commands to the host OS, MQTT scaling events

## 3) End-to-End Flow (service-specific)
LB publishes scale request -> MQTT broker -> HostManager -> Docker CLI -> new containers

## 4) Code Flow (key execution path)
- Startup: `HostManager` initializes Docker, MQTT, scaling, health checks.
- Request handling: MQTT subscribe -> `MqttMessageParser` -> `ScalingLogic`.
- Error/logging: Log4j logs scaling and Docker command outcomes.

## 5) Class Summary (short notes)
- `HostManager` - main app, MQTT subscription, scheduler.
- `MqttConnectionManager` - MQTT connect/subscribe/publish.
- `MqttMessageParser` - parses scaling JSON payloads.
- `ScalingLogic` - decides which containers to start/stop.
- `DockerCommandExecutor` - ProcessBuilder wrapper for Docker CLI.
- `ContainerManager` - tracks active containers and health.
- `HealthCheckManager` - periodic Docker inspect health checks.

## 6) Directory Tree (depth 4)
```
hostmanager
|-- src
|   |-- main
|   |   |-- java
|   |   |   `-- com
|   |   `-- resources
|   |       `-- application.properties
|   `-- test
|       `-- java
|           `-- com
|-- dependency-reduced-pom.xml
`-- pom.xml
```

## 7) Environment Variables
- `MQTT_BROKER_HOST` default: `localhost`
- `MQTT_BROKER_PORT` default: `1883`
- `MQTT_TOPIC` default: `traffic/progress`
- `MQTT_TOPIC_EVENTS` default: `hm/scale/event`
- `STORAGE_BASE_DIR` default: `./storage`
- `SFTP_USER` default: `ntu-user`
- `SFTP_PASS` default: (warns if unset)
- `PUID` default: `1000`
- `PGID` default: `1000`
- `TZ` default: `Etc/UTC`

## 8) How to Run / Verify
```bash
mvn -f hostmanager/pom.xml clean package
java -jar hostmanager/target/hostmanager-1.0-SNAPSHOT.jar
```

Verify scaling by publishing a message:
```bash
mosquitto_pub -h localhost -t traffic/progress -m '{"action":"up","count":1}'
```

Check running containers:
```bash
docker ps | grep soft40051-files-container
```

## 9) Known Issues / Troubleshooting
- Docker not accessible: ensure Docker is running and your user has permissions.
- MQTT topic mismatch: confirm `MQTT_TOPIC` aligns with LB topic.
- SFTP_PASS missing: containers start with an empty password (warning logged).
