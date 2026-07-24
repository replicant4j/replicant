# Repository Guidelines

This guide captures the repo-specific rules and conventions for working effectively in the Replicant codebase.

## Non-negotiable Rules

1. When asked to perform a task, ask clarifying questions one at a time until you have enough context to proceed. Make reasonable assumptions when the codebase makes the answer clear, and ask the user to confirm when there are meaningful alternatives.

2. Keep `AGENTS.md` aligned with the codebase.
   - Update this file in the same change whenever build steps, module structure, generated-code paths, runtime flags, or architectural concepts change.
   - Remove stale references to deleted classes, endpoints, workflows, or dependencies instead of preserving compatibility lore.
   - Prefer concise, accurate guidance over exhaustive historical notes.

3. Prefer direct API evolution over compatibility shims.
   - Treat this repo as greenfield unless the user says otherwise.
   - When changing constructors, methods, or internal interfaces, update all production code and tests directly instead of adding overloads, chained constructors, default interface methods, or adapter layers solely to reduce churn.
   - Keep multiple constructors or overloads only when they are justified by the final design, not to preserve compatibility inside this repo.

4. When the user reports a bug, start by writing or updating a test that reproduces it. Then fix the bug and prove the fix with a passing test.

5. Do not use `glob()` in Bazel targets. List source files explicitly.

6. Every Java source directory owns its own `BUILD.bazel`. Bazel targets must not list source files from child,
   sibling, or parent directories.
   - Exception: `client/src/main/java/replicant/BUILD.bazel` owns the aggregate client library and may list
     sources and resources under `replicant.messages`, `replicant.react4j`, `replicant.spy`, and
     `replicant.spy.tools`.

7. Run `./bazelw run //:buildifier` after changing any `BUILD.bazel` file.

## Project Structure

- Java modules:
  - `client/` contains the GWT-enabled client runtime and JS interop code.
  - `server/` contains the CDI/WebSocket server transport and replication logic.
  - `shared/` contains constants and types shared by client and server.
- Build configuration:
  - `buildfile` defines the Buildr build.
  - `build.yaml` defines artifact coordinates and dependency versions.
  - `MODULE.bazel`, `.bazelrc`, `.bazelversion`, and `BUILD.bazel` define the parallel Bazel build.
  - `bazelw` runs Bazel 9.2.0 via Bazelisk.
  - `.github/workflows/ci.yml` defines the GitHub Actions CI workflow.
  - `third_party/java/dependencies.yml` is the depgen source for Bazel Java dependencies.
  - `tools/java-format/dependencies.yml` isolates the pinned Palantir Java formatter dependency graph.
  - `tools/intellij/.managed.bazelproject` is the shared IntelliJ Bazel project view.
  - `tools/release/` and `tools/package_maven_central.sh` define the Bazel release and Maven Central workflow.
  - `tasks/*.rake` contains the remaining legacy GWT, packaging, and diagnostic helper tasks.
  - `Gemfile` configures the Ruby/Buildr toolchain.
- Source layout:
  - Production code lives under `*/src/main/java/...`.
  - Tests live under `*/src/test/java/...`.
- Generated sources:
  - Bazel generates annotation processor outputs; generated Java remains untracked.
  - The client Maven main and source jars include generated Arez sources.
  - Do not hand-edit files under local generated-output directories.
- Documentation:
  - Keep `README.md`, `CHANGELOG.md`, and `AGENTS.md` aligned with user-visible or workflow-relevant changes.

### Module Notes

- `shared/`
  - Keep transport path fragments, shared constants, and message keys centralized here.
  - Example files: `shared/src/main/java/replicant/shared/SharedConstants.java`, `shared/src/main/java/replicant/shared/Messages.java`.
  - The public Bazel label is `//shared:shared_lib`; it aliases the package-owned
    `//shared/src/main/java/replicant/shared:shared_lib` target and is merged into the public client/server jars.
- `client/`
  - GWT modules live under `client/src/main/java/replicant/*.gwt.xml`.
  - JVM-only code must use the package-local `replicant.GwtIncompatible` annotation, or `replicant.messages.GwtIncompatible` inside the messages package.
  - Client and shared code must avoid `var`; use explicit local types.
  - The public Bazel output target is `//client:client`; `//client:client_lib` is also public for vendored downstream JVM wrappers.
  - The source-owned client library is `//client/src/main/java/replicant:client_lib`; `//client:client_lib`
    is a source-free public aggregate that exports it.
  - Bazel runs Arez, React4j, and Grim annotation processors for the JVM client compile.
