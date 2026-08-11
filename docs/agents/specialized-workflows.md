# Specialized Workflows

## Domain Semantics

Before changing Replicant domain language or semantics, read:

- the canonical terminology in [`docs/glossary/README.md`](../glossary/README.md);
- the applicable requirements in [`docs/specs/`](../specs/);
- the [Core Concepts section of `README.md`](../../README.md#core-concepts).

## Client Diagnostic Messages

Before changing client diagnostic-message fixtures, inspect `MessageCollector`,
`client/src/test/java/replicant/diagnostic_messages.json`, and `client/src/test/java/replicant/BUILD.bazel`.

## Releases

For release preparation or Maven Central publication, follow [`tools/release/README.md`](../../tools/release/README.md).
