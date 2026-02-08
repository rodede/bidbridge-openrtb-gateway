# BidBridge Engine

Core OpenRTB bidder gateway service for BidBridge.

## Scope

- Exposes `POST /openrtb2/bid`
- Normalizes and validates OpenRTB requests
- Applies MVP rules
- Executes enabled adapters
- Merges adapter responses into OpenRTB bid/no-bid responses

## Run Locally

From repository root:

```bash
mvn -pl bidbridge-engine spring-boot:run
```

Default port: `8080`

## Quick Request

From repository root:

```bash
curl -i -X POST "http://localhost:8080/openrtb2/bid" \
  -H "Content-Type: application/json" \
  --data-binary @bidbridge-loadgen/src/main/resources/sample-request.json
```

Expected outcomes:
- `200` with bid response
- `204` for no-bid
- `400` for invalid input
- `429` when in-flight request limit is exceeded
- `503` for overload/adapter/config failures

## Configuration

Main config file:
- `bidbridge-engine/src/main/resources/application.yml`

Key sections:
- `engine.auth.*` (inbound API key auth; enabled in `aws` profile)
- `engine.limits.maxInFlight` (concurrent in-flight cap on `/openrtb2/**`)
- `adapters.configs.*`
- `rules.*`
- `bid.globalTimeoutMs`

## Tests

From repository root:

```bash
mvn -pl bidbridge-engine test
```

## Engine Docs

- `bidbridge-engine/docs/00-implementation-details.md`
- `bidbridge-engine/docs/01-architecture.md`
- `bidbridge-engine/docs/02-api-contract.md`
- `bidbridge-engine/docs/03-adapters.md`
- `bidbridge-engine/docs/04-rules-engine.md`
- `bidbridge-engine/docs/05-observability.md`
- `bidbridge-engine/docs/06-testing.md`
- `bidbridge-engine/docs/07-performance-security.md`
