---
name: structured-delivery-workflow
description: Run a transparent, repeatable engineering workflow with requirement analysis, implementation planning, fine-grained task tracking, evidence-backed validation, and clean step-by-step commits. Use when work spans multiple files, has process risk, or needs strong project visibility.
license: Apache-2.0
compatibility: Works best in repositories that allow markdown/yaml planning artifacts and test/build execution.
metadata:
  author: realityforge
  version: "1.3"
---

# Structured Delivery Workflow

Use this skill to execute medium-to-large engineering work with clear planning artifacts, granular execution tracking, and auditable outcomes.

## Core Objectives

1. Establish clear requirements before implementation.
2. Turn requirements into a concrete, dependency-aware plan.
3. Track execution with fine-grained tasks and live status.
4. Validate with targeted checks and full quality gates.
5. Leave evidence (commits, task-board entries, test commands) that others can audit quickly.
6. Resolve open questions with the user before finalizing the plan.
7. Require explicit user review before marking a plan as accepted.

## When to Use

Use this workflow when any of the following are true:

- The work spans 3+ meaningful implementation steps.
- The change impacts architecture, behavior parity, or migration safety.
- Multiple commands/files/tests must stay coordinated.
- The user asks for traceability, progress visibility, or process clarity.

## Planning Artifact Strategy

If the repository already has a planning convention, follow it.

If not, create a planning tree (example):

```
plans/<initiative>/
  00-requirements.md
  10-implementation-plan.md
  20-task-board.yaml
  30-compatibility-matrix.md   # optional, for parity/migration work
  40-test-strategy.md           # optional, for complex validation
```

## Workflow

Normative language used below:

- `MUST` = required, no exceptions unless user explicitly overrides.
- `MUST NOT` = prohibited.
- `SHOULD` = default expectation; deviate only with documented rationale.

### 1) Analyze Requirements

Create/update `00-requirements.md`.

It MUST include:

- Mission and scope boundaries
- Locked decisions and non-negotiables
- Explicit command surface and behavior expectations
- Quality/test/coverage gates
- Known intentional divergences (if any)
- Open questions and decisions needed from the user (`Open Questions Register`)

For large features, add dedicated requirement deep dives in separate docs.

Use separate requirement docs when one feature has at least one of:

- 6+ acceptance criteria
- 3+ external integrations or subsystems
- risky migrations/data-impact behavior
- significant parity logic vs existing baseline

Name them predictably (examples):

- `01-feature-auth-migration.md`
- `02-feature-package-reproducibility.md`

Template: `references/requirements-deep-dive-template.md`

Open question handling requirements:

- Unresolved questions MUST be tracked with stable IDs (for example `Q-01`, `Q-02`).
- The implementation plan MUST NOT be finalized while any question is still `open`.
- The assistant MUST ask the user about one open question at a time.
- For each question prompt, the assistant MUST include enough context to decide confidently:
  - why the decision matters,
  - available options,
  - tradeoffs/risks for each option,
  - recommended default and why.
- After each user decision, planning artifacts MUST be updated in the same iteration.
- The question entry MUST move from `open` to `resolved` and record the selected option.

Required `Open Questions Register` fields per question:

- `id` (stable, e.g. `Q-01`)
- `status` (`open` or `resolved`)
- `question`
- `context`
- `options`
- `tradeoffs`
- `recommended_default`
- `user_decision` (required when resolved)
- `artifacts_updated` (which planning docs changed)

### 2) Build Implementation Plan

Create/update `10-implementation-plan.md` with:

- ordered phase sequence
- delivery approach
- high-risk areas and mitigations
- required full-gate command
- explicit decision outcomes for previously open questions
- a `Decision Log` that maps each resolved `Q-*` to concrete plan changes

Break work into fine-grained tasks. Prefer tasks small enough to complete and validate in one focused iteration.

Plan finalization rule:

- The plan MUST NOT be marked final until all tracked open questions are resolved and reflected in `10-implementation-plan.md`.
- Any plan change at any stage MUST be recorded in planning docs (`00-requirements.md`, `10-implementation-plan.md`, and `20-task-board.yaml` where relevant).
- If any decision changes later, the prior decision and the new decision SHOULD both remain visible in the decision history.

Task granularity guidance:

