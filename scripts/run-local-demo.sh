#!/usr/bin/env bash
set -euo pipefail

# Starts the simulator and engine, sends one demo request, then shuts them down.
# Logs are written to /tmp/bidbridge-simulator.log and /tmp/bidbridge-engine.log.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

ENGINE_PORT=8080
SIMULATOR_PORT=8081

ENGINE_PID=""
SIMULATOR_PID=""

cleanup() {
  if [[ -n "${ENGINE_PID}" ]] && kill -0 "${ENGINE_PID}" 2>/dev/null; then
    kill "${ENGINE_PID}" 2>/dev/null || true
    wait "${ENGINE_PID}" 2>/dev/null || true
  fi
  if [[ -n "${SIMULATOR_PID}" ]] && kill -0 "${SIMULATOR_PID}" 2>/dev/null; then
    kill "${SIMULATOR_PID}" 2>/dev/null || true
    wait "${SIMULATOR_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

wait_for_port() {
  local port="$1"
  local retries=120
  local i=0
  while (( i < retries )); do
    if (echo > /dev/tcp/127.0.0.1/"${port}") >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.25
    i=$((i + 1))
  done
  return 1
}

cd "${ROOT_DIR}"

echo "Starting simulator on :${SIMULATOR_PORT}..."
mvn -pl bidbridge-simulator spring-boot:run >/tmp/bidbridge-simulator.log 2>&1 &
SIMULATOR_PID=$!

if ! wait_for_port "${SIMULATOR_PORT}"; then
  echo "Simulator failed to start. See /tmp/bidbridge-simulator.log"
  exit 1
fi

echo "Starting engine on :${ENGINE_PORT} (HTTP adapter to simulator)..."
mvn -pl bidbridge-engine spring-boot:run \
  -Dspring-boot.run.arguments="--rules.allowAdapters=simulatorHttp" \
  >/tmp/bidbridge-engine.log 2>&1 &
ENGINE_PID=$!

if ! wait_for_port "${ENGINE_PORT}"; then
  echo "Engine failed to start. See /tmp/bidbridge-engine.log"
  exit 1
fi

echo "Sending request to engine..."
curl -s -X POST "http://localhost:${ENGINE_PORT}/openrtb2/bid" \
  -H "Content-Type: application/json" \
  -d '{"id":"req-1","imp":[{"id":"1","banner":{},"bidfloor":0.5}],"site":{}, "tmax":10000}'
echo

# Optional: run the load generator (set RUN_LOADGEN=1 to enable).
if [[ "${RUN_LOADGEN:-0}" == "1" ]]; then
  LOADGEN_REQUEST_FILE="${ROOT_DIR}/bidbridge-loadgen/src/main/resources/sample-request.json"
  LOADGEN_REPLAY_FILE="${ROOT_DIR}/bidbridge-loadgen/src/main/resources/sample-requests.jsonl"
  LOADGEN_MODE="${LOADGEN_MODE:-single}"
  if [[ "${LOADGEN_MODE}" == "replay" ]]; then
    mvn -pl bidbridge-loadgen -DskipTests exec:java \
      -Dexec.mainClass=ro.dede.bidbridge.loadgen.LoadGenMain \
      -Dexec.args="--url http://localhost:${ENGINE_PORT}/openrtb2/bid --replay-file ${LOADGEN_REPLAY_FILE} --qps ${LOADGEN_QPS:-50} --duration-seconds ${LOADGEN_DURATION:-5} --concurrency ${LOADGEN_CONCURRENCY:-50}"
  else
    mvn -pl bidbridge-loadgen -DskipTests exec:java \
      -Dexec.mainClass=ro.dede.bidbridge.loadgen.LoadGenMain \
      -Dexec.args="--url http://localhost:${ENGINE_PORT}/openrtb2/bid --request-file ${LOADGEN_REQUEST_FILE} --qps ${LOADGEN_QPS:-50} --duration-seconds ${LOADGEN_DURATION:-5} --concurrency ${LOADGEN_CONCURRENCY:-50}"
  fi
fi

echo "Done. Shutting down."
