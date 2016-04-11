## 0.5.55 (Pending):
* Update simple-session-filter dependency to enable CDI support for session managers.
* Ensure ReplicantSessionManager and ReplicantJsonSessionManager can be CDI beans by removing final
  methods and adding tests to enforce this feature.

## 0.5.54:
* Remove per request hash as the "Pragma: no-cache" header gets around caching in proxy servers.
* Fix implementation of `JsoChange.containsKey` so that the method will return true even if the value is null.

## 0.5.53:
* Set "Pragma: no-cache" header when polling for changes.
* Generate a per request hash added to each poll request to punch through overly zealous caching proxy servers.
* Revert to using @EJB rather than @Inject for ReplicantPollSource to work-around limitations when deploying to GlassFish.

## 0.5.52:
* Specify further header in CacheUtil to avoid caching.

## 0.5.51:
* Add some documentation to README covering the basic concepts.
* Eliminate BadSessionException and require AbstractDataLoaderService to implement ensureSession().

## 0.5.50:
* Update the AbstractDataLoaderService so that it only purges subscriptions that are "owned" by
  the data loader service and ignores any subscriptions owned by other data loaders.
* Add EntityChangeBroker.removeAllChangeListeners() to purge listeners for a specific entity.
* Update EntitySubscriptionImpl to remove type map when empty.
* Ensure that entities are unloaded from EntityRepository and listeners in the change broker
  are removed when subscriptions are removed as part of disconnect() method in AbstractDataLoaderService.
* Update EntityRepositoryDebugger to add methods to support debugging of subscriptions.

## 0.5.49:
* Add a guard in EntityMessageCacheUtil so that if EntityMessageCacheUtil is accessed outside of a
  replication context, an exception is thrown. This forces all entity modifications to occur within
  a replication context.

## 0.5.48:
* Clear all state stored in TransactionSynchronizationRegistry in ReplicationRequestUtil.completeReplication()
  so that multiple replication contexts can be started in the scope of one transaction.
* Update AbstractReplicationInterceptor to add useful logging at FINE level.
* Rework AbstractReplicationInterceptor so that it always starts and stops a replication context
  and clears all state in ReplicantContextHolder when it is no longer required.
* In ReplicationRequestUtil.startReplication() ensure that the registry is cleared of old values
  of SESSION_ID_KEY and REQUEST_ID_KEY in case multiple replication contexts occur within the
  scope of one transaction.
* Add ReplicantContextHolder.getContext() to expose a copy of the current replication context.
* In ReplicationRequestUtil guard against overlapping replication contexts calls by
  raising an exception if attempting to start a new replication context when one is
  already active.
* Fix bug in ReplicantSessionManager that attempted use ReplicantContextHolder to flag an
  incomplete request rather than the TransactionSynchronizationRegistry.
* Update ReplicantContextHolder.remove() so that it returns the value that was removed.
* Remove duplicated "lastAccessedAt" json key in output for SessionRestService.

## 0.5.47:
* When a client-side session is disconnected, delete the server-side session.
* Upgrade to compile and test against GWT 2.7.0.
* Replace TokenRestService with enhanced SessionRestService that provides status details
  about the session and other candidate sessions.
* Add field_filter dependency to expand the capabilities of the token service.
* Add a template method ReplicantSession.emitStatus() to make it possible for sub-classes
  to provide additional details about session status.

## 0.5.46:
* Add CollectorEntityChangeListener that records entity change events, useful during testing.
* Introduce ReplicantClientTestModule in the client-side test code that registers the standard set
  of client-side dependencies. Update AbstractClientTest to add ReplicantClientTestModule to the
  list of modules.

## 0.5.45:
* Remove unused EntityMessageGenerator.
* Remove ChangeRecorder as downstream projects no longer use the class and instead
  generate the entire artifact.

## 0.5.44:
* Re-add inadvertently removed resumeBroker() and pauseBroker() to AbstractClientTest.
* Add some minimal tests around how the WebPollerDataLoader will handle system failures

## 0.5.43:
* Add abstract method AbstractDataLoaderService.getSystemKey() that helps identify which replication
  system that the data loader is supporting. This is important when replicant is used with multiple
  replication data sources.
* Introduce an EntityBrokerTransaction structure that identifies the data loader that initiated the
  "transaction" (a.k.a the disable or pause actions).

## 0.5.42:
* Ensure TokenRestService sets HTTP headers so IE performs no caching.

