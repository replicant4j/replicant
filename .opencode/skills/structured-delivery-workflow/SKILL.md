---
name: structured-delivery-workflow
description: Run a transparent, repeatable engineering workflow with requirement analysis, implementation planning, fine-grained task tracking, evidence-backed validation, and clean step-by-step commits. Use when work spans multiple files, has process risk, or needs strong project visibility.
license: Apache-2.0
compatibility: Works best in repositories that allow markdown/yaml planning artifacts and test/build execution.
metadata:
  author: realityforge
  version: "1.0"
---

# Structured Delivery Workflow

Use this skill to execute medium-to-large engineering work with clear planning artifacts, granular execution tracking, and auditable outcomes.

## Core Objectives

1. Establish clear requirements before implementation.
2. Turn requirements into a concrete, dependency-aware plan.
3. Track execution with fine-grained tasks and live status.
4. Validate with targeted checks and full quality gates.
5. Leave evidence (commits, task-board entries, test commands) that others can audit quickly.

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

### 1) Analyze Requirements

Create/update `00-requirements.md` with:

- Mission and scope boundaries
- Locked decisions and non-negotiables
- Explicit command surface and behavior expectations
- Quality/test/coverage gates
- Known intentional divergences (if any)

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

### 2) Build Implementation Plan

Create/update `10-implementation-plan.md` with:

- ordered phase sequence
- delivery approach
- high-risk areas and mitigations
- required full-gate command

Break work into fine-grained tasks. Prefer tasks small enough to complete and validate in one focused iteration.

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

Template: `references/task-board-template.yaml`

### 4) Execute Per-Task Delivery Loop

For each task, do this sequence:

1. Set task status to `in_progress`.
2. Implement minimal diff for the task scope.
3. Run targeted checks while iterating.
4. Run full required gates before completion.
5. Update task evidence and commit metadata.
6. Commit with a message that describes behavior/process impact.
7. Move to next task.

### 5) Keep Artifacts and Code Aligned

Whenever behavior changes:

- update requirements/plan/task board as needed
- update compatibility notes if parity/divergence is relevant
- ensure docs reflect final behavior, not planned behavior

### 6) Close Out Cleanly

At completion:

- verify no pending tasks remain for the scope
- verify working tree is clean (unless user asked to defer commit)
- provide concise completion summary with commit hashes and gate command results

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

## References

- Requirements deep dive template: `references/requirements-deep-dive-template.md`
- Implementation plan template: `references/implementation-plan-template.md`
- Task board template: `references/task-board-template.yaml`
