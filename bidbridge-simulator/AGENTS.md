# AI Agent Guidelines - BidBridge Simulator

Root `AGENTS.md` rules apply here. This file only adds simulator-local guidance.

## Module Focus

- Keep simulator implementation minimal and deterministic for testing.
- Prioritize changes that improve `bidbridge-engine` integration testing.
- Avoid simulator-only complexity that does not improve engine validation.

## Documentation Boundaries

- `bidbridge-simulator/README.md`: behavior, API, configuration semantics, and local usage.
- `AWS_Architecture.md`: account-specific infrastructure and deployment runbook.