- Good: one behavior slice + tests + docs alignment
- Too broad: entire subsystem rewrite in one task
- Too narrow: trivial one-line edits as standalone tasks

Template: `references/implementation-plan-template.md`

### 3) Maintain Task Board

Create/update `20-task-board.yaml` with machine-readable task state.

Each task should include:

- `id`, `title`, `status`, `priority`, `depends_on`
- acceptance criteria
- files touched (as patterns or explicit paths)
- evidence entries (`command` + `result`)
- commit metadata (`hash` + `message`)

Rules:

- Keep only one active task in progress at a time.
- Mark tasks complete immediately after gates pass.
- Append history entries as each task closes.
- For completed tasks, `commit.hash` must be either a real commit SHA or `not_required` (never `pending`).
- Use `not_required` only when no commit is expected for that task.
- When one commit closes multiple tasks, reuse the same SHA in each task.
- If plan approval is tracked as a dedicated task, it MUST remain incomplete until user review is requested and feedback is incorporated.
- If there are unresolved `Q-*` items, any planning-completion task MUST remain `pending`.

Template: `references/task-board-template.yaml`

### 4) Execute Per-Task Delivery Loop

For each task, do this sequence:

1. Set task status to `in_progress`.
2. Implement minimal diff for the task scope.
3. Run targeted checks while iterating.
4. Run full required gates before completion.
5. Update task evidence and prepare commit message.
6. Commit with a message that describes behavior/process impact.
7. Record commit metadata (`hash` + `message`) or `not_required`, then mark task `completed`.
8. Move to next task.

### 5) Keep Artifacts and Code Aligned

Whenever behavior changes:

- update requirements/plan/task board as needed
- update compatibility notes if parity/divergence is relevant
- ensure docs reflect final behavior, not planned behavior
- record decision changes immediately whenever plan assumptions or scope shift

Also apply this rule before implementation starts:

- Any modification to sequencing, scope, risks, or dependencies in the plan must be reflected in planning artifacts in the same iteration.
- The task board evidence/history SHOULD include a short note whenever such plan updates occur.

### 6) Close Out Cleanly

At completion:

- verify no pending tasks remain for the scope
- verify no completed task has `commit.hash: pending`
- verify working tree is clean (unless user asked to defer commit)
- provide concise completion summary with commit hashes and gate command results

Plan acceptance gate:

- Before marking a plan as `accepted`, the assistant MUST explicitly ask the user to review the latest plan and request updates as needed.
- This approval MUST be captured either:
  - as a dedicated task (for example `PLAN-APPROVAL`) in `20-task-board.yaml`, or
  - as part of the final planning/requirements task.
- The plan MUST NOT be marked `accepted` until:
  - user review has been requested,
  - user feedback (if any) has been applied,
  - resulting plan updates are reflected in planning artifacts,
  - the review outcome is recorded.

## Quality and Evidence Standards

- Prefer targeted tests during iteration; reserve full gates for task completion.
- Full-gate command should be explicit and reusable.
- Never claim completion without command evidence.
- Keep commit boundaries aligned with task boundaries.

## Output Style Guidelines

- Keep planning docs concise but specific.
- Prefer checklists and criteria over long prose.
- Keep phase/task IDs stable across revisions.
- Avoid hidden process state; record key decisions in artifacts.
- Keep decision logs and question IDs traceable from first draft through accepted plan.

## Compliance Checklist

Before moving the plan to `final` or `accepted`, verify all items below are true:

- [ ] `00-requirements.md` contains an `Open Questions Register` with stable `Q-*` IDs.
- [ ] No question is still `open`.
- [ ] Each resolved question records `user_decision` and `artifacts_updated`.
- [ ] `10-implementation-plan.md` includes a `Decision Log` mapping each resolved `Q-*` to plan changes.
- [ ] Any plan change made during execution is reflected in planning artifacts in the same iteration.
- [ ] User review of the latest plan has been explicitly requested.
- [ ] User feedback (if provided) has been incorporated and documented.
- [ ] Approval outcome is recorded in planning artifacts (dedicated `PLAN-APPROVAL` task or integrated planning task).
- [ ] Plan status is set to `accepted` only after all checklist items above pass.

## References

- Requirements deep dive template: `references/requirements-deep-dive-template.md`
- Implementation plan template: `references/implementation-plan-template.md`
- Task board template: `references/task-board-template.yaml`