- `server/`
  - The active transport is CDI + WebSocket + JSON-P (`javax.json`).
  - The public Bazel output target is `//server:server`; `//server:server_lib` is also public for vendored downstream JVM wrappers.
  - The WebSocket endpoint lives at `"/api" + SharedConstants.REPLICANT_URL_FRAGMENT`.
  - Runtime support shared by transport and EE lives under `server/src/main/java/replicant/server/runtime`.
    Keep this package below both `transport` and `ee`; `transport` must not import `replicant.server.ee`.
  - Session mutation is guarded by `ReplicantSession.getLock()`; follow the locking patterns in `server/src/main/java/replicant/server/transport/ReplicantSessionManagerImpl.java` and `server/src/main/java/replicant/server/transport/ReplicantMessageBrokerImpl.java`.
  - `ReplicantResources` exposes `replicant.server.runtime.ReplicantSystem` CDI producers for the transaction
    registry, managed executors, and broker tuning entries.
  - `ReplicantMessageBrokerImpl` uses demand-driven drain tasks submitted to the container-managed executor.
    Delayed retries and session maintenance use the container-managed scheduled executor. Broker runtime
    tuning comes from `java:comp/env` entries under `replicant/broker/*`; keep README details in sync when
    changing those knobs or managed executor JNDI names.

## Protocol and Hotspots

- Channel descriptor grammar: `channelId[.rootId][#filterInstanceId]`.
  - `#` is reserved; the filter instance id is the substring after the first `#` and may be empty.
  - No escaping is supported; JSON transport handles encoding.
- Instanced filter types:
  - `DYNAMIC_INSTANCED` requires `#` on subscribe, update, and unsubscribe, and allows filter updates.
  - `STATIC_INSTANCED` requires `#` on subscribe and unsubscribe, and rejects filter updates.
- Bulk subscribe and unsubscribe uses a shared filter for all addresses; the instance id lives on each `ChannelAddress`.

Implementation hotspots:

- Channel descriptor parsing and formatting:
  - `client/src/main/java/replicant/ChannelAddress.java`
  - `server/src/main/java/replicant/server/ChannelAddress.java`
- Client validation and AOI flow:
  - `client/src/main/java/replicant/Connector.java`
  - `client/src/main/java/replicant/Converger.java`
  - `client/src/main/java/replicant/SubscriptionService.java`
- Server validation and routing:
  - `server/src/main/java/replicant/server/ee/ReplicantEndpoint.java`
  - `server/src/main/java/replicant/server/transport/ReplicantSessionManagerImpl.java`
  - `server/src/main/java/replicant/server/transport/ReplicantSession.java`
- Change encoding:
  - `server/src/main/java/replicant/server/json/JsonEncoder.java`
  - `server/src/main/java/replicant/server/Change.java`
  - `server/src/main/java/replicant/server/ChangeSet.java`

## Coding Conventions

- Write for readability first. Prefer simple, direct code over clever abstractions.
- Keep naming, formatting, and architecture consistent with nearby code.
- Use the narrowest practical visibility. Helpers are typically `final` and package-private unless they are part of the public API.
- Use comments sparingly and explain why, not what, when the code is otherwise hard to understand.

### Java Conventions

- The build targets Java 17 and compiles with strict Error Prone,
  `-Xlint:all,-processing,-serial,-path,-options,-classfile,-this-escape`, and `-Werror`. Fix findings or suppress
  them narrowly with justification.
- Mark every maintained Java package with JSpecify `@NullMarked`, and use JSpecify `@NonNull` and `@Nullable` for
  exceptions to the package default.
- `javax.annotation` remains only for server EE lifecycle/resource annotations and as a build-time dependency for
  the legacy nullness imports emitted by the current Arez processor. Do not use its nullness annotations in
  maintained source.
- On JAX-RS-style validation boundaries, prefer `@NotNull` from `javax.validation` when that style is already in use. Do not add new REST-specific guidance to this file unless the codebase adds REST resources again.
- Use `final` where practical.
- Use `final var` for local variables in server production/tests and JVM-only tooling unless Java requires an
  explicit type or the explicit type materially improves clarity.
- Never use `var` in client/shared production or tests; use explicit local types there.
- Public API changes should include matching Javadoc updates. Keep package documentation in `package-info.java` aligned with the code.

### Generated Code

- Do not hand-edit or commit files under `client/generated/processors/main/java/...`.
- Bazel owns annotation processor output and regenerates it from maintained sources.
- NullAway treats `@Generated` processor output as unannotated because Arez 0.254 still emits legacy
  `javax.annotation` nullness metadata.

## Build and Test

Prerequisites: JDK 17+ for Bazel. The remaining legacy Buildr/GWT workflow also requires Ruby 2.7.x and Bundler.

CI workflow:

- GitHub Actions runs Bazel, not Buildr.
- CI installs Temurin JDK 17, builds the public Bazel jars, runs all Bazel tests, and checks Bazel formatting.

- Bazel is pinned to 9.2.0 via `.bazelversion`; run it with `./bazelw`.
- Run the current repository gate: `tools/check.sh`.
- Build public Bazel jars: `./bazelw build //client:client //server:server`.
- Build Maven publication artifacts: `./bazelw build //tools/release:maven_artifacts`.
- Run all Bazel tests, including depgen hash verification: `./bazelw test //...`.
- Check that every maintained Java package is explicitly null-marked: `tools/check_null_marked_packages.sh`.
- Check Bazel formatting: `./bazelw run //:buildifier_check`.
- Apply Bazel formatting: `./bazelw run //:buildifier`.
- Check Java formatting: `tools/java_format.sh check`.
- Apply Java formatting: `tools/java_format.sh write`.
- Import `tools/intellij/.managed.bazelproject` with IntelliJ IDEA's Bazel plugin; do not recreate legacy
  `.ipr`, `.iml`, or `.iws` project metadata.

