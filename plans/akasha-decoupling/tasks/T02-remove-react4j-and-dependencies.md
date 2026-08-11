# T02 — Remove React4j and dependency wiring

- Status: `complete`
- Blocked by: `T01`
- Spec coverage: `R2`, `R4`, `R6`; `AC1`, `AC2`, `AC5`

## Delivers

The public React4j adapter and module are gone, eliminating the final transitive Akasha compile path. Client, GWT,
J2CL, dependency input/generated output, release, and publication wiring contain neither React4j nor Akasha.

## Acceptance criteria

- [x] `AreaOfInterestView`, the React4j GWT module, and their GWT/J2CL smoke inputs are removed without removing the
      binding-owner linker roots added by `T01`.
- [x] Client targets no longer use the React4j annotation processor or depend on React4j/Akasha.
- [x] Java dependency inputs are regenerated so active generated Bazel/module declarations contain neither library.
- [x] Client source/Javadoc jars and POM contain no React4j module, Akasha classpath entry, or Akasha/React4j coordinate.
- [x] README build coverage describes the three remaining real GWT variants.

## Validation

- `tools/update_java_deps.sh` — regenerates dependency outputs from the authoritative input.
- `./bazelw test //third_party/java:verify_config_sha256` — proves dependency input/generated-output consistency.
- `./bazelw run //:buildifier` — formats all changed Bazel files.
- `./bazelw build //client/src/test/gwt:all_gwt_assets` — proves every remaining GWT module compiles without the removed integration.
- `./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke` — proves the aggregate J2CL graph is independent of the removed integration.
- `./bazelw build //tools/release:maven_artifacts` — proves release artifacts build without either dependency.
- Inspect generated `replicant-client.pom` and active dependency files for Akasha/React4j coordinates — proves publication and compile graph removal.

## Evidence

- `tools/update_java_deps.sh` — passed; regenerated `MODULE.bazel` and the depgen section of
  `third_party/java/BUILD.bazel` without Akasha, React4j, or their exclusive javaemul dependency.
- `./bazelw test //third_party/java:verify_config_sha256` — passed.
- `./bazelw run //:buildifier` and `tools/java_format.sh check` — passed.
- `./bazelw build //client/src/test/gwt:all_gwt_assets` — passed for Replicant, ReplicantDev, and ReplicantDebug.
- `./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke` — passed with no Akasha sanitizer/transpile or
  React4j action in the build.
- `./bazelw build //tools/release:maven_artifacts` — all eight artifacts built successfully; pre-existing Javadoc
  warnings remained non-fatal.
- Generated client POM and client main/source jar audits found no Akasha or React4j coordinate, module, or class.
- Active configuration audit found no Akasha/React4j reference in `MODULE.bazel`, dependency input/generated targets,
  client targets, or release configuration. Remaining Akasha references are confined to the rewrite removed by `T03`.
- `git diff --check` and task diff inspection — passed; generated and hand-authored removals match the task scope.
