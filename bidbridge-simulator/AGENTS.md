# AI Agent Guidelines - BidBridge Simulator

Root `AGENTS.md` rules apply here. This file only adds simulator-local guidance.

## Context Index (Simulator)

- `bidbridge-simulator/docs/01-architecture.md` — simulator architecture and boundaries
- `bidbridge-simulator/README.md` — API/usage/config/metrics contract

## Module Focus

- Keep simulator implementation minimal and deterministic for testing.
- Prioritize changes that improve `bidbridge-engine` integration testing.
- Avoid simulator-only complexity that does not improve engine validation.

## Documentation Boundaries

- `README.md`: behavior, API, configuration semantics, and local usage.
- `docs/01-architecture.md`: layer responsibilities and component boundaries.
