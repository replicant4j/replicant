# T01 — Replace Akasha browser bindings

- Status: `pending`
- Blocked by: `None`
- Spec coverage: `R1`, `R3`, `R4`; `AC1`, `AC3`

## Delivers

Replicant-owned client code performs its existing JSON, object-key, WebSocket, storage, page-visibility, and console
interactions through minimal internal native JsInterop bindings. The Akasha-typed storage overload is removed while the
two supported install methods retain local-storage-only behavior.

## Acceptance criteria

- [ ] No maintained client Java source imports or exposes an Akasha type.
- [ ] Browser bindings are private or package-local and contain only operations used by their owner; only the repeated
      JSON surface is shared within `replicant`.
- [ ] WebSocket callbacks remain JavaScript-callable and preserve connect, message, error, close, deferred-close, and
      string-send behavior.
- [ ] JSON value handling, object-key ordering/array conversion, hidden-page draining, storage failure recovery, and
      styled console logging retain their current semantics.
- [ ] `install()` and `install(ReplicantContext)` remain; the Akasha storage overload is absent and Javadoc accurately
      describes local storage.
- [ ] The J2CL smoke entry point roots representative methods from every replacement-binding owner, including JSON,
      object keys, WebSocket, storage, visibility, and console paths, so their method bodies cannot be tree-shaken from
      optimized-link validation. Test-only linker code must not execute browser side effects at module load.

## Validation

- `rg -n --glob '*.java' '^import akasha|akasha\.' client/src/main/java` — proves direct Java source/API removal at this
  task boundary without claiming the GWT/dependency cleanup owned by `T02`.
- `./bazelw build //client/src/main/java/replicant:client_lib` — proves the JVM client compiles.
- `./bazelw test //client/src/test/java/replicant:ConnectorTest //client/src/test/java/replicant:TransportContextImplTest //client/src/test/java/replicant/spy/tools:SpyEventProcessorTest` — proves affected JVM-testable behavior remains green.
- Inspect the J2CL smoke linker roots, then run
  `./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke` — proves every replacement-binding owner is retained
  and optimized-links under J2CL.

## Evidence

- `pending`
