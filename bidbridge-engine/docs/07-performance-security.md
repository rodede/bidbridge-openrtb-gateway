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
- Rate limiting
- Request size limits
- Input validation
- Secure secrets storage

MVP note:
- Input validation is implemented.
- HTTPS/rate limiting are deployment/edge concerns in the current phase.

---

## Compliance

- GDPR flags propagation
- CCPA support
- Consent fields preserved
