#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "${ROOT}"

tools/update_java_deps.sh

GENERATED_OUTPUTS=(
  MODULE.bazel
  MODULE.bazel.lock
  third_party/java/BUILD.bazel
  tools/java-format/BUILD.bazel
)
if ! git diff --quiet -- "${GENERATED_OUTPUTS[@]}"; then
  echo "Generated dependency outputs are stale:" >&2
  git diff -- "${GENERATED_OUTPUTS[@]}" >&2
  exit 1
fi

./bazelw run //:buildifier_check
tools/java_format.sh check
tools/check_null_marked_packages.sh

var_matches=""
var_status=0
var_matches="$(rg -n '(^|[;({])[[:space:]]*(final[[:space:]]+)?var[[:space:]]+[A-Za-z_$]' \
  client shared --glob '*.java')" || var_status=$?
if [[ "${var_status}" -eq 0 ]]; then
  echo "Client/shared Java must use explicit local types:" >&2
  echo "${var_matches}" >&2
  exit 1
elif [[ "${var_status}" -ne 1 ]]; then
  exit "${var_status}"
fi

testng_query='attr(main_class, "org.testng.TestNG", kind(java_test, //...))'
testng_targets="$(./bazelw query "${testng_query}" --output=label)"
if [[ -z "${testng_targets}" ]]; then
  echo "No concrete TestNG targets were found" >&2
  exit 1
fi
non_small_testng_targets="$(./bazelw query \
  "${testng_query} except attr(size, \"small\", ${testng_query})" \
  --output=label)"
if [[ -n "${non_small_testng_targets}" ]]; then
  echo "Concrete TestNG targets must be sized small:" >&2
  echo "${non_small_testng_targets}" >&2
  exit 1
fi

./bazelw build //...
./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke
./bazelw build //client/src/test/gwt:all_gwt_assets
./bazelw test //...
./bazelw test //tools/release:all_tests --release_version=6.999
