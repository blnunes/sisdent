#!/usr/bin/env bash
set -euo pipefail

# Kill processes listening on the dev ports used by Angular and Spring Boot.
# This is intended for macOS and Linux environments where lsof is available.

PORTS=(4200 8080)

for port in "${PORTS[@]}"; do
  pids=$(lsof -ti tcp:"$port" || true)
  if [[ -n "$pids" ]]; then
    echo "Killing processes on port $port: $pids"
    echo "$pids" | xargs -r kill -9
  else
    echo "No process found on port $port"
  fi
done

echo "Done."
