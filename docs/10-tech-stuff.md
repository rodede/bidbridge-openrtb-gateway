# Tech stuff

## Project init
```
spring init \
  --type=maven-project \
  --language=java \
  --boot-version=4.0.1 \
  --java-version=25 \
  --groupId=ro.dede \
  --artifactId=bidbridge-engine \
  --name="BidBridge Engine" \
  --package-name=ro.dede.bidbridge.engine \
  --dependencies=webflux,actuator,validation \
  bidbridge-engine
```
- webflux
```
Non-blocking HTTP stack (Reactor Netty by default)
Great for low-latency request/response services like OpenRTB bidders
Keeps concurrency efficient at higher QPS
```
- actuator
```
Production endpoints like /actuator/health, /actuator/info
Easy to plug into ops workflows
```
- validation
```
Bean Validation (Jakarta Validation / Hibernate Validator)
Helps enforce required fields and constraints on request objects
Good for “fail-fast” OpenRTB request checks
```
- TODO
```
micrometer-registry-prometheus: Expose /actuator/prometheus for Prometheus scraping
opentelemetry: Tracing (often via OpenTelemetry Java agent rather than a dependency)
```

### Ohers

#### Logging
```
    private static final Logger log =
        LoggerFactory.getLogger(BidController.class);

    log.info("Started processing request");
    log.warn("Slow adapter");
    log.error("Adapter failed", ex);

```

#### How Spring Validation Works (Basics)

Spring uses Jakarta Bean Validation (JSR 380).
```
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public class BidRequest {
    @NotBlank
    private String id;
    @NotNull
    private List<Imp> imp;
}
```
```
public class Imp {
    @NotBlank
    private String id;
    @Positive
    private Double bidfloor;
}
```
```
@PostMapping("/openrtb2/bid")
public Mono<ResponseEntity<?>> bid(
        @Valid @RequestBody BidRequest request) {
    return service.process(request);
}
```
If invalid → Spring returns `400 Bad Request`.

Custom Validation:  rules are cross-field.
```
@AssertTrue(message = "Either site or app must be present")
public boolean isInventoryValid() {
    return site != null || app != null;
}
```

Handling Validation Errors Cleanly
```
@RestControllerAdvice
public class ValidationHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handle(...) {
        return ResponseEntity
            .badRequest()
            .body("Invalid OpenRTB request");
    }
}
```
wire validation into your normalizer layer!




### Others



CI
GitHub Actions: build + tests on PR
Cache Maven repo

Quality gates
JUnit 5 + Mockito baseline
Testcontainers (optional later, but wire the deps now)
JaCoCo coverage report (even if no strict threshold)

Runtime basics (Spring WebFlux)
Global error handler (@RestControllerAdvice) with clean 400/500 mapping
Request size limits + timeouts
Structured logging (JSON) + request-id in MDC
Observability (MVP level)
Micrometer + Prometheus endpoint (/actuator/prometheus)
Basic metrics: request count, latency, no-bid, errors, adapter timeouts
Health endpoints via Actuator
Config + boundaries
Typed config (@ConfigurationProperties)
Clear packages: api / domain / normalize / rules / adapters / observability
DTO vs internal model separation (don’t mix)
HTTPS-ready, rate limiting placeholder (even if stub)
Dependabot (or Renovate) for deps updates

