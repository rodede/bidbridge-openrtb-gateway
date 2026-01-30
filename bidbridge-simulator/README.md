# BidBridge Simulator

Minimal OpenRTB bidder simulator used for local and integration testing.

## Purpose

- Accepts OpenRTB `POST /openrtb2/bid`
- Returns a fixed bid or no-bid based on simple config
- Designed to be deployable later (e.g., AWS) for end-to-end testing

## Run locally

From repo root:

```bash
mvn spring-boot:run
```

Default port: `8081`

## Configuration

`bidbridge-simulator/src/main/resources/application.yml`

```yaml
simulator:
  enabled: true
  bidProbability: 1.0
  fixedPrice: 1.5
  currency: "USD"
  admTemplate: "<vast/>"
  responseDelayMs: 0
```

## Example request

```json
{"id":"req-1","imp":[{"id":"1"}]}
```

## Example wget

```bash
wget -q -O - --header="Content-Type: application/json" --post-data='{"id":"req-1","imp":[{"id":"1"}]}' http://localhost:8081/openrtb2/bid
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
