# Bazel Cutover Requirements

## Baseline

- Source: the 2026-07-24 request to remove Buildr and follow the completed
  BrainCheck Bazel migration, refined through the accepted `grill-me`
  interview.
- Basis of acceptance: the user explicitly confirmed shared understanding and
  directed implementation to proceed with the plan considered accepted.
- Migration baseline: repository `HEAD` at the start of planning,
  `d019d5e5f2c8cff0e625b1171514dcee71bb7b2e`.
- Reference implementation: `/Users/peter/Code/realityforge/braincheck` at
  `c3d7f0bc5e96d76ffee673185ae4b0a4e38209b1`.

## Outcome

Replicant uses Bazel as its only build, test, formatting, IDE, GWT, J2CL,
packaging, publication, release, and CI system. The Buildr, Ruby, and Rake
infrastructure is removed only after Bazel has demonstrated the accepted
publication shape and all replacement workflows pass.

## Scope

- In scope:
  - BrainCheck-equivalent strict Error Prone, JSpecify/NullAway, Palantir Java
    Format, Bazel BSP, release tooling, J2CL, GWT, repository check, and CI
    hardening.
  - Bazel-owned generation of Arez processor sources for JVM, J2CL, GWT, and
    publication inputs.
  - Maven Central packaging and the complete release lifecycle for
    `replicant-client` and `replicant-server`.
  - Removal of Buildr, Ruby, Rake, generated IntelliJ project files, and every
    obsolete reference to those workflows.
- Out of scope:
  - Runtime feature changes unrelated to satisfying the imported build
    hardening.
  - Compatibility shims for Buildr, Ruby, Rake, or legacy IntelliJ metadata.
  - Permanent Replicant-specific publication-shape integration tests after the
    migration has established parity.
  - Browser runtime tests for J2CL or GWT.

## Constraints

- Follow the BrainCheck migration conventions and retain separate focused
  commits for the accepted phases. The implementation plan must enumerate the
  accepted phase sequence without combining or reordering phases unless the
  user approves a revision.
- Use Bazel 9.2.0 and a pinned remote JDK 25 for Bazel tooling while retaining
  Java 17 source and bytecode compatibility for published libraries.
- Preserve the existing Maven coordinates, classifiers, dependency semantics,
  embedded shared content, generated client sources, GWT resources, metadata,
  signing, checksums, and Central bundle layout except for accepted changes in
  `30-output-contract.md`.
- Never check generated Arez Java sources into Git. Generate them with Bazel
  and include them in the published client sources jar.
- Exclude all `BUILD.bazel` files from every published jar.
- Keep Buildr's working JVM artifact-generation path functional until temporary
  automated parity validation proves the accepted Bazel publication output.
  The immutable baseline's standard Buildr package path already fails in its
  obsolete GWT and client diagnostic gates; repairing those paths is not a
  migration requirement because Bazel replaces them directly.
- Capture the fresh Buildr artifact and POM baseline from the immutable
  migration baseline before any Java source migration, and preserve its
  normalized evidence in the temporary parity fixture.
- Remove the temporary Buildr-to-Bazel parity validation together with Buildr.
- Retain focused unit tests for release-tool implementation logic, but do not
  retain a permanent Replicant artifact-contract integration test.
- Apply BrainCheck's strict Error Prone policy directly, retaining its
  GWT-related `Varifier` exception and adding only narrowly evidenced
  Replicant-specific exceptions.
- Replace nullness uses of `javax.annotation` with JSpecify without a
  dual-annotation compatibility period. Retain non-nullness Java EE annotations
  and their required dependency.
- Compile all four existing GWT module variants with real compiler smoke builds:
  `Replicant`, `ReplicantDev`, `ReplicantDebug`, and
  `replicant.react4j.React4j`.
- Compile and optimized-link the complete client/shared J2CL graph, including
  Bazel-generated Arez sources, through one synthetic entry point pinned to the
  latest observed J2CL `master` commit at implementation time.
- Preserve user changes and do not hand-edit generated dependency or processor
  outputs.
- Match the current BrainCheck reference hardening by enabling explicit Java
  test dependencies, testing both the default and explicit release-version
  configurations, decoupling release distribution tests from a fixed version,
  marking TestNG tests as small, validating every required release credential,
  and adapting its type-inference policy to Replicant's module boundaries.
