#!/usr/bin/env bash

set -euo pipefail

readonly DEPLOY_DIR="${DEPLOY_DIR:-/srv/sisdent}"
readonly COMPOSE_FILE="${DEPLOY_DIR}/compose.preprod.yml"
readonly RUNTIME_ENV="${DEPLOY_DIR}/runtime.env"
readonly LAST_SUCCESSFUL_IMAGE="${DEPLOY_DIR}/.last-successful-image"
readonly HEALTH_URL="${HEALTH_URL:-http://127.0.0.1/actuator/health}"
readonly NEW_IMAGE_TAG="${1:?Usage: deploy.sh <image-tag>}"

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "Missing Compose file: ${COMPOSE_FILE}" >&2
  exit 1
fi

if [[ ! -f "${RUNTIME_ENV}" ]]; then
  echo "Missing runtime configuration: ${RUNTIME_ENV}" >&2
  echo "Create it from deploy/preprod/runtime.env.example during host bootstrap." >&2
  exit 1
fi

previous_image_tag=""
if [[ -f "${LAST_SUCCESSFUL_IMAGE}" ]]; then
  previous_image_tag="$(<"${LAST_SUCCESSFUL_IMAGE}")"
fi

compose() {
  SISDENT_IMAGE_TAG="${SISDENT_IMAGE_TAG}" \
    docker compose \
      --project-directory "${DEPLOY_DIR}" \
      --env-file "${RUNTIME_ENV}" \
      --file "${COMPOSE_FILE}" \
      "$@"
}

healthy() {
  for attempt in {1..36}; do
    if curl --fail --silent --show-error --max-time 5 "${HEALTH_URL}" >/dev/null; then
      return 0
    fi
    echo "Health check ${attempt}/36 failed; retrying in 5 seconds"
    sleep 5
  done
  return 1
}

export SISDENT_IMAGE_TAG="${NEW_IMAGE_TAG}"
echo "Deploying Sisdent image tag ${SISDENT_IMAGE_TAG}"
compose pull
compose up --detach --remove-orphans

if healthy; then
  printf '%s\n' "${SISDENT_IMAGE_TAG}" >"${LAST_SUCCESSFUL_IMAGE}"
  echo "Pre-production deployment is healthy"
  exit 0
fi

echo "Deployment failed its health check" >&2
compose ps >&2 || true
compose logs --tail 200 >&2 || true

if [[ -z "${previous_image_tag}" || "${previous_image_tag}" == "${NEW_IMAGE_TAG}" ]]; then
  echo "No previous image is available for rollback" >&2
  exit 1
fi

export SISDENT_IMAGE_TAG="${previous_image_tag}"
echo "Rolling back to image tag ${SISDENT_IMAGE_TAG}" >&2
compose up --detach --remove-orphans

if healthy; then
  echo "Rollback completed successfully" >&2
else
  echo "Rollback also failed its health check" >&2
  compose ps >&2 || true
  compose logs --tail 200 >&2 || true
fi

exit 1
