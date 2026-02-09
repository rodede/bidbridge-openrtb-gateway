# API Contract

## Endpoint
The gateway is a **bidder endpoint** for SSPs; it does **not** communicate directly with players.

Endpoint: `POST /openrtb2/bid`
Content-Type: `application/json`

For audio/video, creative is typically embedded as **VAST XML in `adm`** and returned to SSP.

---

## Request

- Must comply with OpenRTB 2.6
- Must contain at least one impression
- Supports banner, audio, video, and native

Backward compatibility:
- OpenRTB **2.5 requests are fully accepted**
- 2.5 is treated as a **subset of 2.6**
- Unknown fields are ignored and preserved via `ext`
- Requests MUST NOT be rejected based on minor version

---

## Responses

| Case       | Status | Body             |
|------------|--------|------------------|
| Bid        | 200    | BidResponse JSON |
| No-bid     | 204    | Empty            |
| Bad input  | 400    | Error message    |
| Rate limited | 429  | Error message    |
| Configuration failure | 503 | Error message |
| Internal error | 500 | Error message    |

---

## Headers

- X-OpenRTB-Version: 2.6 (response)
- X-Request-Id (optional request header, echoed in response)
- X-Caller (optional request header, echoed in response)
- X-Api-Key (required only when auth is enabled, e.g. AWS profile)

Rate-limit behavior:
- `429` is returned when `engine.limits.maxInFlight` is exceeded.
- Error payload: `{"error":"Too many requests"}`.

No-bid behavior:
- `204` is used for regular no-fill, timeout no-bid, rules-filtered no-bid, and all-adapters-error no-bid.
- Timeout no-bid is split internally into deadline timeout vs all-adapters-timeout outcomes.
- Distinction is internal-only via logs/metrics (see observability docs).

---

## BidResponse Rules

- id must match request id
- impid must match impression id
- price must be > 0
- adm contains creative or VAST

---

## Error Handling

See `bidbridge-engine/docs/00-implementation-details.md` for error handling behavior, internal normalization rules and defaults.
