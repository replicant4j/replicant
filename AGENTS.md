# Repository Guidelines

This guide helps contributors work effectively on the Replicant codebase.

## User Interaction

When asked to perform a task, ask the user questions one at a time until you have enough context. Feel free to make
reasonable assumptions based on patterns present in the code and ask the user to confirm the assumptions if there are
reasonable alternatives.

## Project Structure & Module Organization

- Java modules: `client/` (client-side code), `server/` (server-side code) and `shared/` (code used on both the server-side and client-side).
- Build configuration: `buildfile` (Buildr), `tasks/*.rake` (release, GWT support, packaging), `build.yaml` (artifact coordinates), `Gemfile` (Buildr plugins).
- Source layout: `*/src/main/java/...`; tests: `*/src/test/java/...`.
- Generated binaries and build artifacts should stay out of version control and should stay untouched unless you are troubleshooting a local build.
- Keep `README.md` aligned with new features so downstream teams stay informed.

### Module-specific notes

- `shared/`
  - Pure Java constants and shared types used by both client and server. Example: `shared/src/main/java/replicant/shared/SharedConstants.java`.
  - Keep URL path fragments and message keys centralized here so client/server stay consistent.
- `client/`
  - GWT-enabled client runtime with JS interop. GWT modules are defined under `client/src/main/java/replicant/*.gwt.xml` (e.g. `Replicant.gwt.xml`, `ReplicantDev.gwt.xml`, `ReplicantDebug.gwt.xml`).
  - Uses annotation processors (Arez, React4j, Grim). Pre-generated outputs are checked in under `client/generated/processors/...` to support GWT; do not hand-edit these files.
  - Mark JVM-only code with `replicant.GwtIncompatible` (or `replicant.messages.GwtIncompatible` within the messages package) to keep GWT builds clean.
- `server/`
  - Java EE/Jakarta EE style server with CDI, JAX-RS, WebSocket, and JSON-P (javax.json). WebSocket endpoint lives at `"/api" + SharedConstants.REPLICANT_URL_FRAGMENT` and REST resources are rooted at `SharedConstants.CONNECTION_URL_FRAGMENT`.
  - Guard session access with the session lock pattern (see `server/src/main/java/replicant/server/ee/rest/ReplicantSessionRestService.java:110`).

## General Principles

- Readability: Write code that is easy to read and understand. Prioritize clarity over overly clever or obscure
  solutions.
- Consistency: Strive for consistency in naming, formatting, and architectural patterns throughout the project.
- Simplicity (KISS): Keep It Simple, Stupid. Avoid unnecessary complexity.
- Don't Repeat Yourself (DRY): Avoid code duplication. Utilize functions, classes, and reusable components.
- Commenting:
    - Comment code that is complex, non-obvious, or critical.
    - Explain why something is done, not just what is being done (if the what is clear from the code).
    - Keep comments up-to-date with code changes.
- Modularity: Design components to be as self-contained and reusable as possible.
- Performance: Be mindful of performance implications, especially for real-time operations. Profile and optimize
  critical code paths.
- Error Handling: Implement robust error handling and provide clear feedback to users or logs when errors occur.

## Derived Conventions

- Nullability:
  - Prefer `@Nonnull`/`@Nullable` from `javax.annotation` for general code.
  - On JAX-RS resources, also use `@NotNull` from `javax.validation` for parameter validation.
- Immutability and locals:
  - Use `final` where practical. Local variables commonly use `final var` for readability when the type is obvious (Java 17).
- Warnings and lint:
  - Compilation runs with `-Xlint:all,-processing,-serial` and `-Werror` (see `buildfile:15-19`). Fix warnings or explicitly and narrowly suppress with justification.
- Package visibility:
  - Keep visibility as small as practical. Many helpers are `final` and/or package-private unless part of the public API.

## Build, Test, and Development Commands

Prerequisites: JDK 17+, Ruby 2.7.x with Bundler, Node.js (for docs site) and Yarn.

- Bootstrap once: `bundle install`.
- Build all modules: `bundle exec buildr clean package`.
- Run tests: `bundle exec buildr test`.

Additional details derived from the build:

- The build is managed via Buildr with artifacts declared in `build.yaml`. Update that file when adding/upgrading dependencies.
- GWT support is wired via `tasks/gwt.rake` and `gwt_enhance(project)` in `buildfile`.
- Test JVM properties default to development settings for Braincheck/Arez/Replicant (see `buildfile:104-121`).

