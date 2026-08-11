# Task Map

- Spec: [`SPEC.md`](../SPEC.md)
- Status: `implementing`
- Current frontier: `T02`
- Planning reviewer: `/root/planning_reviewer` (`2/3` rounds; passed with `Findings: none`)
- Human approval: `approved 2026-08-11; user said "proceed" after reviewing the passed plan`
- Implementation reviewer: `pending` (`0/5` rounds)

## Full-scope validation

- Gate: `tools/check.sh`
- Evidence: `pending`

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| `T01` | [`Replace Akasha browser bindings`](T01-replace-browser-bindings.md) | `complete` | None |
| `T02` | [`Remove React4j and dependency wiring`](T02-remove-react4j-and-dependencies.md) | `pending` | `T01` |
| `T03` | [`Remove Akasha source rewrite`](T03-remove-akasha-rewrite.md) | `pending` | `T02` |
| `T04` | [`Document and verify the hard cut`](T04-document-and-verify.md) | `pending` | `T03` |

## Sequencing notes

- `T01` preserves a buildable client while replacing every Replicant-owned Akasha interaction.
- `T02` removes the remaining transitive Akasha path before `T03` deletes the rewrite that path required.
- `T04` owns the final active-reference audit and repository-wide acceptance gate after all code and generated outputs
  have stabilized.

## Promoted knowledge

- `not-required`
