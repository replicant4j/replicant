# Partial Channel Link Resolution Implementation Plan

Status: accepted after user review.

## Delivery Approach
Implement this as a server-only change centered on explicit partial-state modeling plus a concrete-resolution boundary
in `ReplicantSessionManagerImpl`. `ReplicantSession` remains concrete-only and gains assertion-backed invariants.
Transport encoding and cache paths also remain concrete-only and gain assertion-backed invariants. The new
session-context API stays narrow: application code derives only missing target `filterInstanceId`, while the session
manager assembles the concrete target address after deriving the target filter. `EntityMessage` also gains an
assertion-backed invariant that delete messages cannot carry links.

## Ordered Phases
1. Add explicit partial-state support and validation helpers to server `ChannelAddress` and `ChannelLink`.
2. Add boundary assertions to `ReplicantSession` mutation entry points, subscribe flows, transport encoding, and cache
   lookup/store paths.
3. Refactor link expansion in `ReplicantSessionManagerImpl` so one template link can fan out to multiple concrete link
   entries based on matching source subscriptions.
4. Extend `ReplicantSessionContext` with the application hook needed to derive a missing target
   `filterInstanceId` from a concrete source subscription and entity message.
5. Rework delete/delink processing so partial message links are resolved before `ReplicantSession` delink methods are
   invoked.
6. Add targeted tests, then run the full gate.
7. Enforce the `EntityMessage` delete-without-links invariant in construction, merge, and delete-conversion paths,
   then update affected regression tests.
8. Replace aggregate-only graph-link tracking with provenance-aware tracking that supports both entity-scoped and
   graph-scoped downstream references.

## High-Risk Areas And Mitigations
- Risk: partial addresses accidentally leak into session state.
  - Mitigation: assert at `ReplicantSession` non-private mutation boundaries and validate before recording links or
    subscriptions.
- Risk: partial addresses leak into transport or cache paths where they would be silently serialized or keyed.
  - Mitigation: add concrete-only assertions at encoder and cache boundaries and cover them with focused tests.
- Risk: fan-out introduces duplicate subscriptions or unstable ordering.
  - Mitigation: normalize resolved concrete entries before enqueueing, preserve existing sort-by-target behavior, and
    add tests for duplicate-elimination.
- Risk: delete/delink resolution removes the wrong downstream subscriptions.
  - Mitigation: resolve against the concrete source subscription context and cover delete paths with focused tests.
- Risk: changing `EntityMessage` delete semantics breaks tests or merge flows that currently preserve links.
  - Mitigation: add focused tests for constructor assertions, merge-to-delete behavior, and `toDelete()` link clearing,
    and update existing delete-path tests to stop constructing invalid messages.
- Risk: provenance-aware delinking breaks existing downstream applications that record graph links without an entity
  source.
  - Mitigation: preserve the current `recordGraphLink(source, target)` behavior as graph-scoped ownership and add
    entity-aware APIs alongside it.
- Risk: multiple entities targeting the same downstream address cause premature unsubscribe when one entity updates or
  deletes.
  - Mitigation: track per-owner targets plus aggregate source-to-target reference counts and only delink when the last
    owner releases a target.
## Required Full-Gate Command
- `bundle exec buildr test`

## Decision Outcomes
- Q-01: target `filterInstanceId` derivation is application-specific and may use the concrete source address and
  entity-message attribute values.
- Q-02: the new session-context hook derives only the missing target `filterInstanceId`.
- Q-03: update `EntityMessage.links` represents the full current entity-owned downstream link set for that entity in
  the addressed source graph.

## Decision Log
- Q-01:
  - Plan change: follow-link expansion must operate per concrete source subscription rather than from a single
    template lookup.
  - Plan change: the session-context API must receive concrete source subscription context during target resolution.
- Q-02:
  - Plan change: `ReplicantSessionManagerImpl` continues to assemble the concrete target address after deriving the
    target filter and any missing instance id.
  - Plan change: implementation must add assertions to transport and cache paths instead of introducing encoding or
    caching support for partial addresses.
- Follow-on invariant update:
  - Plan change: `EntityMessage` constructors and merge paths must enforce that delete messages never retain links.
  - Plan change: regression tests that model delete messages with links must be updated or removed.
- Provenance update:
  - Plan change: graph-link state must distinguish graph-scoped ownership from entity-scoped ownership.
  - Plan change: existing graph-link APIs used by downstream applications must continue to work for graph-scoped links,
    including source-graph to target-type-graph references with no entity source.
  - Plan change: delete and update handling must diff per-entity owned targets rather than relying on delete messages
    carrying links.
- Q-03:
  - Plan change: update processing can diff `previous entity-owned targets` versus `current message links` without
    requiring a separate recompute API.
  - Plan change: provenance-aware reconciliation should anchor to the concrete source addresses already present on each
    `Change`.

## Planned Tasks
- T1: Model partial server addresses and links with validation helpers and tests.
- T2: Add assertion-backed concrete-only boundaries in `ReplicantSession`, subscribe flows, transport encoding, and
  cache paths.
- T3: Refactor follow-link expansion to return zero or more concrete `ChannelLinkEntry` values.
- T4: Add the new `ReplicantSessionContext` hook for deriving missing target `filterInstanceId` values and update
  implementations/tests.
- T5: Refactor delete/delink handling for partial links.
- T6: Run targeted tests and the full gate, then capture outcomes in the task board.
- T7: Enforce the `EntityMessage` delete-without-links invariant and update affected tests.
- T8: Re-run targeted validation for `EntityMessage`, session, and session-manager delete-path coverage.
- T9: Introduce provenance-aware graph-link tracking with graph-scoped and entity-scoped ownership.
- T10: Refactor update/delete link processing to use provenance-aware add/diff/remove behavior.
