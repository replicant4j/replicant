# Palantir Java Formatting Spec

## Source

The user's authorized migration request and references settle the design. The user explicitly waived grill confirmation and the delivery entry gate when repository/reference evidence answers every question. The later clarification limits enforcement to Java reachable through the target graph. No material question remains.

Evidence: existing `tools/java_format.sh`, `tools/check.sh`, CI workflow, root module/configuration, client JVM import/source library boundary, client diagnostic test aggregate, server tests, GWT smoke library, and J2CL rule implementation. The jdbt reference demonstrates the public rule and worker strategy; the rose reference demonstrates public write/watch tools. Downloaded v0.1.1 release integrity matches `sha256-4h/h0cVmPQ/7jkXnvW69rmABbfXOOzoXuBh3gMJDMk8=`; BCR metadata returns 404. Latest release is v0.1.1 and its formatter is 2.93.0, matching the local formatter. The source-file-filtered ownership query finds 309 maintained Java sources, all graph-owned; the earlier unfiltered count also included one generated J2CL Java file. GitHub reports auto-merge and merge commits allowed, with master unprotected and no required checks.

## Problem and required outcome

Formatting uses a separately maintained local binary/dependency graph and starts a formatter outside Bazel's source check actions. Adopt the public rules module and use persistent PalantirJavaFormat worker actions through the existing CI gate, preserving formatting for graph-owned maintained Java.

## Scope and constraints

- Migrate formatter module, check target, wrappers, dependency regeneration, locks, and build documentation.
- Preserve `./bazelw`, check/write conventions, and existing source roots: client, shared, server, tools. Add watch through the public tool.
- No extra enumeration or scratch checks for sources absent from the target graph. No application changes, formatter-version upgrade, unrelated dependency upgrades, or formatting churn.
- Preserve non-mutating checks and specialized consumers if any exist. Exploration found no staged-file wrapper, formatter fixture consumer, or ahab configuration to migrate.
- Work in an isolated checkout from current master. Publish an assigned PR with auto-merge using allowed settings; never force-merge or bypass CI. Preserve plan history with the permitted merge-commit method. If unprotected master prevents pending auto-merge, wait for every actual check on the exact PR head to pass and no blocking review, then use `gh pr merge --auto --merge --match-head-commit`; report an immediate merge accurately. Never change protection/settings.

## Requirements and acceptance criteria

- R1 / AC1: The root check rejects malformed formatting in graph-owned production, tests, build tools, GWT, and J2CL sources, names the affected file and write remediation, and leaves source bytes unchanged. An action-input audit proves all graph-owned maintained sources are covered and generated/external Java is excluded.
- R2 / AC2: Formatting checks actually execute with PalantirJavaFormat worker strategy, bounded to one instance. Execution logs prove worker execution, not just configured flags.
- R3 / AC3: Existing CI invokes the migrated gate. Check/write/watch remain usable; write repairs deliberately dirty graph sources, repeated write causes no diff, and watch repairs a change in an admitted root.
- R4 / AC4: Remove local formatter dependency plumbing and regenerate strict locks without unrelated upgrades. Buildifier and `tools/check.sh` pass and the final diff is inspected. The full gate may run in existing Ubuntu CI when a personally observed local environmental blocker prevents completion; report that local blocker without claiming a local pass.
- R5 / AC5: Read-only planning and implementation reviews pass; commit plan, implementation/evidence, and removal. Publish PR assigned to realityforge, enable auto-merge if settings permit, and report exact state/blockers.

## Completed design tree

1. Enforcement ownership: existing CI -> existing repository check -> wrapper check -> root public java_format_check -> worker/local strategy with one worker.
2. Distribution: released v0.1.1 -> verified archive override while absent from BCR -> formatter stays 2.93.0 -> remove duplicate depgen graph.
3. Coverage: graph-owned sources -> explicit minimal target roots and supported dependency edges -> client implementation must be selected directly because client_lib is a source-attached import -> diagnostic aggregate covers client test libraries -> concrete server tests cover server graph -> existing release suite and tool roots -> GWT JVM smoke root -> J2CL sources need existing private compiled JavaInfo exposed via a focused patch, then direct J2CL library roots.
4. Developer commands: keep bazelw and existing write roots -> public write and watch executables -> check never writes, write changes only formatted bytes.
5. Evidence: action-input audit + negative probes + worker log + write/watch -> narrow build + buildifier + repository CI gate -> independent review -> closeout -> assigned auto-merge PR.

## Significant decisions

| Decision | Rationale | Impact | User verification |
| --- | --- | --- | --- |
| Graph-only enforcement | Explicit user scope | Java absent from graph is not checked | README states scope |
| Expose J2CL's existing compiled JavaInfo | Public formatter checks only JavaInfo owners; J2CL hides this provider | Tiny J2CL override patch; no source copies or extra compilation targets | Verify J2CL builds and negative probes |
| Explicit minimal root labels | Aspect cannot dynamically query targets or traverse import input jars | Root check lists client implementation, aggregate tests, server tests and tools; new isolated graphs need a root | Review action-input coverage |
| Keep formatter 2.93.0 | Already in use and owned by module v0.1.1 | No source reformatting expected | Clean write and final diff |
| Use existing CI | Existing gate already covers formatting | CI workflow needs no new job | Check call chain |

## Technical and testing decisions

Add root visibility to client_lib_impl only as needed by the check. J2CL exposes its already compiled JavaInfo alongside current providers via the existing archive patch mechanism; compilation semantics stay unchanged. Pin module integrity, use testonly on the root check, and remediation `tools/java_format.sh write`. Preserve source-root arguments in write/watch.

Audit action inputs against source files owned in the target graph, probe representative production/test/tool/GWT/J2CL sources, and validate worker logs. Temporary probes are restored before commit. Run the narrow root check, buildifier, dependency stability checks, then the full repository gate. Local `./bazelw build //...` passed, but optimized J2CL compilation ran out of disk. Publish an authorized draft PR with implementation/progress evidence to execute the unchanged `tools/check.sh` in existing Ubuntu CI before implementation review; promote it after passed review and closeout. The final exact PR head must pass every actual CI check before auto-merge. Use a private Bazel output base during local checks because the user's global rc shares an output base with other tasks.

## Knowledge classification

- Formatting workflow, graph scope and commands: existing README build section is the durable owner.
- Module pin, strategies, roots, patch and dependency plumbing: code/configuration.
- Coverage audit, review and validation evidence: temporary task plan.
- No domain language change. The reversible, ordinary tooling selection fails the ADR threshold; no new ADR.
- No deliberately deferred material question or new domain specification.

## Open questions

None.
