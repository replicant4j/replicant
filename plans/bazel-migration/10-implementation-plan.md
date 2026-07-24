# Bazel Cutover Implementation Design

## Status

- Approval: pre-accepted by the user on 2026-07-24, conditional only on the
  structured stage reviews completing without unresolved material choices.
- Requirements baseline:
  `plans/bazel-migration/00-requirements.md`.
- Temporary publication contract:
  `plans/bazel-migration/30-output-contract.md`.

## Requirement Coverage

- `R1`, `R11`, `R12`, `R13`, `R14` → establish `tools/check.sh` as the local
  and CI gate, then remove Buildr/Ruby/Rake and rewrite repository guidance.
- `R2`, `R3`, `R4` → import BrainCheck's Java rules, migrate nullness, apply
  Palantir formatting, and enforce all three policies in Bazel.
- `R5` → replace tracked generated IDEA metadata with BrainCheck's Bazel
  BSP/managed-project structure.
- `R6` → expose the existing Bazel annotation processor generated-source jar
  as a first-class input shared by J2CL, GWT, and release packaging.
- `R7`, `R8`, `R15`, `R16`, `R17` → adapt BrainCheck's release rules, Java
  builders, lifecycle CLI, shell entry points, credentials, and tests for
  Replicant's two artifacts and `v6.x` release stream.
- `R9` → pin the latest observed J2CL revision and add an optimized full-client
  link through a synthetic Closure entry point.
- `R10` → import BrainCheck's Rose-derived GWT aspect and compile four
  synthetic module entry points with the real GWT compiler.

## Chosen Design

### Migration Baseline and Temporary Parity

Before source hardening, create a detached temporary Git worktree under
`/private/tmp` at immutable baseline
`d019d5e5f2c8cff0e625b1171514dcee71bb7b2e`, run the Buildr package path there
with an explicit test release version, and record the exact capture commands,
Java/Ruby/Buildr versions, and artifact paths. Copy only normalized, reviewable
parity fixtures into the active plan tree: archive entry manifests, POMs, and
main-jar manifests. Do not store binary release artifacts in Git. Remove the
temporary worktree after fixture capture so the active worktree and user
changes are never disturbed.

The baseline's standard Buildr package command is recorded as pre-existing
failure evidence: its GWT 2.10 compiler rejects annotations in current
Zemeckis/JSpecify dependencies, and its client diagnostic fixture check is
stale. Capture the still-working JVM publication contract with
`GWT=skip TEST=no`. Do not repair obsolete Buildr GWT/test gates; Bazel's real
GWT builds and TestNG suites replace them.

The Bazel release phase adds a temporary integration test that compares its
output to those fixtures with only the accepted differences in
`30-output-contract.md`. It verifies signatures and digests cryptographically.
The comparator derives an exact allowlist of new `package-info.java` and
`package-info.class` entries from the tracked `@NullMarked` declarations
introduced by the accepted JSpecify migration; it does not ignore other entry
additions or removals.
The test and fixtures are removed in the Buildr-removal phase. Generic unit
tests for release builders and lifecycle behavior remain.

### Bazel and Java Toolchains

Upgrade `.bazelversion` to 9.2.0. Configure Bazel's tool runtime as pinned
remote JDK 25 and retain `--release 17` for every maintained Java target. Import
the applicable current BrainCheck `.bazelrc` hardening, including explicit Java
test dependencies, strict dependency checks, release-version flag aliasing,
stable output configuration, and small TestNG sizing.

Update dependency generation through
`third_party/java/dependencies.yml` and `tools/update_java_deps.sh`; never
hand-edit generated dependency sections. Continue using explicit package-owned
source lists and never introduce `glob()`.

### Error Prone, JSpecify, NullAway, and Formatting

Import BrainCheck's current Java rule policy. First run the policy against the
existing source and fix findings without enabling the repository-wide policy.
Then enable the strict Error Prone list. Add `-Xep:Varifier:ERROR` to server
production, server test, and JVM-only release-tool targets. Do not enable it
for client/shared production or tests; the repository gate instead rejects
local `var` declarations under `client/**` and `shared/**`.

Replace nullness imports and annotations with JSpecify, add `@NullMarked`
package declarations, and enable NullAway plus explicit-null-marking checks
with `OnlyNullMarked=true`. Retain the Java EE `javax.annotation` dependency
only in targets and the server POM that still need `PostConstruct`,
`PreDestroy`, `Priority`, or `Resource`. The client compile target also retains
a build-only direct dependency because the latest Arez processor still emits
legacy `javax.annotation` imports; this dependency is excluded from the client
POM, whose normal Arez dependency already supplies it transitively to source
consumers.

