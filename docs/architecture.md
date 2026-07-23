# Sarb (سرب) — Architecture

**Sarb** is a drone fleet operations platform. Arabic, RTL-first UI; English code/APIs/topics. Built incrementally (see the build checklist in `../drone-fleet-platform-brief.md`, Section 17).

## Target architecture (Phase 3+)

The system is event-driven around a single high-volume telemetry stream. The simulator publishes to `drone.telemetry`; multiple independent Kafka consumers each do their own job (WebSocket push, alerting, flight-log persistence) without knowing about each other. **This fan-out is why Kafka is here.**

```mermaid
flowchart LR
  subgraph Client["Browser"]
    FE["Frontend<br/>React + TS · shadcn/ui<br/>Arabic RTL · Leaflet · STOMP"]
  end

  subgraph Edge["Edge"]
    GW["API Gateway"]
    AUTH["Auth Service<br/>JWT (Spring Security)"]
  end

  subgraph Core["Backend services — Spring Boot, Java 21"]
    SIM["Simulator Service<br/>N virtual drones · tick loop"]
    TEL["Telemetry Service<br/>WebSocket push + flight-log writer"]
    ALERT["Alert Service<br/>geofence / battery / signal rules"]
    MIS["Mission Service"]
    RAG["RAG Service<br/>Spring AI"]
  end

  subgraph Bus["Apache Kafka (KRaft)"]
    T1(["drone.telemetry"])
    T2(["drone.alerts"])
    T3(["mission.events"])
  end

  subgraph Data["Storage"]
    PG[("PostgreSQL<br/>relational + pgvector")]
  end

  CLAUDE["Claude API<br/>+ embeddings"]

  FE -->|REST| GW
  FE <-->|STOMP / WebSocket| TEL
  GW --> AUTH
  GW --> MIS
  GW --> RAG
  GW --> TEL

  SIM -->|produces| T1
  T1 --> TEL
  T1 --> ALERT
  ALERT -->|produces| T2
  T2 --> TEL
  MIS -->|produces| T3
  T3 --> SIM
  T3 --> TEL

  TEL -->|flight logs| PG
  MIS --> PG
  AUTH --> PG
  RAG -->|vectors + similarity search| PG
  RAG -->|LLM + embed calls| CLAUDE
```

## Current shape (Phase 4 — mission assignment is real)

