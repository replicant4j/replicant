# T01 — Migrate and verify formatting

- Status: pending
- Blocked by: None
- Spec coverage: R1–R5, AC1–AC5

## Delivers

Public formatter module and worker-based graph checks enforced by existing CI, supported write/watch commands, and obsolete local dependency removal. Independent review and publication follow this implementation slice.

## Acceptance criteria

- [ ] Verified immutable module pin and regenerated stable locks; obsolete local binary/config/references removed.
- [ ] Root check and minimal roots cover graph-owned production/tests/tools/GWT/J2CL; action-input audit confirms coverage and generated/external exclusions.
- [ ] Negative cases reject dirty source without mutation, with file/remediation; public write repairs and is idempotent; watch repairs an event.
- [ ] Execution evidence shows worker actions; buildifier and full repository gate pass, or environmental failure evidence explicitly remains unresolved.
- [ ] README/changelog updated, final diff inspected, implementation evidence committed.
- [ ] Planning and implementation reviews pass; plan removed in closeout; PR assigned and auto-merge state verified (workflow closeout/publication).

## Validation

Release hash/BCR check; dependency regeneration stability; shell syntax; root Bazel check; execution-log worker/input audit; temporary negative probes and public write/watch behavior; buildifier; `tools/check.sh`; read-only review and GitHub PR state.

## Evidence

Pending.
