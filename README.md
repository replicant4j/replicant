# Replicant

[![Build Status](https://api.travis-ci.com/replicant4j/replicant.svg?branch=master)](http://travis-ci.com/replicant4j/replicant)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.replicant/replicant-client.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.replicant%22%20a%3A%22replicant-client%22)
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

It should be noted that replicant is designed to be integrated with other technologies, most notably
[Domgen](https://github.com/realityforge/domgen), to provide a complete solution. It is most commonly
used with an Java EE server component and a GWT front-end but it was originally derived from a client-server
Swing application that used a custom application server.

## Core Concepts

### Entities and Replicas

Replicant assumes that there is a client-side representation of the domain model. Each entity within the
server-side domain model that is to be replicated to the client-side should have a client-side entity
that mirrors the server-side model. The client-side representation or replica, need not be identical
to the server-side model but one replica should map to one entity on the server. The replica may also
omit attributes and relationships that are not needed on the client. The state and lifecycle of the
replica's will be managed by the replicant system.

NB: It should be noted that as replicant was extracted and derived from several existing systems that
used slightly different terminology, you may see terms such as _imitation_ used to refer to client-side
entities or replicas. Over time these terms will be evolved out of the codebase and documentation.

### Graphs and Subscriptions

When a client connects to the replicant system, they are typically interested in a subset of the
data on the server; it is usually prohibitively expensive to transfer and store the entire server-side
domain model on the client. A more typical example is that a client wants to receive data about a subset
of the domain model, for example they may query:

* All payment classes.
* All alerts within a 50km radius of coordinate X
* All details about a particular vehicle or person
* All data pertaining to a particular roster over a particular date range
* etc.

Each of these queries is represented as a graph within replicant. When a client subscribes to a graph,
the client will receive an initial message that contains the state of the world at the time of
subscription, that match the query. All subsequent changes to the world that match the query will be
propagated to the subscribed clients until they unsubscribe or disconnect from the replicant system.

There are two major dimensions on which graphs are defined within the replicant system:
* Is the graph a type graph or an instance graph?
* Is the graph filtered or unfiltered?

**Type Graphs**: A type graph is used when you want to replicate instances in the domain model based on
 the type of the entity. It is common for applications to place common reference data in type graphs,
 so that the entire set of reference data received in one message.

**Instance Graphs**: An instance graph is used when you want to replicate details about a particular
 entity or root instance. All entities related to the root instance are considered to be part of the
 graph. An entity is related to the root instance if it references the root instance
 and the root instance can traverse to the entity. This relationship is transitive. For example, a
 `Person` entity may be referenced by the `Accreditation` entity, the `Accreditation` entity may be
 referenced by the `EvaluationResult` entity. If the `Person` entity is able to traverse to the
 `EvaluationResult` entity via the `Accreditation` entity then all three would be included in the
 instance graph rooted at a particular person. When the client subscribes to the Person graph with
 the root set to the person "Bob", then they will receive all of Bob's `Accreditation`s and all of
 Bob's `EvaluationResult`s. The developer may also explicitly shape and prune the graph so include
 or exclude entities from the instance graph.

**Unfiltered Graph**: An unfiltered graph includes all entities in the _type graph_ or _instance graph_
 without further filtering.

**Filtered Graph**: An filtered graph allows the developer to customize which entity instances are
 included in the graph. In the typical scenario where replicant is used in conjunction with domgen,
 the developer specifies which fields of which entities participate in the routing decision and the
 parameter that the client passes to the replicant engine to control the routing. Domgen then generates
 some template methods that the developer must implement to customize the subscription and routing
 capabilities.

There is several other features of graphs within the replicant engine, but these are typically used to
meet operational or system requirements. Two common features used in most replicant implementations are
cacheable graphs and making filter parameters in filtered graphs immutable.

A cacheable graph is used when the data within the graph has a relatively low frequency of change,
the volume of data is relatively large or the time to load the data from the database is relatively
long. If a graph is cacheable, then the client will store the entire graph in a client-side cache
along with a cache-key that supplied by the server. When the client re-requests that graph data, it
supplies the cache-key and the server can either indicate to the client should use the cached version
or send a new version of the data contained within the graph.

Immutable filter parameters indicate that it is not possible to update a parameter supplied during
subscription and that the client will need to unsubscribe and re-subscribe to change the parameter.
For example, if the graph for "All alerts within a 50km radius of coordinate X" has an immutable
parameter for X, then the only way to change X is to unsubscribe from the graph and re-subscribe
supplying another value for parameter X. Immutable filter parameters are used to optimize routing
and subscription mechanics.

It is possible and expected that one client may be subscribed to more than one graph and the graphs
may be overlapping. Often applications will be built such that one graph will link to another graph
and automatically subscribe the client to the related graph.

Consider a roster application. The developer may define one graph that includes assignment of people
to activities on a single day. If the client was to subscribe to three days that shared people, then
the subscription would send the same people data down to the client multiple times. To avoid this the
developer can define another graph that contains details about people and **link** the day graph to
zero or more person graphs.

TODO: Insert diagram here

It is also possible to define multiple instance graphs for a single entity. For example there could be
one graph that includes a person and all their related accreditations, and another graph that includes
a person and all their related contact details.

The codebase often refers to the "Area of Interest" or AOI of a client. This essentially indicates
whether an entity is contained within one of the graphs that client is subscribed to.

NB: The codebase(s) for replicant map graphs to channels or data channels at the transport layer.
The identifier for the root entity in instance graphs is used to name a sub-channel. This is useful
to understand when monitoring the communication between replicant clients and the replicant engines.

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

## Client-Side Developer Components

There are several replicant components that developers directly interact with in client-side code.

### EntitySubscriptionManager

The `EntitySubscriptionManager` records the state of subscriptions on the client-side. This includes
which graphs are subscribed two and the mapping between client-side replicas and the graph(s) that
caused the replica to be replicated to the client. The subscription state is typically managed by the
server-side but it is not uncommon for clients to query which subscriptions state.

# History

Replicant is derived from several existing implementations of this strategy. It was initially based on code
extracted from a client-server Swing application and a client-server game server. However it is predominantly
used in GWT/HTML applications in it's current incarnation. Replicant also incorporates some ideas from
[HLA](http://en.wikipedia.org/wiki/High-level_architecture_\(simulation\)) extracted from a research project
conducted during the completion of a PhD.
