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

* Move to CBOR or protobuf for serialization.
  - https://www.ietf.org/about/participate/tutorials/technical/cbor/
  - http://cbor.io/impls.html
  - https://github.com/dcodeIO/protobuf.js

* Add optional Map to Entity with current data and make userObject reference optional. The optional userObject
  could be lazily created on access via `EntityLocator`.

* Add `ReplicantContext.pause()` that pauses converger and runtime and update all the tests to use that.

* Introduce TransportFactory and pass that into context.

* Add https://developer.mozilla.org/en-US/docs/Web/API/User_Timing_API to find out where time
  is and time each phase in message processing occurs

* Rather than DEFAULT_LINKS_TO_PROCESS_PER_TICK and DEFAULT_CHANGES_TO_PROCESS_PER_TICK try to use time based feedback
