# Temporary Maven Publication Output Contract

This migration contract records the publication shape that temporary
Buildr-to-Bazel validation must establish. It is removed with the completed
plan tree after Buildr removal and final implementation review. Downstream
integration tests, not a permanent Replicant-specific contract test, own
long-term consumer compatibility.

## Artifact Families

| Coordinates | Main jar | Sources jar | Javadoc jar | POM |
| --- | --- | --- | --- | --- |
| `org.realityforge.replicant:replicant-client` | required | required | required | required |
| `org.realityforge.replicant:replicant-server` | required | required | required | required |

The requested release version must be used consistently in Maven paths,
filenames, POMs, tags, and release metadata.

## Accepted Archive Shape

### Common

- Main jars contain exactly the deterministic
  `Manifest-Version: 1.0` header and `META-INF/LICENSE`.
- Legacy `Build-By`, `Created-By`, `Implementation-Title`, and
  `Implementation-Version` headers are intentionally absent.
- No published main, sources, or Javadoc jar contains a `BUILD.bazel` file.
- No published jar contains test classes or test sources.
- Shared production classes and sources remain embedded wherever the legacy
  artifact contract embeds them.
- The JSpecify migration may add only the `package-info.java` and corresponding
  `package-info.class` entries required to make every maintained published Java
  package explicitly `@NullMarked`. The temporary comparator derives this exact
  allowlist from the tracked package declarations and permits no broader
  archive-entry difference.

### `replicant-client`

- The main jar contains client and shared compiled classes.
- The main jar preserves the source-bearing GWT library shape required by
  consumers, including client/shared Java sources, GWT module XML resources,
  and Grim metadata.
- Generated Arez compiled classes and generated Java sources are present where
  required by the legacy artifact shape.
- The sources jar contains client and shared production sources plus
  Bazel-generated Arez sources.
- The Javadoc jar contains generated documentation for the intended public
  client API.

### `replicant-server`

- The main jar contains server and shared compiled classes and preserves the
  legacy embedded shared-source behavior.
- The sources jar contains server and shared production sources.
- The Javadoc jar contains generated documentation for the intended public
  server API.

## POM Contract

- Preserve group IDs, artifact IDs, packaging, project URL, Apache 2.0 license,
  SCM, issue-management, and developer metadata.
- Preserve the fresh migration-baseline dependency versions, dependency scopes,
  exclusions, and transitive semantics except for these accepted changes:
  - JSpecify replaces `javax.annotation` as the nullness dependency.
  - The server retains the required provided dependency for non-nullness Java
    EE annotations.
  - Changes required by the accepted current J2CL/GWT toolchains remain
    build-only and do not change published consumer dependencies.
- The shared module remains merged and is not published as a third artifact or
  declared as a Maven dependency.

The temporary parity test must derive and normalize the exact dependency
matrix from a fresh Buildr package at the migration baseline before comparing
the Bazel POMs. It must make the accepted differences above explicit rather
than ignoring arbitrary POM changes. If implementation evidence indicates that
any other published dependency change is necessary, that change requires a new
user decision.

## Distribution Layout, Signatures, and Checksums

The Maven Central ZIP stores every primary artifact beneath:

`org/realityforge/replicant/<artifact-id>/<version>/<artifact-id>-<version><classifier>.<extension>`

For every primary main jar, sources jar, Javadoc jar, and POM:

- Include the primary file.
- Include a detached ASCII-armored signature `<file>.asc`.
- Include `<file>.md5` and `<file>.sha1`.
- Include `<file>.asc.md5` and `<file>.asc.sha1` for the detached signature.

The normalized distribution manifest contains 48 files: 24 for each artifact
family's four primary artifacts.

Temporary validation compares normalized archive entry manifests, POM content,
normalized `META-INF/MANIFEST.MF` content, and distribution paths. It verifies
every MD5 and SHA-1 digest against its primary artifact or detached signature,
and verifies every detached signature against its primary artifact. It does
not require byte-identical ZIP timestamps, Javadoc rendering, detached
signatures, or jar bytes.

## Removal

The temporary parity test is deleted in the same phase that deletes Buildr.
Focused unit tests for generic release implementation logic remain. No
permanent Replicant-specific artifact manifest or POM contract test remains.
