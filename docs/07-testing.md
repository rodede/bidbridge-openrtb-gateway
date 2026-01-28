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

- wrk
- k6
- Replay tools

---

## Chaos Testing

- Inject timeouts
- Kill adapters
- Network delay simulation
