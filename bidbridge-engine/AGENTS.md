# AI Agent Guidelines - BidBridge Engine

Root `AGENTS.md` rules apply here. This file adds engine-specific constraints.

## Context Index (Engine)

- `bidbridge-engine/docs/00-implementation-details.md` — MVP behavior and defaults
- `bidbridge-engine/docs/01-architecture.md` — architecture and boundaries
- `bidbridge-engine/docs/02-api-contract.md` — endpoint contract
- `bidbridge-engine/docs/03-adapters.md` — adapter behavior
- `bidbridge-engine/docs/04-rules-engine.md` — implemented rules
- `bidbridge-engine/docs/05-observability.md` — metrics/logging conventions
- `bidbridge-engine/docs/06-testing.md` — testing strategy
- `bidbridge-engine/docs/07-performance-security.md` — performance/security constraints
- `bidbridge-engine/README.md` — module run/test quickstart

## Engine Rules

- Fail fast on invalid OpenRTB input.
- Validate inbound requests early and keep normalization deterministic.
- Keep clear boundaries: `api` / `normalization` / `rules` / `adapters` / `service` / `observability`.
- Use reactive/non-blocking flow only; do not introduce blocking calls.
- Preserve current timeout budget model and adapter error mapping semantics unless explicitly changing spec/docs.

## Implementation Plan Requirement

Before code changes, provide a short plan that includes:
- 3-6 steps
- risks/assumptions
- validation commands

## Validation (Minimum)

Run after engine changes:

```bash
mvn -pl bidbridge-engine test
```

Add/adjust tests with behavior changes.

## Docs Sync

When engine behavior/config/metrics changes, update:
- relevant files in `bidbridge-engine/docs/`
- `bidbridge-engine/README.md` when run/config usage changes
