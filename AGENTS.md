# Repository Guidelines

This guide helps contributors work effectively on the Replicant codebase.

## User Interaction

When asked to perform a task, ask the user questions one at a time until you have enough context. Feel free to make
reasonable assumptions based on patterns present in the code and ask the user to confirm the assumptions if there are
reasonable alternatives.

## Project Structure & Module Organization

- Java modules: `client/` (client-side code), `server/` (server-side code) and `shared/` (code used on both the server-side and client-side).
- Build configuration: `buildfile` (Buildr), `tasks/*.rake` (CI/site tasks).
- Source layout: `*/src/main/java/...`; tests: `*/src/test/java/...`.
- Generated binaries and build artifacts should stay out of version control and should stay untouched unless you are
  troubleshooting a local build.
- Keep `README.md` aligned with new features so downstream teams stay informed.

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

## Build, Test, and Development Commands

Prerequisites: JDK 17+, Ruby 2.7.x with Bundler, Node.js (for docs site) and Yarn.

- Bootstrap once: `bundle install`.
- Build all modules: `bundle exec buildr clean package`.
- Run tests: `bundle exec buildr test`.

## Coding Style & Naming Conventions

- Language: Java 17; compilation uses `-Xlint:all` and `-Werror` (warnings must be fixed).
- Indentation: 2 spaces; braces on a new line for types/methods; keep imports ordered and minimal.
- Annotations: prefer `@Nonnull`/`@Nullable`; use `final` where practical.
- Naming: packages lowercase (`replicant.*`), classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Public API must include Javadoc; keep package-level docs in `package-info.java`.

## Testing Guidelines

- Framework: TestNG across modules.
- Location: place tests under `*/src/test/java`.
- Naming: suffix unit tests with `Test`.
- Run all tests with `bundle exec buildr test` before submitting.

## Commit & Pull Request Guidelines

- Follow `CONTRIBUTING.md` and the Code of Conduct.
- Commits: small, focused, imperative subject; reference issues where relevant; update `CHANGELOG.md` for user-visible changes.
- PRs: include a clear description, linked issues, tests for behavior, and docs updates if APIs change. Add screenshots or generated artifacts when helpful.

## Security & Configuration Tips (Optional)

- Never commit secrets.
- Release-related env vars: `PRODUCT_VERSION`, `PREVIOUS_PRODUCT_VERSION`.
