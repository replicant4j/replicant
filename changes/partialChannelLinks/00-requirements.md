# Partial Channel Link Resolution Requirements

## Mission
Make partial or template `replicant.server.ChannelLink` and `replicant.server.ChannelAddress` safe and explicit for
server-side change processing, while preserving the invariant that concrete session state only contains concrete
addresses and links.

## Scope Boundaries
- In scope:
  - Server-side `ChannelAddress` and `ChannelLink` partial/template representation.
  - Runtime assertions around partial versus concrete boundaries.
  - Follow-link expansion in `ReplicantSessionManagerImpl`.
  - Delete/delink handling for links emitted from `EntityMessage`.
  - Application hook changes needed to derive missing target `filterInstanceId` values.
  - Transport/cache assertions that prevent partial addresses from being encoded or cached.
  - Server-side tests covering invariants and fan-out behavior.
- Out of scope:
  - Client protocol grammar and client `ChannelAddress`, unless required by a later decision.
  - Wire encoding changes for partial state.
  - Cache behavior changes unrelated to partial link resolution.

## Problem Statement
`ReplicantChangeRecorder` may emit `EntityMessage` links that do not have enough context to supply a concrete source
or target `filterInstanceId`, and may also lack a concrete target filter when the target graph requires one. The
current link-follow path in `ReplicantSessionManagerImpl.createChannelLinkEntryIfRequired()` assumes a single exact
source subscription lookup and can only repair a missing target filter. This is insufficient once a link or address
is only a template for one or more session-specific subscriptions.

## Locked Decisions
- `ReplicantSession` must not store, subscribe, unsubscribe, link, or delink partial addresses.
- Assertions should be added at non-private `ReplicantSession` mutation boundaries so downstream logic can rely on
  concrete addresses.
- Partial addresses must never be transmitted across the transport layer.
- Encoding paths should assert they only receive concrete addresses.
- Partial addresses must never be used for cache lookup or cache storage.
- Cache paths should assert they only receive concrete addresses.
- Target `filterInstanceId` derivation is application-specific and may depend on the concrete source address and the
  `EntityMessage` attribute values.
- The new session-context hook derives only the missing target `filterInstanceId`; `ReplicantSessionManagerImpl`
  remains responsible for assembling the concrete target address.
- The current single-result follow-link flow must be refactored to support fan-out from one partial link template to
  zero or more concrete link entries.
- Delete/delink processing must resolve partial links before invoking `ReplicantSession` delink or unsubscribe flows.
- Delete `EntityMessage` instances must not contain links; in Replicant, `attributeValues == null` implies `links == null`.
- Link provenance must distinguish graph-scoped links from entity-scoped links.
- It must remain possible to represent a reference from a source graph to a target type graph without an entity source.

## Invariants
- A non-partial `ChannelLink` must not contain a partial source or target `ChannelAddress`.
- A non-partial `ChannelLink` whose target channel requires a filter parameter must carry a non-null target filter of
  the correct type.
- A partial `ChannelLink` must contain at least one unresolved element:
  - partial source address, or
  - partial target address, or
  - null target filter when the target channel requires a filter parameter.
- A concrete `ChannelAddress` must satisfy channel metadata requirements for root id and `filterInstanceId`.
- Partial addresses and links must not cross into `ReplicantSession` mutation APIs.
- Partial addresses must not cross into transport encoding or cache APIs.
- Delete `EntityMessage` instances must not contain links.
- Link provenance must support both:
  - entity-scoped ownership keyed by source entity identity, and
  - graph-scoped ownership with no source entity.

## Behavior Expectations
- Follow-link processing must:
  - detect partial source and target addresses,
  - expand a template link against all matching concrete source subscriptions for the session,
  - derive any missing target filter,
  - derive any missing target `filterInstanceId`,
  - emit zero or more concrete `ChannelLinkEntry` values.
- Re-follow logic for already subscribed targets must operate on concrete source and target addresses.
- Delete/delink logic must resolve concrete downstream targets before calling `delinkDownstreamSubscription(...)`.
- Delete and update processing must remove only the downstream links owned by the affected entity, unless the link is
  graph-scoped.
- Existing downstream application code that records graph links without an entity source must remain supported.
- Update `EntityMessage.links` must be treated as the full current entity-owned downstream link set for that entity in
  the addressed source graph.
