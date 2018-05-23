# TODO

Some actions that should occur at some point in the future.

* Change should take a ChannelDescriptor as a parameter.
* The Client/Server communication should be over generic interface and avoid generating typed
  interfaces via domgen. i.e. Eliminate sub-classing of ReplicantSessionManagerImpl by using
  generic interface. The remaining custom code can be moved to a context class.
* Introduce an EntityMetaData class that describes type id and possibly other data (i.e. can
  it generate channel links, attribute defs etc)
* Introduce EntityDescriptor that binds EntityMetaData/type + ID.
* Consolidate some of the common code across server and client libraries. i.e.
  - Move ChannelMetaData into common and make domgen generate meta data in shared space.
  - Stop domgen generating XMessageConstants and use generated ChannelMetaData data.
  - Stop domgen generating XReplicantGraph and use generated ChannelMetaData data.
* Rework the request system so that all the inputs/outputs are explicit.
    Inputs include:
      * SessionID
      * RequestID
      * Job Name/Service+method name
      * Job Parameters
    Outputs include:
      * Request Complete Flag
      * Job Return value
    This will ultimately allow the jobs and potentially polling results to move to being
    handled across a WebSocket.

* Handle TODOs in Entity

* Figure out a way how to move Linkable, Verifiable and EntityLocator to Arez. Will need to somehow annotate
  inverse relationships and have Arez generate glue-code to link up outside of accessor. Will also need to mark
  some properties as links ... some of which are lazy loaded.

  - `Verifiable`: Is only used to verify that:
    * the entity is not disposed.
    * the `EntityLocator` when passed own `ComponentId` and type returns self.
    * All relationships are verified.
  - `Verifiable` should be completely optimized out in production builds.
  - `EntityLocator` should be passed in constructor to any entity that has relationships defined. The entity
    uses the `EntityLocator` to lookup any relationships. The lookup can occur on access or lazily (typically
    used for cross system links ala `acal` -> `calendar`). The lookup can also occur when `Linkable.link()`
    is invoked (for all non-lazy relationships). `link()` is typically after a message/transaction has updated
    all the required entities within the system.

* Move to CBOR for serialization.
  - https://www.ietf.org/about/participate/tutorials/technical/cbor/
  - http://cbor.io/impls.html

* Add optional Map to Entity with current data and make userObject reference optional. The optional userObject
  could be lazily created on access via `EntityLocator`.

* Enhance spy system.
  - Entity create/update/destroy changes should all result in spy events
  - ReplicantSystem state changes should result in spy events
  - Should add `Replicant.shouldEntityChangesProduceSpyEvent()` so that ehy can be filtered out in common case.

* Add `ReplicantContext.pause()` that pauses converger and runtime and update all the tests to use that.

* Remove type from `EntitySchema` and somehow get connector to manage mapping `EntitySchema` -> `Class`.
  The motivation for this is that then the `EntitySchema` can be used outside of runtime ... hopefully
  to build the source code for entities.

* Remove `Connector.SystemType` and replace with reference to schema.
  - Done but we should change the code that registers to ensure uniqueness
  - we should also start using to stop updates of filters that should not be.
  - Should create EntityMetaData and move EntitySchema.Type to it and also move factory methods for creating
    and updating userobject to it. This would allow us to eliminate `CHangeMapper`
  - Add `ChannelMetaData` that has `FilterParameterType` and functions for `doesEntityMatchFilter`

* Strategy for having JVM specific code.
  - Define class `MyServiceA` that is the gwt compatible variant. Extend this variant as `JvmMyServiceA`
    and override all the methods that use gwt code but annotate methods with `@GwtIncompatible`. Then just do
    `new JvmMyServiceA()` in code. GWT will remove all the `@GwtIncompatible` methods leaving actual gwt
    compatible code. JVM will run jvm code. Win-win but only works if the library can be rewritten to handle
    this approach.
