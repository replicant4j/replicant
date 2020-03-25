# TODO

Some actions that should occur at some point in the future.

* Consolidate some of the common code across server and client libraries. i.e.
  - Move ChannelMetaData into common and make domgen generate meta data in shared space.

* Add optional Map to Entity with current data and make userObject reference optional. The optional userObject
  could be lazily created on access via `EntityLocator`.

* Add `ReplicantContext.pause()` that pauses converger and runtime and update all the tests to use that.

* Introduce TransportFactory and pass that into context.