During this phase, temporarily add JSpecify to Buildr's dependency metadata and
the client/server compile/POM configuration while retaining the server's
non-nullness Java EE annotation dependency. Run the working Buildr JVM
artifact path with `GWT=skip TEST=no` after migration so publication artifacts
remain available until parity. Delete this temporary Buildr configuration with
the rest of Buildr in phase 12.

Add a repository check that derives every maintained Java package from
production and test sources repository-wide, including JVM build/release
tooling, excludes generated output, and requires the corresponding package
declaration to be explicitly `@NullMarked`. This closes the
`OnlyNullMarked=true` gap where an accidentally unmarked package would
otherwise bypass NullAway.

Run the pinned BrainCheck Palantir formatter over tracked Java sources in a
source-only phase. Add the formatter repository, wrapper, and non-mutating
`check` mode in a later phase and invoke it from `tools/check.sh`. Generated
Arez output is never formatted or tracked.

### Generated Arez Sources

Keep the current client JVM `java_library` as the single annotation-processing
owner. Its `JavaInfo` already exposes
`libclient_lib-gensrc.jar`, containing all 12 generated `Arez_*` sources, while
the class jar contains their compiled classes.

Add a narrow Starlark adapter that exposes direct generated source jars from
`JavaInfo.java_outputs` as normal Bazel files. Reuse that output:

- as a source-jar input to the client release main/sources jars;
- as a J2CL source-jar input;
- through the imported GWT aspect, which already collects
  `generated_source_jar` from `JavaInfo`.

Do not copy generated sources into the workspace or add a second annotation
processor invocation.

### Bazel BSP and IntelliJ

Adopt BrainCheck's checked-in managed Bazel project view, adapting source roots
for `client`, `shared`, `server`, tests, and release tooling. IntelliJ's
plugin-generated `.bazelbsp` and `.idea` state remains local and untracked.
Delete legacy `.ipr`, `.iml`, and `.iws` files and their obsolete
ignore/configuration rules. Bazel becomes the sole IDE project model.

### Maven Artifacts and Release Lifecycle

Adapt BrainCheck's deterministic Java release builders and Starlark release
rules:

- `replicant-client`: main, sources, Javadoc, and POM;
- `replicant-server`: main, sources, Javadoc, and POM.

Release jars merge only explicitly declared Java/source/resource inputs,
normalize entry ordering and timestamps, reject conflicting duplicate entries,
exclude `BUILD.bazel`, and emit only `Manifest-Version: 1.0`. Client release
inputs include generated Arez sources and required GWT/Grim resources. Shared
classes and sources retain the accepted embedded topology.

The distribution builder creates the 48-file Maven Central layout, invokes GPG
for detached ASCII-armored signatures, and writes verified MD5/SHA-1 digests.
Unit tests cover jar merging, distribution assembly, argument validation,
default/explicit version behavior, and lifecycle transformations without
retaining a project-specific manifest/POM contract test after migration.

Temporary cryptographic parity creates an isolated `GNUPGHOME` under
`/private/tmp`, generates a non-production test key non-interactively, passes
its fingerprint explicitly to both packaging and verification, runs real
`gpg --verify` for every detached signature, validates every digest, and
destroys the temporary keyring afterward. No developer or production signing
key is required by migration validation.

Adapt BrainCheck's release scripts and Java lifecycle tool to Replicant's
two-segment `6.x` versions, changelog structure, repository coordinates, two
artifacts, and release notes. Require `MAVEN_CENTRAL_USERNAME`,
`MAVEN_CENTRAL_PASSWORD`, and `GPG_USER`; accept optional `GPG_PASS` and
`RELEASE_DATE`. Readiness validates tools, credentials, GitHub authentication,
cleanliness, TODO policy, and the full repository gate before state mutation.
Preparation supports dry-run and creates the release commit/tag; upload uses
Central automatic publication and polls to `PUBLISHED`; finalization updates
the changelog, pushes commits/tags, creates or updates the GitHub release, and
closes a matching milestone.

### J2CL

At the start of the J2CL phase, resolve `google/j2cl` `master`, record the
observed immutable commit, observation date, archive SHA-256, and pin only that
archive. Reuse the BrainCheck J2CL/Bazel compatibility configuration and add
only evidenced patches.

Create J2CL libraries for the shared/client source graph and required
J2CL-compatible dependency sources. Include the Bazel-generated Arez source
jar. A synthetic Closure module imports representative Replicant public types
from the linked full graph, and `j2cl_application` produces optimized
JavaScript. No browser runtime test is added.

### GWT

