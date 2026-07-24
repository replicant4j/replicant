# Maven Central Release Workflow

This workflow publishes Replicant to Maven Central using the split release scripts in this directory. Run commands
from the repository root.

## Credentials and Tooling

Release commands expect these tools and credentials:

* `gh` for GitHub releases and milestone updates. Confirm authentication before release work:

  ```bash
  gh auth status
  ```

* Maven Central username comes from `MAVEN_CENTRAL_USERNAME`.
* Maven Central password or token comes from `MAVEN_CENTRAL_PASSWORD`.
* GPG signing uses the existing defaults: `GPG_USER` selects the signing key and optional `GPG_PASS` supplies the
  passphrase. Packaging also accepts `--gpg-key-id KEYID` to override `GPG_USER`.

## Release Steps

Check readiness before preparing the release:

```bash
tools/release/check_ready.sh
```

Derive the next release version from `CHANGELOG.md`:

```bash
tools/release/next_version.sh
```

Prepare the release with an explicit version and inspect the result without changing the working tree:

```bash
tools/release/prepare_release.sh <version> --dry-run
```

The dry run prints the `CHANGELOG.md` and `README.md` diffs that would be applied.

Prepare the release for real when the dry run looks correct:

```bash
tools/release/prepare_release.sh <version>
```

This updates release metadata, commits the release preparation changes, and tags `v<version>`.

Package the signed Maven Central bundle:

```bash
tools/package_maven_central.sh <version>
```

The package command writes `dist/replicant-<version>/` and `dist/replicant-<version>.zip`.

Upload the bundle to Maven Central:

```bash
tools/release/upload_maven_central.sh <version>
```

The upload uses Central Portal automatic publication by default. The script waits until Central reports the deployment
as `PUBLISHED` before returning success.

After Central publication succeeds, finalize the release:

```bash
tools/release/finalize_release.sh <version>
```

Finalization prepares `CHANGELOG.md` for the next development iteration, pushes commits and tags, creates or updates the
GitHub release, and closes the matching open milestone when present.

## All-In-One Release

For the normal end-to-end flow, run:

```bash
tools/release/perform_release.sh <version>
```

This runs readiness checks, prepares the release, packages the Maven Central bundle, uploads with automatic Central
publication, and finalizes the release after Central success.

## Recovery and Reruns

The split scripts are the recovery path. Rerun the step that failed after fixing the underlying issue:

* Readiness failure: fix the reported check, credentials, or tooling issue, then rerun `tools/release/check_ready.sh`.
* Prepare failure before a commit or tag: fix the reported issue, then rerun `tools/release/prepare_release.sh <version>`.
* Packaging failure: fix the build, signing key, or passphrase issue, then rerun `tools/package_maven_central.sh <version>`.
* Upload failure before Central accepts the deployment: rerun `tools/release/upload_maven_central.sh <version>` after the
  network, credential, or Central Portal issue is resolved.
* Upload failure after Central accepts the deployment: inspect the Central Portal deployment id printed by the script
  before rerunning, so a duplicate deployment is not created unintentionally.
* Finalize failure after Central reports `PUBLISHED`: rerun `tools/release/finalize_release.sh <version>`.
