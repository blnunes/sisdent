#!/usr/bin/env bash

set -euo pipefail

readonly BEFORE_SHA="${1:?Previous commit SHA is required}"
readonly AFTER_SHA="${2:?Current commit SHA is required}"

git cat-file -e "${BEFORE_SHA}^{commit}"
git cat-file -e "${AFTER_SHA}^{commit}"

changed_files_output="$(git diff --name-only "${BEFORE_SHA}" "${AFTER_SHA}")"

if [[ "${changed_files_output}" == "pom.xml" ]]; then
  old_pom="$(mktemp)"
  trap 'rm -f "${old_pom}"' EXIT
  git show "${BEFORE_SHA}:pom.xml" > "${old_pom}"

  old_version="$(deploy/release/version.sh project-version "${old_pom}")"
  new_version="$(git show "${AFTER_SHA}:pom.xml" |
    deploy/release/version.sh project-version /dev/stdin)"

  if deploy/release/version.sh is-next-snapshot \
    "${old_version}" "${new_version}"; then
    echo "version-bump"
    exit 0
  fi
fi

echo "release-change"
