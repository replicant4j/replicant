#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "Usage: capture_buildr_baseline.sh <buildr-worktree> <output-directory>" >&2
  exit 1
fi

SOURCE_ROOT="$1"
OUTPUT_ROOT="$2"
VERSION="6.999"

mkdir -p "${OUTPUT_ROOT}"

{
  echo "baseline_commit=d019d5e5f2c8cff0e625b1171514dcee71bb7b2e"
  echo "capture_date=2026-07-24"
  echo "artifact_version=${VERSION}"
  echo "artifact_command=bundle exec buildr clean package PRODUCT_VERSION=${VERSION} GWT=skip TEST=no"
  java -version 2>&1 | sed 's/^/java=/'
  ruby --version | sed 's/^/ruby=/'
  bundle --version | sed 's/^/bundler=/'
  (
    cd "${SOURCE_ROOT}"
    bundle exec buildr --version
  ) | sed 's/^/buildr=/'
} > "${OUTPUT_ROOT}/capture.properties"

for artifact in replicant-client replicant-server; do
  module="${artifact#replicant-}"
  artifact_dir="${SOURCE_ROOT}/target/replicant_${module}"
  for classifier in main sources javadoc; do
    if [[ "${classifier}" == "main" ]]; then
      jar_file="${artifact_dir}/${artifact}-${VERSION}.jar"
    else
      jar_file="${artifact_dir}/${artifact}-${VERSION}-${classifier}.jar"
    fi
    LC_ALL=C jar tf "${jar_file}" | LC_ALL=C sort > "${OUTPUT_ROOT}/${artifact}-${classifier}.entries"
    shasum -a 256 "${jar_file}" | sed "s#${jar_file}#${artifact}-${classifier}.jar#" \
      > "${OUTPUT_ROOT}/${artifact}-${classifier}.sha256"
  done
  unzip -p "${artifact_dir}/${artifact}-${VERSION}.jar" META-INF/MANIFEST.MF \
    | tr -d '\r' \
    | sed -e 's/[[:blank:]]*$//' -e '/^$/d' \
    > "${OUTPUT_ROOT}/${artifact}.manifest"
  cp "${artifact_dir}/${artifact}-${VERSION}.pom" "${OUTPUT_ROOT}/${artifact}.pom"
done
