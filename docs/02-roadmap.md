# Development Roadmap

## Current Project State (February 2026)

Roadmap execution has stopped and this project is in archive/maintenance mode.

Current snapshot:
- `bidbridge-simulator`: deployed in AWS, no active usage, retained as sample/reference.
- `bidbridge-loadgen`: working and usable.
- `bidbridge-engine`: minimal structure only, not deployed.

## Phase 1 — MVP

- HTTP endpoint
- Parsing and validation
- Simulator adapter
- Fixed bid responses

---

## Phase 2 — Core Platform

- Adapter framework
- HTTP adapter base + first real bidder integration
- Response merger
- Metrics
- Health checks

---

## Phase 3 — Control Layer

- Rules engine
- Config API
- A/B routing

---

## Phase 4 — Production Readiness

- Tracing
- Load testing (SSP simulator tooling: client + replay + QPS control)
- Performance + security hardening (limits, rate limiting, validation, Netty tuning)
- Documentation + CI (README polish, milestone readiness, badges)

---

## Phase 5 — Advanced Features

- Dynamic rules
- Traffic replay
- Creative optimization
- Admin UI
