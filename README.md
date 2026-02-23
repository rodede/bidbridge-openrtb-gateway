# BidBridge — OpenRTB Gateway

BidBridge is a lightweight, low-latency OpenRTB 2.6 bidding gateway built in Java.

It provides a unified layer for normalizing bid requests, applying business rules, and routing traffic to multiple demand sources.

## Project Status

This project is in archive/maintenance mode.

- No further product development is planned.
- `bidbridge-simulator` is deployed in AWS but currently has no real traffic usage; it can be reused as a sample/reference service.
- `bidbridge-loadgen` is working and can be used for local/integration/performance testing.
- `bidbridge-engine` currently has a minimal structure and is not deployed.

## Modules

This repository is organized as a multi-module Maven project.

```
bidbridge-openrtb-gateway/
├── docs/ # Repo-level docs (vision/roadmap)
├── bidbridge-engine/ # Core OpenRTB gateway service
│   └── docs/ # Engine architecture/specification docs
├── bidbridge-simulator/ # SSP simulator and test tools
├── bidbridge-loadgen/ # OpenRTB load generator for integration/perf tests
└── pom.xml # Parent Maven configuration
```

## Prerequisites

- Java 25
- Maven 3.9+
- Optional: Docker (for container-based simulator runs)

## Quick Start

1. Build all modules

```bash
mvn clean test
```

2. Run simulator (port `8081`)

```bash
mvn -pl bidbridge-simulator spring-boot:run
```

3. Run engine (port `8080`)

```bash
mvn -pl bidbridge-engine spring-boot:run
```

4. Send a sample bid request

```bash
curl -i -X POST "http://localhost:8080/openrtb2/bid" \
  -H "Content-Type: application/json" \
  --data-binary @bidbridge-loadgen/src/main/resources/sample-request.json
```

Expected outcomes: `200` (bid), `204` (no-bid), or `400` (invalid request).

## Module Docs

- Engine: `bidbridge-engine/README.md`
- Engine architecture: `bidbridge-engine/docs/01-architecture.md`
- Simulator: `bidbridge-simulator/README.md`
- Simulator architecture: `bidbridge-simulator/docs/01-architecture.md`
- Load generator: `bidbridge-loadgen/README.md`
- Load generator architecture: `bidbridge-loadgen/docs/01-architecture.md`
- Repo-level docs: `docs/`

## Observability

- Engine actuator: `GET /actuator/health`, `GET /actuator/info`
- Simulator actuator: `GET /actuator/health`, `GET /actuator/info`, `GET /actuator/prometheus`
- Dashboard/config assets live under your monitoring repo/config path

## Status Summary

- `bidbridge-engine`: MVP/minimal structure, not deployed.
- `bidbridge-simulator`: deployed in AWS, no current usage, useful as reference/sample.
- `bidbridge-loadgen`: operational and ready for testing workflows.
