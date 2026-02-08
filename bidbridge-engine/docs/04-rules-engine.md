# Rules Engine

## Purpose

Apply configurable MVP business logic before adapter execution.

---

## Implemented Rule Types (MVP)

- `minBidfloor` (filters impressions)
- `allowInventory` / `denyInventory` (SITE/APP inventory gating)
- `allowAdapters` / `denyAdapters` (adapter filtering)

---

## Evaluation Order (Current)

1. Bidfloor filtering on impressions
2. Inventory allow/deny evaluation
3. Adapter allow/deny filtering
4. If all impressions or all adapters are filtered, return no-bid (204)

---

## Configuration

Rules are loaded from:

- Spring config properties under `rules.*`
- Environment variable overrides via Spring binding

---

## Performance

- In-memory evaluation
- No blocking calls
- No expression engine in MVP
