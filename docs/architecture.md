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

## Current shape (Phase 6 — gateway, auth, and full `docker compose up`)

`simulator-service` is a Kafka producer of `drone.telemetry` (one message per drone per tick, keyed by `droneId`) and, since Phase 4, also a consumer/producer on `mission.events`. `telemetry-service` and `alert-service` are independent consumers of `drone.telemetry` - neither knows the other exists. `alert-service` produces `drone.alerts`, which `telemetry-service` also consumes and relays onto the WebSocket the frontend already holds. `mission-service` owns mission CRUD (Postgres) and publishes `ASSIGNED`/`CANCELLED` lifecycle events; `simulator-service` is the only party that knows when a drone actually starts/finishes flying a route, so it publishes `STARTED`/`COMPLETED` back onto the same topic, and `mission-service` consumes its own topic to keep mission rows in sync. `telemetry-service` also relays `mission.events` onto the WebSocket for live mission status in the UI. `rag-service` sits outside the Kafka fan-out entirely - it's a plain REST service (no topic involvement) that answers regulation/SOP questions via retrieval-augmented generation against pgvector.

Since Phase 6, `auth-service` and a real API `gateway` sit in front of everything. `auth-service` is a small hand-rolled JWT issuer - `POST /api/auth/register`/`login`, BCrypt password hashes in its own Postgres schema, HS512 tokens signed with a shared secret (jjwt). The `gateway` is built on **Spring Cloud Gateway Server MVC** (the servlet, non-reactive variant - the rest of this gateway is plain WebMVC, and this is the one that's actually compatible with that stack): every REST route the frontend calls (`/api/auth/**`, `/api/alerts/**`, `/api/missions/**`, `/api/assistant/**`) is now proxied through it, and every route except `/api/auth/**` is gated by a JWT-checking `HandlerFilterFunction` that rejects with 401 before the request ever reaches a backend. **One deliberate exception**: `telemetry-service`'s WebSocket/STOMP endpoint is *not* routed through the gateway - Spring Cloud Gateway's MVC variant doesn't support WebSocket proxying (only the WebFlux gateway does), so the frontend still connects to `telemetry-service` directly, and the JWT is instead checked on the STOMP CONNECT frame by a `ChannelInterceptor`. `telemetry-service` therefore needs its own copy of the JWT secret and verification logic, same as `auth-service` and `gateway`.

```mermaid
flowchart LR
  FE["Frontend<br/>shadcn/ui · Arabic RTL · Leaflet"]
  GW["Gateway<br/>Spring Cloud Gateway (MVC) · JWT filter"]
  AUTH["Auth Service<br/>register/login · issues JWT"]
  SIM["Simulator Service<br/>tick loop · flies assigned routes"]
  T1(["drone.telemetry"])
  T2(["drone.alerts"])
  T3(["mission.events"])
  TEL["Telemetry Service<br/>consumes all 3 topics · STOMP host · checks JWT on CONNECT"]
  ALERT["Alert Service<br/>consumes drone.telemetry · rule engine · produces drone.alerts"]
  MIS["Mission Service<br/>mission CRUD · produces ASSIGNED/CANCELLED"]
  RAG["RAG Service<br/>Spring AI · Claude + Ollama + pgvector"]
  PG[("PostgreSQL")]
  OLLAMA["Ollama (host)<br/>bge-m3 embeddings"]
  CLAUDE["Claude API"]

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
  FE -->|"STOMP /topic/telemetry, /topic/alerts, /topic/missions (JWT on CONNECT)"| TEL
  FE -->|"REST, JWT: Bearer token"| GW
  GW -->|"/api/auth/** (public)"| AUTH
  GW -->|"/api/alerts/**"| ALERT
  GW -->|"/api/missions/**"| MIS
  GW -->|"/api/assistant/**"| RAG
  RAG -->|embed query + chunks| OLLAMA
  RAG -->|chat completion| CLAUDE
  RAG -->|"similarity search · rag.vector_store"| PG
  TEL -->|flight_logs| PG
  ALERT -->|alerts| PG
  MIS -->|missions| PG
  AUTH -->|users| PG
```

> Note: `simulator-service`, `telemetry-service`, `alert-service`, and `mission-service` each carry their own copy of the wire-shape classes they need (`TelemetryFrame`/`Position`/`DroneStatus`, `AlertDto`/`AlertType`/`AlertSeverity`, `MissionEventDto`/`WaypointDto`/`MissionEventType`) rather than sharing a library module - each service stays independently deployable, at the cost of manually keeping the mirrored shapes in sync. Kafka JSON (de)serialization: producers set `spring.json.add.type.headers=false` so they don't stamp messages with a class name from their own package; each `@KafkaListener` instead pins `spring.json.value.default.type` to its own local class. `rag-service` doesn't participate in this at all - no Kafka dependency, no mirrored DTOs, just REST in and REST out. The same mirroring tradeoff now also covers JWT verification: `gateway` and `telemetry-service` each carry their own copy of the (small) JWT-verification logic rather than a shared library, matching the pattern.
>
> Only the `frontend` and `gateway` (and, only for the WebSocket exception, `telemetry-service`) are ever reachable from a browser. CORS is therefore only configured on the `gateway` (and on `telemetry-service`'s WebSocket handshake) - enabling it on the proxied backend services too caused a duplicated `Access-Control-Allow-Origin` response header that curl ignores but browsers reject outright.
>
> `docker compose up` now brings the entire stack up together: Postgres, Kafka(+kafka-ui), all 7 backend services, and the frontend (built and served via nginx). Each backend service has an `application-docker.properties` profile, activated by `SPRING_PROFILES_ACTIVE=docker`, that swaps `localhost` ports for container-network hostnames (`postgres:5432`, `kafka:29092`, service names for the gateway's routes). Ollama is the one dependency that stays outside docker-compose entirely (a host-level `brew install`, not one of this project's own services) - the dockerized `rag-service` reaches it via Docker Desktop's `host.docker.internal`.

## Message shapes

English field names throughout; ISO-8601 timestamps. These are the wire
formats, as actually implemented as of Phase 5.

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

### Auth (`POST /api/auth/register`, `POST /api/auth/login` on auth-service, via the gateway)
Request (both endpoints):
```json
{ "username": "operator1", "password": "drone-pass-123" }
```
Response:
```json
{ "token": "eyJhbGciOiJIUzUxMiJ9...", "username": "operator1", "role": "OPERATOR" }
```
`role` codes: `ADMIN`, `OPERATOR` (every registered user gets `OPERATOR`; nothing branches on role yet - the enum exists for future authorization rules). The frontend stores `token` and sends it as `Authorization: Bearer <token>` on every other REST call through the gateway, and as a `connectHeaders` entry on the direct STOMP connection to telemetry-service.

### Assistant Q&A (`POST /api/assistant/ask` on rag-service, reached via the gateway - REST only, no Kafka topic)
Request:
```json
{ "question": "ما هو الحد الأقصى للارتفاع المسموح به لطيران الطائرة المسيرة؟" }
```
Response:
```json
{
  "answer": "الحد الأقصى للارتفاع...",
  "citations": [
    { "source": "gacar-part-107.pdf", "sourceType": "regulation", "page": 18, "score": 0.56, "excerpt": "..." },
    { "source": "sop-ops-001-flight-operations.txt", "sourceType": "sop", "page": null, "score": 0.69, "excerpt": "..." }
  ]
}
```
`sourceType` codes: `regulation` (GACAR Part 107 PDF, English source text), `sop` (the two self-written Arabic SOP docs). `page` is only populated for PDF-sourced chunks. The corpus is genuinely bilingual - GACAR Part 107 is only published in English by GACA, so retrieval is cross-lingual (Arabic question → `bge-m3` multilingual embeddings → matches against both English and Arabic chunks) and the answer is always generated in Arabic regardless of which language the matched chunk was in, citing the exact source section.

## Conventions

- **Package base:** `com.dronefleet.<service>`
- **Config:** Spring `application.properties` (not YAML)
- **Build:** Maven (each service ships its own Maven wrapper — `./mvnw`, no global Maven install required), Java 21, **Spring Boot 4.1.0**.
  > Note: the brief calls for "latest stable Spring Boot 3.x", written before Spring Boot 3.x reached end of life on the public Spring Initializr. As of scaffold time (`start.spring.io` metadata, verified live rather than assumed), only 4.0.x/4.1.x are offered; 4.1.0 is the current stable default. Spring Boot 4 renamed the web starter to `spring-boot-starter-webmvc` (to disambiguate from WebFlux) and introduced per-starter test artifacts (e.g. `spring-boot-starter-webmvc-test`) — both confirmed by generating a throwaway project and running `./mvnw dependency:resolve` against Maven Central before adopting them across all 7 services.
- **Frontend:** Vite + React + TypeScript, Tailwind CSS v4 + **shadcn/ui** (`radix-nova` style, RTL mode enabled via `components.json`), react-leaflet, `@stomp/stompjs` + `sockjs-client`, react-i18next (`ar` bundle), `@fontsource/cairo` (bundled offline, no CDN)
- **Language rule:** all user-facing strings Arabic (RTL); code, API paths, Kafka topic names, JSON field names, and enum codes stay English; frontend maps codes → Arabic labels
- **Gateway (Phase 6):** Spring Cloud Gateway Server MVC (`spring-cloud-starter-gateway-server-webmvc`, Spring Cloud 2025.1.2 - verified compatible with Boot 4.1.0 via a throwaway `start.spring.io` project + `dependency:resolve` before adopting, same as the Boot-version check above), not the reactive/WebFlux gateway - the rest of this service is plain WebMVC, and the WebFlux gateway would've pulled in a whole separate reactive stack for no benefit.
- **Deployment:** one multi-stage `Dockerfile` per Java service (`eclipse-temurin:21-jdk` build → `eclipse-temurin:21-jre` runtime) and one for the frontend (`node:22-alpine` build → `nginx:alpine` serve); `docker-compose.yml` wires all 7 backend services + frontend + Postgres + Kafka together, `docker compose up` brings the whole stack up at once.

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

> `rag-service` is not on Kafka at all - it's a self-contained REST service (`POST /api/assistant/ask`, reached via the gateway since Phase 6) with its own local dependency, **Ollama** (`brew install ollama`, model `bge-m3`, `localhost:11434` on the host / `host.docker.internal:11434` when dockerized), used only for embeddings. Chat completion goes to the real Claude API. Both are configured in `rag-service/application.properties`; the Anthropic key is loaded from a gitignored `rag-service/.env` (see `.env.example`) via a small first-party `EnvironmentPostProcessor` when run via `mvnw`, or from the root `.env` via `docker compose` when dockerized.

### Auth / JWT

- `auth-service` and `gateway` (and `telemetry-service`, for the WebSocket exception) all need the **same** `JWT_SECRET` value - `auth-service` signs, the others only verify. Each holds it in its own gitignored `.env` (see each service's `.env.example`) for local `mvnw spring-boot:run`, or via the root `.env`'s `${JWT_SECRET}` for `docker compose`.
- Tokens are HS512, 24h expiry, carrying `sub` (username) and a `role` claim (`ADMIN`/`OPERATOR`) - see the Auth message shape above.
- Every gateway route except `/api/auth/**` requires `Authorization: Bearer <token>`; the WebSocket endpoint checks the same token on the STOMP CONNECT frame instead (see "Current shape" above for why it isn't proxied through the gateway).
