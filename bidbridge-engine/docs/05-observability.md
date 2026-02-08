# Observability

## Metrics

### Engine Metrics (with tags)

- `requests_total` (`outcome` = `bid|nobid|error|unknown`)
- `errors_total` (`type`)
- `adapter_timeouts` (`adapter`)
- `adapter_bad_response` (`adapter`)
- `adapter_errors` (`adapter`)
- `engine_rejected_total` (`reason` = `in_flight_limit`) increments when `/openrtb2/**` is rejected with `429`
- `request_latency` (`outcome`; timer with p95/p99)

---

## Tracing

- Request IDs are implemented (`X-Request-Id` in engine and simulator)
- Distributed tracing spans are planned for a later phase

---

## Logging

- Plain text (MVP), request ID prefixed in log pattern
- Request ID propagated from Reactor context into MDC
- Request ID (`X-Request-Id`) for engine and simulator
- Caller (`X-Caller`) for engine and simulator
- Adapter name
- Latency
- Result code

---

## Dashboards

- Traffic overview
- Latency distribution
- Error rates
- Adapter health
