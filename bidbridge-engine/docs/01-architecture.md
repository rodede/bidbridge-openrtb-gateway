# System Architecture

## High-Level Architecture

```text
SSP / Publisher       POST /openrtb2/bid
↓
API                   Filters (auth, in-flight limiting, logging) + Validation (schema/shape)
↓
Normalization         Maps payload to internal 2.6-first model
↓
Rules Engine          Evaluate inventory, floor, and adapter allow/deny policy
↓
Orchestration         Computes timeout budget and fans out to enabled adapters
↓
Adapters              Execute bidder calls and map outcomes to internal result types
↓
Response Merger       Selects the best valid bid, or no-bid if none is eligible
↓
SSP                   Returns contract-compliant status/body/headers
```

Class diagram source: `architecture-class-diagram.puml`

![Architecture Class Diagram](architecture-class-diagram.png)

---

## Layer Responsibilities

### API Layer (Ingress)

**Purpose**: Own the public HTTP contract and endpoint behavior.

Responsibilities:

- Expose `POST /openrtb2/bid`.
- Parse request body and perform early validation.
- Apply inbound request filters (`/openrtb2/**`) for:
    - optional auth (`X-Api-Key`, profile-based)
    - in-flight rejection (`429`)
    - request correlation and logging headers
- Return contract-aligned status codes and response body shape.

See `bidbridge-engine/README.md` for public endpoint and response contract details.

### Normalization Layer

**Purpose**: Convert external OpenRTB payloads to a deterministic internal model.

Responsibilities:

- Accept OpenRTB 2.5/2.6 inputs and normalize into a 2.6-first internal representation.
- Apply deterministic defaults and field shaping.
- Preserve extension payloads (`ext`) needed for downstream adapters.
- Keep normalization side-effect free and deterministic for identical input.

See `bidbridge-engine/docs/00-implementation-details.md` for normalization rules, defaults, and pass-through behavior.

### Rules Engine

**Purpose**: Determine eligibility of impressions/adapters before bidder fan-out.

Responsibilities:

- Apply configured inventory and bidfloor constraints.
- Resolve adapter allow/deny filtering.
- Produce a filtered execution context used by orchestration.

### Service Orchestration Layer

**Purpose**: Coordinate request execution across rules, adapters, and merge logic.

Responsibilities:

- Compute request deadline and per-adapter timeout budget.
- Trigger enabled adapters concurrently using reactive flows.
- Collect adapter results and pass them to merger.
- Enforce no-adapter-enabled and timeout/error outcome handling rules.

Must not:

- Embed adapter-specific protocol logic.
- Duplicate merger winner-selection logic.

### Adapter Layer

**Purpose**: Isolate bidder integration details behind a stable internal interface.

Responsibilities:

- Encapsulate bidder endpoint communication and protocol mapping.
- Convert bidder responses/errors into internal adapter result types.
- Treat bidder non-2xx and timeout scenarios with standard mappings.

MVP constraints:

- Retries are not part of current behavior.
- No blocking I/O; use reactive HTTP clients only.

See `bidbridge-engine/docs/03-adapters.md` for adapter contract and lifecycle details.

### Response Merger

**Purpose**: Convert a set of adapter outcomes into one API response.

Responsibilities:

- Ignore invalid/non-positive bids.
- Select the highest valid bid.
- Return no-bid when no eligible bid remains.
- Build a contract-compliant OpenRTB response payload.

### Observability

**Purpose**: Provide latency/error visibility without changing request decisions.

Responsibilities:

- Emit request and adapter metrics.
- Emit structured logs with request correlation identifiers.
- Distinguish internal no-bid reasons through metrics/logging dimensions.

See `bidbridge-engine/docs/05-observability.md` for metric names and logging conventions.

---

## Data and Boundary Rules

- Contract boundary: API layer owns external HTTP/OpenRTB contract.
- Internal model boundary: normalization output is the canonical input for rules and orchestration.
- Policy boundary: rules engine decides eligibility, not transport behavior.
- Integration boundary: adapter layer owns bidder protocol concerns.
- Composition boundary: merger builds final response from adapter outcomes.

---

## Cross-Cutting Architecture Constraints

- Reactive/non-blocking execution only (WebFlux/Reactor).
- Fail-fast validation at ingress and normalization boundaries.
- Latency-aware timeout budgeting across request and adapters.
- Stateless request handling; no per-request shared mutable global state.
- Low-allocation preference in hot paths (normalization, fan-out, merge).

---

## Design Principles

- Keep boundaries explicit between `api`, `normalization`, `rules`, `service`, `adapters`, and `observability`.
- Keep external contract concerns separate from internal decision mechanics.
- Prefer deterministic behavior for identical inputs and configuration.
- Keep architecture MVP-oriented: add complexity only when driven by measured need.

---

## Related Documents

- `bidbridge-engine/README.md` (module entrypoint, API contract, quick run/test)
- `bidbridge-engine/docs/00-implementation-details.md` (internal defaults and flow details)
- `bidbridge-engine/docs/03-adapters.md` (adapter model and error mapping)
- `bidbridge-engine/docs/04-rules-engine.md` (rule types and evaluation order)
- `bidbridge-engine/docs/05-observability.md` (metrics and logs)
- `bidbridge-engine/docs/06-testing.md` (test strategy)
- `bidbridge-engine/docs/07-performance-security.md` (constraints and targets)