Buildr workflow:

- Bootstrap once: `bundle install`.
- Build all modules locally when specifically validating the remaining legacy GWT workflow:
  `bundle exec buildr clean package`.
- Run the Buildr test path when specifically validating Buildr behavior: `bundle exec buildr test`.

Additional build notes:

- Bazel is the primary JVM build, test, packaging, and release system.
- GWT support is wired via `tasks/gwt.rake` and `gwt_enhance(project)` in `buildfile`.
- Test JVM properties default to development settings for Braincheck, Arez, Replicant, and Zemeckis.
- The Bazel build does not yet replace the Buildr GWT compiler tasks.
- Bazel Java dependencies are generated by bazel-depgen from `third_party/java/dependencies.yml` and
  `tools/java-format/dependencies.yml`.
- Keep depgen options using ArtifactId naming and module/build generation. After changing either dependency file,
  run `tools/update_java_deps.sh` to regenerate the Bazel outputs and Bzlmod lockfile, then run
  `./bazelw test //third_party/java:verify_config_sha256`.
- The Bazel public output jars merge `//shared:shared_lib` into `//client:client` and `//server:server`; third-party jars remain separate.
- `//client:client_lib` is a source-free aggregate that exports the source-owned client library under
  `client/src/main/java/replicant`.
- `//server:server_lib` is a source-free aggregate that exports package-owned server libraries under
  `server/src/main/java/replicant/server/**`; keep the package graph acyclic when adding dependencies.
- Bazel uses remote JDK 25 for Java build tools and emits Java 17 bytecode for project targets.
- The Maven targets under `//tools/release` build client/server main, source, Javadoc, and POM artifacts. Published
  jars exclude `BUILD.bazel`; the client main and source jars include Bazel-generated Arez Java.

### Testing Guidelines

- Test framework: TestNG across modules.
- Place tests under `*/src/test/java/...`.
- Name tests with the `Test` suffix.
- Run `tools/check.sh` before submitting changes unless the user explicitly asks you not to.
- Bazel exposes one `java_testng` target per concrete TestNG test class; name the target after its source file without
  the `.java` suffix.
- Client and server `java_testng` targets live in the test source directory that owns each test class.
- `client/src/test/java/replicant/AbstractReplicantTest.java` is abstract support code and belongs in
  `//client/src/test/java/replicant:client_test_support_lib`; `//client:client_test_support` is a source-free
  aggregate for client tests.
- Client per-class Bazel tests disable diagnostic fixture comparison.
  `//client/src/test/java/replicant:client_diagnostic_messages_test` runs the concrete client suite with the
  diagnostic fixture check enabled.

Diagnostics fixtures and invariants:

- Client tests validate diagnostic messages via `MessageCollector` and fixtures in `client/src/test/java/replicant/diagnostic_messages.json`.
- Relevant properties:
  - `replicant.check_diagnostic_messages` verifies emitted diagnostics against the fixture file.
  - `replicant.output_fixture_data` rewrites fixture data when expected outputs intentionally change.
  - `replicant.diagnostic_messages_file` points at the fixture JSON file. Bazel test targets pass this via
    `$(rootpath ...)` so the path works when Replicant is run as the main workspace or as a vendored external repo.
- In IntelliJ, use the generated TestNG configurations such as `client - update invariant messages` when fixture updates are intentional.

## Runtime and Transport Notes

- Runtime and GWT properties are defined in `client/src/main/java/replicant/ReplicantConfig.java` and `client/src/main/java/replicant/Replicant.gwt.xml`.
- Key properties include:
  - `replicant.environment`
  - `replicant.check_invariants`
  - `replicant.check_api_invariants`
  - `replicant.enable_names`
  - `replicant.enable_zones`
  - `replicant.enable_spies`
  - `replicant.validateChangeSetOnRead`
  - `replicant.validateEntitiesOnLoad`
  - `replicant.logger`
- Server broker runtime entries include:
  - `java:replicant/concurrent/ManagedExecutorService`
  - `java:replicant/concurrent/ManagedScheduledExecutorService`
  - `replicant/broker/maxConcurrentDrainTasks`
  - `replicant/broker/maxPacketsPerRun`
  - `replicant/broker/maxSessionsPerDrainTask`
- Keep transport routes and message formats in sync with shared constants and message keys.
- Prefer JSON-P builders and generators for message encoding rather than ad-hoc string concatenation.

## Commit and Release Guidance

- Follow `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.
- Keep commits small, focused, and imperative.
- Update `CHANGELOG.md` for user-visible changes.
- Update `README.md` when public APIs, workflows, or integration expectations change.
- Follow `tools/release/README.md` for the split or end-to-end release workflow.
- Maven Central uses `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`. GPG signing uses `GPG_USER` and optional
  `GPG_PASS`.
- Build and sign a Maven Central bundle with `tools/package_maven_central.sh <version>`.