- Enable Error Prone `Varifier` for server production, server tests, and
  JVM-only build/release tooling, where local `var` is the accepted style.
  Disable `Varifier` and prohibit local `var` in client/shared production and
  tests, where explicit local types are required for GWT compatibility.
- Record the exact immutable J2CL commit, archive digest, and observation date
  when resolving the latest `master` revision; no moving branch reference may
  remain in the final dependency configuration.

## Requirements

- `R1`: Bazel is the sole supported repository build and test system.
- `R2`: Every Java production and test target satisfies the accepted strict
  Error Prone policy.
- `R3`: Production and test packages use JSpecify nullness and satisfy NullAway
  with explicit null marking.
- `R4`: All maintained Java sources satisfy the pinned Palantir Java Format
  policy, enforced by the repository gate.
- `R5`: Bazel BSP and a managed project view replace generated legacy IntelliJ
  project files.
- `R6`: Bazel generates required Arez sources and supplies them consistently to
  JVM, J2CL, GWT, sources-jar, and release inputs without tracking them in Git.
- `R7`: Bazel produces and publishes the two accepted Maven artifact families
  according to `30-output-contract.md`.
- `R8`: The release workflow supports readiness, version derivation, dry-run
  preparation, preparation, signed packaging, Maven Central upload with
  publication polling, and GitHub finalization, plus an all-in-one command.
- `R9`: A pinned current J2CL toolchain compile-and-links the complete client
  graph to optimized JavaScript.
- `R10`: The real GWT compiler builds all four accepted module variants.
- `R11`: A single repository check command validates generated dependency
  state, build formatting, Java formatting, all builds, J2CL, GWT, release
  tooling, and all tests.
- `R12`: GitHub Actions executes the repository check and rejects generated or
  formatted file drift.
- `R13`: Buildr, Ruby, Rake, and their files, tasks, documentation, ignores, and
  release paths are absent after replacement parity passes.
- `R14`: Repository guidance, README, changelog, and contribution/release
  documentation describe only the final Bazel workflows.
- `R15`: The release command contract is:
  - `tools/release/check_ready.sh`
  - `tools/release/next_version.sh`
  - `tools/release/prepare_release.sh <version> [--dry-run]`
  - `tools/package_maven_central.sh <version> [--gpg-executable PATH]
    [--gpg-key-id KEYID]`
  - `tools/release/upload_maven_central.sh <version>`
  - `tools/release/finalize_release.sh <version>`
  - `tools/release/perform_release.sh <version>`
- `R16`: Release readiness requires authenticated `gh`, available `gpg` and
  `curl`, `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, and `GPG_USER`.
  `GPG_PASS` and `RELEASE_DATE` are optional inputs. Missing required tools,
  authentication, credentials, or invalid arguments fail before state-changing
  release work.
- `R17`: Bazel exposes `--release_version` as an alias to the release-version
  build setting. The default development value is `0.0.0-SNAPSHOT`; explicit
  release commands reject empty or `-SNAPSHOT` versions.

## Acceptance Criteria

- `AC1`: `tools/check.sh` passes with Bazel 9.2.0 after Buildr removal.
- `AC2`: `./bazelw build //...` and `./bazelw test //...` pass under the final
  configuration.
- `AC3`: strict Error Prone, NullAway, explicit null marking, buildifier, and
  Palantir Java Format checks pass without broad suppressions.
- `AC4`: the J2CL smoke target produces an optimized JavaScript link output for
  the complete client/shared graph and Bazel-generated Arez sources.
- `AC5`: real GWT compiler outputs are produced for all four accepted module
  variants.
- `AC6`: temporary automated comparison proves the Bazel Maven artifacts and
  Central bundle satisfy `30-output-contract.md` relative to a fresh Buildr
  baseline captured from
  `d019d5e5f2c8cff0e625b1171514dcee71bb7b2e`.
- `AC7`: release-tool unit tests pass for jar assembly, distribution assembly,
  command validation, and release lifecycle behavior.
- `AC8`: after `AC6`, Buildr/Ruby/Rake files and the temporary parity test are
  absent and no tracked documentation or configuration references their
  removed workflows.
