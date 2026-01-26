# Rules Engine

## Purpose

Apply configurable business logic before routing and bidding.

---

## Rule Types

- Floor price rules
- Geo filters
- Publisher filters
- Time windows
- A/B routing

---

## Evaluation Order

1. Validation rules
2. Eligibility rules
3. Routing rules
4. Optimization rules

---

## Configuration

Rules are loaded from:

- YAML / JSON files
- Environment variables
- Remote config (future)

---

## Performance

- In-memory evaluation
- No blocking calls
- Precompiled expressions
