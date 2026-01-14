# Filter Instance Id Task Breakdown

This plan is incremental and intended for step-by-step implementation with review at each step.
If any changes diverge from the spec, update `changes/filterInstanceId/spec.md` before proceeding.

## Phase 0: Prep and Review
- [ ] Confirm current spec and this task list are aligned.
- [ ] Identify any external implementations of `ReplicantSessionContext` and `ReplicantSessionManager` that will need updates.

## Phase 1: Core Types and Parsing (No Behavioral Changes)
- [ ] Add `filterInstanceId` field to client `ChannelAddress` and update parsing/formatting/equality/compare/hash/cache key.
- [ ] Add `filterInstanceId` field to server `ChannelAddress` and update parsing/formatting/equality/compare/hash.
- [ ] Update `ChannelAddress` unit tests on client and server for the new grammar.
- [ ] Review: confirm descriptor round-trip and ordering rules.

## Phase 2: Protocol Constants and FilterType
- [ ] Add `DYNAMIC_INSTANCED` to `ChannelSchema.FilterType` and `ChannelMetaData.FilterType`.
- [ ] Update filter type checks/invariants on client and server to include `DYNAMIC_INSTANCED` where `DYNAMIC` is expected.
- [ ] Review: confirm invariant logic and error messages are still correct.

## Phase 3: Message Parsing/Encoding (Descriptor-Only)
- [ ] Update server JSON encoding to emit channel descriptors using full `ChannelAddress` with `#` support.
- [ ] Update client message parsing to handle `#` in channel descriptors:
  - channel actions (`channels`, `fchannels`)
  - entity change channel lists
- [ ] Review: confirm protocol round-trip and backwards behavior for non-instanced channels.

## Phase 4: Server Change Model and Routing
- [ ] Change `Change` to store a set of `ChannelAddress` instead of `Map<Integer,Integer>`.
- [ ] Update `ChangeUtil` and `JsonEncoder` accordingly.
- [ ] Update server update routing to fan out per subscription instance and emit per-instance channel descriptors.
- [ ] Review: confirm entity routing and channel change emission match spec.

## Phase 5: Subscription Storage and Indexes
- [ ] Update server `ReplicantSession` to key subscriptions by full address, and add index by `(channelId, rootId)` for fan-out.
- [ ] Update client `SubscriptionService` to include `filterInstanceId` as part of the identity key.
- [ ] Review: confirm lookups and removal paths work for multiple instances.

## Phase 6: Session Manager API and Endpoint Changes
- [ ] Remove `subscribe`/`unsubscribe` from `ReplicantSessionManager` and update all callers.
- [ ] Update endpoint handlers to use bulk methods with full `ChannelAddress` lists and shared filter.
- [ ] Validate `DYNAMIC_INSTANCED` requirements: reject missing `#` and reject `#` for non-instanced channels.
- [ ] Review: confirm bulk behaviors and validation rules.

## Phase 7: Link Expansion
- [ ] Add `deriveFilterInstanceId` to `ReplicantSessionContext` and update implementations.
- [ ] Update `createChannelLinkEntryIfRequired` to derive instance id for instanced channels.
- [ ] Review: confirm linking behavior and derived instance id propagation.

## Phase 8: Client Convergence and AOI Behavior
- [ ] Update converger grouping logic to include instance id.
- [ ] Ensure client-side validation enforces `#` on subscribe/update/unsubscribe for `DYNAMIC_INSTANCED`.
- [ ] Review: confirm no incorrect grouping or missing validation.

## Phase 9: REST Introspection
- [ ] Update REST encoder to emit `filterInstanceId` fields in JSON descriptors.
- [ ] Review: confirm no `#` in URL paths.

## Phase 10: Tests and Fixture Updates
- [ ] Update tests for message parsing/encoding and any fixtures with channel descriptors.
- [ ] Run relevant unit tests.
- [ ] Review: confirm failures are fixed and fixtures updated intentionally.

## Phase 11: Docs
- [ ] Update `README.md` if user-facing behavior needs documenting.
- [ ] Update `CHANGELOG.md` for the feature.
