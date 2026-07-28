# Replicant

[<img src="https://img.shields.io/maven-central/v/org.realityforge.replicant/replicant-client.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.replicant%22%20a%3A%22replicant-client%22)
[![CI](https://github.com/replicant4j/replicant/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/replicant4j/replicant/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/replicant4j/replicant/branch/master/graph/badge.svg)](https://codecov.io/gh/replicant4j/replicant)

The replicant library aims to provide infrastructure for replicating a portion of a complex server-side
domain model to zero or more clients who have subscribed to the replication engine. When changes are
applied on the server-side, the changes are batched and transmitted to interested clients. Upon receiving
the changes, the client will atomically apply the changes to a local client-side representation. The
application is then notified of the changes via a local message broker.

The library uses a client-side repository of Replicas that represent the subscribed subset of server-side Entities.
Entity Changes are transmitted from the server to the client in Change Sets and the Replicas are dynamically updated.
When the Replicas are updated, changes are propagated through to the user interface through events and a centralized
event broker. To avoid exposing inconsistent Replica state to the UI, each Change Set is applied atomically and only
after the complete Change Set has been applied are the changes propagated through the event broker.

## Build

Replicant uses GitHub Actions for CI. The workflow runs `tools/check.sh` on Ubuntu 24.04 with Temurin JDK 17 and
rejects any generated or formatting drift.

The Bazel workflow requires JDK 17+ on `JAVA_HOME` or `PATH` and uses `./bazelw`, which pins Bazel through
`.bazelversion`:

* Build public output jars: `./bazelw build //client:client //server:server`
* Build the eight Maven publication artifacts: `./bazelw build //tools/release:maven_artifacts`
* Optimized-link the full J2CL client graph: `./bazelw build -c opt //client/src/test/j2cl:replicant_j2cl_smoke`
* Compile all four real GWT module variants: `./bazelw build //client/src/test/gwt:all_gwt_assets`
* Run all Bazel tests: `./bazelw test //...`
* Run the current repository gate: `tools/check.sh`
* Check Bazel file formatting: `./bazelw run //:buildifier_check`
* Update Bazel file formatting: `./bazelw run //:buildifier`
* Check Java formatting: `tools/java_format.sh check`
* Update Java formatting: `tools/java_format.sh write`

For IntelliJ IDEA, import `tools/intellij/.managed.bazelproject` with the Bazel plugin. Legacy `.ipr`, `.iml`,
and `.iws` project metadata is not part of the project model.

The public Bazel output libraries are `//client:client` and `//server:server`. Both output jars merge the
internal shared classes from `//shared:shared_lib` and keep third-party jars separate from the merged outputs.

Java dependencies for the Bazel build are managed by
[bazel-depgen](https://github.com/realityforge/bazel-depgen) from `third_party/java/dependencies.yml` and
`tools/java-format/dependencies.yml`. After changing either file, regenerate the checked-in Bazel dependency
outputs and lockfile with:

* `tools/update_java_deps.sh`
* `./bazelw test //third_party/java:verify_config_sha256`

The Bazel release targets build the client/server main, source, Javadoc, and POM artifacts. Generated Arez sources
remain untracked and are included in the client main and source jars; published jars exclude `BUILD.bazel`. Create a
signed Maven Central bundle with `tools/package_maven_central.sh <version>` and see
[`tools/release/README.md`](tools/release/README.md) for the complete release and recovery workflow.

The full client and shared graph, including generated Arez sources, is permanently verified with an optimized J2CL
link and real GWT 2.13.1 compiler builds for Replicant, ReplicantDev, ReplicantDebug, and React4j.

It should be noted that replicant is designed to be integrated with other technologies, most notably
[Domgen](https://github.com/realityforge/domgen), to provide a complete solution. It is most commonly
used with an Java EE server component and a GWT front-end but it was originally derived from a client-server
Swing application that used a custom application server.

## Core Concepts

### Entities and Replicas

Replicant assumes that each server-side Entity selected for replication has a client-side Replica. The Replica need
not be identical to the Entity, but one Replica maps to one Entity and may omit attributes and relationships that the
client does not need. Replicant manages the Replica's state and lifecycle.

### System Schemas

A System Schema catalogs every Entity Type and Dataset belonging to one isolated replicated system. Its identifier
distinguishes that system within a Replicant Context. Connectors and Dataset Addresses use the identifier to keep
multiple systems in the same context isolated.

### Replicant Contexts

A Replicant Context is the client-side boundary that owns System Schema registrations, Areas of Interest,
Subscriptions, connector lifecycle, cache-service integration, and shared Replica instances. One Context can own
multiple System Schemas. A Replica may be shared by overlapping Subscriptions in the same Context, but no Replicant
state is shared between Contexts.

A Zone is only an activation scope that selects which Replicant Context is returned by `Replicant.context()`. It is
not a Replicant Session. A Replicant Session is the distinct server-side state for one active transport session.

### Datasets and Subscriptions

When a client connects to the replicant system, they are typically interested in a subset of the
data on the server; it is usually prohibitively expensive to transfer and store the entire server-side
domain model on the client. A more typical example is that a client wants to receive data about a subset
of the domain model, for example they may query:

* All payment classes.
* All alerts within a 50km radius of coordinate X
* All details about a particular vehicle or person
* All data pertaining to a particular roster over a particular date range
* etc.

Each query is represented by a reusable Dataset that selects Entities of one or more Entity Types and declares its
filtering behavior. A Subscription materializes that Dataset at a Dataset Address for one client. The client receives
an initial Change Set that materializes the matching Entities as Replicas, followed by subsequent matching Entity
Changes until it unsubscribes or disconnects.

Within its System Schema, each Dataset definition has a compact Dataset ID used by runtime metadata and Dataset
Addresses. It may also have a human-readable Dataset name for diagnostics. The Dataset ID identifies the reusable
definition; it is distinct from the Dataset Root identifier and Dataset Key that complete particular Dataset
Addresses.

Datasets have several independent dimensions:

* Selection shape: Type Dataset or Instance Dataset.
* Filter source, represented by `FilterMode`: `UNFILTERED`, `IMPLICIT`, or `PARAMETER_FILTERED`.
* Keying: unkeyed or keyed. Keying is valid only for a Parameter-Filtered Dataset.
* Dataset Visibility: `EXTERNAL`, `INTERNAL`, or `UNIVERSAL`.

A Parameter-Filtered Dataset also has a `FilterParameterMode`: `FIXED` or `UPDATABLE`.

Dataset Visibility controls how a Subscription may originate. External visibility permits an Area of Interest to
request the Dataset directly. Internal visibility permits the Dataset to be reached through a Dataset Link or Required
Type Dataset. Universal visibility permits both origins. Dataset Visibility does not authorize a particular
subscriber; authorization is evaluated separately for every direct Subscription request.

**Type Datasets**: A Type Dataset starts with the populations of one or more configured Entity Types. Applications
commonly use Type Datasets for reference data so that the complete configured populations arrive in one selection.

**Instance Datasets**: An Instance Dataset selects candidates by configured traversal beginning at a Dataset Root. The
Dataset definition specifies the Dataset Root Entity Type, while each Dataset Address supplies the Dataset Root
identifier. Traversal is transitive but may be explicitly shaped or pruned and need not form a mathematical graph. For
example, an Instance Dataset whose Dataset Root Entity Type is `Person` may traverse through `Accreditation` entities
to `EvaluationResult` entities. A subscription whose Dataset Root is "Bob" then receives the selected entities
associated with Bob.

**Unfiltered Dataset**: An unfiltered Dataset includes every otherwise eligible Entity without further filtering.

**Implicitly Filtered Dataset**: An implicitly filtered Dataset restricts membership without a Filter Parameter. The
system supplies the filtering rule and its inputs.

**Parameter-Filtered Dataset**: A parameter-filtered Dataset restricts membership using a subscriber-supplied Filter
Parameter. In the typical Domgen integration, the developer identifies the Entity fields involved in routing and
defines the Filter Parameter supplied by the client. Domgen then generates the integration hooks used to customize
subscription and routing behavior.

A Fixed Filter Parameter cannot change while the Subscription at a Dataset Address persists. Changing it replaces the
Subscription. An Updatable Filter Parameter may change while retaining the same Subscription and Dataset Address. A
Filter Parameter is never part of the Dataset Address.

Datasets also support optional client caching. A Cacheable Dataset permits, but does not require, a complete
Subscription result's Change Set to be stored for one concrete Dataset Address with an opaque Dataset Cache Version.
Together, that Dataset Address, Dataset Cache Version, and serialized Change Set form a Dataset Cache Entry. On a
later request the client advertises the version, and the server either authorizes reuse of the equal current Dataset
Cache Entry or sends a fresh Change Set. Missing, unreadable, corrupt, or mismatched Dataset Cache Entries are
recoverable cache misses; failure to store an entry does not prevent the Subscription from being established.

Declaring a Dataset cacheable asserts that every authorized subscriber able to reuse one shared Dataset Cache Version
would receive an equal result. Subscriber-specific context that affects collection must participate in cache identity
and validation, or the Dataset must not be cacheable. Authorization is still evaluated for every Subscription request,
Required Type Datasets become available before dependent Dataset Cache Entries, and each stored Change Set is applied
atomically. The current server implementation does not support filtered or Instance Datasets as Cacheable Datasets;
this is an implementation capability limit rather than a domain definition.

It is possible and expected that one client may have Subscriptions to more than one Dataset, and the
materialized selections may overlap. Often applications link one Dataset to another and automatically
subscribe the client to the related Dataset.

Consider a roster application. The developer may define one Dataset that includes assignment of people
to activities on a single day. If the client was to subscribe to three days that shared people, then
the subscription would send the same people data down to the client multiple times. To avoid this the
developer can define another Dataset that contains details about people and **link** the day Dataset to
zero or more person Datasets.

TODO: Insert diagram here

It is also possible to define multiple Instance Datasets with the same Dataset Root Entity Type. For example, one
Dataset could include a person and related accreditations, while another includes the same person and related contact
details.

A Keyed Dataset is a Parameter-Filtered Dataset that allows a client to subscribe to multiple independently
addressable selections of the same Dataset. The Dataset Key distinguishes and routes each selection independently.
Keying is independent of whether the Filter Parameter is Fixed or Updatable.

The codebase often refers to the "Area of Interest" or AOI of a client. This declares that a Subscription should exist
at a Dataset Address using the latest desired Filter Parameter. Its satisfaction status is exactly `PENDING`,
`SATISFIED`, or `INVALIDATED`. Data Availability is reported independently because complete data can remain usable
while a changed Filter Parameter is pending. Invalidation is terminal for that Dataset Address within the Replicant
Context, including across reconnects.

The Dataset Root identifier for an Instance Dataset forms part of the Dataset Address. A Dataset Key is also part of
the address and is embedded in its descriptor after a `#` suffix. The Filter Parameter remains outside the Dataset
Address.

### Services

Within the replicant system, it is expected that changes to entities occur on the server-side and
are integrated with the replicant engine. The replicant client then has to make service calls to the
server-side to initiate changes. At the completion of the service call, the server component collects
all Entity Change Candidates captured for server-side entities during the service call and passes them to the
Replicant engine. Each Entity Change Candidate exists before routing and per-Subscription filtering. Replicant derives
Routing Keys to find Dataset Addresses that may contain the Entity, applies the Dataset Filter for each Subscription,
and records the resulting Filter Decision. One candidate may therefore produce Entity Changes for zero or more
Subscriptions, including Entity Changes that remove a Replica. Those client-visible Entity Changes are packaged into
Change Sets.

The service infrastructure within replicant is such that it is possible to treat services as either;

**fire and forget**: The client does not need to be notified when the service call completes.

**immediate return**: The client is notified when the service call returns, potentially receiving a result
 from the server. Any changes made to entities on the service _may not_ be present on the client.

**return when complete**: The client is notified when the service completes, potentially receiving a result
 from the server. Any changes made to entities on the service _must_ be present on the client.

### Change Notifications

Entity Changes are replicated out to the clients in Change Sets. Each Change Set typically represents a unit
of work, transaction or a single service call on the server-side. So all changes that occur within
a single transaction are routed and packaged as a single Change Set when sent to the client. The Change
Set is then applied atomically to the client-side replication. This is an attempt to provide some consistency
guarantees around the client-side representation.

After each server message is completely processed, Replicant emits a `MessageProcessedEvent` through the Spy subsystem
when spies are enabled. Its `MessageProcessingSummary` reports the Subscription Operations, Entity updates and
removals, and Replica links applied while processing the message. Replica changes remain atomic at the Change Set
boundary, so application observers see the resulting state only after the complete Change Set has been applied.

### Server-Side Broker Scheduling

On the server, `ReplicantMessageBrokerImpl` queues pending packets on the target `ReplicantSession` and
submits demand-driven drain tasks to the container-managed executor. Delayed retries and periodic session
maintenance use the container-managed scheduled executor. `ReplicantResources` exposes both resources through
qualified CDI producers. The broker coalesces work by session, so one session is processed by at most one
drain task at a time, and hot sessions are yielded after a bounded packet batch so other sessions can make
progress.

Replicant expects these managed executor JNDI resources:

* `java:replicant/concurrent/ManagedExecutorService`
* `java:replicant/concurrent/ManagedScheduledExecutorService`

The broker reads optional component environment entries from `java:comp/env` during startup:

* `replicant/broker/maxConcurrentDrainTasks`: maximum number of concurrent drain tasks. Defaults to
  `max(2, Runtime.getRuntime().availableProcessors())`.
* `replicant/broker/maxPacketsPerRun`: maximum packets processed for one session claim. Defaults to `64`.
* `replicant/broker/maxSessionsPerDrainTask`: maximum session claims processed by one drain task. Defaults
  to `64`.

Each value must be at least `1`. Missing entries use the defaults. Values may be numeric JNDI entries or
numeric strings; non-numeric strings, wrong types, and values below `1` fail startup.

## Client-Side Developer Components

There are several replicant components that developers directly interact with in client-side code.

### Replica Registry

The `ReplicaRegistry` owns the shared `ReplicaEntry` tracking wrapper for each Replica. Each `Subscription` records
which Replica Entries belong to it, allowing one Replica to be shared across multiple Subscriptions. Subscription
state is typically managed by the server, but client code can query both the registry and Subscription membership.

# History

Replicant is derived from several existing implementations of this strategy. It was initially based on code
extracted from a client-server Swing application and a client-server game server. However it is predominantly
used in GWT/HTML applications in it's current incarnation. Replicant also incorporates some ideas from
[HLA](http://en.wikipedia.org/wiki/High-level_architecture_\(simulation\)) extracted from a research project
conducted during the completion of a PhD.
