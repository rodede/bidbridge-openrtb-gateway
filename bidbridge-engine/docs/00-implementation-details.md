# Implementation Details (MVP)

This document captures MVP-level internal behavior that goes beyond the public API contract.
Refer to `bidbridge-engine/docs/01-architecture.md` for component-level context.

---

## Error Handling (MVP)

Public HTTP status/error contract is defined in `bidbridge-engine/README.md`.
Internal no-bid outcome keys used in logs/metrics:

- `nobid_timeout_deadline`
- `nobid_filtered`
- `nobid_timeout_adapters`
- `nobid_adapter_failure`
- `nobid_no_fill`

---

## WebFilter Order (MVP)

Current filter execution order for inbound HTTP requests:

1. `EngineAuthFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE + 5)`)
    - Active only on `aws` profile when `engine.auth.enabled=true`
    - Applies to `/openrtb2/**`
    - Rejects with `401` on missing/invalid `X-Api-Key`
2. `InFlightLimitFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE + 10)`)
    - Applies to `/openrtb2/**`
    - Rejects with `429` when `engine.limits.maxInFlight` is exceeded
    - Increments `engine_rejected_total{reason="in_flight_limit"}`
3. `RequestLoggingFilter` (no explicit `@Order`)
    - Uses default Spring ordering (runs after explicitly ordered filters)
    - Adds/echoes `X-Request-Id`, echoes `X-Caller`, records request metrics/logs

---

## Normalization

The gateway normalizes incoming OpenRTB requests into a 2.6-first internal model.
Validation-required request fields are documented in `bidbridge-engine/README.md`.

### Impression rules

- If multiple types are present, select deterministically: `video` > `audio` > `banner` > `native`
- `imp.bidfloor` defaults to `0` if missing

### Context and defaults

- `inventoryType` is derived from `site` vs `app`
- `tmax`:
    - Default to `100ms` if missing
    - Clamp to the range `10–60000ms`

### Pass-through (lossless for partner-specific data)

- Preserve `ext` on:
    - top-level request
    - `imp`
    - `site` / `app`
    - `device`
    - `user`
    - `regs`

### Device fields kept

- `ua`, `ip`, `os`, `devicetype` (plus `device.ext`)

---

## Adapter Framework (MVP)

### Configuration

Configured under `adapters.configs.<adapterName>`:

- `enabled` (boolean)
- `endpoint` (string, optional)
- `timeoutMs` (integer, optional)
- `bidProbability` (double, default `1.0`)
- `fixedPrice` (double, optional)
- `admTemplate` (string, optional)

### HTTP adapters

- HTTP-based adapters use the `HttpBidderClient` abstraction (WebClient implementation by default).
- 204 responses map to no-bid.
- Non-2xx responses from bidders are treated as bad responses and mapped to adapter errors.

### Timeouts and budget

- Request deadline uses `request.tmaxMs` unless `bid.globalTimeoutMs > 0`, then
  `min(request.tmaxMs, bid.globalTimeoutMs)`
- Adapter timeout uses `min(adapter.timeoutMs, requestDeadlineMs - reserve)` (reserve is 10ms for merge/response)
- Timeouts are treated as no-bid for response selection
- If all adapters time out, return 204 (no-bid)
- If all adapters error, return 204 (no-bid)
- If no adapters are enabled, return 503 (configuration error)

### Result model

- Store: status, latency, bidder name, selected bid, lightweight debug fields
- Do not store raw responses (log only)

---

## Rules Engine (MVP)

Concrete `rules.*` keys are configured in runtime config.
Architecture-level rule behavior (families, order, and boundaries) is documented in
`bidbridge-engine/docs/01-architecture.md`.
Implementation detail: applied rules and final adapter set are logged per request.

---

## Response Merger (MVP)

Architecture-level merge behavior is documented in `bidbridge-engine/docs/01-architecture.md`.
Implementation detail: merger consumes lightweight adapter result objects (no raw payload retention).

---

## Technical Notes

### Validation notes

- Validation uses Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, etc.).
- Cross-field checks can use custom assertions (for example: site/app exclusivity).
- Keep validation fail-fast and close to API/normalization boundaries.

### Runtime notes

- Simulator currently exposes `/actuator/prometheus`.
- Engine metrics are present; broader tracing rollout is a later phase.
