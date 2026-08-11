# T03 — Remove Akasha source rewrite

- Status: `pending`
- Blocked by: `T02`
- Spec coverage: `R5`; `AC4`

## Delivers

The generic J2CL source-jar builder no longer exposes, parses, or implements the obsolete Akasha WindowGlobal rewrite,
and its tests cover only retained generic behavior.

## Acceptance criteria

- [ ] The Starlark rule has no `rewrite_akasha_window_global` attribute or command-line forwarding.
- [ ] The Java builder has no Akasha path constant, option parser branch, transformation, or rewrite-only state.
- [ ] Akasha rewrite fixtures and assertions are removed without weakening retained source-jar behavior tests.
- [ ] No active repository reference to `--rewrite-akasha-window-global` remains.

## Validation

- `./bazelw test //tools/j2cl/org/realityforge/replicant/j2cl:source_jar_builder_test` — proves retained builder behavior.
- `rg -n 'rewrite_akasha_window_global|rewrite-akasha-window-global|AKASHA_WINDOW_GLOBAL' tools third_party MODULE.bazel` — proves the feature surface is absent.
- `./bazelw run //:buildifier` — proves Starlark/Bazel formatting.

## Evidence

- `pending`