Import `tools/gwt` from the current BrainCheck reference without behavioral
changes. Add synthetic entry-point modules and compiler targets for
`Replicant`, `ReplicantDev`, `ReplicantDebug`, and
`replicant.react4j.React4j`. Each target uses the real GWT 2.13.1 compiler,
Java 17 source mode, the full Java/runtime/source graph, and generated Arez
sources collected by the GWT aspect. `tools/check.sh` explicitly builds all
four output archives.

### Repository Gate, CI, and Removal

Build `tools/check.sh` incrementally until its final sequence:

1. regenerate Java dependency outputs and the Bzlmod lockfile;
2. immediately reject a scoped Git diff in generated dependency outputs and
   the Bzlmod lockfile;
3. check buildifier formatting;
4. check Palantir Java formatting;
5. verify explicit `@NullMarked` coverage for every maintained production/test
   package repository-wide;
6. reject local `var` declarations under client/shared production and tests;
7. query all concrete TestNG targets and reject any not sized `small`;
8. build all Bazel targets;
9. build the optimized J2CL smoke output;
10. build all four GWT compiler outputs;
11. run all Bazel tests with the default `0.0.0-SNAPSHOT` release setting;
12. run the focused release-tool test suite again with an explicit valid
    `--release_version`.

GitHub Actions installs only the required host prerequisites, runs
`tools/check.sh`, whose own scoped generated-file diff check makes local and CI
behavior identical, and then rejects any remaining tracked formatting drift.
Before Buildr removal, validate the phase-11 commit in an Ubuntu 24.04
container using the same host prerequisites and `tools/check.sh` command as
the workflow; this provides the equivalent pre-removal Linux execution
environment without pushing or triggering remote state. If a phase-11 branch
is later pushed with user authorization, its Actions run must also pass before
claiming remote CI success. Once temporary parity, release tests, J2CL, GWT,
and the local Ubuntu CI-equivalent pass, delete Buildr/Ruby/Rake files and
references plus the temporary parity test. Run the full gate again, review
final implementation alignment, promote durable repository guidance, and
delete the completed plan tree in a deletion-only commit.

## Load-Bearing Assumptions

- Claim: the existing Bazel JVM graph is a sound base for hardening.
  - Evidence: `./bazelw build
    //client/src/main/java/replicant:client_lib` passed on 2026-07-24; the
    repository already has package-owned production and TestNG targets.
- Claim: one Bazel annotation-processing path can supply all generated Arez
  Java.
  - Evidence: the built client `JavaInfo` output contains
    `libclient_lib-gensrc.jar` with the 12 expected `Arez_*` Java sources, and
    its class jar contains the corresponding classes.
- Claim: the selected GWT aspect consumes generated annotation-processor
  sources.
  - Evidence: BrainCheck
    `tools/gwt/defs.bzl` explicitly adds each `JavaInfo.java_outputs`
    `generated_source_jar` to GWT source inputs.
- Claim: Replicant has four GWT configurations that must remain compiler-valid.
  - Evidence: the current Buildr output under `target/replicant_client/assets`
    contains compiler assets for `ReplicantTest`, `ReplicantDevTest`,
    `ReplicantDebugTest`, and `replicant.react4j.React4jTest`.
- Claim: the J2CL boundary is the client/shared graph, not server code.
  - Evidence:
    `client/src/main/java/replicant:client_j2cl_srcs` and
    `shared/src/main/java/replicant/shared:shared_j2cl_srcs` already expose that
    intended source boundary; server packages have no corresponding target.
- Claim: `javax.annotation` cannot be removed completely from server build/POM
  dependencies.
  - Evidence: server production code has six non-nullness imports:
    `PostConstruct`, `PreDestroy`, `Priority`, and `Resource`.
- Claim: the tracked legacy IDEA metadata is Buildr-owned and replaceable.
  - Evidence: Git tracks the root/client/shared/server `.ipr`, `.iml`, and
    `.iws` files, while current BrainCheck uses Bazel BSP and a managed project
    view.
- Claim: BrainCheck release builders are an appropriate direct base.
  - Evidence: the reference implements deterministic jar/source/Javadoc/POM
    rules, distribution signing/checksums, split lifecycle scripts, and focused
    Java tests under `tools/release`.

## Alternatives and Trade-offs

- Keep Buildr for releases while Bazel owns builds — rejected because the
  requested outcome is a full cutover and retaining two systems preserves
  drift.
- Check generated Arez sources into Git — rejected because the user explicitly
  requires Bazel generation and untracked outputs; a single generated-source
  jar also avoids stale copies.
- Retain a permanent Replicant publication-contract integration test —
  rejected by user decision; temporary parity proves migration shape and
  downstream integration tests own compatibility.
- Recreate BrainCheck's manifest builder with legacy Buildr headers — rejected
  by user decision; final manifests contain only `Manifest-Version: 1.0`.
