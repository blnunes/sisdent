#!/usr/bin/env bash

set -euo pipefail

readonly PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly TEST_DIR="$(mktemp -d)"
trap 'rm -rf "${TEST_DIR}"' EXIT

cp -R "${PROJECT_DIR}/deploy" "${TEST_DIR}/deploy"
cd "${TEST_DIR}"

git init --quiet
git config user.email "release-test@example.invalid"
git config user.name "Release test"

cat > pom.xml <<'EOF'
<project>
  <artifactId>sisdent</artifactId>
  <version>0.0.2-SNAPSHOT</version>
</project>
EOF
git add pom.xml deploy
git commit --quiet -m "release"
release_sha="$(git rev-parse HEAD)"

sed -i.bak 's/0.0.2-SNAPSHOT/0.0.3-SNAPSHOT/' pom.xml
rm pom.xml.bak
git add pom.xml
git commit --quiet -m "prepare next snapshot"
bump_sha="$(git rev-parse HEAD)"

classification="$(deploy/release/classify-change.sh "${release_sha}" "${bump_sha}")"
[[ "${classification}" == "version-bump" ]]

echo "documentation" > README.md
git add README.md
git commit --quiet -m "regular change"
change_sha="$(git rev-parse HEAD)"

classification="$(deploy/release/classify-change.sh "${bump_sha}" "${change_sha}")"
[[ "${classification}" == "release-change" ]]

if deploy/release/classify-change.sh \
  "0000000000000000000000000000000000000000" "${change_sha}" 2>/dev/null; then
  echo "Missing commits must fail classification" >&2
  exit 1
fi
