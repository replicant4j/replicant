# Dataset Caching Specification

## Purpose

This specification defines the observable guarantees for storing and reusing the subscription state of a
[Cacheable Dataset](../glossary/README.md#cacheable-dataset) in a
[Dataset Cache Entry](../glossary/README.md#dataset-cache-entry) validated by a
[Dataset Cache Version](../glossary/README.md#dataset-cache-version).

## Requirements

1. Dataset caching must be optional, and Replicant must remain correct when no cache service is available.
2. Only a [Dataset](../glossary/README.md#dataset) explicitly designated as a Cacheable Dataset may participate in
   caching.
3. Designating a Dataset as cacheable must permit caching without requiring a Dataset Cache Entry to be created or
   reused.
4. Applying a Dataset Cache Entry must produce the same observable
   [Subscription](../glossary/README.md#subscription), [Replica](../glossary/README.md#replica),
   [Area of Interest](../glossary/README.md#area-of-interest) satisfaction, and
   [Data Availability](../glossary/README.md#data-availability) as applying an equivalent fresh
   [Change Set](../glossary/README.md#change-set).
5. A Dataset Cache Entry must belong to exactly one concrete
   [Dataset Address](../glossary/README.md#dataset-address).
6. A Dataset Cache Version must be opaque and compared only for equality.
7. A client must not parse, order, or derive meaning from a Dataset Cache Version.
8. Replicant may reuse a client Dataset Cache Entry only when its Dataset Cache Version exactly equals the server's
   current Dataset Cache Version for the same Dataset Address.
9. Different Change Sets stored for the same Dataset Address must have different Dataset Cache Versions.
10. The server must authorize the current Subscription request before permitting cache reuse.
11. A shared Dataset Cache Version may identify a Change Set only when that Change Set is equal for every authorized
    subscriber able to reuse it.
12. Any subscriber-specific context that can affect a stored Change Set must participate in cache identity and
    validation; otherwise the Dataset must not be cacheable.
13. Failure to store a Dataset Cache Entry must not prevent establishment of the corresponding Subscription.
14. An absent, unreadable, corrupt, or version-mismatched client Dataset Cache Entry must be invalidated and treated as
    a recoverable cache miss.
15. A recoverable cache miss must obtain a fresh Change Set without making the connector fatal or leaving the Area of
    Interest permanently pending.
16. A failed Dataset Cache Entry must not be advertised repeatedly without a successful replacement.
17. When an [Entity Change Candidate](../glossary/README.md#entity-change-candidate) can alter a stored Change Set,
    Replicant must invalidate or recompute the Dataset Cache Entry before authorizing further reuse.
18. Replicant may invalidate a Dataset Cache Entry whose Change Set has not changed, but must never reuse an entry
    whose Change Set differs from the current Dataset state.
19. Every [Required Type Dataset](../glossary/README.md#required-type-dataset) must be available before a dependent
    Cacheable Dataset becomes available.
20. Applying a dependent Dataset Cache Entry must not expose references to unavailable Required Type Dataset Replicas.
21. Change Sets from Dataset Cache Entries and fresh collection may be combined only when Required Type Dataset
    ordering and consistent Change Set visibility are preserved.
22. A Change Set from a Dataset Cache Entry must become observable only after it has been applied as one complete,
    consistent unit.
