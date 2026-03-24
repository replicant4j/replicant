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

## Project Structure

- Java modules:
  - `client/` contains the GWT-enabled client runtime and JS interop code.
  - `server/` contains the CDI/WebSocket server transport and replication logic.
  - `shared/` contains constants and types shared by client and server.
- Build configuration:
  - `buildfile` defines the Buildr build.
  - `build.yaml` defines artifact coordinates and dependency versions.
  - `tasks/*.rake` contains GWT, release, packaging, and diagnostic helper tasks.
  - `Gemfile` configures the Ruby/Buildr toolchain.
- Source layout:
  - Production code lives under `*/src/main/java/...`.
  - Tests live under `*/src/test/java/...`.
- Generated sources:
  - Annotation processor outputs checked in for GWT compatibility live under `client/generated/processors/main/java/...`.
  - Do not hand-edit generated sources.
- Documentation:
  - Keep `README.md`, `CHANGELOG.md`, and `AGENTS.md` aligned with user-visible or workflow-relevant changes.

### Module Notes

- `shared/`
  - Keep transport path fragments, shared constants, and message keys centralized here.
  - Example files: `shared/src/main/java/replicant/shared/SharedConstants.java`, `shared/src/main/java/replicant/shared/Messages.java`.
- `client/`
  - GWT modules live under `client/src/main/java/replicant/*.gwt.xml`.
  - JVM-only code must use the package-local `replicant.GwtIncompatible` annotation, or `replicant.messages.GwtIncompatible` inside the messages package.
  - Client and shared code must avoid `var`; use explicit local types.
- `server/`
  - The active transport is CDI + WebSocket + JSON-P (`javax.json`).
  - The WebSocket endpoint lives at `"/api" + SharedConstants.REPLICANT_URL_FRAGMENT`.
  - Session mutation is guarded by `ReplicantSession.getLock()`; follow the locking patterns in `server/src/main/java/replicant/server/transport/ReplicantSessionManagerImpl.java` and `server/src/main/java/replicant/server/transport/ReplicantMessageBrokerImpl.java`.

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

- The build targets Java 17 and compiles with `-Xlint:all,-processing,-serial` and `-Werror`. Fix warnings or suppress them narrowly with justification.
- Use `@Nonnull` and `@Nullable` from `javax.annotation` for nullability.
- On JAX-RS-style validation boundaries, prefer `@NotNull` from `javax.validation` when that style is already in use. Do not add new REST-specific guidance to this file unless the codebase adds REST resources again.
- Use `final` where practical.
- Use `final var` for local variables in `server/` code and under `*/src/test/java/...` unless Java requires an explicit type or the explicit type materially improves clarity.
- Never use `var` in `client/` or `shared/`; use explicit local types there.
- Public API changes should include matching Javadoc updates. Keep package documentation in `package-info.java` aligned with the code.

### Generated Code

- Do not hand-edit files under `client/generated/processors/main/java/...`.
- If annotation processor output changes, regenerate or update the generated sources consistently with the source change that required it.

## Build and Test

Prerequisites: JDK 17+, Ruby 2.7.x, and Bundler.

- Bootstrap once: `bundle install`
- Build all modules: `bundle exec buildr clean package`
- Run all tests: `bundle exec buildr test`

Additional build notes:

- The build is managed via Buildr; update `build.yaml` when adding or upgrading dependencies.
- GWT support is wired via `tasks/gwt.rake` and `gwt_enhance(project)` in `buildfile`.
- Test JVM properties default to development settings for Braincheck, Arez, and Replicant.

### Testing Guidelines

- Test framework: TestNG across modules.
- Place tests under `*/src/test/java/...`.
- Name tests with the `Test` suffix.
- Run `bundle exec buildr test` before submitting changes unless the user explicitly asks you not to.

Diagnostics fixtures and invariants:

- Client tests validate diagnostic messages via `MessageCollector` and fixtures in `client/src/test/java/replicant/diagnostic_messages.json`.
- Relevant properties:
  - `replicant.check_diagnostic_messages` verifies emitted diagnostics against the fixture file.
  - `replicant.output_fixture_data` rewrites fixture data when expected outputs intentionally change.
  - `replicant.diagnostic_messages_file` points at the fixture JSON file.
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
- Keep transport routes and message formats in sync with shared constants and message keys.
- Prefer JSON-P builders and generators for message encoding rather than ad-hoc string concatenation.

## Commit and Release Guidance

- Follow `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md`.
- Keep commits small, focused, and imperative.
- Update `CHANGELOG.md` for user-visible changes.
- Update `README.md` when public APIs, workflows, or integration expectations change.
- Release-related environment variables include `PRODUCT_VERSION` and `PREVIOUS_PRODUCT_VERSION`.
- Publishing to Maven Central uses `tasks/package_for_maven_central.rake` and requires `.netrc` credentials for `central.sonatype.com` plus GPG configured via Buildr.
