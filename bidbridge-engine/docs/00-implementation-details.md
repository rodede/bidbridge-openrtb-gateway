# Implementation Details (MVP)

This document captures MVP-level internal behavior that goes beyond the public API contract.
Refer to `bidbridge-engine/docs/01-architecture.md` for component-level context.

---

## Error Handling (MVP)

- Parsing errors → 400
- Validation errors → 400
- Internal timeout → 204 or 503
- Rules-filtered request → 204
- Error responses return a JSON body: `{ "error": "<message>" }`

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

### Required fields

- `id` must be present and non-blank
- `imp` must exist and contain at least one impression
- Exactly one of `site` or `app` must be present

### Impression rules

- `imp.id` must be present and non-blank
- At least one of `banner`, `video`, `audio`, or `native` must be present
- If multiple types are present, select deterministically: `video` > `audio` > `banner` > `native`
- `imp.bidfloor` defaults to `0` if missing

### Context and defaults

- `inventoryType` is derived from `site` vs `app`
- `tmax`:
  - Default to `100ms` if missing
  - Clamp to the range `10–1000ms`

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

- Adapter timeout uses `min(adapter.timeoutMs, request.tmaxMs - reserve)`
- Overall request deadline uses `min(request.tmaxMs, bid.globalTimeoutMs)` when configured
- Timeouts are treated as no-bid for response selection
- If all adapters time out, return 503 (overload)
- If all adapters error, return 503 (adapter failure)
- If no adapters are enabled, return 503 (configuration error)

### Result model

- Store: status, latency, bidder name, selected bid, lightweight debug fields
- Do not store raw responses (log only)

---

## Rules Engine (MVP)

Configured under `rules`:

- `allowInventory`: list of allowed inventory types (SITE/APP)
- `denyInventory`: list of disallowed inventory types
- `minBidfloor`: drop imps below this bidfloor
- `allowAdapters`: allowlist of adapter names
- `denyAdapters`: denylist of adapter names

Behavior:

- If no rules are set, allow all.
- Inventory rules can filter all adapters → no-bid.
- Bidfloor rule can filter all imps → no-bid.
- Applied rules and final adapter set are logged.

Example:

```
rules:
  allowInventory: [SITE]
  minBidfloor: 0.5
  allowAdapters: [simulator]
```

---

## Response Merger (MVP)

- Select the highest-priced valid bid.
- Ignore non-positive prices.
- If no bid remains, return 204.
- If all adapters time out, return 503 (overload).
- If all adapters error, return 503 (adapter failure).

---

## Technical Notes

### Project bootstrap command

```
spring init \
  --type=maven-project \
  --language=java \
  --boot-version=4.0.1 \
  --java-version=25 \
  --groupId=ro.dede \
  --artifactId=bidbridge-engine \
  --name="BidBridge Engine" \
  --package-name=ro.dede.bidbridge.engine \
  --dependencies=webflux,actuator,validation \
  bidbridge-engine
```

### Validation notes

- Validation uses Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, etc.).
- Cross-field checks can use custom assertions (for example: site/app exclusivity).
- Keep validation fail-fast and close to API/normalization boundaries.

### Runtime notes

- Simulator currently exposes `/actuator/prometheus`.
- Engine metrics are present; broader tracing rollout is a later phase.
