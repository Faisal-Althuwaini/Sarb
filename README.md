# Sarb (سرب)

**Sarb** — Arabic for "flock/swarm" — is a drone fleet operations platform: a
portfolio project simulating the ground-control backbone an operations team
would use to monitor a fleet of drones in real time, assign missions, raise
alerts, and answer regulatory questions with a RAG assistant. There is no real
drone hardware — a simulator service generates realistic telemetry, exactly as
this kind of software is developed and tested in industry.

The UI is **Arabic, RTL-first** (`dir="rtl" lang="ar"`); code, APIs, Kafka
topics, JSON fields, and enum codes stay English, with the frontend mapping
codes to Arabic labels.

See [`drone-fleet-platform-brief.md`](./drone-fleet-platform-brief.md) for the
full design brief (goals, architecture, settled decisions, build phases) and
[`docs/architecture.md`](./docs/architecture.md) for diagrams and conventions.

## Status

Building incrementally, phase by phase (see brief Section 17 for the full
checklist). Currently complete: **Phase 0** (repo skeleton, infra, health
checks) and **Phase 1** (simulator + live map — drones visibly moving in
real time over WebSocket).

## Why Kafka?

The simulator publishes each drone's telemetry to a single Kafka topic
(`drone.telemetry`). Multiple independent consumers — the WebSocket push to
the browser, the alert engine, the flight-log writer — each subscribe to that
one stream and do their own job without knowing about each other. That
fan-out (one producer, many decoupled consumers, each able to fail/retry/scale
independently) is the core architectural story of this project. It arrives in
Phase 3; Phase 1 talks to the browser directly to get the visible demo working
fast.

## Architecture at a glance

```
Simulator (N virtual drones, tick loop)
   --STOMP/WebSocket--> Frontend (React, live map)        [Phase 1, done]

Simulator --> drone.telemetry (Kafka) --> Telemetry / Alert / Flight-log      [Phase 3]
Mission Service --> mission.events (Kafka)                                    [Phase 4]
RAG Service <--> Postgres/pgvector, Claude API                                [Phase 5]
```

Full Mermaid diagrams: [`docs/architecture.md`](./docs/architecture.md).

## Repo layout

```
Sarb/
├── docker-compose.yml       # infra only: Postgres+pgvector, Kafka (KRaft), kafka-ui
├── gateway/                 # API gateway (stub — Phase 6)
├── auth-service/            # JWT auth (stub — Phase 6)
├── simulator-service/       # REAL: N virtual drones, tick loop, STOMP/WebSocket (Phase 1)
├── telemetry-service/       # stub — split out from simulator at Phase 3
├── alert-service/           # stub — geofence/battery/signal rules at Phase 2/3
├── mission-service/         # stub — missions at Phase 4
├── rag-service/             # stub — Spring AI RAG assistant at Phase 5
├── frontend/                # Vite + React + TS, shadcn/ui, Arabic RTL, Leaflet
└── docs/
    └── architecture.md
```

## Stack

- **Backend:** Java 21, Spring Boot 4.1 (Maven, `application.properties`)
- **Real-time:** STOMP over SockJS (`/ws`, broker `/topic`, telemetry on `/topic/telemetry`)
- **Frontend:** Vite + React + TypeScript, Tailwind CSS + shadcn/ui, react-leaflet, react-i18next (`ar` bundle), `@fontsource/cairo`
- **Messaging (from Phase 3):** Apache Kafka (KRaft mode, no Zookeeper)
- **Data (from Phase 2):** PostgreSQL + pgvector
- **AI (from Phase 5):** Spring AI — Claude (Anthropic) chat + OpenAI embeddings, `PgVectorStore`

## Running it

### Prerequisites
- Java 21 (each service ships its own Maven wrapper — no local Maven install needed)
- Node.js 20+ and npm
- Docker + Docker Compose (for Postgres/Kafka; not required just to see the live map)

### 1. Infra (optional for Phase 1 — the simulator needs no database or Kafka yet)
```bash
docker compose up -d
docker compose ps
```

### 2. Simulator (the hook — drones moving on the map)
```bash
cd simulator-service
./mvnw spring-boot:run
```
Health check: `curl localhost:8082/actuator/health` → `{"status":"UP", ...}`

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173** — the map centers on Riyadh and drones should
appear moving within a few seconds as telemetry streams in over STOMP/WebSocket.

### 4. The other stub services (optional at this phase)
Each has its own Maven wrapper and a health endpoint on its own port:

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

```bash
cd <service>
./mvnw spring-boot:run
curl localhost:<port>/actuator/health
```

## License / disclaimer

Portfolio project. The RAG assistant (Phase 5) cites public regulatory
documents for demonstration purposes only — it is not legal advice.
