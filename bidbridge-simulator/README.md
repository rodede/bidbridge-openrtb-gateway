# BidBridge Simulator

Minimal OpenRTB bidder simulator used for local and integration testing.

## Purpose

- Accepts OpenRTB `POST /openrtb2/{dsp}/bid`
- Returns a fixed bid or no-bid based on simple config
- Designed to be deployable later (e.g., AWS) for end-to-end testing

## AWS deployment (visual)

```mermaid
flowchart LR
    client[Client services] --> apigw[API Gateway (HTTP API)]
    apigw --> vpc[VPC Link]
    vpc --> alb[Internal ALB]
    alb --> ecs[ECS Fargate tasks]
    ecs --> sim[BidBridge Simulator]
    sim --> s3[S3 dsps.yml]
    cw[CloudWatch Logs] <-- ecs
```

![AWS deployment diagram](docs/aws-architecture.svg)

## Run locally

From repo root:

```bash
mvn spring-boot:run
```

Default port: `8081`.

Actuator endpoints:

- `GET /actuator/health`
- `GET /actuator/prometheus`

## Configuration

`bidbridge-simulator/src/main/resources/application.yml` points to an external `dsps.yml`.
By default it looks for `dsps.yml` at the repo root.

The file can either:
1) use a top-level `dsps:` map, or
2) place DSP names at the top level.

`dsps.file` supports local paths and `s3://bucket/key` URLs (uses default AWS credentials).

Profiles:

- `local` → `application-local.yml` (local file - default)
- `aws` → `application-aws.yml` (S3 file)

Run with a profile: `SPRING_PROFILES_ACTIVE=local|aws`

```bash
SPRING_PROFILES_ACTIVE=local 
mvn -pl bidbridge-simulator spring-boot:run
```

AWS region:
- Set `AWS_REGION` or `AWS_DEFAULT_REGION` when using an S3 `dsps.file`.

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
- `simulator.maxInFlight`: max concurrent in-flight requests (429 when exceeded).
- `<dsp>.currency`: value used for the `cur` field in the response.
- `<dsp>.admTemplate`: string inserted into `adm` (often VAST XML).
- `<dsp>.responseDelayMs`: artificial delay (in ms) before responding, to simulate bidder latency.

Polling:
- `dsps.pollIntervalMs`: reload interval in milliseconds (default 2000).

## Example request

```json
{"id":"req-1","imp":[{"id":"1"}]}
```

## Example curl

```bash
curl -s -H "Content-Type: application/json" -d '{"id":"req-1","imp":[{"id":"1"}]}' http://localhost:8081/openrtb2/simulator/bid
```

## Admin endpoints

- `POST /admin/reload-dsps` — reloads dsps.yml on demand and returns `{status, loadedCount, versionTimestamp, message}`
- `GET /admin/dsps` — returns the loaded DSP configs and version timestamp

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

Prometheus endpoint:

- `GET /actuator/prometheus`

Emitted metrics:

- `sim_requests_total{outcome=bid|nobid|error}`
- `sim_latency_ms`
- `sim_reload_success_total`
- `sim_reload_fail_total`
- `sim_active_dsps`
- `sim_rejected_total{reason=in_flight_limit}`

## Example response

```json
{
  "id": "req-1",
  "seatbid": [
    {
      "bid": [
        {
          "id": "bid-1",
          "impid": "1",
          "price": 1.5,
          "adm": "<vast/>"
        }
      ]
    }
  ],
  "cur": "USD"
}
```
