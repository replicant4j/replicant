# Dataset Caching Specification

## Purpose

This specification defines the observable guarantees for reusing the subscription result of a
[Cacheable Dataset](../glossary/README.md#cacheable-dataset) through a
[Dataset Cache Version](../glossary/README.md#dataset-cache-version).

## Requirements

1. Dataset caching must be optional, and Replicant must remain correct when no cache service is available.
2. Only a [Dataset](../glossary/README.md#dataset) explicitly designated as a Cacheable Dataset may participate in
   caching.
3. Designating a Dataset as cacheable must permit caching without requiring a cache entry to be created or reused.
4. Applying a cached result must produce the same observable
   [Subscription](../glossary/README.md#subscription), [Replica](../glossary/README.md#replica),
   [Area of Interest](../glossary/README.md#area-of-interest) satisfaction, and data availability as applying an
   equivalent fresh [Change Set](../glossary/README.md#change-set).
5. A cache entry must belong to exactly one concrete
   [Dataset Address](../glossary/README.md#dataset-address).
6. A Dataset Cache Version must be opaque and compared only for equality.
7. A client must not parse, order, or derive meaning from a Dataset Cache Version.
8. Replicant may reuse a client cache entry only when its Dataset Cache Version exactly equals the server's current
   Dataset Cache Version for the same Dataset Address.
9. Different cached Dataset results for the same Dataset Address must have different Dataset Cache Versions.
10. The server must authorize the current Subscription request before permitting cache reuse.
11. A shared Dataset Cache Version may identify a result only when that result is equal for every authorized
    subscriber able to reuse it.
12. Any subscriber-specific context that can affect a cached result must participate in cache identity and validation;
    otherwise the Dataset must not be cacheable.
13. Failure to store a cache entry must not prevent establishment of the corresponding Subscription.
14. An absent, unreadable, corrupt, or version-mismatched client cache entry must be invalidated and treated as a
    recoverable cache miss.
15. A recoverable cache miss must obtain a fresh Dataset result without making the connector fatal or leaving the Area
    of Interest permanently pending.
16. A failed cache entry must not be advertised repeatedly without a successful replacement.
17. When an [Entity Change](../glossary/README.md#entity-change) can alter a cached Dataset result, Replicant must
    invalidate or recompute that result before authorizing further reuse.
18. Replicant may invalidate a cache entry whose result has not changed, but must never reuse an entry whose result
    differs from the current Dataset result.
19. Every [Required Type Dataset](../glossary/README.md#required-type-dataset) must be available before a dependent
    cached Dataset becomes available.
20. Applying a dependent cached result must not expose references to unavailable Required Type Dataset Replicas.
21. Cached and freshly collected results may be combined only when Required Type Dataset ordering and consistent
    Change Set visibility are preserved.
22. A cached result must become observable only after it has been applied as one complete, consistent unit.
