# System Architecture

## High-Level Architecture

```
SSP / Publisher
↓
OpenRTB API
↓
Normalizer
↓
Rules Engine
↓
Adapters
↓
Response Merger
↓
SSP
```

---

## Core Components

### API Layer
- Exposes HTTP endpoint
- Performs initial schema/shape validation
- Propagates correlation ID and request timing logs
- Applies optional inbound API key auth (profile-based)
- Applies in-flight request limiting on `/openrtb2/**`

### Normalization Layer
Normalization must support OpenRTB 2.5 and 2.6.
The internal model follows 2.6; missing fields default safely.
- Converts raw OpenRTB into internal model
- Extracts relevant attributes
- Preserves extensions
See `bidbridge-engine/docs/00-implementation-details.md` for normalization rules, defaults, and pass-through behavior.

### Rules Engine
- Applies routing and filtering logic
- Enforces floors and policies

### Adapter Layer
- Encapsulates bidder integrations
- Manages timeout budgets and response mapping
- Retries are not part of MVP behavior

### Response Merger
- Collects adapter responses
- Selects winning bid
- Builds OpenRTB response

### Observability
- Metrics
- Tracing
- Logging

---

## Design Principles

- Stateless services
- Non-blocking I/O
- Minimal allocations
- Clear module boundaries
- Fail-fast validation

### Validation Boundaries

- API layer: schema/shape validation (JSON + required fields)
- Normalization layer: business validation and defaults
