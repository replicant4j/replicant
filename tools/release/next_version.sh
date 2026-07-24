#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/release/next_version.sh" >&2
}

if [[ $# -ne 0 ]]; then
  usage
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT}"
"${ROOT}/bazelw" run //tools/release:release_lifecycle -- next-version --changelog "${ROOT}/CHANGELOG.md"
