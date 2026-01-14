# Filter Instance Id Specification

## Overview
This document specifies how Replicant supports multiple concurrent subscriptions to the same channel
instance using a client-supplied filter instance id. The feature is exposed through a new
filter type and an extended channel descriptor grammar.

Primary goals:
- Allow multiple subscriptions to the same channel/root with different filters.
- Keep protocol compact by embedding the instance id in the channel descriptor string.
- Preserve existing filter semantics for non-instanced channels.

## Terminology
- **Filter instance id**: Client-supplied string used to distinguish multiple subscriptions to the
  same channel/root.
- **Channel descriptor**: String used in the protocol to identify a channel (subscribe/unsubscribe,
  update actions, entity change channels, etags, etc.).
- **DYNAMIC_INSTANCED**: New filter type that allows filter updates and multiple concurrent instances.

## Protocol

### Channel Descriptor Grammar

```
channelId[.rootId][#filterInstanceId]
```

Rules:
- `#` is a reserved separator and **must not** appear inside the filter instance id.
- `filterInstanceId` is the substring after the first `#`. It may be empty.
- The left side of `#` remains `channelId[.rootId]` and is parsed identically to today.

Examples:
- `12` -> channel 12, no root, no instance id
- `12.5` -> channel 12, root 5, no instance id
- `12#alpha` -> channel 12, instance id `alpha`
- `12.5#` -> channel 12, root 5, instance id empty

### Validation Rules

- For channels with `filterType == DYNAMIC_INSTANCED`:
  - A `#` **must** be present in subscribe, update, and unsubscribe descriptors.
  - Missing `#` is rejected.
- For channels with any other filter type:
  - A `#` in a descriptor is **rejected**.

## Filter Type

### New FilterType
Add `DYNAMIC_INSTANCED`:
- Client: `replicant.ChannelSchema.FilterType`
- Server: `replicant.server.transport.ChannelMetaData.FilterType`

Semantics:
- Filter parameter is required (same as `DYNAMIC`).
- Filter updates are allowed.
- Multiple concurrent subscriptions are allowed, distinguished by `filterInstanceId`.

## Channel Address

### Data Model
Add `@Nullable String filterInstanceId` to `ChannelAddress` on both client and server.

- Client: `client/src/main/java/replicant/ChannelAddress.java`
- Server: `server/src/main/java/replicant/server/ChannelAddress.java`

### Behavior
- `parse(...)` must split on the first `#` and populate `filterInstanceId`.
- `asChannelDescriptor()` and `toString()` include `#filterInstanceId` when non-null.
- `equals`, `hashCode`, and `compareTo` must include `filterInstanceId`.
- Cache keys must include `filterInstanceId` to avoid collisions.

## Client Behavior

### Subscription Identity
- Subscription identity includes `filterInstanceId`.
- Update `SubscriptionService` maps to include instance id as part of the key.

### Converger
- Grouping logic must include `filterInstanceId` so bulk requests do not mix instances.

### Message Parsing
- All channel descriptors in incoming messages must parse instance id and route accordingly:
  - Channel actions (`channels`, `fchannels`)
  - Entity change channel lists

### Validation
- For `DYNAMIC_INSTANCED`, require non-null `filterInstanceId` in all subscribe/update/unsubscribe
  requests, even if the instance id is empty.

## Server Behavior

### Session Manager API
Remove single-subscribe methods and use bulk forms only:

```
void bulkSubscribe( ReplicantSession session,
                    int requestId,
                    List<ChannelAddress> addresses,
                    @Nullable Object filter );

void bulkUnsubscribe( ReplicantSession session,
                      int requestId,
                      List<ChannelAddress> addresses );
```

### Subscription Storage
- Subscription maps must key on full `ChannelAddress` including `filterInstanceId`.
- Add a secondary index keyed by `(channelId, rootId)` to efficiently retrieve all instances
  during update routing.

### Change Representation
- `Change` must store a set of full `ChannelAddress` entries rather than a map of
  `channelId -> rootId`.
- JSON encoding must emit channel descriptors using full `ChannelAddress`.

### Update Routing
- For each channel/root in routing keys, fan out to all subscription instances:
  - Apply `filterEntityMessage` per subscription instance.
  - Emit channel descriptors for each instance that receives the entity.

### Delete Routing
- When an instance root is deleted, unsubscribe **all** subscriptions for that channel/root.

### Validation
- For `DYNAMIC_INSTANCED`, `#` is required in subscribe/update/unsubscribe.
- For other types, `#` is rejected.

## Channel Links

### New Context Hook
Add to `ReplicantSessionContext`:

```
@Nonnull
String deriveFilterInstanceId( @Nonnull EntityMessage entityMessage,
                               @Nonnull ChannelLink link,
                               @Nullable Object sourceFilter,
                               @Nullable Object targetFilter );
```

### Link Expansion
- Derive target filter (existing behavior).
- If target channel is `DYNAMIC_INSTANCED`, call `deriveFilterInstanceId` and build a new
  `ChannelAddress` with the same `channelId/rootId` and the derived instance id.
- Links do not carry instance ids.

## Bulk Subscribe Semantics
- Bulk subscribe continues to accept a single shared filter for all addresses.
- Bulk subscribe/unsubscribe must preserve instance id on each address.

## REST / Introspection
- Session and channel JSON must include `filterInstanceId` as a descriptor field.
- Do not place `#` in REST URL paths.

## Tests

- Update ChannelAddress unit tests (client and server) for `#` parsing, formatting, and ordering.
- Update message parsing tests for channel descriptors with instance ids.
- Update any fixtures that embed channel descriptors.

## Backwards Compatibility

- The protocol is not backward-compatible with old clients/servers when `#` is used.
- For non-instanced channels, descriptors remain unchanged.
