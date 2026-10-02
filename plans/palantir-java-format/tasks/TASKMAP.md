# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: `planned`
- Current frontier: `T01`
- Planning reviewer: `/root/planning_review` (`3/3` rounds), Findings: none (design, CI validation sequence, existing CI prerequisite)
- Plan checkpoint: automatic — completed evidence-based grill design tree, explicit user gate exception, passing planning review
- Implementation reviewer: `pending` (`0/5` rounds)

## Full-scope validation

- Gate: narrow `./bazelw build //:java_format_check`, `./bazelw run //:buildifier`, worker/action-input audit, negative/write/watch probes, then `tools/check.sh`.
- Evidence: local formatter/worker/input/negative/write/watch gates pass and whole-repository build passes; full tools/check.sh stops during optimized J2CL compilation with No space left on device. Existing Ubuntu CI will supply full-gate evidence.

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate and verify formatting](T01-migrate-formatting.md) | in_progress | None |

## Sequencing notes

One end-to-end slice replaces dependency plumbing and enforcement together. Commit implementation/progress evidence and publish an authorized draft PR to run the existing full CI gate after local disk-full failure. When CI passes, commit completed task evidence and activate fresh read-only implementation review. Record review result before the separate closeout deletion commit, promote the PR to ready, wait for final exact-head CI, then enable auto-merge. Archive the chat only after GitHub confirms merge.

## Promoted knowledge

README is the existing owner for formatting workflow/scope. No qualifying new ADR, specification, glossary or deferred artifact.