- Compile only production/development GWT variants — rejected because Buildr
  currently validates four public configurations and the user accepted all
  four.
- Add J2CL browser tests — rejected as unnecessary for the accepted
  compile/link compatibility boundary.
- Combine hardening into one source rewrite — rejected because separate,
  reviewable commits were explicitly accepted.

## Risks and Compatibility

- Risk: strict Error Prone and NullAway expose a large source migration.
  - Impact: public nullness metadata changes and broad formatting can obscure
    semantic fixes.
  - Response: fix Error Prone first, enable policy separately, migrate
    JSpecify/NullAway separately, and format only afterward.
- Risk: a global type-inference rule conflicts with GWT source restrictions.
  - Impact: client/shared compilation fails or JVM-only code loses the accepted
    `var` convention.
  - Response: enable `Varifier` only on server/server-test/JVM-tool targets and
    use a scoped repository check to prohibit `var` in client/shared code and
    tests.
- Risk: J2CL's current `master` may require compatibility patches or expose
  unsupported dependencies.
  - Impact: the full client link may fail despite JVM/GWT success.
  - Response: pin the exact observed revision, prove every required source
    dependency, add only focused patches, and record evidence.
- Risk: release artifacts silently omit generated/client/shared content or
  alter POM scopes.
  - Impact: downstream GWT/JVM consumers or Maven resolution break.
  - Response: capture the immutable Buildr baseline before source changes and
    keep temporary normalized parity through the removal gate.
- Risk: release automation mutates Git or remote services during tests.
  - Impact: unintended commits, tags, uploads, or releases.
  - Response: unit-test pure lifecycle/builders, use dry-run/local fixtures,
    and never execute upload/finalization in migration validation.
- Risk: temporary signing validation accidentally uses a production key.
  - Impact: test artifacts are signed with developer identity or validation
    depends on private credentials.
  - Response: create and destroy an isolated ephemeral keyring and pass the test
    fingerprint explicitly.
- Risk: macOS-only validation misses Linux CI behavior.
  - Impact: Buildr could be removed before the final workflow is executable on
    its declared runner platform.
  - Response: run the phase-11 gate in an Ubuntu 24.04 container that mirrors
    workflow prerequisites before phase 12; require remote Actions success only
    if pushing is separately authorized.
- Risk: imported BSP/GWT/J2CL code expands the repository substantially.
  - Impact: review and maintenance burden.
  - Response: copy the accepted BrainCheck/reference implementation, adapt only
    project-specific labels, and verify copied files where byte identity is
    intended.

## Validation Strategy

- Targeted checks:
  - legacy Buildr package and normalized parity-fixture capture before source
    migration;
  - strict Error Prone analysis, then all Java tests;
  - NullAway/explicit-null-marking analysis, then all Java tests;
  - maintained-package `@NullMarked` coverage check;
  - Buildr clean package after JSpecify migration;
  - formatter dry-run check;
  - negative `var` scan under client/shared production and tests;
  - Bazel query comparison proving every concrete TestNG target is `small`;
  - scoped clean-diff check immediately after dependency/lockfile regeneration;
  - BSP file and tracked legacy metadata checks;
  - explicit/default release artifact builds and release-tool unit tests;
  - temporary artifact/POM/manifest/signature/digest parity;
  - real signature verification using an isolated ephemeral GPG keyring;
  - optimized J2CL link;
  - four real GWT compiler outputs;
  - CI YAML parse and generated-drift behavior;
  - complete `tools/check.sh` execution in Ubuntu 24.04 before Buildr removal;
  - negative search for Buildr/Ruby/Rake references after removal.
- Full gates:
  - strongest available phase gate after each focused commit;
  - final `tools/check.sh`;
  - final `./bazelw build //...`;
  - final `./bazelw test //...`;
  - final `./bazelw run //:buildifier_check`;
  - clean-worktree implementation alignment review.

## Delivery Sequence

1. Capture immutable Buildr publication evidence and align Bazel 9.2.0/tool
   JDK 25.
2. Fix existing strict Error Prone findings.
3. Enable strict Error Prone.
4. Migrate nullness to JSpecify and enable NullAway.
5. Apply Palantir formatting.
6. Add permanent formatter enforcement.
7. Replace legacy IntelliJ metadata with Bazel BSP.
8. Add Bazel-generated Arez source plumbing and Maven Central release tooling,
   including temporary parity validation.
9. Add the full-client optimized J2CL link.
10. Add all four GWT compiler smoke builds.
11. Align CI and the complete `tools/check.sh` gate.
12. Remove Buildr/Ruby/Rake, temporary parity validation, and obsolete
    references.
13. Perform implementation review and remove the completed plan tree in a
    deletion-only commit.
