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

## Example request

```json
{"id":"req-1","imp":[{"id":"1"}]}
```

## Example wget

```bash
wget -q -O - --header="Content-Type: application/json" --post-data='{"id":"req-1","imp":[{"id":"1"}]}' http://localhost:8081/openrtb2/simulator/bid
```

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

## Notes for AWS

- Expose port 8081
- Keep config in environment variables or a mounted config file
- Consider adding a Dockerfile when needed
- Configure the load balancer health check to `/actuator/health`
- Ensure security group allows inbound from the ALB/NLB only