## 0.5.41:
* Ensure ReplicantPollResource sets HTTP headers to ensure IE performs no caching.
* Extract out a constant for the query parameter used to identify the last received packet sequence.

## 0.5.40:
* Send a DataLoadCompleteEvent when a data load completes.
* Send a SystemErrorEvent on the EventBus when a system error occurs.
* Merge the WebPollerDataLoaderService.connect() method from downstream projects as they are all
  identical in their implementation.
* Extract TokenRestService from downstream projects. The TokenRestService simply generates a new
  session and returns the token.

## 0.5.39:
* Introduce EntityChangeListenerAdapter to make writing listeners easier.

## 0.5.38:
* Remove the ReplicationRequestManager abstraction as it implies a transactional boundary crossing
  which is not the intention. Implement the same functionality as a collection of static methods
  in the ReplicationRequestUtil utility class.

## 0.5.37:
* Add AbstractDataLoaderService.supportMultipleDataLoaders() template method that should return
  true if the data loader source loader should gracefully share common resources between data
  loaders. Avoid Pausing already paused loader during data load processing when this return true.

## 0.5.36:
* Avoid ConcurrentModificationException in AbstractDataLoaderService.unsubscribeInstanceGraphs
  by duplicating list before unsubscribing.

## 0.5.35:
* Extract the handling of context management from AbstractReplicationInterceptor into
  ReplicationRequestManager and AbstractReplicationRequestManager.

## 0.5.34:
* Fix concurrency bug triggered in AbstractDataLoaderService.updateSubscriptionForFilteredEntities
  when entities are removed from subscription.

## 0.5.33:
* Add Runnable parameters to the connect and disconnect methods in WebPollerDataLoaderService.

## 0.5.32:
* Add source channel to ChannelLink.

## 0.5.31:
* Default ReplicantPollResource to 30 seconds long polling before making another request.

## 0.5.30:
* Add support for debugging entity subscriptions and requests as well as local
  repository state.
* If repository debug output is enabled in application then print out a helper message
  when the GwtDataLoaderService is defined. Helps developers remember this feature is
  present.
* Associate a symbolic key with each session context. Prefix log messages using key and
  use it to restrict debugging to a particular GwtDataLoaderService subclass.

## 0.5.29:
* Update WebPollerDataLoaderService to annotate the exception in handleSystemFailure as nullable.
* Make ReplicantRpcRequestBuilder a non final class.
* Update SessionContext to have a per-session base url.
* Remove reference to unused module (com.google.gwt.rpc.RPC) that is not present in GWT 2.7.

## 0.5.28:
* Support the ability to debug just a single DataSourceLoader services
  changes.
* Add ReplicantDev.gwt.xml configuration that turns on all debug features.
* Exclude test classes form GWT compiler's path.

## 0.5.27:
* Introduce AbstractClientTest to help write client-side tests.

## 0.5.26:
* Remove the usage of a Synchronized map and replace with ConcurrentHashMap in
  ReplicantPollResource as the version if jersey in GlassFish 4.1.0 can result in deadlocks
  when timeouts are triggered.
* Remove final qualifier for method in ReplicantPollResource as CDI attempts to intercept
  method in GlassFish 4.1.0.
* Refactor SessionContext is not static, thus allowing multiple contexts within a single
  application.

## 0.5.25:
* Introduce ReplicantJsonSessionManager as all downstream projects use json as their
  transport layer.
* Rename ReplicantSessionManager.poll() to pollPacket to make it easier to sub-class.
* Backport, test and generalize ReplicantPollResource from downstream libraries. This class
  makes it easy to setup polling for replicant based systems. Derive the default poll
  url in the WebPollerDataLoaderService assuming the ReplicantPollResource implementation.

## 0.5.24:
* Extract utility method AbstractDataLoaderService.unsubscribeInstanceGraphs().
* Add close handler in WebPollerDataLoaderService that disconnects WebPoller
  when the windows closes.
* Correct nullability annotation for filterParameter in
  AbstractDataLoaderService.requestUpdateSubscription().

## 0.5.23:
* Implement WebPollerDataLoaderService to simplify construction of polling based data loaders.
* Implement purging of subscriptions when the session changes.

## 0.5.22:
* Move to EE7.
* Fix bug in ChangeAccumulator where change initiator can be incorrectly identified
  as having been routed to if a changeset was was accessed via getChangeSet()
  but no message was ever added to ChangeSet.

