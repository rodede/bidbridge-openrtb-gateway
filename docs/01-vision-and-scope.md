# Vision and Scope

## Vision

Build a lightweight, reliable, and extensible OpenRTB bidding gateway that enables publishers and platforms to integrate multiple demand sources through a unified, low-latency interface.

---

## Primary Objectives

- Provide a compliant OpenRTB 2.5 bidder endpoint
- Maintain sub-50ms processing latency
- Support pluggable bidder integrations
- Enable controlled experimentation
- Ensure high reliability and observability

---

## Target Users

- Audio and video publishers
- SSPs and exchanges
- Ad operations teams
- Platform engineers

---

## In Scope

- Bid request parsing and validation
- Adapter-based bidder integrations
- Rule-based routing
- VAST creative embedding
- Metrics and tracing
- SSP simulation tools

---

## Out of Scope

- Full ad serving platform
- User-level targeting
- Campaign management UI
- Billing and invoicing
- Machine learning optimization

---

## Success Criteria

- Stable operation at 5k+ QPS
- P95 latency below 50ms
- Clean modular codebase
- Reusable architecture patterns
