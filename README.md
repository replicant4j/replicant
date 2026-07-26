# Replicant

[<img src="https://img.shields.io/maven-central/v/org.realityforge.replicant/replicant-client.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.replicant%22%20a%3A%22replicant-client%22)
[![CI](https://github.com/replicant4j/replicant/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/replicant4j/replicant/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/replicant4j/replicant/branch/master/graph/badge.svg)](https://codecov.io/gh/replicant4j/replicant)

The replicant library aims to provide infrastructure for replicating a portion of a complex server-side
domain model to zero or more clients who have subscribed to the replication engine. When changes are
applied on the server-side, the changes are batched and transmitted to interested clients. Upon receiving
the changes, the client will atomically apply the changes to a local client-side representation. The
application is then notified of the changes via a local message broker.

The library uses a client-side repository of objects, or replicas, that maintain the state of a subset of
the world. Changes are transmitted from the server to the client and the replicas are dynamically updated.
When the replica's are updated, changes are propagated through to the user interface through the use of events
and a centralized event broker. To avoid the scenario where the UI is updated when the repository is an
inconsistent state, changes are applied in changesets and only when the complete changeset has been applied are
the changes propagated through the event broker.

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

Each query is represented by a reusable Dataset definition. A Subscription materializes that Dataset at a
Dataset Address for one client. The client receives an initial message containing the matching state of the
world, followed by subsequent matching changes until it unsubscribes or disconnects.

Datasets have several independent dimensions:

* Selection shape: Type Dataset or Instance Dataset.
* Filter source, represented by `FilterMode`: `UNFILTERED`, `IMPLICIT`, or `PARAMETER_FILTERED`.
* Keying: unkeyed or keyed. Keying is valid only for a Parameter-Filtered Dataset.

A Parameter-Filtered Dataset also has a `FilterParameterMode`: `FIXED` or `UPDATABLE`.

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

Datasets also support operational features such as caching. Cacheable Datasets are unfiltered Type Datasets whose data
changes relatively infrequently, is relatively large, or is relatively expensive to load.

A cacheable Dataset is used when the data within the Dataset has a relatively low frequency of change,
the volume of data is relatively large or the time to load the data from the database is relatively
long. If a Dataset is cacheable, then the client will store the materialized selection in a client-side cache
along with a cache-key supplied by the server. When the client re-requests that Dataset, it
supplies the cache-key and the server can either indicate that the client should use the cached version
or send a new version of the selected data.

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

The codebase often refers to the "Area of Interest" or AOI of a client. This declares that a Subscription
should exist at a Dataset Address and records progress toward that desired state.

The Dataset Root identifier for an Instance Dataset forms part of the Dataset Address. A Dataset Key is also part of
the address and is embedded in its descriptor after a `#` suffix. The Filter Parameter remains outside the Dataset
Address.

### Services

Within the replicant system, it is expected that changes to entities occur on the server-side and
are integrated with the replicant engine. The replicant client then has to make service calls to the
server-side to initiate changes. At the completion of the service call, the server component collects
all changes that were made to the server-side entities during the service call and passes them to the
replicant engine. The replicant engine is then responsible for replicating changes out to the interested
clients.

The service infrastructure within replicant is such that it is possible to treat services as either;

**fire and forget**: The client does not need to be notified when the service call completes.

**immediate return**: The client is notified when the service call returns, potentially receiving a result
 from the server. Any changes made to entities on the service _may not_ be present on the client.

**return when complete**: The client is notified when the service completes, potentially receiving a result
 from the server. Any changes made to entities on the service _must_ be present on the client.

### Change Notifications

Changes are replicated out to the clients in Change Sets. Each change set typically represents a unit
of work, transaction or a single service call on the server-side. So all changes that occur within
a single transaction are routed and packaged as a single change set when sent to the client. The change
set is then applied atomically to the client-side replication. This is an attempt to provide some consistency
guarantees around the client-side representation.

After a change set is applied a `DataLoadComplete` message is fired on the client-side. To get fine-grain
notification of changes, the developer can register listeners on the client-side broker and receive
notification when an entity is added, removed or updated. This is only possible when the change set is
marked as an _incremental load_ rather than as a _bulk load_. The vast majority of service calls result
in _incremental load_ change sets, but sometimes for the sake of performance subscribe service calls and
other calls that result in mass change may result in _bulk load_ change sets.

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
