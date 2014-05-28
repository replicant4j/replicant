Replicant
=========

[![Build Status](https://secure.travis-ci.org/realityforge/replicant.png?branch=master)](http://travis-ci.org/realityforge/replicant)

The Replicant library is a aimed at providing client-side state representation infrastructure for complex domain models that drive rich user experiences.

The library uses a client-side repository of objects, or replicas, that maintain the state of a subset of the world. Changes are transmitted from the server to the client and the replicas are dynamically updated. When the replica's are updated, changes are propagated through to the user interface through the use of events and a centralized event broker. To avoid the scenario where the UI is updated when the repository is an inconsistent state, changes are applied in changesets and only when the complete changeset has been applied are the changes propagated through the event broker.

Notes
-----

There exists several implementations of this strategy including one derived from a client-server Swing application and one extracted from a GWT application. This library aims at centralizing the code in one location and extracting the best parts of both libraries.

There is also plans to expand the library to save state into HTML5 client side stores such as Web Storage (a.k.a. Local Storage) or Indexed DB.
