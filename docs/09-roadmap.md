# Development Roadmap

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

---

## Tasks examples

### Golden prompt
```
Use codex.md to select context.
Follow AGENTS.md rules.
Implement MVP only.
```

### Task list
#### OpenRTB API MVP: 
endpoint + request parsing/validation with 2.6/2.5 compatibility and basic error handling
#### Normalization layer: 
map OpenRTB request into internal model (2.6-first), fail-fast validations
#### Adapter framework: 
define interfaces, adapter registry, mock/simulator adapter, timeout handling
####  Response merger: 
collect adapter bids, select winner, build OpenRTB response
####  Rules engine MVP: 
routing and simple business rules (floors/filters), config loading
#### Observability: 
metrics/tracing/logging integration per docs
#### Adapter HTTP integration:
base HTTP adapter + stub bidder + request/response mapping hooks
#### Response policy:
currency handling + bid validation + defaults
#### Budgeting/timeout strategy:
tmax split, per-adapter timeout, overall deadline
####  SSP simulator tooling: 
client + replay + QPS control for integration/load testing
#### Performance + security hardening:
limits, rate limiting, validation, Netty tuning
####  Docs/README polish + milestone readiness; CI setup + badges

---
