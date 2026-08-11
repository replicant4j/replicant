# Replicant

Replicant is a Java library that atomically replicates subscribed portions of a server-side domain model to clients.

Before completing a code, build, or configuration change, run the narrowest affected Bazel target and then
`tools/check.sh` unless the user explicitly limits verification.

## Task Guidance

- [Build and Bazel](docs/agents/build-and-bazel.md) — Read before changing Bazel targets, dependencies, compiler
  gates, IntelliJ integration, or publication artifacts.
- [Java conventions](docs/agents/java.md) — Read before changing maintained Java or generated-code integration.
- [Architecture](docs/agents/architecture.md) — Read before changing APIs, protocols, transport, server boundaries,
  session state, or message encoding.
- [Testing and documentation](docs/agents/testing-and-documentation.md) — Read when fixing a bug or changing
  user-visible behavior, public APIs, workflows, or integration expectations.
- [Specialized workflows](docs/agents/specialized-workflows.md) — Read when changing domain language or semantics,
  client diagnostic-message fixtures, or release preparation.
