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

Built incrementally, phase by phase (see brief Section 17 for the full
checklist). **Phases 0–6 are done**: live simulator + map, persistence +
alert rules, Kafka fan-out, missions, a RAG regulatory assistant, and — as of
Phase 6 — JWT auth, an API gateway in front of everything, and a single
`docker compose up` that brings the whole stack up together. Phase 7
(replay, analytics, deconfliction, NL telemetry queries) is optional stretch
and not started.

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
Frontend (React, live map, Arabic RTL)
  --STOMP/WebSocket (JWT on CONNECT)--> Telemetry Service      [Phase 1/3/6]
  --REST (JWT: Bearer token)-------------------> Gateway       [Phase 6]
                                                    |-- Auth Service (issues JWT)
                                                    |-- Alert Service
                                                    |-- Mission Service
                                                    '-- RAG Service (Claude + pgvector)

Simulator --> drone.telemetry (Kafka) --> Telemetry / Alert / Flight-log     [Phase 3]
Mission Service --> mission.events (Kafka)                                   [Phase 4]
RAG Service <--> Postgres/pgvector, Ollama (embeddings), Claude API          [Phase 5]
```

Full Mermaid diagrams: [`docs/architecture.md`](./docs/architecture.md).

## Repo layout

```
Sarb/
├── docker-compose.yml       # Postgres+pgvector, Kafka (KRaft), kafka-ui, and all 7 services + frontend
├── gateway/                 # Spring Cloud Gateway (MVC) - JWT-gated routing to every backend
├── auth-service/            # Hand-rolled JWT auth (register/login, BCrypt, Postgres)
├── simulator-service/       # N virtual drones, tick loop, flies assigned missions
├── telemetry-service/       # Kafka consumer -> STOMP/WebSocket host, flight-log writer
├── alert-service/           # geofence/battery/signal rule engine
├── mission-service/         # mission CRUD + lifecycle events
├── rag-service/             # Spring AI RAG assistant (Claude + Ollama + pgvector)
├── frontend/                # Vite + React + TS, shadcn/ui, Arabic RTL, Leaflet
└── docs/
    └── architecture.md
```

## Stack

- **Backend:** Java 21, Spring Boot 4.1 (Maven, `application.properties`)
- **Gateway:** Spring Cloud Gateway Server MVC (servlet, non-reactive variant)
- **Auth:** hand-rolled JWT (jjwt), BCrypt via Spring Security
- **Real-time:** STOMP over SockJS (`/ws`, broker `/topic`, telemetry on `/topic/telemetry`) - the one thing that bypasses the gateway (see `docs/architecture.md`)
- **Frontend:** Vite + React + TypeScript, Tailwind CSS + shadcn/ui, react-leaflet, react-i18next (`ar` bundle), `@fontsource/cairo`
- **Messaging:** Apache Kafka (KRaft mode, no Zookeeper)
- **Data:** PostgreSQL + pgvector
- **AI:** Spring AI - Claude (Anthropic) chat + Ollama (`bge-m3`) embeddings, `PgVectorStore`
- **Deployment:** one Dockerfile per service, `docker compose up` for the whole stack

## Running it

### Option A — `docker compose up` (everything, one command)

**Prerequisites:** Docker + Docker Compose, and [Ollama](https://ollama.com)
running on the host with the `bge-m3` model pulled (`ollama pull bge-m3`) -
it's the one dependency that isn't dockerized, since it's a local install
rather than one of this project's own services.

```bash
cp .env.example .env   # fill in JWT_SECRET and ANTHROPIC_API_KEY
docker compose up -d --build
docker compose ps
```

Open **http://localhost:5173**, register an account, and the whole system
is live: drones moving on the map, alerts, missions, and the RAG assistant.

### Option B — run services individually (development)

Each service has its own Maven wrapper, a `.env.example` to copy from where
relevant, and a health endpoint on its own port:

| Service | Port | Needs its own `.env`? |
|---|---|---|
| gateway | 8080 | yes - `JWT_SECRET` (must match auth-service's) |
| auth-service | 8081 | yes - `JWT_SECRET` |
| simulator-service | 8082 | no |
| telemetry-service | 8083 | yes - `JWT_SECRET` (checks it on the STOMP CONNECT frame) |
| alert-service | 8084 | no |
| mission-service | 8085 | no |
| rag-service | 8086 | yes - `ANTHROPIC_API_KEY` |
| frontend (dev) | 5173 | no |

```bash
docker compose up -d postgres kafka   # infra only
cd <service> && cp .env.example .env  # where applicable - fill in the real value
cd <service> && ./mvnw spring-boot:run
curl localhost:<port>/actuator/health
```

Frontend:
```bash
cd frontend
npm install
npm run dev
```

## License / disclaimer

Portfolio project. The RAG assistant cites public regulatory documents for
demonstration purposes only — it is not legal advice. Auth is a portfolio
demo of the JWT pattern (no password reset, no refresh tokens, no rate
limiting) - not production-hardened.
