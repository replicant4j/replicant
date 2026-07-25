#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: tools/release/check_ready.sh" >&2
}

if [[ $# -ne 0 ]]; then
  usage
  exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${ROOT}"

tools/check.sh
git diff --check

for command in gpg curl gh; do
  if ! command -v "${command}" >/dev/null 2>&1; then
    echo "Required command not found: ${command}" >&2
    exit 1
  fi
done

gh auth status >/dev/null

if [[ -z "${MAVEN_CENTRAL_USERNAME:-}" ]]; then
  echo "MAVEN_CENTRAL_USERNAME must be set." >&2
  exit 1
fi

if [[ -z "${GPG_USER:-}" ]]; then
  echo "GPG_USER must be set." >&2
  exit 1
fi

if [[ -z "${MAVEN_CENTRAL_PASSWORD:-}" ]]; then
  echo "MAVEN_CENTRAL_PASSWORD must be set." >&2
  exit 1
fi