- Transport encoding must continue to encode only concrete addresses and should assert this invariant.
- Cache lookup/store paths must continue to operate only on concrete addresses and should assert this invariant.
- Runtime assertions should be located at:
  - `ChannelAddress` and `ChannelLink` construction/validation helpers,
  - `ReplicantSessionManagerImpl` boundaries where partials are expanded,
  - non-private `ReplicantSession` mutation entry points used by subscribe/unsubscribe/link/delink flows,
  - transport encoding entry points,
  - cache lookup/store entry points,
  - `EntityMessage` construction and merge boundaries that can create or propagate delete messages.

## Quality Gates
- Targeted unit tests for:
  - partial address/link invariants,
  - follow-link fan-out from one partial source template to multiple concrete source subscriptions,
  - application-specific derivation of target `filterInstanceId` using concrete source address and entity-message
    attribute values,
  - delete/delink resolution when entity messages contain partial links,
  - entity-scoped link ownership with multiple entities targeting the same concrete downstream address,
  - graph-scoped links to target type graphs without an entity source,
  - assertion coverage for `ReplicantSession` public/package-visible mutation boundaries,
  - assertion coverage for transport encoding and cache boundaries.
- Full gate:
  - `bundle exec buildr test`

## Intentional Divergences
- None yet.

## Open Questions Register

### Q-01
- status: resolved
- question: How is a target `filterInstanceId` derived when the link emitted by `ReplicantChangeRecorder` is partial?
- context: Existing logic can derive a missing target filter but not a session-specific target instance id.
- options:
  - Application-specific derivation.
  - Fixed derivation from the source instance id alone.
- tradeoffs:
  - Application-specific derivation is more flexible and matches current domain-specific filter derivation behavior.
  - Fixed derivation is simpler but would not support applications where the target instance id depends on entity data
    or other source-address context.
- recommended_default: Application-specific derivation.
- user_decision: Application-specific derivation is required and it may use the concrete source graph address plus
  `EntityMessage` attribute values.
- artifacts_updated:
  - `changes/partialChannelLinks/00-requirements.md`
  - `changes/partialChannelLinks/10-implementation-plan.md`
  - `changes/partialChannelLinks/20-task-board.yaml`

### Q-02
- status: resolved
- question: Should the new session-context hook derive only the missing target `filterInstanceId`, or should it
  derive the full concrete target address?
- context: Partial resolution now concerns both target filter and target address materialization. A narrower hook keeps
  the API small but splits responsibility across manager and context. A broader hook centralizes application-specific
  address resolution.
- options:
  - Derive only missing target `filterInstanceId`.
  - Derive the full concrete target address.
- tradeoffs:
  - Instance-id-only derivation is a smaller API change and preserves the existing split where the manager assembles
    the concrete target address after deriving a filter and any missing instance id.
  - Full-address derivation would centralize more logic in application code, but it would widen the context API and
    push generic address assembly out of the session manager.
- recommended_default: Derive only missing target `filterInstanceId`.
- user_decision: The new `ReplicantSessionContext` hook should derive only the missing target `filterInstanceId`.
- artifacts_updated:
  - `changes/partialChannelLinks/00-requirements.md`
  - `changes/partialChannelLinks/10-implementation-plan.md`
  - `changes/partialChannelLinks/20-task-board.yaml`

### Q-03
- status: resolved
- question: When an update `EntityMessage` carries links, are those links the full current downstream link set for the
  entity in the addressed source graph, or can they be partial?
- context: Provenance-aware diffing for entity-owned links can only safely remove stale links on update if the update
  message represents the complete current link set for that entity.
- options:
  - Treat update links as the full current entity-owned link set.
  - Treat update links as partial/incremental and add a separate recompute mechanism.
- tradeoffs:
  - Full-set semantics enable straightforward diff-based add/remove behavior during normal update processing.
  - Partial semantics would require additional APIs or side-channel state to know when a previously owned link should
    be removed.
- recommended_default: Treat update links as the full current entity-owned link set.
- user_decision: Update `EntityMessage.links` is the full current downstream link set for the entity in the addressed
  source graph.
- artifacts_updated:
  - `changes/partialChannelLinks/00-requirements.md`
  - `changes/partialChannelLinks/10-implementation-plan.md`
  - `changes/partialChannelLinks/20-task-board.yaml`
