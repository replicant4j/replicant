#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"

tools/update_java_deps.sh
./bazelw run //:buildifier_check
tools/java_format.sh check
tools/check_null_marked_packages.sh
./bazelw build //...
./bazelw test //...
