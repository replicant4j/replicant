#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/release/prepare_release.sh <version> [--dry-run]" >&2
}

validate_version() {
  local version="$1"
  if [[ ! "${version}" =~ ^6\.[0-9]+$ ]]; then
    echo "Release version must match 6.<number>: ${version}" >&2
    exit 1
  fi
}

run_prepare() {
  local changelog="$1"
  local readme="$2"
  local version="$3"
  local args=(prepare --changelog "${changelog}" --readme "${readme}" --version "${version}")
  if [[ -n "${RELEASE_DATE:-}" ]]; then
    args+=(--release-date "${RELEASE_DATE}")
  fi
  "${ROOT}/bazelw" run //tools/release:release_lifecycle -- "${args[@]}"
}

print_diff() {
  local label="$1"
  local before="$2"
  local after="$3"

  echo
  echo "${label} diff:"
  set +e
  diff -u "${before}" "${after}"
  local status=$?
  set -e
  if [[ ${status} -gt 1 ]]; then
    exit "${status}"
  fi
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 1
fi

VERSION="$1"
shift
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=1
      ;;
    *)
      usage
      exit 1
      ;;
  esac
  shift
done

validate_version "${VERSION}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT}"

if [[ ${DRY_RUN} -eq 1 ]]; then
  if [[ -n "$(git status --short)" ]]; then
    echo "Warning: working tree is dirty; dry-run will use temporary CHANGELOG.md and README.md copies." >&2
  fi

  TMP_DIR="$(mktemp -d /private/tmp/replicant-release.XXXXXX)"
  trap 'rm -rf "${TMP_DIR}"' EXIT
  CHANGELOG_COPY="${TMP_DIR}/CHANGELOG.md"
  README_COPY="${TMP_DIR}/README.md"
  cp CHANGELOG.md "${CHANGELOG_COPY}"
  cp README.md "${README_COPY}"

  run_prepare "${CHANGELOG_COPY}" "${README_COPY}" "${VERSION}"
  echo "Dry-run prepared release v${VERSION}. Review the generated diffs before running without --dry-run."
  print_diff "CHANGELOG.md" CHANGELOG.md "${CHANGELOG_COPY}"
  print_diff "README.md" README.md "${README_COPY}"
  exit 0
fi

if [[ -n "$(git status --short)" ]]; then
  echo "Working tree must be clean before preparing a release." >&2
  exit 1
fi

run_prepare "${ROOT}/CHANGELOG.md" "${ROOT}/README.md" "${VERSION}"
git add CHANGELOG.md README.md
git commit -m "Update CHANGELOG.md and README.md in preparation for release"
git tag "v${VERSION}"
