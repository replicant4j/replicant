# Replicant Agent Guidance

Treat this repository as greenfield despite Replicant being a library. Make direct API changes and update every
production and test caller; retain compatibility only when the user explicitly requires it.

For a reported bug, first add or update the narrowest practical test that reproduces it. Fix the root cause and prove
the test passes.

## Hard Constraints

### Bazel

- Do not use `glob()` in Bazel targets. List sources and resources explicitly.
- Every Java source directory owns its own `BUILD.bazel`; targets must not list files from parent, child, or sibling
  directories. The exception is `client/src/main/java/replicant/BUILD.bazel`, which owns the aggregate client library
  and may list files under `replicant.messages`, `replicant.react4j`, `replicant.spy`, and `replicant.spy.tools`.
- Define one `java_testng` target per concrete TestNG test class in the test source directory that owns it. Name the
  target after the source file without `.java`.
- After changing a `BUILD.bazel` file, run `./bazelw run //:buildifier`.

### Java and Generated Code

- Every maintained Java package must have a `package-info.java` marked with JSpecify `@NullMarked`. Use JSpecify
  `@NonNull` and `@Nullable` for exceptions to the package default.
- Use `final var` for local variables in server production/tests and JVM-only tooling unless Java requires an explicit
  type or the type materially improves clarity. Never use `var` in client/shared production or tests.
- JVM-only client code must use the package-local `replicant.GwtIncompatible` annotation, or
  `replicant.messages.GwtIncompatible` inside the messages package.
- Use `javax.annotation` only for server EE lifecycle/resource annotations and the legacy nullness imports produced by
  the current Arez processor. Do not introduce its nullness annotations in maintained source.
- Update Javadoc for public API changes and keep package documentation aligned with the code.

### Architecture

- Keep shared transport path fragments, constants, and message keys in `shared/`.
- Keep `server.runtime` below both `server.transport` and `server.ee`; transport code must not import `server.ee`.
- Guard session mutation with `ReplicantSession.getLock()` and follow the locking patterns in
  `ReplicantSessionManagerImpl` and `ReplicantMessageBrokerImpl`.
- Keep client and server transport routes, validation, and message formats synchronized through shared constants and
  message keys. Use JSON-P builders and generators for JSON encoding.

## Context Pointers

- Before changing Replicant domain language or semantics, read `docs/glossary/README.md` and the Core Concepts section
  of `README.md`. Treat those documents and the code as the source of truth; locate current implementation hotspots
  with `rg`.
- For build targets, dependency regeneration, compiler gates, IntelliJ integration, or publication artifacts, follow
  the Build section of `README.md` and the scripts it names.
- For client diagnostic-message fixtures, inspect `MessageCollector`,
  `client/src/test/java/replicant/diagnostic_messages.json`, and the owning test `BUILD.bazel`.
- For release preparation or Maven Central publication, follow `tools/release/README.md`.

## Verification and Documentation

- Run the narrowest affected Bazel target while developing. Before completing a code, build, or configuration change,
  run the full repository gate with `tools/check.sh` unless the user explicitly limits verification.
- Update `CHANGELOG.md` for user-visible changes and `README.md` when public APIs, workflows, or integration
  expectations change.
- Update this file only when these constraints or context pointers change; keep detailed architecture and workflow
  reference in their owning documentation.