`simulator-service` is a Kafka producer of `drone.telemetry` (one message per drone per tick, keyed by `droneId`) and, since Phase 4, also a consumer/producer on `mission.events`. `telemetry-service` and `alert-service` are independent consumers of `drone.telemetry` - neither knows the other exists. `alert-service` produces `drone.alerts`, which `telemetry-service` also consumes and relays onto the WebSocket the frontend already holds. `mission-service` owns mission CRUD (Postgres) and publishes `ASSIGNED`/`CANCELLED` lifecycle events; `simulator-service` is the only party that knows when a drone actually starts/finishes flying a route, so it publishes `STARTED`/`COMPLETED` back onto the same topic, and `mission-service` consumes its own topic to keep mission rows in sync. `telemetry-service` also relays `mission.events` onto the WebSocket for live mission status in the UI. Gateway/auth (and the REST proxying they'd give the frontend) are still Phase 6 - until then the frontend talks to `telemetry-service` (WebSocket), `alert-service` (REST), and `mission-service` (REST) directly.

```mermaid
flowchart LR
  FE["Frontend<br/>shadcn/ui · Arabic RTL · Leaflet"]
  SIM["Simulator Service<br/>tick loop · flies assigned routes"]
  T1(["drone.telemetry"])
  T2(["drone.alerts"])
  T3(["mission.events"])
  TEL["Telemetry Service<br/>consumes all 3 topics · STOMP host · flight-log writer"]
  ALERT["Alert Service<br/>consumes drone.telemetry · rule engine · produces drone.alerts"]
  MIS["Mission Service<br/>mission CRUD · produces ASSIGNED/CANCELLED"]
  PG[("PostgreSQL")]

  SIM -->|produces| T1
  T1 --> TEL
  T1 --> ALERT
  ALERT -->|produces| T2
  T2 --> TEL
  MIS -->|produces ASSIGNED/CANCELLED| T3
  T3 --> SIM
  SIM -->|produces STARTED/COMPLETED| T3
  T3 --> TEL
  T3 --> MIS
  TEL -->|"STOMP /topic/telemetry, /topic/alerts, /topic/missions"| FE
  FE -->|"REST GET /api/alerts/active"| ALERT
  FE -->|"REST POST/GET /api/missions"| MIS
  TEL -->|flight_logs| PG
  ALERT -->|alerts| PG
  MIS -->|missions| PG
```

> Note: `simulator-service`, `telemetry-service`, `alert-service`, and `mission-service` each carry their own copy of the wire-shape classes they need (`TelemetryFrame`/`Position`/`DroneStatus`, `AlertDto`/`AlertType`/`AlertSeverity`, `MissionEventDto`/`WaypointDto`/`MissionEventType`) rather than sharing a library module - each service stays independently deployable, at the cost of manually keeping the mirrored shapes in sync. Kafka JSON (de)serialization: producers set `spring.json.add.type.headers=false` so they don't stamp messages with a class name from their own package; each `@KafkaListener` instead pins `spring.json.value.default.type` to its own local class.

## Message shapes

English field names throughout; ISO-8601 timestamps. These are the wire
formats, as actually implemented as of Phase 4.

### Telemetry (`drone.telemetry` from Phase 3; broadcast on `/topic/telemetry` since Phase 1)
```json
{
  "droneId": "drone-017",
  "timestamp": "2026-01-15T10:32:04.120Z",
  "position": { "lat": 24.7136, "lon": 46.6753 },
  "altitudeM": 85.4,
  "speedMps": 12.3,
  "headingDeg": 271.0,
  "batteryPct": 62.5,
  "missionId": "mission-4",
  "status": "IN_FLIGHT"
}
```
`status` codes: `IN_FLIGHT`, `IDLE`, `LOW_BATTERY`, `LOST_SIGNAL`, `LANDED`.

### Alert (`drone.alerts`, produced by alert-service since Phase 3)
```json
{
  "alertId": 88,
  "droneId": "drone-017",
  "type": "GEOFENCE_BREACH",
  "severity": "HIGH",
  "message": "Drone drone-017 left the permitted operating area",
  "triggeredAt": "2026-01-15T10:32:05.000Z",
  "resolvedAt": null
}
```
`resolvedAt == null` means the alert is still active. `type` codes: `GEOFENCE_BREACH`, `LOW_BATTERY`, `CRITICAL_BATTERY`,
`ALTITUDE_VIOLATION`, `LOST_SIGNAL`.

### Mission event (`mission.events`, produced by mission-service and simulator-service since Phase 4)
```json
{
  "missionId": 4,
  "droneId": "drone-017",
  "type": "ASSIGNED",
  "waypoints": [
    { "lat": 24.71, "lon": 46.67, "altitudeM": 80 },
    { "lat": 24.72, "lon": 46.69, "altitudeM": 80 }
  ],
  "timestamp": "2026-01-15T10:32:04.120Z"
}
```
`type` codes: `ASSIGNED`, `CANCELLED` (published by mission-service, the system of record); `STARTED`, `COMPLETED` (published back by simulator-service, the only party that knows when a drone actually starts/finishes a route). `waypoints` is only populated on `ASSIGNED`. This is a lifecycle event, not the full mission record - `GET /api/missions` on mission-service returns the full `MissionDto` (adds `status`, `createdAt`, `startedAt`, `completedAt`).

## Conventions

- **Package base:** `com.dronefleet.<service>`
- **Config:** Spring `application.properties` (not YAML)
- **Build:** Maven (each service ships its own Maven wrapper — `./mvnw`, no global Maven install required), Java 21, **Spring Boot 4.1.0**.
  > Note: the brief calls for "latest stable Spring Boot 3.x", written before Spring Boot 3.x reached end of life on the public Spring Initializr. As of scaffold time (`start.spring.io` metadata, verified live rather than assumed), only 4.0.x/4.1.x are offered; 4.1.0 is the current stable default. Spring Boot 4 renamed the web starter to `spring-boot-starter-webmvc` (to disambiguate from WebFlux) and introduced per-starter test artifacts (e.g. `spring-boot-starter-webmvc-test`) — both confirmed by generating a throwaway project and running `./mvnw dependency:resolve` against Maven Central before adopting them across all 7 services.
- **Frontend:** Vite + React + TypeScript, Tailwind CSS v4 + **shadcn/ui** (`radix-nova` style, RTL mode enabled via `components.json`), react-leaflet, `@stomp/stompjs` + `sockjs-client`, react-i18next (`ar` bundle), `@fontsource/cairo` (bundled offline, no CDN)
- **Language rule:** all user-facing strings Arabic (RTL); code, API paths, Kafka topic names, JSON field names, and enum codes stay English; frontend maps codes → Arabic labels

### Service ports

| Service | Port |
|---|---|
| gateway | 8080 |
| auth-service | 8081 |
| simulator-service | 8082 |
| telemetry-service | 8083 |
| alert-service | 8084 |
| mission-service | 8085 |
| rag-service | 8086 |
| frontend (dev) | 5173 |

### Kafka topics

- `drone.telemetry` — one message per drone per tick (high volume)
- `drone.alerts` — emitted by the alert engine on a rule violation
- `mission.events` — mission lifecycle: `ASSIGNED`/`CANCELLED` (mission-service) and `STARTED`/`COMPLETED` (simulator-service)
