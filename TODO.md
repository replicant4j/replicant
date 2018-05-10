# TODO

Some actions that should occur at some point in the future.

* ChannelDescriptor should emit the "system" in toString (i.e. Rose, Planner etc)
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

* Channel should be disposeOnDeactivate?

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

* Add optional Map to Entity with current data and make userObject reference optional. The optional userObject
  could be lazily created on access via `EntityLocator`.

* Consider extracting out transport layer of replicant. The whole system could be relatively easily tested in
  isolation with `ChangeMapper` updating this entity and all driven by metadata loaded into system.

* Enhance spy system.
  - Entity create/update/destroy changes should all result in spy events
  - ReplicantSystem state changes should result in spy events
  - Requests should result in spy events
  - Add ability to dump subscriptions as table?
  - `Replicant.can*DebugOutputBeEnabled()` should be removed.
  - Should add `Replicant.shouldEntityChangesProduceSpyEvent()` so that ehy can be filtered out in common case.

* The `Channel` class should be made package access and the `AreaOfInterest` and `Subscription` should expose
  the publicly accessible details (i.e. Filter read? ChannelAddress).

* Make sure ConsoleLogger logs all spy events
