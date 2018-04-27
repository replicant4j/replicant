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

* Remove AreaOfInterestListener, ReplicantSystemListener, DataLoaderListener - replace with observable Arez state?

* Merge all interface/impl separations - i.e. ReplicantConnection

* All of the AreaOfInterestListener infra should move to using AreaOfInterest object?

* Channel should be disposeOnDeactivate?

* Upgrade Arez

* Figure out a way how to move Linkable, Verifiable and EntityLocator to Arez. Will need to somehow annotate
  inverse relationships and have Arez generate glue-code to link up outside of accessor. Will also need to mark
  some properties as links ... some of which are lazy loaded.
