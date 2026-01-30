# Implementation Details (MVP)

This document captures MVP-level internal behavior that goes beyond the public API contract.
Refer to `docs/02-architecture.md` for component-level context.

---

## Error Handling (MVP)

- Parsing errors → 400
- Validation errors → 400
- Internal timeout → 204 or 503
- Rules-filtered request → 204
- Error responses return a JSON body: `{ "error": "<message>" }`

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
- Ignore bids with missing id/impid, non-positive prices, or impid not in request.
- Apply currency policy:
  - If currency is missing, use `response.defaultCurrency` (defaults to USD).
  - If `response.allowedCurrencies` is set, drop bids with other currencies.
- If no bid remains, return 204.
- If all adapters time out, return 503 (overload).
- If all adapters error, return 503 (adapter failure).
