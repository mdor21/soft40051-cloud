# mqtt

## 1) Service Purpose
Provides the MQTT broker used for scaling signals between the load balancer and
the host manager.

## 2) Runtime & Interfaces
- Container name: `mqtt-broker`
- Internal ports: `1883` (MQTT), `9001` (optional external listener)
- Protocols: MQTT
- Incoming: publishes/subscribes from Load Balancer and Host Manager
- Outgoing: broker fan-out to subscribers

## 3) End-to-End Flow (service-specific)
Load Balancer publishes scale request -> MQTT broker -> Host Manager subscribes

## 4) Code Flow (key execution path)
- `mqtt/config/mosquitto.conf` defines listeners and persistence.

## 5) Class Summary (short notes)
- Not applicable (broker config only).

## 6) Directory Tree (depth 4)
```
mqtt
`-- config
    `-- mosquitto.conf
```

## 7) Environment Variables
No service-specific env vars. Topics are controlled by app configs:
- Load Balancer publishes to `traffic/progress` (default).
- Host Manager subscribes to `MQTT_TOPIC` (default `traffic/progress`).

## 8) How to Run / Verify
- Docker Compose: `docker compose up -d --build`
- Verify message flow:
```bash
mosquitto_sub -h localhost -t traffic/progress
```

## 9) Known Issues / Troubleshooting
- Broker not reachable: ensure port 1883 is open and container is healthy.
- Topic mismatch: confirm LB topic matches Host Manager topic.
