# Task Map

- Spec: [`SPEC.md`](../SPEC.md)
- Status: `implementation-review`
- Current frontier: `none`
- Planning reviewer: `/root/planning_reviewer` (`2/3` rounds; passed with `Findings: none`)
- Human approval: `approved 2026-08-11; user said "proceed" after reviewing the passed plan`
- Implementation reviewer: `/root/implementation_reviewer` (`1/5` rounds; one P2 Javadoc finding corrected and awaiting re-review)

## Full-scope validation

- Gate: `tools/check.sh`
- Evidence: `tools/check.sh` passed again on 2026-08-11 after the implementation-review Javadoc correction; 283
  build targets, optimized J2CL, 3 GWT variants, 98 repository tests, and 3 release-version tests passed.

## Tasks

| ID | Task | Status | Blocked by |
| --- | --- | --- | --- |
| `T01` | [`Replace Akasha browser bindings`](T01-replace-browser-bindings.md) | `complete` | None |
| `T02` | [`Remove React4j and dependency wiring`](T02-remove-react4j-and-dependencies.md) | `complete` | `T01` |
| `T03` | [`Remove Akasha source rewrite`](T03-remove-akasha-rewrite.md) | `complete` | `T02` |
| `T04` | [`Document and verify the hard cut`](T04-document-and-verify.md) | `complete` | `T03` |

## Sequencing notes

- `T01` preserves a buildable client while replacing every Replicant-owned Akasha interaction.
- `T02` removes the remaining transitive Akasha path before `T03` deletes the rewrite that path required.
- `T04` owns the final active-reference audit and repository-wide acceptance gate after all code and generated outputs
  have stabilized.

## Promoted knowledge

- `not-required` — domain-modeling classification found only implementation/build choices and migration history;
  existing glossary and observable caching/reconnection specifications remain unchanged.
