# T01 — Migrate and verify formatting

- Status: in_progress
- Blocked by: None
- Spec coverage: R1–R5, AC1–AC5

## Delivers

Public formatter module and worker-based graph checks enforced by existing CI, supported write/watch commands, and obsolete local dependency removal. Independent review and publication follow this implementation slice.

## Acceptance criteria

- [x] Verified immutable module pin and regenerated stable locks; obsolete local binary/config/references removed.
- [x] Root check and minimal roots cover graph-owned production/tests/tools/GWT/J2CL; action-input audit confirms coverage and generated/external exclusions.
- [x] Negative cases reject dirty source without mutation, with file/remediation; public write repairs and is idempotent; watch repairs an event.
- [ ] Execution evidence shows worker actions; buildifier and full repository gate pass, or environmental failure evidence explicitly remains unresolved.
- [ ] README/changelog updated, final diff inspected, implementation evidence committed.
- [ ] Planning and implementation reviews pass; plan removed in closeout; PR assigned and auto-merge state verified (workflow closeout/publication).

## Validation

Release hash/BCR check; dependency regeneration stability; shell syntax; root Bazel check; execution-log worker/input audit; temporary negative probes and public write/watch behavior; buildifier; `tools/check.sh`; read-only review and GitHub PR state.

## Evidence

- Downloaded release v0.1.1 and independently verified archive SRI and strip prefix; BCR metadata HTTP 404.
- `BAZELISK_WRAPPER_DIRECTORY=tmp/isolated-bazel tools/update_java_deps.sh` passed. Lockfile and third-party Java generated outputs remained byte-identical. No dependency versions changed apart from adding the formatter module.
- Shell syntax (`bash -n`) and Buildifier passed. Root analysis exposed private test target visibility; granted root-package visibility only for the selected client aggregate and server test packages.
- `./bazelw build //:java_format_check --worker_verbose --execution_log_json_file=/tmp/replicant-format-execution.json` passed. Execution log contains 51 PalantirJavaFormat actions, all runner=worker; verbose output shows one singleplex PalantirJavaFormat worker created.
- Independent `kind("source file", labels(srcs, kind("java_library|java_binary|java_test|j2cl_library", //...)))` query identifies 309 workspace Java source files. Format action input union matches exactly; no generated/external Java inputs. The previous unfiltered count included one generated J2CL test-suite Java output.
- Eight whitespace probes (client production, shared production, server test, client test, GWT smoke, both J2CL-only sources, source-jar tool) all failed with affected paths and `tools/java_format.sh write`. Check preserved source bytes and `git write-tree`. Public write repaired all eight, repaired check passed, repeat write left identical bytes/index. All probes restored.
- Public watch wrapper repaired a modification to `BrowserInteropLinker.java`; watcher was stopped and original bytes restored. Final clean check passed; no maintained Java diff.
- `validate_doc_links.py` passed. Existing README owns workflow/scope; no qualifying new domain artifacts or ADR.
- Full `tools/check.sh`: initial dependency/buildifier/format checks and `./bazelw build //...` passed (2400 actions). Optimized J2CL build stopped with No space left on device. Existing Ubuntu CI will complete the unchanged full gate; this is not a local full-gate pass.
- Pause/resume: prior checkout was removed externally; recreated it from plan commit, reused task-owned dependency cache and reconstructed scoped changes. Earlier disk-full failure did not pass verification; resumed dependency/check gates now pass. Latest user instruction requests archiving the chat after confirmed PR merge.

- Draft PR [#28](https://github.com/replicant4j/replicant/pull/28) assigned to realityforge; existing Ubuntu CI is running. Trimmed blank patch context so final diff has no whitespace errors; root check passed again with the same applied J2CL content.

- Ubuntu CI run 37006542481 passed the new worker check then failed with `tools/check.sh: line 29: rg: command not found` (127). Default-branch run 35935021520 fails with the same missing rg dependency. Workflow only sets up JDK17; install ripgrep in the existing job before executing the gate. No new job, fallback, skipped check or protection change.

- Added ripgrep installation to the existing CI job and documented the gate prerequisite; YAML parsed successfully. Existing gate/job boundaries are unchanged; rerun CI remains required.
