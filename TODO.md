# TODO

Some actions that should occur at some point in the future.

* Change should take a ChannelDescriptor as a parameter.
* Consolidate some of the common code across server and client libraries. i.e.
  - Move ChannelMetaData into common and make domgen generate meta data in shared space.
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

* Move to CBOR or protobuf for serialization.
  - https://www.ietf.org/about/participate/tutorials/technical/cbor/
  - http://cbor.io/impls.html
  - https://github.com/dcodeIO/protobuf.js
  - Or event better and faster - Capt Proto - https://capnproto.org/language.html
  - Or Protobuf+gRPC = https://www.cncf.io/blog/2018/10/24/grpc-web-is-going-ga/

* Add optional Map to Entity with current data and make userObject reference optional. The optional userObject
  could be lazily created on access via `EntityLocator`.

* Add `ReplicantContext.pause()` that pauses converger and runtime and update all the tests to use that.

* Introduce TransportFactory and pass that into context.

* Add https://developer.mozilla.org/en-US/docs/Web/API/User_Timing_API to find out where time
  is and time each phase in message processing occurs

* Rather than DEFAULT_LINKS_TO_PROCESS_PER_TICK and DEFAULT_CHANGES_TO_PROCESS_PER_TICK try to use time based feedback

* Generate DebugTool to show client side replicant graph subscriptions

* Fix TODO in `ReplicantSession`

* Make sure that the requestId going back and forth is correct and increasing in sequence

* Change `invalidateSession()` to take a `ReplicantSession` rather than `sessionId`
