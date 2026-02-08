# Performance and Security

## Performance Targets

- P95 < 50ms
- P99 < 80ms
- Throughput > 5k QPS

---

## Performance Techniques

- Netty event loops
- Object pooling
- Zero-copy buffers
- Async I/O

---

## Security

- HTTPS only
- In-flight request limiting (`engine.limits.maxInFlight` -> HTTP `429` on `/openrtb2/**`)
- Request size limits
- Input validation
- Secure secrets storage

MVP note:
- Input validation is implemented.
- In-flight limiting is implemented in the engine; broader rate limiting and HTTPS termination remain deployment/edge concerns.

---

## Compliance

- GDPR flags propagation
- CCPA support
- Consent fields preserved
