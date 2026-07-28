# Replicant Glossary

This glossary defines the core language for describing Replicant's entity replication model.

## Language

### System Schema

A catalog containing each [Entity Type](#entity-type) and [Dataset](#dataset) belonging to one isolated replicated
system. Its identifier distinguishes that system within a [Replicant Context](#replicant-context).

### Replicant Context

The client-side boundary that owns [System Schema](#system-schema) definitions, [Area of Interest](#area-of-interest)
declarations, [Subscription](#subscription) state, and shared [Replica](#replica) instances. Replicant state is not
shared between Replicant Contexts.

_Avoid_: Client context

### Connector

The client runtime component for one [System Schema](#system-schema) that owns its transport connection, sends
[Subscription Operation](#subscription-operation) instances and commands, applies server messages, and coordinates
[Synchronization Point](#synchronization-point) processing.

_Avoid_: DataLoader

### Synchronization Point

A client-server protocol checkpoint confirming that a [Connector](#connector) has processed every request sent before
the checkpoint and its resulting server messages. Reaching a Synchronization Point does not mean that
[Subscription Reconciliation](#subscription-reconciliation) is complete.

_Avoid_: Sync when referring to the protocol checkpoint

### Replicant Session

The server-side state associated with one active Replicant transport session, including authorization,
[Subscription](#subscription) state, [Dataset Cache Version](#dataset-cache-version) values, and queued changes.

_Avoid_: Client session, WebSocket Session when referring to Replicant-owned state

### Entity Type

A schema definition for a kind of server-side [Entity](#entity), including how its corresponding
[Replica](#replica) is created and updated. Within its [System Schema](#system-schema), the definition has a compact
[Entity Type ID](#entity-type-id) used by runtime metadata and the replication protocol.

### Entity Type ID

A compact integer identifying one [Entity Type](#entity-type) within a [System Schema](#system-schema).

_Avoid_: Type ID, type identifier

### Entity

One identifiable server-side domain object eligible for replication. Its replication identity consists of an
[Entity Type ID](#entity-type-id) and [Entity ID](#entity-id).

### Entity ID

An identifier that distinguishes one [Entity](#entity) from other Entities of the same
[Entity Type](#entity-type). An Entity ID is unique only within its Entity Type.

_Avoid_: ID, entity identifier

### Entity Change

A [Change Set](#change-set) member directing the client to update an [Entity](#entity)'s [Replica](#replica) or remove
it from specified [Subscription](#subscription) instances.

_Avoid_: Entity message

### Entity Change Candidate

A potential server-side [Entity](#entity) change captured before routing and filtering for individual
[Subscription](#subscription) instances. It may produce [Entity Change](#entity-change) instances for zero or more
Subscriptions, including changes that remove a [Replica](#replica).

_Avoid_: Message, Entity message

### Replication Invocation

A server-side application invocation during which [Entity Change Candidate](#entity-change-candidate) instances and an
[Initiating Session Change Set](#initiating-session-change-set) are captured and submitted for replication after
successful transaction completion.

_Avoid_: Service call, request, unit of work, transaction when referring specifically to the Replicant capture boundary

### Initiating Session Change Set

A [Change Set](#change-set) accumulated during a [Replication Invocation](#replication-invocation) and merged only into
the Change Set delivered to the [Replicant Session](#replicant-session) that initiated the invocation. Its changes are
not routed to other Replicant Sessions.

_Avoid_: Session changes, session Change Set

### Replica

The client-side representation of one [Entity](#entity). A Replica is shared when it belongs to more than one
[Subscription](#subscription).

_Avoid_: Imitation, user object, client entity

### Dataset

A reusable definition of a replicable population of [Entity](#entity) instances, including its selection shape and
filtering behaviour. Within its [System Schema](#system-schema), the definition has a compact Dataset ID used by
runtime metadata and [Dataset Address](#dataset-address) values, plus a human-readable Dataset name used for
diagnostics. A [Subscription](#subscription) materializes that definition for one client at a Dataset Address.

_Avoid_: Graph, channel, replication graph

### Dataset Visibility

A [Dataset](#dataset) declaration controlling how its [Subscription](#subscription) instances may originate. External
visibility permits an [Area of Interest](#area-of-interest) to request it directly, internal visibility permits it to
be reached through a [Dataset Link](#dataset-link) or [Required Type Dataset](#required-type-dataset), and universal
visibility permits both.

_Avoid_: External Dataset, Internal Dataset

### Cacheable Dataset

A [Dataset](#dataset) whose [Subscription](#subscription) result is permitted to be stored and reused while its
[Dataset Cache Version](#dataset-cache-version) and consistency requirements remain satisfied.

_Avoid_: Cached Dataset

### Type Dataset

A [Dataset](#dataset) whose initial candidates are the populations of one or more configured
[Entity Type](#entity-type) definitions, rather than entities reached by traversal from a root.

_Avoid_: Type graph, types dataset

### Instance Dataset

A [Dataset](#dataset) whose candidates are selected by a configured [Dataset Traversal](#dataset-traversal) beginning
at a [Dataset Root](#dataset-root). The traversal may be shaped and pruned and need not produce a mathematical graph.

_Avoid_: Instance graph, rooted dataset

### Dataset Root

The identified [Entity](#entity) from which an [Instance Dataset](#instance-dataset) traversal begins. The Dataset
definition specifies its [Entity Type](#entity-type), while the [Dataset Address](#dataset-address) supplies its
identifier.

_Avoid_: Instance root

### Dataset Traversal

The schema-defined traversal from a [Dataset Root](#dataset-root) across configured [Entity](#entity) relationships
that determines the initial candidates of an [Instance Dataset](#instance-dataset).

_Avoid_: Replication path, replication edge

### Unfiltered Type Dataset

A [Type Dataset](#type-dataset) with no [Filter](#filter); every otherwise eligible [Entity](#entity) belongs to the
Dataset selection.

_Avoid_: Unfiltered type graph

### Unfiltered Instance Dataset

An [Instance Dataset](#instance-dataset) with no [Filter](#filter); every [Entity](#entity) reached by its configured
traversal belongs to the Dataset selection.

_Avoid_: Unfiltered instance graph

### Implicitly Filtered Type Dataset

A [Type Dataset](#type-dataset) whose [Filter](#filter) restricts membership without a
[Filter Parameter](#filter-parameter). The filtering rule and its inputs are supplied by the system.

_Avoid_: Internally filtered type graph

### Implicitly Filtered Instance Dataset

An [Instance Dataset](#instance-dataset) whose [Filter](#filter) restricts membership without a
[Filter Parameter](#filter-parameter). The filtering rule and its inputs are supplied by the system.

_Avoid_: Internally filtered instance graph

### Parameter-Filtered Type Dataset

A [Type Dataset](#type-dataset) whose [Filter](#filter) consumes a subscriber-supplied
[Filter Parameter](#filter-parameter).

_Avoid_: Filtered type dataset, filtered type graph

### Parameter-Filtered Instance Dataset

An [Instance Dataset](#instance-dataset) whose [Filter](#filter) consumes a subscriber-supplied
[Filter Parameter](#filter-parameter).

_Avoid_: Filtered instance dataset, filtered instance graph

### Keyed Type Dataset

A [Parameter-Filtered Type Dataset](#parameter-filtered-type-dataset) that permits multiple independently addressable
selections of the same [Dataset](#dataset), distinguished by a [Dataset Key](#dataset-key).

_Avoid_: Instanced type graph

### Keyed Instance Dataset

A [Parameter-Filtered Instance Dataset](#parameter-filtered-instance-dataset) that permits multiple independently
addressable selections of the same [Dataset](#dataset) and [Dataset Root](#dataset-root), distinguished by a
[Dataset Key](#dataset-key).

_Avoid_: Instanced instance graph

### Filter

A membership rule specific to a [Dataset](#dataset) that further restricts which otherwise eligible
[Entity](#entity) instances belong to a [Subscription](#subscription).

### Filter Decision

The effect of applying a [Filter](#filter) to an [Entity Change Candidate](#entity-change-candidate) for a
[Subscription](#subscription): forward the change, replace it with a [Replica](#replica) removal, or ignore it.

_Avoid_: Filter result, interesting

### Filter Parameter

A value supplied for a [Dataset](#dataset) selection and consumed by its [Filter](#filter) to determine membership. It
is either a [Fixed Filter Parameter](#fixed-filter-parameter) or an
[Updatable Filter Parameter](#updatable-filter-parameter) and is not part of the
[Dataset Address](#dataset-address).

### Fixed Filter Parameter

A [Filter Parameter](#filter-parameter) whose value cannot change while the [Subscription](#subscription) at the same
[Dataset Address](#dataset-address) persists. Changing the value requires replacing the Subscription.

_Avoid_: Static filter

### Updatable Filter Parameter

A [Filter Parameter](#filter-parameter) whose value may change while retaining the same
[Subscription](#subscription) and [Dataset Address](#dataset-address).

_Avoid_: Dynamic filter

### Dataset Key

A value that distinguishes independently addressable selections of the same
[Keyed Type Dataset](#keyed-type-dataset) or [Keyed Instance Dataset](#keyed-instance-dataset). It forms part of the
[Dataset Address](#dataset-address) and may exist before any [Area of Interest](#area-of-interest) or
[Subscription](#subscription).

_Avoid_: Filter instance ID, subscription key

### Dataset Address

The identity of a subscribable [Dataset](#dataset) selection. It combines the [System Schema](#system-schema) and
Dataset identities with a [Dataset Root](#dataset-root) identifier and [Dataset Key](#dataset-key) when required.

_Avoid_: Channel address, graph address, subscription address

### Dataset Cache Version

An opaque value identifying the cached representation of a [Dataset](#dataset) at a
[Dataset Address](#dataset-address). Cached data may be reused only when the client and server hold equal values.

_Avoid_: ETag, cache key

### Dataset Cache Entry

A stored representation belonging to one concrete [Dataset Address](#dataset-address), containing its
[Dataset Cache Version](#dataset-cache-version) and the serialized [Change Set](#change-set) needed to materialize its
cached [Subscription](#subscription) state.

_Avoid_: Cached Dataset, cached result, generic cache entry

### Dataset Address Template

A partially specified [Dataset](#dataset) selection used while evaluating a [Dataset Link](#dataset-link). It is
matched against or resolved to a [Dataset Address](#dataset-address) before a
[Subscription Dependency](#subscription-dependency) is recorded.

_Avoid_: Partial Dataset Address

### Dataset Address Invalidation

A notification that a [Dataset Address](#dataset-address) is no longer subscribable, normally because its
[Dataset Root](#dataset-root) has been deleted. It removes the [Subscription](#subscription) at that address and
marks any [Area of Interest](#area-of-interest) as unable to be satisfied.

_Avoid_: Delete Subscription

### Area of Interest

A client declaration that a [Subscription](#subscription) should exist at a [Dataset Address](#dataset-address), using
a particular [Filter Parameter](#filter-parameter) when required. It represents desired state and records progress
toward that state.

### Area of Interest Status

The reconciliation state of an [Area of Interest](#area-of-interest): `PENDING` until its desired
[Explicit Subscription Mode](#explicit-subscription-mode) Subscription exists, `SATISFIED` while it exists, or
terminal `INVALIDATED` after [Dataset Address Invalidation](#dataset-address-invalidation). Area of Interest Status is
independent of [Data Availability](#data-availability).

_Avoid_: Subscription status when referring to desired-state reconciliation

### Data Availability

Whether complete data for a [Dataset Address](#dataset-address) is currently usable within a
[Replicant Context](#replicant-context). Data Availability is independent of [Area of Interest](#area-of-interest)
satisfaction and may remain true while the Area of Interest is pending.

_Avoid_: Data presence, data loaded

### Subscription

The actual replication state at a [Dataset Address](#dataset-address), including its current
[Filter Parameter](#filter-parameter) and the [Replica](#replica) instances belonging to it.

### Subscription Collection

The server-side process that materializes the current contents of a [Dataset](#dataset) selection into a
[Change Set](#change-set) when establishing or replacing a [Subscription](#subscription).

_Avoid_: Initial data load, bulk load

### Subscription Operation

A subscribe, update, or unsubscribe request issued by [Subscription Reconciliation](#subscription-reconciliation) to
move actual [Subscription](#subscription) state toward an [Area of Interest](#area-of-interest).

_Avoid_: Area of Interest request

### Subscription Change

A server-reported transition in actual [Subscription](#subscription) state, delivered in a
[Change Set](#change-set).

_Avoid_: Subscription action

### Subscription Mode

The current reason a [Subscription](#subscription) is retained. It may transition between
[Explicit Subscription Mode](#explicit-subscription-mode) and [Implicit Subscription Mode](#implicit-subscription-mode)
without replacing the Subscription.

### Explicit Subscription Mode

The [Subscription Mode](#subscription-mode) of a [Subscription](#subscription) currently backed by an
[Area of Interest](#area-of-interest), whether or not a [Subscription Dependency](#subscription-dependency) also
requires it.

### Implicit Subscription Mode

The [Subscription Mode](#subscription-mode) of a [Subscription](#subscription) with no
[Area of Interest](#area-of-interest), retained only through one or more
[Subscription Dependency](#subscription-dependency) relationships.

### Dataset Link

A schema rule declaring that encountering a configured [Entity](#entity) relationship in one [Dataset](#dataset) may
require a [Subscription](#subscription) to another Dataset.

_Avoid_: Graph link, channel link

### Required Type Dataset

A [Type Dataset](#type-dataset) that another [Dataset](#dataset) always requires whenever the latter is subscribed.
Unlike a [Dataset Link](#dataset-link), this requirement is unconditional.

_Avoid_: Required type graph

### Subscription Dependency

A runtime relationship recording that one [Subscription](#subscription) currently requires another. The target
remains in [Implicit Subscription Mode](#implicit-subscription-mode) unless an
[Area of Interest](#area-of-interest) also places it in [Explicit Subscription Mode](#explicit-subscription-mode).

### Subscription Dependency Candidate

A possible [Subscription Dependency](#subscription-dependency) emitted before its
[Dataset Address Template](#dataset-address-template) values and target [Filter Parameter](#filter-parameter) have
been completely resolved. Resolution either produces a concrete Subscription Dependency or discards the candidate.

_Avoid_: Dependency message

### Routing Key

A named value derived from an [Entity Change Candidate](#entity-change-candidate) and used to determine which
[Dataset Address](#dataset-address) values might contain the [Entity](#entity). A [Filter](#filter) then determines
whether the change belongs to each [Subscription](#subscription); a Routing Key does not identify the Subscription.

### Change Set

A batch of [Subscription Change](#subscription-change) and [Entity Change](#entity-change) instances delivered and
applied to the client as one consistent unit before observers are notified.

### Subscription Reconciliation

The process of comparing each desired [Area of Interest](#area-of-interest) with the actual
[Subscription](#subscription) state and issuing [Subscription Operation](#subscription-operation) instances until
they agree.

_Avoid_: Convergence
