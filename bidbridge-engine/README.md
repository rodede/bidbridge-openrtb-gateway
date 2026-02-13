# BidBridge Engine

Core OpenRTB bidder gateway service for BidBridge application.

## Scope

- Exposes `POST /openrtb2/bid`
- Normalizes and validates OpenRTB requests
- Applies MVP rules
- Executes enabled adapters
- Merges adapter responses into OpenRTB bid/no-bid responses

### Run Locally

```bash
mvn -pl bidbridge-engine spring-boot:run
```

### Quick Request

```bash
curl -i -X POST "http://localhost:8080/openrtb2/bid" \
  -H "Content-Type: application/json" \
  --data-raw '{"id":"req-1","imp":[{"id":"1","banner":{},"bidfloor":0.5}],"site":{},"tmax":10000}'
```

---

## API Contract

### Endpoint

The gateway is a bidder endpoint for SSPs and does not communicate directly with players.

- Endpoint: `POST /openrtb2/bid`
- Content-Type: `application/json`
- For audio/video, creative is typically embedded as VAST XML in `adm` and returned to SSP.

### Request

- Must comply with OpenRTB 2.6
- Must contain at least one impression
- Supports banner, audio, video, and native

Backward compatibility:

- OpenRTB 2.5 requests are accepted
- 2.5 is treated as a subset of 2.6
- Unknown fields are ignored and preserved via `ext`
- Requests are not rejected based on minor version

### Responses

| Case                  | Status | Body             |
|-----------------------|--------|------------------|
| Bid                   | 200    | BidResponse JSON |
| No-bid                | 204    | Empty            |
| Bad input             | 400    | Error message    |
| Rate limited          | 429    | Error message    |
| Configuration failure | 503    | Error message    |
| Internal error        | 500    | Error message    |

### Headers

- `X-OpenRTB-Version: 2.6` (response)
- `X-Request-Id` (optional request header, echoed in response)
- `X-Caller` (optional request header, echoed in response)
- `X-Api-Key` (required only when auth is enabled, for example in AWS profile)

Rate-limit behavior:

- `429` is returned when `engine.limits.maxInFlight` is exceeded.
- Error payload: `{"error":"Too many requests"}`.

No-bid behavior:

- `204` is used for regular no-fill, timeout no-bid, rules-filtered no-bid, and all-adapters-error no-bid.
- Timeout no-bid is split internally into deadline timeout vs all-adapters-timeout outcomes.
- Distinction is internal-only via logs and metrics (see observability docs).

### BidResponse Rules

- `id` must match request id
- `impid` must match impression id
- `price` must be `> 0`
- `adm` contains creative or VAST

## Configuration

Main config file: `bidbridge-engine/src/main/resources/application.yml`

Implementation/default behavior details are defined in: `bidbridge-engine/docs/00-implementation-details.md`

## Tests

```bash
mvn -pl bidbridge-engine test
```

## Engine Docs

- `bidbridge-engine/docs/00-implementation-details.md`
- `bidbridge-engine/docs/01-architecture.md`
- `bidbridge-engine/docs/03-adapters.md`
- `bidbridge-engine/docs/04-rules-engine.md`
- `bidbridge-engine/docs/05-observability.md`
- `bidbridge-engine/docs/06-testing.md`
- `bidbridge-engine/docs/07-performance-security.md`
