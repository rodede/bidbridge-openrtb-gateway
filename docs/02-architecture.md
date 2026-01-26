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
- Converts raw OpenRTB into internal model
- Extracts relevant attributes
- Preserves extensions

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

