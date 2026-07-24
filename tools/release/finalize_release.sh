#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/release/finalize_release.sh <version>" >&2
}

validate_version() {
  local version="$1"
  if [[ ! "${version}" =~ ^6\.[0-9]+$ ]]; then
    echo "Release version must match 6.<number>: ${version}" >&2
    exit 1
  fi
}

major_version() {
  local version="$1"
  echo "${version%%.*}"
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

VERSION="$1"
validate_version "${VERSION}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT}"

"${ROOT}/bazelw" run //tools/release:release_lifecycle -- finalize --changelog "${ROOT}/CHANGELOG.md" --version "${VERSION}"

if ! git diff --quiet -- CHANGELOG.md; then
  git add CHANGELOG.md
  git commit -m "Update CHANGELOG.md in preparation for next development iteration"
fi

git push
git push --tags

TMP_DIR="$(mktemp -d /private/tmp/replicant-release.XXXXXX)"
trap 'rm -rf "${TMP_DIR}"' EXIT
NOTES_FILE="${TMP_DIR}/release-notes.md"

"${ROOT}/bazelw" run //tools/release:release_lifecycle -- release-notes --changelog "${ROOT}/CHANGELOG.md" --version "${VERSION}" --output "${NOTES_FILE}"

TAG="v${VERSION}"
RELEASE_ARGS=(--title "${TAG}" --notes-file "${NOTES_FILE}")
if [[ "$(major_version "${VERSION}")" == "0" ]]; then
  RELEASE_ARGS+=(--prerelease)
fi

if gh release view "${TAG}" >/dev/null 2>&1; then
  gh release edit "${TAG}" "${RELEASE_ARGS[@]}"
else
  gh release create "${TAG}" "${RELEASE_ARGS[@]}"
fi

MILESTONE_NUMBER="$(gh api "repos/{owner}/{repo}/milestones?state=open" --jq ".[] | select(.title == \"${TAG}\") | .number" 2>/dev/null | head -n 1 || true)"
if [[ -n "${MILESTONE_NUMBER}" ]]; then
  gh api --method PATCH "repos/{owner}/{repo}/milestones/${MILESTONE_NUMBER}" -f state=closed >/dev/null 2>&1 || true
fi
