# Testing Strategy

## Unit Tests

- Request parsing
- Validation
- Rules evaluation
- Response building

---

## Integration Tests

- SSP simulator
- Adapter mocks
- End-to-end flow
- Integration-style tests live under `ro.dede.bidbridge.engine.it` and use the `*IT` naming convention

---

## Load Testing

- `bidbridge-loadgen` module (QPS control + JSON/JSONL replay)
- wrk / k6 (optional external tooling)
- Replay files for deterministic request sets

---

## Chaos Testing

- Inject timeouts
- Kill adapters
- Network delay simulation
