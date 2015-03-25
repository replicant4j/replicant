# Replicant

[![Build Status](https://secure.travis-ci.org/realityforge/replicant.png?branch=master)](http://travis-ci.org/realityforge/replicant)

The replicant library aims to provide infrastructure for replicating a portion of a complex server-side
domain model to zero or more clients who have subscribed to the replication engine. When changes are
applied on the server-side, the changes are batched and transmitted to interested clients. Upon receiving
the changes, the client will atomically apply the changes to a local client-side representation. The
application is then notified of the changes via a local message broker.

It should be noted that replicant is designed to be integrated with other technologies, most notably
[Domgen](https://github.com/realityforge/domgen), to provide a complete solution. It is most commonly
used with an Java EE server component and a GWT front-end but it was originally derived from a client-server
Swing application that used a custom application server.

## Core Concepts

### Entities and Replicas

Replicant assumes that there a client-side representation of the domain model. Each entity within the
server-side domain model that is to be replicated to the client-side should have a client-side entity
that mirrors the server-side model. The client-side representation or replica, need not be identical
to the server-side model but one replica should map to one entity on the server. The replica may also
omit attributes and relationships that are not needed on the client. The state and lifecycle of of the
replica's will be managed by the replicant system.

NB: It should be noted that as replicant was extracted and derived from several existing systems that
used slightly different terminology, you may see terms such as _imitation_ used to refer to client-side
entities or replicas. Over time these terms will be evolved out of the codebase and documentation.

# Old Documentation

The Replicant library is a aimed at providing client-side state representation infrastructure for complex domain models that drive rich user experiences.

The library uses a client-side repository of objects, or replicas, that maintain the state of a subset of the world. Changes are transmitted from the server to the client and the replicas are dynamically updated. When the replica's are updated, changes are propagated through to the user interface through the use of events and a centralized event broker. To avoid the scenario where the UI is updated when the repository is an inconsistent state, changes are applied in changesets and only when the complete changeset has been applied are the changes propagated through the event broker.

# Notes

There exists several implementations of this strategy including one derived from a client-server Swing application and one extracted from a GWT application. This library aims at centralizing the code in one location and extracting the best parts of both libraries.

There is also plans to expand the library to save state into HTML5 client side stores such as Web Storage (a.k.a. Local Storage) or Indexed DB.

# TODO

* Test WebPollerDataLoaderService
* Test SessionRestService
