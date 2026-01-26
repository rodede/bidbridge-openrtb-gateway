# BidBridge — OpenRTB Gateway

BidBridge is a lightweight, low-latency OpenRTB 2.6 bidding gateway built in Java.

It provides a unified layer for normalizing bid requests, applying business rules, and routing traffic to multiple demand sources.

---

## Modules

This repository is organized as a multi-module Maven project.

```
bidbridge-openrtb-gateway/
├── docs/ # Architecture and design documents
├── bidbridge-engine/ # Core OpenRTB gateway service
├── bidbridge-console/ # (Optional) Admin UI
├── bidbridge-simulator/ # SSP simulator and test tools
└── pom.xml # Parent Maven configuration
```

---

## Status

Project is under active development.
Initial focus: core gateway + simulator.