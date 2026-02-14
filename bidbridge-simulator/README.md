# BidBridge Simulator

Minimal OpenRTB bidder simulator used for local and integration testing.

## Documentation boundaries

This file documents simulator behavior and usage:
- API endpoints, config semantics, metrics, and local run commands.
- Keep this file cloud-agnostic.

Architecture and layer responsibilities live in:
- `bidbridge-simulator/docs/01-architecture.md`
- `bidbridge-simulator/docs/architecture-class-diagram.puml`

## Purpose

- Support `bidbridge-engine` integration and behavior testing.
- Act as a controllable fake DSP endpoint for experiments.
- Stay intentionally simple and low-cost to operate.
- Accepts OpenRTB `POST /openrtb2/{dsp}/bid`
- Returns a fixed bid or no-bid based on simple config

## Run locally

From repo root:

```bash
mvn -pl bidbridge-simulator spring-boot:run
```

1. Run with Docker (from repo root):

```bash
docker build -t bidbridge-simulator:local -f bidbridge-simulator/Dockerfile .
docker run --rm -p 8081:8081 --name "bidbridge-simulator" -v "$PWD/dsps.yml:/simulator/dsps.yml:ro" -e DSPS_FILE=/simulator/dsps.yml bidbridge-simulator:local
```

2. Override container port (default `8081`):

```bash
docker run --rm -p 8085:8085 --name "bidbridge-simulator" -v "$PWD/dsps.yml:/simulator/dsps.yml:ro" -e DSPS_FILE=/simulator/dsps.yml -e SERVER_PORT=8085 bidbridge-simulator:local
```

3. Remove container and image:

```bash
docker rm -f bidbridge-simulator && docker rmi bidbridge-simulator:local
```

## API contract

### Bid endpoint

- `POST /openrtb2/{dsp}/bid`
- Request: OpenRTB-like JSON with required fields:
  - `id`
  - `imp` (non-empty)
  - each impression contains `imp.id`
- Response statuses:
  - `200` bid response JSON
  - `204` no-bid
  - `400` bad request or unknown dsp
  - `401` when auth is enabled and token is invalid/missing
  - `429` when in-flight limit is exceeded
  - `500` unexpected internal error
- Response header: `X-OpenRTB-Version: 2.6`

### Admin endpoints

- `POST /admin/reload-dsps` — triggers config reload and returns `{status, loadedCount, versionTimestamp, message}`
- `GET /admin/dsps` — returns active config snapshot `{loadedCount, versionTimestamp, versionTime, configs}`

## Configuration

`bidbridge-simulator/src/main/resources/application.yml` points to an external `dsps.yml`.
By default it looks for `dsps.yml` at the repo root.

Canonical format: place DSP names at the top level.

`dsps.file` supports local paths. Cloud/object-storage configuration is documented in environment-specific runbooks.

Profiles:

- `local` → `application-local.yml` (default)

```bash
SPRING_PROFILES_ACTIVE=local
mvn -pl bidbridge-simulator spring-boot:run
```

```yaml
simulator:
  enabled: true
  bidProbability: 1.0
  fixedPrice: 1.5
  currency: "USD"
  admTemplate: "<vast/>"
  responseDelayMs: 0
```

Config notes:

- `<dsp>.enabled`: toggles the dsp endpoint on/off.
- `<dsp>.bidProbability`: probability (0.0–1.0) of returning a bid vs 204 no-bid.
- `<dsp>.fixedPrice`: bid price returned when a bid is produced.
- `simulator.maxInFlight`: max concurrent in-flight bid requests under `/openrtb2/**` (429 when exceeded).
- `<dsp>.currency`: value used for the `cur` field in the response.
- `<dsp>.admTemplate`: string inserted into `adm` (often VAST XML).
- `<dsp>.responseDelayMs`: artificial delay (in ms) before responding, to simulate bidder latency.
- `simulator.auth.enabled`: enables auth filter for bid/admin routes.
- `simulator.auth.bidApiKey`: expected `X-Api-Key` for `/openrtb2/**` when auth is enabled.
- `simulator.auth.adminApiToken`: expected `X-Admin-Token` for `/admin/**` when auth is enabled.

Polling:
- `dsps.pollIntervalMs`: reload interval in milliseconds (default 2000).

## Logging

One-line request summary logs (plain text). Fields:

- `requestId` (from `X-Request-Id`, generated if missing)
- `caller` (from `X-Caller`)
- `path`
- `status`
- `dspId`
- `latencyMs` (simulated delay)
- `durationMs` (total request time)
- `errorType` / `errorMessage` (only for 4xx/5xx)

Headers:

- `X-Request-Id` (echoed back on response)
- `X-Caller` (optional)

## Metrics

Observability endpoints:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

Emitted metrics:

- `sim_requests_total{outcome=bid|nobid|error}` — total request outcomes derived from response status (`200=bid`, `204=nobid`, other statuses=`error`).
- `sim_latency_ms` — Micrometer timer name (exported in Prometheus as `sim_latency_ms_seconds_count`, `sim_latency_ms_seconds_sum`, and `sim_latency_ms_seconds_bucket` when histogram buckets are enabled).
- `sim_reload_success_total` — cumulative number of successful `dsps.yml` reloads.
- `sim_reload_fail_total` — cumulative number of failed `dsps.yml` reload attempts.
- `sim_active_dsps` — gauge with the current number of loaded DSP configurations.
- `sim_rejected_total{reason=in_flight_limit}` — number of requests rejected with `429` because the in-flight limit was exceeded on `/openrtb2/**`.
