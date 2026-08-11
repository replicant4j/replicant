# T04 — Document and verify the hard cut

- Status: `complete`
- Blocked by: `T03`
- Spec coverage: `R1`, `R2`, `R3`, `R4`, `R5`, `R6`, `R7`; `AC1`, `AC2`, `AC3`, `AC4`, `AC5`, `AC6`, `AC7`

## Delivers

The current changelog records the two public compatibility cuts, all focused and full gates pass, and the final diff
contains no active Akasha/React4j coupling or temporary delivery artifacts outside the active plan tree.

## Acceptance criteria

- [x] The current changelog records removal of the Akasha storage overload and React4j adapter without altering
      historical entries.
- [x] Active-reference audits find no maintained Akasha coupling, React4j integration, or Akasha rewrite.
- [x] Focused JVM, J2CL, GWT, source-builder, dependency, release, and formatting gates pass.
- [x] `tools/check.sh` passes.
- [x] The final implementation diff is intentional and contains no scratch/debugging output.

## Validation

- Repeat the focused validation commands owned by `T01` through `T03` as needed after final edits — proves each affected
  surface remains green.
- `rg -n --hidden --glob '!**/.git/**' --glob '!CHANGELOG.md' --glob '!plans/akasha-decoupling/**' 'akasha|Akasha|react4j|React4j|rewrite-akasha-window-global|rewrite_akasha_window_global' .` — proves no active coupling remains outside historical changelog entries and the temporary active delivery plan.
- `tools/check.sh` — proves the repository-wide acceptance gate.
- `git diff --check && git diff --stat && git status --short` — proves diff hygiene and exposes final scope for review.

## Evidence

- Current `CHANGELOG.md` contains distinct hard-cut entries for the Akasha storage overload and React4j adapter; prior
  historical entries remain unchanged.
- Repository-wide active-reference audit excluding `CHANGELOG.md` and the exact temporary plan tree — no matches.
- Focused task evidence: client compile and 3 affected JVM tests passed; optimized J2CL output retained all six browser
  surfaces; three GWT modules compiled; source-jar builder and dependency checksum tests passed; eight Maven artifacts
  built and contained no Akasha/React4j publication surface.
- First `tools/check.sh` run exposed the orphaned manual `javaemul_internal_annotations-j2cl` import after dependency
  regeneration. The import was removed and committed with T02 evidence.
- Final `tools/check.sh` — passed again after the implementation-review Javadoc correction: 283 build targets
  analyzed/built, optimized J2CL and all GWT assets built, 98 tests passed, and all 3 release-version tests passed.
- Domain-modeling promotion audit — no promotion required: browser bindings and dependency choice are implementation
  concerns; observable caching/reconnection specifications and glossary-owned language are unchanged; no ADR or
  deferred-question directory exists.
- `git diff --check`, final task diff/stat, and worktree status inspection — passed; only changelog and active plan-state
  files remained before the task commit.
