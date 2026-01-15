# cloudlb

## 1) Service Purpose
The load balancer exposes the public HTTP API for file operations. It queues
requests, applies scheduling and artificial latency, checks node health, and
publishes scaling requests via MQTT.

## 2) Runtime & Interfaces
- Container name: `load-balancer`
- Internal port: `6869` (HTTP API)
- Protocols: HTTP, MQTT, TCP health probes
- Incoming: HTTP requests from GUI/client
- Outgoing: HTTP to Aggregator, MQTT to broker, TCP health checks to storage nodes,
  HTTP to Aggregator log ingestion

## 3) End-to-End Flow (service-specific)
GUI sends request -> LB queues + schedules + delays -> forwards to Aggregator -> returns response  
Queue depth -> MQTT scale request -> HostManager -> Docker scaling

## 4) Code Flow (key execution path)
- Startup: `MainLb` builds core components, registers nodes, starts health checker,
  scaling scheduler, worker, and HTTP API.
- Request handling: `LoadBalancerAPIServer` -> `RequestQueue` -> `LoadBalancerWorker`
  -> Aggregator API.
- Error/logging: worker logs to Aggregator `LbLogServer` via HTTP.

## 5) Class Summary (short notes)
- `MainLb` - bootstrapper for LB services.
- `LoadBalancerAPIServer` - HTTP endpoints for upload/download/delete/health.
- `LoadBalancerWorker` - consumes queue, schedules, delays, forwards requests.
- `RequestQueue` - priority queue with SJN + aging.
- `Request` - request metadata and priority scoring.
- `ScalingService` - MQTT scale-up/down publisher.
- `HealthChecker` - periodic TCP health probes.
- `NodeRegistry` - healthy node registry.
- `RoundRobinScheduler` / `SjnScheduler` / `RoundRobinDistributionScheduler` - schedulers.

## 6) Directory Tree (depth 4)
```
cloudlb
|-- src
|   `-- main
|       |-- java
|       |   `-- com
|       `-- resources
|           `-- application.properties
|-- nb-configuration.xml
|-- pom.xml
`-- README.md
```

## 7) Environment Variables
- `LB_PORT` default: `6869` (container port mapping; API uses 6869 internally)
- `SCHEDULER_TYPE` default: `ROUNDROBIN`
- `LB_DELAY_MS_MIN` default: `1000`
- `LB_DELAY_MS_MAX` default: `5000`
- `MQTT_BROKER_HOST` default: `mqtt-broker`
- `MQTT_BROKER_PORT` default: `1883`
- `STORAGE_NODES` default: `aggservice-1:9000,aggservice-2:9000`
- `LB_LOG_PORT` default: `9100`
- `AGGREGATOR_HOST` default: `aggregator` (log ingestion host)

## 8) How to Run / Verify
- Docker Compose: `docker compose up -d --build`
- Verify health endpoint:
  - `curl http://localhost:6869/api/health`
- Verify logs in aggregator:
  - `docker logs aggregator | tail`

## 9) Known Issues / Troubleshooting
- `No healthy nodes available`: ensure storage/aggregator nodes are running.
- MQTT errors: verify `mqtt-broker` is up and reachable.
- Scheduler invalid: use `ROUNDROBIN`, `FCFS`, or `SJN` only.
- Long request delays: verify `LB_DELAY_MS_MIN/MAX` values.
