#!/usr/bin/env bash

set -euo pipefail

readonly SEMVER_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+$'
readonly SNAPSHOT_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+-SNAPSHOT$'

project_version() {
  local pom_file="${1:-pom.xml}"

  awk '
    /<artifactId>sisdent<\/artifactId>/ { project = 1 }
    project && /<version>/ {
      version = $0
      sub(/^.*<version>/, "", version)
      sub(/<\/version>.*$/, "", version)
      print version
      exit
    }
  ' "${pom_file}"
}

release_version() {
  local snapshot_version="${1:?Snapshot version is required}"

  if [[ ! "${snapshot_version}" =~ ${SNAPSHOT_PATTERN} ]]; then
    echo "Expected a semantic SNAPSHOT version, got: ${snapshot_version}" >&2
    exit 1
  fi

  echo "${snapshot_version%-SNAPSHOT}"
}

next_snapshot() {
  local released_version="${1:?Released version is required}"

  if [[ ! "${released_version}" =~ ${SEMVER_PATTERN} ]]; then
    echo "Expected a semantic release version, got: ${released_version}" >&2
    exit 1
  fi

  local major minor patch
  IFS=. read -r major minor patch <<<"${released_version}"
  echo "${major}.${minor}.$((patch + 1))-SNAPSHOT"
}

is_next_snapshot() {
  local previous_snapshot="${1:?Previous snapshot version is required}"
  local candidate_snapshot="${2:?Candidate snapshot version is required}"
  local previous_release expected_snapshot

  previous_release="$(release_version "${previous_snapshot}")"
  expected_snapshot="$(next_snapshot "${previous_release}")"
  [[ "${candidate_snapshot}" == "${expected_snapshot}" ]]
}

readonly COMMAND="${1:-}"
case "${COMMAND}" in
  project-version)
    project_version "${2:-pom.xml}"
    ;;
  release-version)
    release_version "${2:-}"
    ;;
  next-snapshot)
    next_snapshot "${2:-}"
    ;;
  is-next-snapshot)
    is_next_snapshot "${2:-}" "${3:-}"
    ;;
  *)
    echo "Usage: $0 {project-version [pom]|release-version VERSION|next-snapshot VERSION|is-next-snapshot OLD NEW}" >&2
    exit 1
    ;;
esac
