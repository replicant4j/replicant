#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

missing=0
while IFS= read -r directory; do
  package_info="${directory}/package-info.java"
  if [[ ! -f "${package_info}" ]] || ! grep -q '^@NullMarked$' "${package_info}"; then
    echo "Missing @NullMarked package declaration: ${package_info}" >&2
    missing=1
  fi
done < <(
  find client/src/main/java client/src/test/java \
       shared/src/main/java \
       server/src/main/java server/src/test/java \
       tools \
       -type f -name '*.java' \
       ! -path '*/generated/*' \
       -exec dirname {} \; |
    LC_ALL=C sort -u
)

exit "${missing}"
