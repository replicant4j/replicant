# T04 — Document and verify the hard cut

- Status: `pending`
- Blocked by: `T03`
- Spec coverage: `R1`, `R2`, `R3`, `R4`, `R5`, `R6`, `R7`; `AC1`, `AC2`, `AC3`, `AC4`, `AC5`, `AC6`, `AC7`

## Delivers

The current changelog records the two public compatibility cuts, all focused and full gates pass, and the final diff
contains no active Akasha/React4j coupling or temporary delivery artifacts outside the active plan tree.

## Acceptance criteria

- [ ] The current changelog records removal of the Akasha storage overload and React4j adapter without altering
      historical entries.
- [ ] Active-reference audits find no maintained Akasha coupling, React4j integration, or Akasha rewrite.
- [ ] Focused JVM, J2CL, GWT, source-builder, dependency, release, and formatting gates pass.
- [ ] `tools/check.sh` passes.
- [ ] The final implementation diff is intentional and contains no scratch/debugging output.

## Validation

- Repeat the focused validation commands owned by `T01` through `T03` as needed after final edits — proves each affected
  surface remains green.
- `rg -n --hidden --glob '!**/.git/**' --glob '!CHANGELOG.md' --glob '!plans/akasha-decoupling/**' 'akasha|Akasha|react4j|React4j|rewrite-akasha-window-global|rewrite_akasha_window_global' .` — proves no active coupling remains outside historical changelog entries and the temporary active delivery plan.
- `tools/check.sh` — proves the repository-wide acceptance gate.
- `git diff --check && git diff --stat && git status --short` — proves diff hygiene and exposes final scope for review.

## Evidence

- `pending`
