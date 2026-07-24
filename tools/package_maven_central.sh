#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/package_maven_central.sh <version> [--gpg-executable PATH] [--gpg-key-id KEYID]" >&2
  echo "Defaults: GPG_USER selects the signing key and GPG_PASS supplies an optional passphrase." >&2
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

VERSION="$1"
shift

if [[ ! "${VERSION}" =~ ^6\.[0-9]+$ ]]; then
  echo "Release version must match 6.<number>: ${VERSION}" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"
"${ROOT}/bazelw" build //tools/release:maven_artifacts --release_version="${VERSION}"
"${ROOT}/bazelw" test //tools/release:all_tests --release_version="${VERSION}"
"${ROOT}/bazelw" run //tools/release:dist --release_version="${VERSION}" -- "$@"
