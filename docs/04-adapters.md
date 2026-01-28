# Adapter Framework

## Purpose

Adapters abstract vendor-specific bidder integrations.

Each adapter implements a standard interface.

---

## Adapter Interface

Responsibilities:

- Transform internal request to vendor format
- Send request
- Handle timeout
- Parse response
- Map to internal model

HTTP adapters use an internal `HttpBidderClient` abstraction (WebClient-based by default) so the
HTTP stack can be swapped later without changing adapter logic.

---

## Lifecycle

1. Receive normalized request
2. Apply adapter mapping
3. Call external bidder
4. Parse response
5. Return internal bid

---

## Error Handling

- Timeouts → no-bid
- Invalid response → error (bad bidder response)
- Network errors → fallback

---

## Reference Adapters

- SimulatorAdapter
- SimulatorHttpAdapter
- StaticBidAdapter
- ReplayAdapter
- ExternalDSPAdapter (mocked)
