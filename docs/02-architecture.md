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
- Handles authentication and rate limiting
- Performs initial validation

### Normalization Layer
Normalization must support OpenRTB 2.5 and 2.6.
The internal model follows 2.6; missing fields default safely.
- Converts raw OpenRTB into internal model
- Extracts relevant attributes
- Preserves extensions
See `docs/00-implementation-details.md` for normalization rules, defaults, and pass-through behavior.

### Rules Engine
- Applies routing and filtering logic
- Enforces floors and policies

### Adapter Layer
- Encapsulates bidder integrations
- Manages timeouts and retries

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