## Coding Style & Naming Conventions

- Language: Java 17; compilation uses `-Xlint:all` and `-Werror` (warnings must be fixed).
- Indentation: 2 spaces; braces on a new line for types/methods; keep imports ordered and minimal.
- Annotations: prefer `@Nonnull`/`@Nullable`; use `final` where practical.
- Naming: packages lowercase (`replicant.*`), classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Public API must include Javadoc; keep package-level docs in `package-info.java`.
- GWT compatibility: annotate JVM-only code with `replicant.GwtIncompatible` (or package-local variant) and keep JS interop types (`@JsType`, `@JsMethod`) isolated as needed.

## Testing Guidelines

- Framework: TestNG across modules.
- Location: place tests under `*/src/test/java`.
- Naming: suffix unit tests with `Test`.
- Run all tests with `bundle exec buildr test` before submitting.

Diagnostics fixtures and invariants:

- Client tests validate diagnostic messages via `MessageCollector` and fixtures in `client/src/test/java/replicant/diagnostic_messages.json`.
- Properties controlling this behaviour (see `buildfile:115-121, 141-146` and `client/src/test/java/replicant/MessageCollector.java:12`):
  - `replicant.check_diagnostic_messages` (true/false) — verify messages match fixtures.
  - `replicant.output_fixture_data` (true/false) — rewrite fixtures when messages change.
  - `replicant.diagnostic_messages_file` — path to fixture JSON.
- In IntelliJ, use the generated TestNG configurations (e.g. “client - update invariant messages”) to refresh fixtures when expected outputs legitimately change.

## Commit & Pull Request Guidelines

- Follow `CONTRIBUTING.md` and the Code of Conduct.
- Commits: small, focused, imperative subject; reference issues where relevant; update `CHANGELOG.md` for user-visible changes.
- PRs: include a clear description, linked issues, tests for behavior, and docs updates if APIs change. Add screenshots or generated artifacts when helpful.

## Security & Configuration Tips (Optional)

- Never commit secrets.
- Release-related env vars: `PRODUCT_VERSION`, `PREVIOUS_PRODUCT_VERSION`.
- Publishing to Maven Central uses `tasks/package_for_maven_central.rake`; requires a `.netrc` entry for `central.sonatype.com` and GPG configured via Buildr.
- The secured REST endpoints can be disabled for local development via the JNDI flag `replicant/env/disable_session_service_protection` (see `server/src/main/java/replicant/server/ee/rest/SecuredReplicantSessionRestService.java:18`). Do not disable this in shared environments.

## Client Runtime Configuration

- Runtime/system properties (JVM) and GWT properties control behaviour (see `client/src/main/java/replicant/ReplicantConfig.java` and `client/src/main/java/replicant/Replicant.gwt.xml`):
  - `replicant.environment` — `production` or `development`.
  - `replicant.check_invariants`, `replicant.check_api_invariants` — enable invariant checks.
  - `replicant.enable_names`, `replicant.enable_zones`, `replicant.enable_spies` — feature toggles.
  - `replicant.validateChangeSetOnRead`, `replicant.validateEntitiesOnLoad` — additional validations.
  - `replicant.logger` — `console`, `proxy`, or `none` (JVM production default effectively disables logging). Use `console` for human-readable logs or `proxy` in tests.

## Generated Code

- The repository includes generated sources under `client/generated/processors/...` produced by annotation processors (Arez/React4j/Grim) for GWT compatibility.
- Do not hand-edit generated code. If generators change API, update the generated sources consistently or re-run the processors and refresh committed outputs.

## Server Endpoints & JSON

- Keep WebSocket and REST routes in sync with `shared` constants to avoid drift:
  - WebSocket endpoint: `@ServerEndpoint("/api" + SharedConstants.REPLICANT_URL_FRAGMENT)`.
  - REST base path: `@Path(SharedConstants.CONNECTION_URL_FRAGMENT)`.
- Prefer JSON-P (`javax.json`) builders/generators (`JsonGeneratorFactory`) for encoding responses and messages (see `server/src/main/java/replicant/server/ee/rest/ReplicantSessionRestService.java:25`). Avoid ad-hoc string concatenation.
