# Akasha Decoupling Spec

## Source

- Shared understanding: completed `grill-me` conversation confirmed by the user on 2026-08-11.
- Repository evidence: Akasha imports in seven client source files; the public storage overload in
  `WebStorageDatasetCacheService`; the bundled `AreaOfInterestView` React4j adapter; client, third-party, release,
  J2CL, GWT, and source-jar rewrite wiring; existing JVM, J2CL-link, GWT-compile, release, and repository checks.

## Problem

Replicant compiles a comprehensive Akasha browser binding library even though it uses only a small set of browser
operations. React4j also keeps Akasha transitively in the aggregate client graph. This adds significant compile
overhead and requires an Akasha-specific source rewrite.

## Required outcome

Replicant's maintained source, tests, client build graph, release artifacts, and published dependency metadata are
independent of Akasha. The limited browser interactions use narrowly scoped Replicant-owned native JsInterop bindings,
and the Akasha-specific source rewrite no longer exists.

## Scope

- In scope: replace direct Akasha browser bindings; remove the Akasha-typed storage API; remove the React4j adapter;
  remove Akasha and React4j build and publication wiring; remove the Akasha WindowGlobal rewrite; update affected
  tests and current documentation.
- Out of scope: add a general browser abstraction; retain compatibility APIs or an optional React4j artifact; add
  session-storage fallback; redesign browser behavior; add a browser-runtime test harness; formally benchmark compile
  time; rewrite historical changelog entries.

## Constraints

- Preserve current WebSocket, JSON, object-key enumeration, page-visibility, web-storage, and console behavior.
- Keep browser bindings private or package-local beside their owners, sharing only a narrow JSON binding where repeated
  use justifies it.
- Keep `WebStorageDatasetCacheService.install()` and `install(ReplicantContext)` local-storage-only.
- Make direct API cuts without compatibility shims.
- Preserve dataset-cache recovery guarantees and hidden-page message processing behavior.
- Use existing repository verification infrastructure; repository instructions require the narrowest affected Bazel
  targets followed by `tools/check.sh`.

## Requirements

- `R1`: Replicant client source and public APIs must not reference Akasha.
- `R2`: The client compile graph and published client metadata must contain neither Akasha nor React4j.
- `R3`: Existing observable browser interaction semantics must remain unchanged behind minimal native JsInterop
  bindings.
- `R4`: The public Akasha storage overload and public React4j adapter must be removed as intentional hard cuts.
- `R5`: The Akasha WindowGlobal source rewrite, rule surface, and rewrite-specific tests must be removed.
- `R6`: Current documentation must describe the remaining build variants and local-storage-only behavior accurately,
  and the changelog must identify both public compatibility cuts.
- `R7`: Existing JVM behavior tests, optimized J2CL linking, all remaining real GWT module compiles, release artifact
  construction, dependency validation, formatting, and the repository gate must pass.

## Acceptance criteria

- `AC1` (`R1`, `R4`): Maintained client sources contain no Akasha imports or Akasha-typed API, and no
  `replicant.react4j` production source or module remains.
- `AC2` (`R2`): Active Bazel, dependency input/generated output, GWT, and release configuration contains no Akasha or
  React4j dependency; the generated client POM contains neither coordinate.
- `AC3` (`R3`): Focused JVM tests pass and optimized J2CL plus all remaining GWT smoke modules compile with the new
  bindings.
- `AC4` (`R5`): `--rewrite-akasha-window-global`, its Starlark attribute, Java transformation branch, and dedicated
  fixture assertions are absent; the generic source-jar builder test passes.
- `AC5` (`R6`): Web-storage Javadoc no longer promises session fallback, README describes the remaining GWT coverage,
  and the current changelog records removal of the storage overload and React4j adapter.
- `AC6` (`R7`): The Maven artifacts target, buildifier, dependency checksum validation, and `tools/check.sh` complete
  successfully.
- `AC7` (`R1`, `R2`, `R5`): A repository-wide active-reference audit finds `akasha` only in immutable historical
  changelog entries and finds no current React4j source, build, release, or documentation wiring.

## Significant decisions

| ID | Decision | Rationale | Impact | User verification |
| --- | --- | --- | --- | --- |
| `D1` | Remove Akasha from the complete maintained client graph, not only direct imports. | The transitive React4j edge still compiles Akasha's 1,853-source jar and defeats the compile-overhead goal. | React4j integration must also leave the aggregate client. | Confirm built client and POM have no Akasha or React4j edge. |
| `D2` | Hard-remove `AreaOfInterestView` and the React4j module. | Keeping or splitting it would preserve an Akasha-backed maintained path. | Existing React4j integration users must migrate outside Replicant. | Confirm the public adapter and fourth GWT smoke variant are gone. |
| `D3` | Hard-remove the public `install(ReplicantContext, akasha.Storage)` overload. | A compatibility abstraction is not needed by repository callers and conflicts with narrow internal bindings. | External custom-storage injection through that overload stops compiling. | Confirm the two remaining install methods meet usage needs. |
| `D4` | Preserve local-storage-only behavior and correct its documentation. | The implementation never provided its documented session fallback; adding it would broaden this refactor into a behavior change. | Runtime behavior stays stable while Javadoc becomes accurate. | Confirm no fallback is expected. |
| `D5` | Use owner-local native bindings, sharing only JSON. | The browser surface is small; a general facade would add unnecessary API and indirection. | Bindings remain deliberately narrow and internal. | Inspect visibility and browser surface size. |
| `D6` | Remove the entire Akasha rewrite feature. | No compiled Akasha source remains after dependency removal. | The source-jar tool loses an obsolete option and special case. | Confirm the flag and tests are absent. |
| `D7` | Use compile/link gates rather than add browser test infrastructure or benchmarks. | Existing infrastructure checks both supported compilers; new runtime infrastructure is outside the agreed scope. | Runtime-native seams retain some test risk, bounded by behavior-preserving bindings and compiler gates. | Review J2CL/GWT and full-check evidence. |

## Technical decisions

- Provide one package-local native global JSON binding for parse/stringify callers in `replicant`.
- Define owner-local native shapes for WebSocket/event callbacks, Web Storage, document visibility, and console logging.
- Provide a package-local `Object.keys` binding within each Java package that needs it rather than expose a public
  cross-package facade.
- Use JsInterop base types and `@JsFunction` callback types to preserve JavaScript calling and value semantics.
- Remove React4j production source/module, smoke inputs, annotation processor, dependency targets, publication entries,
  and generated dependency declarations together.
- Regenerate checked-in Java dependency outputs from `third_party/java/dependencies.yml` rather than hand-edit generated
  declarations.
- Retain prior changelog entries as historical records.

## Testing decisions

- Run focused client compile/tests after replacing bindings: `client_lib`, `ConnectorTest`,
  `TransportContextImplTest`, and `SpyEventProcessorTest`.
- Run `source_jar_builder_test` after removing the rewrite branch.
- Strengthen the J2CL smoke entry point so representative methods from every new binding owner are rooted and cannot
  be removed by tree shaking, then run its optimized link and every remaining real GWT module.
- Build Maven publication artifacts and inspect their dependency output.
- Run buildifier, dependency checksum validation, active-reference audits, and finally `tools/check.sh`.

## Open questions

None.
