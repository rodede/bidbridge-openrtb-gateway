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
Engine request context headers (`X-Request-Id`, `X-Caller`) are propagated to outbound bidder calls when present.

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
- Network/runtime errors → adapter error result (merged at response policy level)

---

## Reference Adapters

- SimulatorAdapter
- SimulatorHttpAdapter
