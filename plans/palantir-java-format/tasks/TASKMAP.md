# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: `planned`
- Current frontier: `T01`
- Planning reviewer: `/root/planning_review` (`1/3` rounds), Findings: none
- Plan checkpoint: automatic — completed evidence-based grill design tree, explicit user gate exception, passing planning review
- Implementation reviewer: `pending` (`0/5` rounds)

## Full-scope validation

- Gate: narrow `./bazelw build //:java_format_check`, `./bazelw run //:buildifier`, worker/action-input audit, negative/write/watch probes, then `tools/check.sh`.
- Evidence: pending.

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate and verify formatting](T01-migrate-formatting.md) | pending | None |

## Sequencing notes

One end-to-end slice replaces dependency plumbing and enforcement together. After implementation/evidence commit, activate fresh read-only implementation review; record review result before the separate closeout deletion commit. Publish, assign and enable auto-merge after closeout.

## Promoted knowledge

README is the existing owner for formatting workflow/scope. No qualifying new ADR, specification, glossary or deferred artifact.
