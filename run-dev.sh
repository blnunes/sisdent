#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORTS=(4200 8080)
MAX_ATTEMPTS=50
HEALTH_ATTEMPTS=120

port_pids() {
  lsof -nP -tiTCP:"$1" -sTCP:LISTEN 2>/dev/null || true
}

stop_processes_on_port() {
  local port="$1"
  local pids pid signal attempt

  pids="$(port_pids "$port")"
  [[ -z "$pids" ]] && return 0

  echo "Stopping processes on port $port: $pids"

  for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
    pids="$(port_pids "$port")"
    [[ -z "$pids" ]] && return 0

    signal=TERM
    (( attempt > 10 )) && signal=KILL

    while IFS= read -r pid; do
      [[ -n "$pid" ]] && kill -"$signal" "$pid" 2>/dev/null || true
    done <<< "$pids"

    sleep 0.2
  done

  echo "Could not free port $port" >&2
  exit 1
}

stop_existing_services() {
  local port

  for port in "${PORTS[@]}"; do
    stop_processes_on_port "$port"
  done
}

stop_existing_services
echo "Starting development services..."

cleanup() {
  if [[ -n "${BACKEND_PID:-}" ]]; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
  if [[ -n "${FRONTEND_PID:-}" ]]; then
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

(
  cd "$ROOT_DIR"
  ./mvnw spring-boot:run
) &
BACKEND_PID=$!

echo "Waiting for the backend health endpoint on port 8080..."
for ((attempt = 1; attempt <= HEALTH_ATTEMPTS; attempt++)); do
  if curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
    echo "Backend is ready. Starting the frontend..."
    break
  fi
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "Backend exited before becoming ready." >&2
    exit 1
  fi
  if (( attempt == HEALTH_ATTEMPTS )); then
    echo "Backend did not become ready within 60 seconds." >&2
    exit 1
  fi
  sleep 0.5
done

(
  cd "$ROOT_DIR/frontend"
  npm start
) &
FRONTEND_PID=$!

wait "$BACKEND_PID" "$FRONTEND_PID"
