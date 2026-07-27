# Replicant Agent Guidance

Within this repository, make direct API and protocol changes and update every production and test caller. Add
compatibility only when the user explicitly requires downstream compatibility.

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
  nullness annotations unless an adjacent comment documents a processor compatibility workaround.
- Prefer `final` when bindings are not reassigned. Never use `var` in client/shared production or tests.
- JVM-only client code must use the package-local `replicant.GwtIncompatible` annotation, or
  `replicant.messages.GwtIncompatible` inside the messages package.
- Limit `javax.annotation` in maintained source to Java EE annotations and documented processor compatibility
  workarounds.
- Update Javadoc for public API changes and keep package documentation aligned with the code.

### Architecture

- Keep shared transport path fragments, constants, and message keys in `shared/`.
- Code shared by `server.transport` and `server.ee` belongs in `server.runtime`; transport code must not depend on
  `server.ee`.
- Guard session mutation with `ReplicantSession.getLock()` and follow the locking patterns in
  `ReplicantSessionManagerImpl` and `ReplicantMessageBrokerImpl`.
- Keep client and server transport routes, validation, and message formats synchronized through shared constants and
  message keys. Use JSON-P builders and generators for server-side JSON encoding.

## Context Pointers

- Before changing Replicant domain language or semantics, read `docs/glossary/README.md` and the Core Concepts section
  of `README.md`. The glossary is canonical for terminology, the README explains concepts, and code and tests define
  operational behavior.
- For build targets, dependency regeneration, compiler gates, IntelliJ integration, or publication artifacts, follow
  the Build section of `README.md` and the scripts it names.
- For client diagnostic-message fixtures, inspect `MessageCollector`,
  `client/src/test/java/replicant/diagnostic_messages.json`, and `client/src/test/java/replicant/BUILD.bazel`.
- For release preparation or Maven Central publication, follow `tools/release/README.md`.

## Verification and Documentation

- Run the narrowest affected Bazel target while developing. Before completing a code, build, or configuration change,
  run the full repository gate with `tools/check.sh` unless the user explicitly limits verification.
- Update `CHANGELOG.md` for user-visible changes and `README.md` when public APIs, workflows, or integration
  expectations change.
- Update this file only when these constraints or context pointers change; keep detailed architecture and workflow
  reference in their owning documentation.
