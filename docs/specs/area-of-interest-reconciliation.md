# Area of Interest Reconciliation Specification

## Purpose

This specification defines how [Subscription Reconciliation](../glossary/README.md#subscription-reconciliation)
compares an [Area of Interest](../glossary/README.md#area-of-interest) with the actual
[Subscription](../glossary/README.md#subscription) at its
[Dataset Address](../glossary/README.md#dataset-address), and when the resulting data becomes observable.

## Requirements

1. Within one Replicant client context, no more than one Area of Interest may exist for a Dataset Address.
2. Re-declaring interest at an existing Dataset Address must reuse the same Area of Interest.
3. Applying a different [Filter Parameter](../glossary/README.md#filter-parameter) at an existing Dataset Address must
   update the shared desired value, with the most recently applied update becoming authoritative for every consumer of
   that Area of Interest.
4. Area of Interest status must describe satisfaction using exactly `PENDING`, `SATISFIED`, and `INVALIDATED`.
5. A newly declared Area of Interest must be `PENDING`.
6. An Area of Interest must be `SATISFIED` exactly when an
   [Explicit Subscription Mode](../glossary/README.md#explicit-subscription-mode) Subscription exists at its Dataset
   Address with an equal Filter Parameter.
7. A non-invalidated Area of Interest that is not satisfied must be `PENDING`.
8. Recoverable [Subscription Operation](../glossary/README.md#subscription-operation) failure must not introduce an
   Area of Interest failure status; the Area of Interest remains `PENDING` unless its Dataset Address is invalidated.
9. When no Subscription exists for a `PENDING` Area of Interest, reconciliation must issue a subscribe operation.
10. When an equal Subscription exists in
    [Implicit Subscription Mode](../glossary/README.md#implicit-subscription-mode), reconciliation must issue a
    subscribe operation to transition that Subscription to Explicit Subscription Mode.
11. When an Explicit Subscription Mode Subscription has a different
    [Updatable Filter Parameter](../glossary/README.md#updatable-filter-parameter), reconciliation must update the
    existing Subscription.
12. When an Explicit Subscription Mode Subscription has a different
    [Fixed Filter Parameter](../glossary/README.md#fixed-filter-parameter), reconciliation must replace that
    Subscription.
13. [Dataset Address Invalidation](../glossary/README.md#dataset-address-invalidation) must transition the Area of
    Interest to `INVALIDATED`.
14. `INVALIDATED` must be terminal for the lifetime of the Dataset Address, and reconciliation must issue no further
    Subscription Operations for that Area of Interest.
15. Withdrawing an Area of Interest must transition its Subscription to Implicit Subscription Mode when at least one
    [Subscription Dependency](../glossary/README.md#subscription-dependency) continues to require it.
16. Withdrawing an Area of Interest must remove its Subscription when no Subscription Dependency continues to require
    it.
17. Transitioning a Subscription from Explicit Subscription Mode to Implicit Subscription Mode must not by itself
    replace the Subscription or make its data unavailable.
18. Data availability must be reported independently of Area of Interest status and must indicate whether a complete,
    locally usable [Dataset](../glossary/README.md#dataset) representation currently exists.
19. A `SATISFIED` Area of Interest must have data available, an `INVALIDATED` Area of Interest must not have data
    available, and a `PENDING` Area of Interest may be in either condition.
20. Data for the previous Filter Parameter must remain available while an existing Subscription changes to Explicit
    Subscription Mode or applies an Updatable Filter Parameter.
21. Data may become unavailable while a Subscription is replaced to apply a Fixed Filter Parameter.
22. Satisfaction and data-availability changes caused by a
    [Change Set](../glossary/README.md#change-set) must become observable only after the complete Change Set has been
    applied as one consistent unit.
23. Disconnecting a Replicant connector must preserve each Area of Interest and its desired Filter Parameter while
    removing the corresponding Subscription and making its data unavailable.
24. After disconnection, each non-invalidated Area of Interest must be `PENDING` and reconciliation must resume after
    reconnection.
25. Disconnection and reconnection must not change an `INVALIDATED` Area of Interest or cause its Dataset Address to be
    retried.
