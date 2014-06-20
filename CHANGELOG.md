## 0.5.17:
* Move to GWT 2.6.1.
* Add toString to ChannelLink and ChannelDescriptor to ease debugging.
* Avoid using Class.getSimpleName() in EntitySubscriptionManagerImpl to support GWT 2.5.1.
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
