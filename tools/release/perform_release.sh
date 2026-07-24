#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/release/perform_release.sh <version>" >&2
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

VERSION="$1"
if [[ ! "${VERSION}" =~ ^6\.[0-9]+$ ]]; then
  echo "Release version must match 6.<number>: ${VERSION}" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT}"

tools/release/check_ready.sh
tools/release/prepare_release.sh "${VERSION}"
tools/package_maven_central.sh "${VERSION}"
tools/release/upload_maven_central.sh "${VERSION}"
tools/release/finalize_release.sh "${VERSION}"
