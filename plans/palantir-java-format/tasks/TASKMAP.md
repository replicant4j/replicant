# Task Map

- Spec: [SPEC.md](../SPEC.md)
- Status: `closeout`
- Current frontier: none (technical task and implementation review complete)
- Planning reviewer: `/root/planning_review` (`3/3` rounds), Findings: none (design, CI validation sequence, existing CI prerequisite)
- Plan checkpoint: automatic — completed evidence-based grill design tree, explicit user gate exception, passing planning review
- Implementation reviewer: `/root/implementation_review` (`1/5` rounds), Findings: none

## Full-scope validation

- Gate: narrow `./bazelw build //:java_format_check`, `./bazelw run //:buildifier`, worker/action-input audit, negative/write/watch probes, then `tools/check.sh`.
- Evidence: local formatter/worker/input/negative/write/watch gates pass and whole-repository build passes; full tools/check.sh stops during optimized J2CL compilation with No space left on device. Full gate now passed in [Ubuntu CI run 37007051995](https://github.com/replicant4j/replicant/actions/runs/37007051995), head dde24e30: tools/check.sh, optimized J2CL, GWT, 99 tests, 3 release tests, and clean generated state. Local disk limitation remains.

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| T01 | [Migrate and verify formatting](T01-migrate-formatting.md) | complete | None |

## Sequencing notes

One end-to-end slice replaces dependency plumbing and enforcement together. Commit implementation/progress evidence and publish an authorized draft PR to run the existing full CI gate after local disk-full failure. When CI passes, commit completed task evidence and activate fresh read-only implementation review. Record review result before the separate closeout deletion commit, promote the PR to ready, wait for final exact-head CI, then enable auto-merge. Archive the chat only after GitHub confirms merge.

## Promoted knowledge

README is the existing owner for formatting workflow/scope. Final domain-modeling sweep confirms no qualifying new ADR, specification, glossary or deferred artifact; the reversible tooling selection fails the ADR threshold, mutable setup belongs in code, and delivery evidence belongs in this plan. Doc-link validation passes. Review introduced no changes.

## Implementation review evidence

Round 1 passed with Findings: none. The fresh read-only reviewer inspected the complete diff and plan history, independently queried the isolated Bazel graph (309 sources, matching formatter inputs), checked source hashes, all 51 worker actions, negative/write/watch probes, module integrity, shell syntax, clean diff/worktree, and full Ubuntu CI run 37007051995. Current HEAD differs from the tested code only in plan evidence. Local full-gate disk exhaustion is documented; CI completed the same gate successfully.

## Delivery gate ownership (R5 / AC5)

Planning review passed 3 rounds and implementation review passed 1 round. Results are recorded. Closeout removal, final exact-head CI, verified auto-merge and post-merge chat archive remain phase gates after the completed technical task. PR #28 is assigned to realityforge and remains draft until closeout.