## 0.5.21:
* Remove BadSessionException and associated ensureSession so domgen can generate
  a customized implementation.

## 0.5.20:
* Move the responsibility for validating the entities in the EntityRepository to
  the EntitySubscriptionValidator.

## 0.5.19:
* Move the responsibility for deleting the entities from the EntityRepository from
  the EntitySubscriptionManager to the DataLoaderService.

## 0.5.18:
* Initial work to add debugging capability to the EntityRepository via a helper
  class EntityRepositoryDebugger.
* Extract AbstractDataLoaderService.updateSubscriptionForFilteredEntities so that
  subclasses can control the order in which types are unloaded due to a change in
  filter.
* Add EntityRepository.findAllIDs method to get ids for entities.

## 0.5.17:
* Support the replication of the filter between the server and client.
* Update the client to unregister entities that are filtered after a channels
  filter is updated.
* Start to record filter on EntitySubscriptionManager on the client-side.
* Move to GWT 2.6.1.
* Move to Java 7.
* Add toString to ChannelLink and ChannelDescriptor to ease debugging.
* Correct the namespace in ReplicantConfig so that the property is read correctly.
* Pass ReplicantConfig through the constructor to make it easier to test.

## 0.5.16:
* Associated with each change, the channel(s)  (a.k.a. subscription(s)) which resulted in
  the change replicating to the client.
* Replicate record of subscription changes to the client.

NOTE: This is a large change and further details are in the source control system.

## 0.5.15:
* Rework ChangeRecorder to make sub-classing easier.
* Rename SubscriptionEntry.subscriptionData to filterParameter to match domgen conventions.
* Use constructor based injection for DataLoaders.
* Extract a separate queue of actions to control subscription in the DataLoaders.

## 0.5.14:
* Associate a key with each request that corresponds to the operation being performed.
* Merge RequestManager and AbstractSessionManager into ClientSession.
* Add dependency on gwt-property-source library so that the choice on whether to
  validate repository after loads is controlled using a compile time configuration
  property.

## 0.5.13:
* Add support for Verifiable interface for entities that can validate their own state.
* Support registration of interfaces in EntityRepository.
* Add support for detecting whether an entity has been lined via Linkable.isLinked().
* Reconfigure the Linkable interface so that implementations no longer implement
  delink and instead rely on the repository invoking invalidate.
* Rework ChangeRecorder to make it easier to sub-class and customize behaviour.

## 0.5.12:
* Mark the EntityMessageSet class as final.
* Add EntityMessageSet.containsEntityMessage(...) to test whether the set
  contains a message.

## 0.5.11:
* Add support for recording arbitrary data in SubscriptionEntry.
* Add AbstractSubscriptionManager.find(Instance|Type)GraphSubscription methods.

## 0.5.10:
* Restore compatibility with JDK 6.

## 0.5.9:
* Support encoding of Longs as strings in change sets.

## 0.5.8:
* Move remaining client specific state in AbstractDataLoaderService to ClientSession.
* Enhance AbstractDataLoaderService.setSession so that it resets state on
  change. The state reset is deferred until the current action is completed.
* Move AbstractDataLoaderService into the transport package.
* Provide an implementation of GwtDataLoaderService.scheduleDataLoad().
* Introduce abstract ReplicantSessionManager to simplify creation of session
  managers.
* Refactor PacketQueue.addPacket to return newly created packet.
* Fix bug in PacketQueue.ack to ignore attempt to ack past acked sequence, and to
  raise an exception if attempting to ack a future packet sequence.
* Add support for AbstractDataLoaderService.getSessionID() utility function.

## 0.5.7:
* Add AbstractDataLoaderService.onTerminatingIncrementalDataLoadProcess() template
  method to provide a useful extension point for subclasses.
* Make it possible for the user to set explicitly set REQUEST_COMPLETE_KEY.
* Enhance EntityMessageAccumulator to support adding collections of EntityMessages.
* Enhance EntityMessageCacheUtil to support variants of method that lookup
  TransactionSynchronizationRegistry in JNDI.
* Enhance EntityMessageSet to support merging collections of EntityMessage.
* Enhance EntityMessageSet to support optional cloning of EntityMessage instances
  on initial merge.
* Define mechanisms for executing out-of-band messages. These are of particular
  relevance when reloading cached content.
* Add ReplicantContextHolder.contains() utility function.
* Remove the onBulkLoadComplete() and onIncrementalLoadComplete() extension
  points in AbstractDataLoaderService as they are subsumed by onDataLoadComplete()
