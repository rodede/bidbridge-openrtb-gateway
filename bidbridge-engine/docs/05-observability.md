# Observability

## Metrics

### Engine Metrics

- requests_total
- bids_total
- nobids_total
- errors_total
- adapter_timeouts
- adapter_bad_response
- adapter_errors
- request_latency (timer with p95/p99)

### Engine Tags

 - requests_total: status
 - errors_total: type
 - adapter_timeouts: adapter
 - adapter_bad_response: adapter
 - adapter_errors: adapter

### Simulator Metrics

- sim_requests_total{outcome=bid|nobid|error}
- sim_latency_ms (Micrometer timer; Prometheus exports as `sim_latency_ms_seconds_*`)
- sim_reload_success_total
- sim_reload_fail_total
- sim_active_dsps
- sim_rejected_total{reason=in_flight_limit}
---

## Tracing

- Correlation IDs are implemented (`X-Correlation-Id` in engine, `X-Request-Id` in simulator)
- Distributed tracing spans are planned for a later phase

---

## Logging

- Plain text (MVP), correlation ID prefixed in log pattern
- Correlation ID propagated from Reactor context into MDC
- Correlation ID (`X-Correlation-Id`) for engine
- Request ID (`X-Request-Id`) and caller (`X-Caller`) for simulator
- Adapter name
- Latency
- Result code

---

## Dashboards

- Traffic overview
- Latency distribution
- Error rates
- Adapter health
