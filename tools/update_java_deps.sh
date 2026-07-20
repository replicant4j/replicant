#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BAZEL="${ROOT}/bazelw"
VERSION="0.28"
URL="https://repo.maven.apache.org/maven2/org/realityforge/bazel/depgen/bazel-depgen/${VERSION}/bazel-depgen-${VERSION}-all.jar"
OUTPUT_BASE="$(cd "${ROOT}" && "${BAZEL}" info output_base)"
TOOLS_DIR="${OUTPUT_BASE}/.depgen-tools"
CACHE_DIR="${OUTPUT_BASE}/.depgen-cache"
JAR="${TOOLS_DIR}/bazel-depgen-${VERSION}-all.jar"

mkdir -p "${TOOLS_DIR}" "${CACHE_DIR}"

if [[ ! -f "${JAR}" ]]; then
  tmp="${JAR}.tmp"
  curl -fsSL -o "${tmp}" "${URL}"
  mv "${tmp}" "${JAR}"
fi

cd "${ROOT}"
java -jar "${JAR}" \
  --directory "${ROOT}" \
  --config-file third_party/java/dependencies.yml \
  --cache-directory "${CACHE_DIR}" \
  generate
"${BAZEL}" mod deps --lockfile_mode=update
"${BAZEL}" run //third_party/java:update_depgen_generated_outputs
"${BAZEL}" mod deps --lockfile_mode=update
"${BAZEL}" run //:buildifier -- MODULE.bazel third_party/java/BUILD.bazel
