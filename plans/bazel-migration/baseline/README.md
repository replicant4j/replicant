# Buildr Publication Baseline

The normalized files in this directory are generated from detached worktree
`d019d5e5f2c8cff0e625b1171514dcee71bb7b2e` by:

```text
bundle exec buildr clean package PRODUCT_VERSION=6.999 GWT=skip TEST=no
plans/bazel-migration/capture_buildr_baseline.sh <worktree> <output-directory>
```

The standard Buildr package command was also exercised and is a pre-existing
failure:

- its GWT 2.10 compiler rejects current Zemeckis/JSpecify annotation targets;
- with GWT skipped, its client diagnostic fixture check reports 79 stale
  messages.

The JVM artifact path still produces the client/server main, sources, Javadoc,
and POM files used for temporary Bazel parity. Binary artifacts are not stored
in Git.