- `AC9`: no generated Arez Java source is tracked by Git, while the published
  client sources jar produced during parity validation contains those sources.
- `AC10`: no published jar produced during parity validation contains a
  `BUILD.bazel` entry.
- `AC11`: the tracked legacy `.ipr`, `.iml`, and `.iws` files are absent and
  Bazel BSP/managed project metadata is present.
- `AC12`: the final implementation-alignment review has no actionable findings,
  the completed plan tree is removed in a deletion-only commit, and the
  worktree is clean.
- `AC13`: every Maven distribution digest matches the corresponding primary
  artifact or signature, and every detached signature verifies against its
  primary artifact during temporary parity validation.
- `AC14`: the final J2CL configuration records an immutable commit and archive
  digest together with the date on which that commit was observed as upstream
  `master`.
- `AC15`: Bazel enforces explicit Java test dependencies for every Java test
  target, and every concrete TestNG test is sized `small`.
- `AC16`: release tests pass both with the default
  `0.0.0-SNAPSHOT` configuration and with an explicit release version, without
  a fixed test version leaking into distribution assembly.
- `AC17`: focused release command tests prove every command's accepted
  arguments and prove that missing `MAVEN_CENTRAL_USERNAME`,
  `MAVEN_CENTRAL_PASSWORD`, or `GPG_USER` fails readiness/upload before remote
  mutation.
- `AC18`: server production, server tests, and JVM-only build/release tooling
  pass with `Varifier` enabled; client/shared production and tests contain no
  local `var` declarations and pass with `Varifier` disabled.
- `AC19`: an automated coverage check enumerates every maintained production
  and test Java package repository-wide, including JVM build/release tooling,
  and fails unless that package is explicitly `@NullMarked`; generated sources
  are excluded.

## Open Questions

- `Q-001`
  - Status: resolved.
  - Question: should Replicant follow the current BrainCheck implementation and
    require `MAVEN_CENTRAL_USERNAME`, or preserve the interview's accepted
    fixed `realityforge` username with only `MAVEN_CENTRAL_PASSWORD` required?
  - Context: BrainCheck documentation still describes a fixed username, but
    its current readiness and upload scripts require both username and password
    environment variables after credential-validation hardening.
  - Options: require both variables for exact implementation parity; or keep
    the fixed existing account name and validate only the password.
  - Recommended default: require both variables to follow current BrainCheck
    code and avoid embedding account identity in release tooling.
  - User decision: require both `MAVEN_CENTRAL_USERNAME` and
    `MAVEN_CENTRAL_PASSWORD`.
  - Artifacts updated: `00-requirements.md`.
- `Q-002`
  - Status: resolved.
  - Question: which deterministic manifest headers should replace the legacy
    Buildr-generated main-jar manifest?
  - Context: the baseline manifest contains `Build-By`, `Created-By: Buildr`,
    `Implementation-Title`, `Implementation-Version`, and
    `Manifest-Version`. BrainCheck's final jar builder emits only
    `Manifest-Version: 1.0`.
  - Options: follow BrainCheck and emit only `Manifest-Version: 1.0`; or retain
    deterministic implementation title/version headers while removing
    Buildr/user-specific headers.
  - Recommended default: follow BrainCheck exactly and emit only
    `Manifest-Version: 1.0`.
  - User decision: emit only `Manifest-Version: 1.0`.
  - Artifacts updated: `00-requirements.md`,
    `30-output-contract.md`.
- `Q-003`
  - Status: resolved.
  - Question: how should BrainCheck's final explicit-type/`Varifier` hardening
    apply across Replicant's GWT and JVM modules?
  - Context: BrainCheck disables `Varifier` globally because its maintained
    library is GWT-compatible, while Replicant also has JVM-only server and
    release-tool code whose convention uses local `var`.
  - Options: use explicit types globally; or enforce type inference only in
    JVM-owned code and explicit types in GWT-owned code.
  - Recommended default: server production/tests and JVM tooling enable
    `Varifier`; client/shared production/tests disable it and prohibit `var`.
  - User decision: accepted the recommended module-specific policy.
  - Artifacts updated: `00-requirements.md`,
    `10-implementation-plan.md`.
