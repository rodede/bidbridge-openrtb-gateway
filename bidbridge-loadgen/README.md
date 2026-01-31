# BidBridge LoadGen

Minimal OpenRTB load generator for integration and performance testing.

It reads a single JSON request or a JSONL replay file, schedules sends at a target QPS,
caps the number of concurrent in-flight requests, and prints basic status/latency stats at the end.

Requests are sent using Spring WebFlux WebClient backed by Reactor Netty, with dedicated event loop
resources that are disposed after the run to avoid lingering threads.

## Run

From repo root:

```bash
mvn -pl bidbridge-loadgen -DskipTests exec:java \
  -Dexec.mainClass=ro.dede.bidbridge.loadgen.LoadGenMain \
  -Dexec.args="--url http://localhost:8080/openrtb2/bid --request-file /path/to/request.json --qps 200 --duration-seconds 15 --concurrency 200"
```

What this run does: sends the request file to the given URL at ~200 QPS for 15 seconds, with up to 200 concurrent in-flight requests.

Replay mode (JSON lines, one request per line):

```bash
mvn -pl bidbridge-loadgen -DskipTests exec:java \
  -Dexec.mainClass=ro.dede.bidbridge.loadgen.LoadGenMain \
  -Dexec.args="--url http://localhost:8080/openrtb2/bid --replay-file /path/to/requests.jsonl --qps 200 --duration-seconds 15 --concurrency 200"
```

Sample files:

- `bidbridge-loadgen/src/main/resources/sample-request.json`
- `bidbridge-loadgen/src/main/resources/sample-requests.jsonl`

## Options

- `--url` (required)
- `--request-file` (required unless `--replay-file` is set)
- `--replay-file` (required unless `--request-file` is set)
- `--qps` (default: 50)
- `--duration-seconds` (default: 10)
- `--concurrency` (default: qps)
- `--timeout-ms` (default: 500)
