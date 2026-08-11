# T03 — Remove Akasha source rewrite

- Status: `complete`
- Blocked by: `T02`
- Spec coverage: `R5`; `AC4`

## Delivers

The generic J2CL source-jar builder no longer exposes, parses, or implements the obsolete Akasha WindowGlobal rewrite,
and its tests cover only retained generic behavior.

## Acceptance criteria

- [x] The Starlark rule has no `rewrite_akasha_window_global` attribute or command-line forwarding.
- [x] The Java builder has no Akasha path constant, option parser branch, transformation, or rewrite-only state.
- [x] Akasha rewrite fixtures and assertions are removed without weakening retained source-jar behavior tests.
- [x] No active repository reference to `--rewrite-akasha-window-global` remains.

## Validation

- `./bazelw test //tools/j2cl/org/realityforge/replicant/j2cl:source_jar_builder_test` — proves retained builder behavior.
- `rg -n 'rewrite_akasha_window_global|rewrite-akasha-window-global|AKASHA_WINDOW_GLOBAL' tools third_party MODULE.bazel` — proves the feature surface is absent.
- `./bazelw run //:buildifier` — proves Starlark/Bazel formatting.

## Evidence

- `./bazelw test //tools/j2cl/org/realityforge/replicant/j2cl:source_jar_builder_test` — passed; retained activation,
  exclusion, determinism, and stable-timestamp behavior remains covered.
- `rg -n 'rewrite_akasha_window_global|rewrite-akasha-window-global|AKASHA_WINDOW_GLOBAL' tools third_party MODULE.bazel`
  — no matches.
- `./bazelw run //:buildifier` and `tools/java_format.sh check` — passed.
- Repository-wide current-reference audit excluding historical changelog entries and the temporary plan — no Akasha or
  React4j matches.
- `git diff --check` and task diff inspection — passed; only the obsolete rewrite surface and fixtures were removed.
