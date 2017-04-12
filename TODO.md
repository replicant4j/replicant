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
