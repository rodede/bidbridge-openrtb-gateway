# Observability

## Metrics

### Core Metrics

- requests_total
- bids_total
- nobids_total
- errors_total
- adapter_timeouts
- adapter_bad_response
- adapter_errors
- request_latency (timer with p95/p99)

### Tags

 - requests_total: status
 - errors_total: type
 - adapter_timeouts: adapter
 - adapter_bad_response: adapter
 - adapter_errors: adapter
---

## Tracing

- One trace per request
- Spans per adapter
- Correlation IDs

---

## Logging

- Plain text (MVP), correlation ID prefixed in log pattern
- Correlation ID propagated from Reactor context into MDC
- Request ID (OpenRTB id)
- Adapter name
- Latency
- Result code

---

## Dashboards

- Traffic overview
- Latency distribution
- Error rates
- Adapter health
