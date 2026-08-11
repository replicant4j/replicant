# Build and Bazel

Follow the [Build section of the README](../../README.md#build) and the scripts it names for build targets,
dependency regeneration, compiler gates, IntelliJ integration, and publication artifacts.

## Bazel Constraints

- List sources and resources explicitly; do not use `glob()`.
- Each Java source directory owns its `BUILD.bazel`. A target must not list files from a parent, child, or sibling
  directory. The exception is `client/src/main/java/replicant/BUILD.bazel`, whose aggregate client library may list
  files under `replicant.messages`, `replicant.react4j`, `replicant.spy`, and `replicant.spy.tools`.
- Define one `java_testng` target per concrete TestNG test class in the test source directory that owns it. Name the
  target after the source file without `.java`.
- After changing a `BUILD.bazel`, run `./bazelw run //:buildifier`.
