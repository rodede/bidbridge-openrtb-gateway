# BidBridge — OpenRTB Gateway

BidBridge is a lightweight, low-latency OpenRTB 2.6 bidding gateway built in Java.

It provides a unified layer for normalizing bid requests, applying business rules, and routing traffic to multiple demand sources.

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

- Engine: `bidbridge-engine/`
- Simulator: `bidbridge-simulator/README.md`
- Load generator: `bidbridge-loadgen/README.md`
- Repo-level docs: `docs/`
- Engine specs: `bidbridge-engine/docs/`

## Observability

- Engine actuator: `GET /actuator/health`, `GET /actuator/info`
- Simulator actuator: `GET /actuator/health`, `GET /actuator/info`, `GET /actuator/prometheus`
- Dashboard/config assets live under your monitoring repo/config path

## Status

Project is under active development, focused on:
- `bidbridge-engine` (primary)
- `bidbridge-simulator` (supporting test endpoint)
- `bidbridge-loadgen` (integration/load tooling)
