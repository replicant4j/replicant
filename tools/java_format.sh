#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-write}"

cd "${ROOT}"

case "${MODE}" in
  check)
    exec "${ROOT}/bazelw" build //:java_format_check
    ;;
  write | watch)
    target=java_format
    if [[ "${MODE}" == "watch" ]]; then
      target=java_format_watch
    fi
    exec "${ROOT}/bazelw" run "@rules_palantir_java_format//:${target}" -- \
      --root=client --root=shared --root=server --root=tools
    ;;
  *)
    echo "usage: tools/java_format.sh [write|check|watch]" >&2
    exit 2
    ;;
esac
