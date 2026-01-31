# BidBridge Simulator

Minimal OpenRTB bidder simulator used for local and integration testing.

## Purpose

- Accepts OpenRTB `POST /openrtb2/{dsp}/bid`
- Returns a fixed bid or no-bid based on simple config
- Designed to be deployable later (e.g., AWS) for end-to-end testing

## Run locally

From repo root:

```bash
mvn spring-boot:run
```

Default port: `8081`
Actuator health: `GET /actuator/health`

## Configuration

`bidbridge-simulator/src/main/resources/application.yml` points to an external `dsps.yml`.
By default it looks for `dsps.yml` at the repo root. The file can either:
1) use a top-level `dsps:` map, or
2) place DSP names at the top level.

`dsps.file` supports local paths and `s3://bucket/key` URLs (uses default AWS credentials).
Profiles:

- `local` → `application-local.yml` (local file - default)
- `aws` → `application-aws.yml` (S3 file)

Run with a profile: SPRING_PROFILES_ACTIVE=local|aws

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
- `<dsp>.currency`: value used for the `cur` field in the response.
- `<dsp>.admTemplate`: string inserted into `adm` (often VAST XML).
- `<dsp>.responseDelayMs`: artificial delay (in ms) before responding, to simulate bidder latency.

Polling:

- `dsps.pollIntervalMs`: reload interval in milliseconds (default 2000).

## Example request

```json
{"id":"req-1","imp":[{"id":"1"}]}
```

## Example wget

```bash
wget -q -O - --header="Content-Type: application/json" --post-data='{"id":"req-1","imp":[{"id":"1"}]}' http://localhost:8081/openrtb2/simulator/bid
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

---

## TODO:

1) Health endpoints (for ALB/ECS)
Expose only:
- GET /actuator/health/liveness
- GET /actuator/health/readiness
Keep the rest of /actuator/** off the public path.

2) Request correlation + summary logs
- Log one line per request (and errors) with:
```
requestId (generate if missing; also echo back as header)
path
status (204 vs 200)
dspId chosen
latencyMs simulated
durationMs total
caller (from JWT claim later; for now maybe X-Caller)
```
In AWS you’ll read this in CloudWatch Logs (plain text).

3) Metrics via Micrometer + Actuator
- Expose Prometheus-format metrics (even if you don’t scrape yet):
`GET /actuator/prometheus`

Track:
- sim_requests_total{outcome=bid|nobid|error}
- sim_latency_ms (timer)
- sim_reload_success_total / sim_reload_fail_total
- sim_active_dsps (gauge)

Later you can wire to CloudWatch Container Insights or Prometheus/Grafana.

4) Timeouts + limits
Even for a simulator:
- Server request timeout
- Max request size (avoid someone sending a huge OpenRTB payload)
- Concurrency/backpressure (especially if using WebFlux)

5) Basic error handling
- Return clean 4xx for bad input
- Don’t leak stack traces in responses
- Log stack traces internally

Nice-to-have (still lightweight)
6) OpenTelemetry tracing (optional now, great later)
Add OpenTelemetry instrumentation
- Export to AWS X-Ray or OTLP collector later
- This helps when multiple services call your simulator.

7) Startup banner/config dump (safe)
On boot, log:
- active profiles
- loaded DSP count + config version timestamp
- whether hot reload is enabled

Minimal “AWS-ready checklist”

✅ /actuator/health/**

✅ requestId correlation + summary logs

✅ /actuator/prometheus + 3–5 custom metrics

✅ input size + timeouts

✅ safe error responses