* Define and implement a CacheService interface and associated implementation.
* Start to provide mechanisms for marking change sets with pseudo etags. This
  would support client-side caching of data.

## 0.5.6:
* In the DataLoaderService, do not execute runnable unless the RequestEntry has
  been successfully processed.
* In the DataLoaderService, remove the RequestEntry if it has been successfully
  processed.
* Support recording on the RequestEntry that the results have arrived to counter
  the race scenario where the change set arrives prior to the the request
  returning.

## 0.5.5:
* Move responsibility for the determination of which change sets are bulk loads
  from the DataLoaderService to the code that creates requests on with the
  RequestManager.
* In the AbstractDataLoaderService avoid resume-ing or re-enabling the broker if
  it was never disabled. This is possible if the server sends a change set with
  zero changes.
* Cache the JsonGeneratorFactory in JsonEncoder for performance reasons.
* Rework the transport system so that each packet can record the request id which
  generated the change set. This is only done when the packet is for the session
  which initiated the job. Support this via ReplicantRpcRequestBuilder that is
  integrated into GWT-RPC.
* Introduce ReplicantSession an base class from which sessions should extend.
* Rework AbstractReplicationInterceptor so that it retrieves the session and request
  context information from the ReplicantContextHolder and passes it along to the
  EntityMessageEndpoint. The EntityMessageEndpoint will return true if the messages
  impact the initiating session.
* Introduce ReplicantContextHolder to ease passing of context information between
  tiers.

## 0.5.4:
* Import BadSessionException from downstream projects.
* Add synchronized modifier to several methods of PacketQueue.
* Import AbstractSubscriptionManager and SubscriptionEntry to help manage
  client-side representation of subscriptions.
* Rework AbstractDataLoaderService so that incoming packets are only processed
  in sequence order.
* Ensure the AbstractDataLoaderService pauses or disables the broker after it
  has parsed the changeset.
* Initialize AbstractDataLoaderService._lastKnownChangeSet to 0 so sequence
  numbers are sequential.
* Throw an exception if null is passed into rawJsonData parameter of the
  AbstractDataLoaderService.enqueueDataLoad method.
* Fix bug in Packet.isLessThan().

## 0.5.3:
* Move EntityMessageAccumulator to the transport package and re-target it to
  deliver messages to the PacketQueue.
* Introduce the Packet and PacketQueue to support creating the transport layer.
* Add Gin module that defines EntityMessageBroker and EntityRepository instances.
* Update EntityChangeListener to add a callback method that is called when
  and entity is added to the system.
* Make EntityChangeBrokerImpl.shouldRaiseErrorOnEventHandlerError() final.

## 0.5.2:
* Add @Replicate annotation to help define interceptor in EE application.
* Link the ChangeRecorder to the EntityMessageGenerator to reduce boilerplate
  code in dependent projects.
* In EntityChangeBrokerImpl, default to raising an IllegalStateException if
  there is an error handling events but support suppression of exceptions via
  EntityChangeBrokerImpl.setRaiseErrorOnEventHandlerError().
* Add EntityMessageAccumulator to help collect and forward EntityMessages to
  the respective clients.
* Add EntityMessageEndpoint to abstract the endpoint to which the interceptor
  delivers EntityMessage instances. Adjust the AbstractReplicationInterceptor
  to deliver messages to endpoint.
* Add the EntityMessageGenerator interface to abstract over code responsible
  for generating EntityMessage instances.
* Import EntityMessageSorter to make it easier to share sorting mechanisms
  across projects that use replicant.

## 0.5.1:
* Include source in jar file to make it easier to integrate with GWT.

## 0.5:
* Remove the Async*Callback interfaces now that they are generated by Domgen.
* Move the RDate, Date*Serializer and Date*Deserializer classes to gwt-datatypes
  library.
* Move the org.realityforge.replicant.client.json.gwt.JsoReadOnly* collections
  to org.realityforge.gwt.datatypes.client.collections in the gwt-datatypes package.
* Add GwtDataLoaderService to simplify creating a DataLoader in GWT.
* Add template method AbstractDataLoaderService.shouldValidateOnLoad() that will
  validate the entity repository on each change. Useful to override and return true
  during development or in debug mode.

## 0.4.8:
* Change AbstractReplicationInterceptor so that subclasses must override a template
  method to provide the EntityManager. The purpose of this change is to allow for
  the use of this library in applications that have multiple persistence contexts.
