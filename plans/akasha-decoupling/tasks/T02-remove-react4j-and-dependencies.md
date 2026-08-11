# T02 — Remove React4j and dependency wiring

- Status: `pending`
- Blocked by: `T01`
- Spec coverage: `R2`, `R4`, `R6`; `AC1`, `AC2`, `AC5`

## Delivers

The public React4j adapter and module are gone, eliminating the final transitive Akasha compile path. Client, GWT,
J2CL, dependency input/generated output, release, and publication wiring contain neither React4j nor Akasha.

## Acceptance criteria

- [ ] `AreaOfInterestView`, the React4j GWT module, and their GWT/J2CL smoke inputs are removed without removing the
      binding-owner linker roots added by `T01`.
- [ ] Client targets no longer use the React4j annotation processor or depend on React4j/Akasha.
- [ ] Java dependency inputs are regenerated so active generated Bazel/module declarations contain neither library.
- [ ] Client source/Javadoc jars and POM contain no React4j module, Akasha classpath entry, or Akasha/React4j coordinate.
- [ ] README build coverage describes the three remaining real GWT variants.

## Validation

- `tools/update_java_deps.sh` — regenerates dependency outputs from the authoritative input.
- `./bazelw test //third_party/java:verify_config_sha256` — proves dependency input/generated-output consistency.
- `./bazelw run //:buildifier` — formats all changed Bazel files.
- `./bazelw build //client/src/test/gwt:all_gwt_assets` — proves every remaining GWT module compiles without the removed integration.
- `./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke` — proves the aggregate J2CL graph is independent of the removed integration.
- `./bazelw build //tools/release:maven_artifacts` — proves release artifacts build without either dependency.
- Inspect generated `replicant-client.pom` and active dependency files for Akasha/React4j coordinates — proves publication and compile graph removal.

## Evidence

- `pending`
