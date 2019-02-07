# Change Log

### Unreleased

* Update the `org.realityforge.arez` dependencies to version `0.128`.
* Update the `org.realityforge.react4j` dependencies to version `0.117`.
* Avoid invariant failure when updating the status of a `AreaOfInterest` for a subscription that
  is implicitly subscribed and is in the process of being explicitly subscribed but is currently
  in `NOT_ASKED` or `LOADING` state.
* Avoid dropping subscription requests in the `Connector` when the request is upgrading an
  implicit subscription to an explicit subscription.
* Fix `Converger` so that `AreaOfInterest` instances that match an existing subscription will only
  have their status updated if the existing subscription is explicit, otherwise the normal
  "request subscription" process will be initiated.

### [v6.27](https://github.com/realityforge/replicant/tree/v6.27) (2019-02-04)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.26...v6.27)

* Remove the prop `onNotAsked` from the `ReplicantSubscription` component as it represents
  a state that is never presented to the user and is followed in quick succession by the
  `OnLoading` state and can thus be replaced by the `onLoading` prop.

### [v6.26](https://github.com/realityforge/replicant/tree/v6.26) (2019-02-04)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.25...v6.26)

* Work around bug resulting from react4j upgrade that merged the `componentWillUnmount` and
  dispose steps for react4j components. Previously we were able to invoke `@Action` annotated
  methods in `componentWillUnmount()` to release resources but this is no longer possible. Arez
  should allow the use of `@CascadeDispose` in this context but due to current limitations in
  Arez this is not possible. A workaround until this limitation has been addressed, has been
  added to the `ReplicantSubscription` class.
* Upgrade Elemental2 artifacts to groupId `org.realityforge.com.google.elemental2`
  and version `1.0.0-b14-2f97dbe`. This makes it possible to use a newer version of the
  Elemental2 library in downstream products.

### [v6.25](https://github.com/realityforge/replicant/tree/v6.25) (2019-01-30)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.24...v6.25)

* Update the `org.realityforge.arez` dependencies to version `0.127`.
* Update the `org.realityforge.react4j` dependencies to version `0.114`.

### [v6.24](https://github.com/realityforge/replicant/tree/v6.24) (2019-01-18)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.23...v6.24)

* Fix access modifiers on `ReplicantSubscription.postRender()` so that subclasses can be in
  different packages.

### [v6.23](https://github.com/realityforge/replicant/tree/v6.23) (2019-01-18)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.22...v6.23)

* Fix the release process to push release to staging repository and Maven Central.

### [v6.22](https://github.com/realityforge/replicant/tree/v6.22) (2019-01-18)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.21...v6.22)

* Rename some react lifecycle methods in `ReplicantSubscription` so that they align with the names of
  the annotations rather than the names of the methods that needed to be overridden in the past.

### [v6.21](https://github.com/realityforge/replicant/tree/v6.21) (2019-01-17)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.20...v6.21)

* Remove deployment from TravisCI infrastructure as it is no longer feasible.
* Update the `org.realityforge.arez` dependencies to version `0.122`.
* Update the `org.realityforge.react4j` dependencies to version `0.110`.

### [v6.20](https://github.com/realityforge/replicant/tree/v6.20) (2018-11-20)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.19...v6.20)

* Update the `org.realityforge.arez` dependencies to version `0.115`.
* Update the `org.realityforge.react4j` dependencies to version `0.107`.

### [v6.19](https://github.com/realityforge/replicant/tree/v6.19) (2018-11-08)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.18...v6.19)

* Update the `org.realityforge.arez` dependencies to version `0.114`.
* Update the `org.realityforge.react4j` dependencies to version `0.106`.

### [v6.18](https://github.com/realityforge/replicant/tree/v6.18) (2018-11-02)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.17...v6.18)

* Enhance the console message for the spy event `AreaOfInterestStatusUpdatedEvent` to include the
  status that the `AreaOfInterest`  was changed to.
* Fix the subscribe and unsubscribe to ensure they responded appropriately to the
  `X-Replicant-RequestComplete` header. Previously an `AreaOfInterest` request (i.e. a subscribe or
  unsubscribe) could occur multiple times if the response to the AOI request has not been received
  by the time the RPC call has returned. Historically this happened infrequently (if at all) but due
  to timing changes in other parts of the application this became a frequent occurrence. The fix was
  to treat subscribe/unsubscribe like any other request to ensure that they are sequenced appropriately.
* Update the `org.realityforge.arez` dependencies to version `0.111`.
* Update the `org.realityforge.react4j` dependencies to version `0.103`.

### [v6.17](https://github.com/realityforge/replicant/tree/v6.17) (2018-10-16)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.16...v6.17)

* Update the `org.realityforge.react4j` dependencies to version `0.102`.

### [v6.16](https://github.com/realityforge/replicant/tree/v6.16) (2018-10-09)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.15...v6.16)

* Fix bug in `ReplicantSubscription` react4j component was incorrectly comparing a primitive id
  and a boxed id in `componentDidUpdate()` lifecycle method.

### [v6.15](https://github.com/realityforge/replicant/tree/v6.15) (2018-10-09)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.14...v6.15)

* Correct bug where id was set as `0` for type graphs.

### [v6.14](https://github.com/realityforge/replicant/tree/v6.14) (2018-10-09)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.13...v6.14)

* Update the `org.realityforge.arez` dependencies to version `0.109`.
* Update the `org.realityforge.react4j` dependencies to version `0.100`.

### [v6.13](https://github.com/realityforge/replicant/tree/v6.13) (2018-10-04)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.12...v6.13)

* Change `ReplicantSubscription.getId()` to return a primitive integer rather than a boxed `Integer`.

### [v6.12](https://github.com/realityforge/replicant/tree/v6.12) (2018-09-27)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.11...v6.12)

* Upgrade the `org.realityforge.gwt.webpoller:gwt-webpoller:jar` artifact to version `0.9.8`.

### [v6.11](https://github.com/realityforge/replicant/tree/v6.11) (2018-09-25)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.10...v6.11)

* Remove `super.componentDidMount()` and `super.componentDidUpdate( prevProps, prevState )` calls from
  the `ReplicantSubscription` react component as not needed as parent methods are empty as of react4j
  version `0.96`
* Fix bug where converger will not re-converge if multiple AreaOfInterest are added simultaneously and the
  the later `AreaOfInterest` instances can not be grouped into the first `AreaOfInterest` instance. The
  converger would previously incorrectly halt after the first action completed.

### [v6.10](https://github.com/realityforge/replicant/tree/v6.10) (2018-09-21)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.09...v6.10)

* Update the `org.realityforge.arez` dependencies to version `0.107`.
* Update the `org.realityforge.react4j` dependencies to version `0.96`.
* Update the `org.realityforge.braincheck` dependencies to version `1.12.0`.

### [v6.09](https://github.com/realityforge/replicant/tree/v6.09) (2018-08-24)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.08...v6.09)

* During code-optimization the `Converger.converge()` method stopped observing filters when converging
  filters which mean that if the filter changed it would no longer re-converge the state of the world.
  This bug has been fixed by an explicit observe of the filter field on `AreaOfInterest`.

### [v6.08](https://github.com/realityforge/replicant/tree/v6.08) (2018-08-23)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.07...v6.08)

* Update the `org.realityforge.arez` dependencies to version `0.105`.
* Update the `org.realityforge.react4j` dependencies to version `0.93`.
* Remove the interface `org.realityforge.replicant.client.EntityRegistry` and related infrastructure.
  The code is currently unused and unlikely to be adopted in the near future.
* Remove the interface `org.realityforge.replicant.client.EntityLocator` and related infrastructure.
  This has been replaced by `arez.Locator` interface.
* Replace the interface `replicant.Linkable` with the `arez.component.Linkable` interface.
* Replace the interface `replicant.Verifiable` with the `arez.component.Verifiable` interface.

### [v6.07](https://github.com/realityforge/replicant/tree/v6.07) (2018-07-30)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.06...v6.07)

* Change the scope of the dependencies enlisted in the `@Autorun` actions on the `Converger`
  to eliminate monitoring of entities in `preConverge()` and to re-add dependencies on
  `AreaOfInterest` collection.

### [v6.06](https://github.com/realityforge/replicant/tree/v6.06) (2018-07-27)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.05...v6.06)

* Update the `org.realityforge.arez` dependencies to version `0.102`.
* Explicitly limit the scope of the dependencies enlisted in the `@Autorun` actions on the `Converger`
  so that only the data required to trigger changes are monitored.

### [v6.05](https://github.com/realityforge/replicant/tree/v6.05) (2018-07-26)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.04...v6.05)

* Change the scheduler used by the browser from using `setTimeout` to using `requestAnimationFrame`. This
  resulted in a shorter and more consistent delay between successive invocations.
* Fix bug in `Connector.onMessageProcessed()` that resulted in an invariant failure when spies were disabled
  but invariant checking was enabled.
* Avoid an invariant failure by passing null for request name when performing "Sync" action if
  `Replicant.areNamesEnabled()` returns false.
* Fix concurrency bug where an invariant failure was triggered if a request was removed by the polling code
  prior to the rpc request returning.
* Update the `org.realityforge.arez` dependencies to version `0.100`.
* Fix bug where the `Subscription` and `AreaOfInterest` enhanced arez subclasses failed to supply a valid
  id in production compiles. This resulted in the objects being incorrectly stored within containers and
  thus applications failing at runtime.
* Update the `org.realityforge.react4j` dependencies to version `0.87`.

### [v6.04](https://github.com/realityforge/replicant/tree/v6.04) (2018-07-24)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.03...v6.04)

* In the `ConsoleSpyEventProcessor` class, correct the log message when a `SubscriptionUpdateCompleted`
  event is generated.
* Fix a concurrency bug where the WebPoller could be left paused on receipt of a "ping" message.
* Add tasks to cleanup artifacts from staging repositories as part of the release process.

### [v6.03](https://github.com/realityforge/replicant/tree/v6.03) (2018-07-17)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.02...v6.03)

* Fix a bug in `Connector` related to interleaving of subscription removal and removal
  of the instance root that would result in invariant failure in normal scenario.
* Fix a bug in `Connector` related to processing the removal of an entity after the
  subscription that contained the entity has been removed.
* Upgrade the `org.realityforge.gwt.webpoller:gwt-webpoller:jar` artifact to version `0.9.7`.
* If the `connectionId` changes between when the `WebPollerTransport` starts and completes
  a subscribe or unsubscribe request then skip handling the response as it is no longer relevant.
* If the underlying connection has been disconnected or is in error then do not attempt
  to perform a poll action. Instead skip the request assuming that it will come good or
  that the poller will be stopped.
* Avoid initiating `subscribe` or `unsubscribe` requests if the connection has been
  disconnected. This can occur if a subscribe or unsubscribe action completes with an error.
* Avoid passing messages back to `Connector` via the  `TransportContext` if it has been
  disconnected. This can happen after an error occurs.
* Update the `org.realityforge.arez` dependencies to version `0.98`.
* Update the `org.realityforge.react4j` dependencies to version `0.84`.
* Implement `Transport.unbind()` so that `Connector` instances that disconnect or result in an error
  can disassociated with prior transport and successfully re-connect without the failure of the prior
  transport either disconnecting new connection or blocking the establishment of the new connection.
* Handle scenario where the `Connector.progressMessages()` method is called back by the timer but
  the underlying connection has already been disconnected. Infrequent occurrence in practice but this
  avoids inconsistent state when and error messages in the console when it does occur.
* Remove some unused `session` parameter from `ReplicantSessionManager.collectDataForSubscribe(...)`
  and `ReplicantSessionManager.collectDataForSubscriptionUpdate(...)` as not used and should never be
  used as methods accept parameters to describe the data required.
* Introduce `SyncRequest`, `InSyncEvent`, `OutOfSyncEvent` and `SyncFailureEvent` spy events to notify
  listeners when synchronization events occur.
* Add the ability to "ping" the backend from the client replicant application to check whether the
  backend has any requests in progress to thus perform a poor-mans network synchronization. This is
  still somewhat error prone if another party sends a message that is added to the servers message
  queue for pinging session and that message relies on existing implicit subscription already present
  that would be removed as an orphan subscription.
* Fix bug in the `AbstractSessionRestService` so that the `synchronized` flag is correct.
* Fix bug in `Converger.convergeAreaOfInterest(...)` so that if an `AreaOfInterest` is added when an
  existing `Subscription` already exists, the `AreaOfInterest` is marked as `LOADED` rather than getting
  stuck in the `NOT_ASKED` state.
* Enhance the `replicant.Transport.Context` interface to allow access to underlying request ids received
  and transmitted. Also support the recording of "sync" request ids so that synchronization attempts can
  be verified.
* Rename `replicant.Connection._lastRequestId` to `_lastTxRequestId` so that it is clear that the request
  id is the last id transmitted.
* Change the way request callbacks are invoked so that they are invoked _after_ the request has been
  removed from the connection. This allows the callback to inspect the connection to get the current
  connection state.
* Move to a J2CL compatible version of the jetbrains annotations libraries.
* Update the `org.realityforge.guiceyloops:guiceyloops:jar` dependency to version `0.95`.
* Make sure that `RequestEntry` instances are removed from the connection after they have been handled.
* Change the time at which orphan subscriptions are removed. Rather than on receipt of each message,
  the removal will only occur when a synchronization has happened on all data sources. This limits the
  window during which the server skips a subscription as the client is implicitly subscribed but the
  client is in the process of removing the subscription that it believes is an orphan.

### [v6.02](https://github.com/realityforge/replicant/tree/v6.02) (2018-07-03)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.01...v6.02)

* Fix `Connector.completeAreaOfInterestRequest()` to handle scenario where the area of interest
  request completes after a connection disconnects. This can happen as a result of errors during
  area of interest request or during normal overlapping requests.

### [v6.01](https://github.com/realityforge/replicant/tree/v6.01) (2018-07-02)
[Full Changelog](https://github.com/realityforge/replicant/compare/v6.00...v6.01)

* Link `EntitySchema` instances associated with channel to `ChannelSchema` to simplify validation
  of graph when or if needed.

### [v6.00](https://github.com/realityforge/replicant/tree/v6.00) (2018-07-02)
[Full Changelog](https://github.com/realityforge/replicant/compare/v5.99...v6.00)

* Stop using replicant specific mechanisms for managing state and move to Arez for state management.
  This is a major rewrite of the library and involved large scale changes. See the git history for
  full details.

### [v5.99](https://github.com/realityforge/replicant/tree/v5.99) (2018-04-26)
[Full Changelog](https://github.com/realityforge/replicant/compare/v5.98...v5.99)

* Make AreaOfInterest public to fix problem when deployed into EE container that attempts to proxy
  package access method that returns AreaOfInterest.  Submitted by James Walker.

### [v5.98](https://github.com/realityforge/replicant/tree/v5.98) (2018-03-27)
[Full Changelog](https://github.com/realityforge/replicant/compare/v5.97...v5.98)

* Increase timeout during replication server-to-server session establishment, to handle
  very large data sets. Bit of a hack.  Submitted by James Walker.

### [v5.97](https://github.com/realityforge/replicant/tree/v5.97) (2017-11-29)
[Full Changelog](https://github.com/realityforge/replicant/compare/v5.96...v5.97)

* Exposed more information in the `status` endpoint of `AbstractDataLoaderServiceRestService`.
  Add details of the timing, errors, and properties to the connection.   Submitted by James Walker.

### [v5.96](https://github.com/realityforge/replicant/tree/v5.96) (2017-11-21)
[Full Changelog](https://github.com/realityforge/replicant/compare/v0.06...v5.96)

* Add ability to query the `ContextConverger` to see if it is idle. Submitted by James Walker.

### [v0.06](https://github.com/realityforge/replicant/tree/v0.06) (2017-11-14)
[Full Changelog](https://github.com/realityforge/replicant/compare/v0.5.94...v0.06)

* Updated the `AbstractDataLoaderService` to support bulk loads. Aggregated consecutive AOI actions that can be
  grouped into a single bulk load. Submitted by James Walker.
* Updated the `ContextConvergerImpl` to schedule multiple AOI actions where they are compatible with bulk
  loading. Submitted by James Walker.

### [v0.5.94](https://github.com/realityforge/replicant/tree/v0.5.94)
[Full Changelog](https://github.com/realityforge/react4j/compare/v0.5.93...v0.5.94)

* in `AbstractSecuredSessionRestService`, check the `PreferredUsename` claim against the UserID associated with the
  Replicant Session, rather than the TokenID. Works with the change on 0.5.93. Submitted by James Walker.

### [v0.5.93](https://github.com/realityforge/replicant/tree/v0.5.93)

* in `ReplicantSecuredSessionManagerImpl`, use the `PreferredUsename` claim as the UserID associated with the
  Replicant Session, rather than the TokenID. The TokenID will change each time the token refreshes. An
  alternative is the `Subject` token but everywhere this is deployed also adds the claim `PreferredUsename`
  which is easier to read. Submitted by James Walker.

### [v0.5.92](https://github.com/realityforge/replicant/tree/v0.5.92)

* Use GWT super-source feature to replace `FilterUtil`.

### [v0.5.91](https://github.com/realityforge/replicant/tree/v0.5.91)

* Made all variants of `ReplicationRequestUtil.runRequest` public.

### [v0.5.90](https://github.com/realityforge/replicant/tree/v0.5.90)

* Restructure the way `BaseRuntimeExtension` converges subscriptions between graphs to take into account
  implicit subscriptions.
* Add `AreaOfInterestService.releaseScopesExcept` and `AreaOfInterestService.findOrCreateScope` helper methods.
* Add mechanisms for hooking into `ContextConverger` converge completion action.
* Refactor the `BaseRuntimeExtension.subscribe()` method so that it gracefully handles the scenario
  where the scope has an existing requirement. In this requirement return the existing reference.
* Improve generic types of `BaseRuntimeExtension.instanceSubscriptionToValues()`.
* Add `Scope.getSubscriptionReference()` helper method.
* Add fields to the `SubscriptionExistsException`,`SubscriptionAlreadyRequiredException` and
  `SubscriptionInactiveException` exceptions to aid debugging the application.
* Fix `ChannelMetaData` so that it supports filtered type graphs.

### [v0.5.89](https://github.com/realityforge/replicant/tree/v0.5.89)

* Move all subscription actions in `WebPollerDataLoaderService` to work via http actions rather than gwt_rpc.
* Introduce `ActionCallbackAdapter` to help managing replicant requests outside gwtrpc
* Support omitting `RequestEntry.RequestKey` during production builds. Removes a large number of strings in GWT
  compilation output. Controlled by use of setting in `ReplicantConfig`.
* Enhance server-side rest session service to accept optional `requestID` query parameter.

### [v0.5.88](https://github.com/realityforge/replicant/tree/v0.5.88)

* Remove `ReplicantConfig` and dependency on `gwt-property-source` and replace with simple access to property
  via `System.getProperty(...)`

### [v0.5.87](https://github.com/realityforge/replicant/tree/v0.5.87)

* Remove `EeContextConvergerImpl` and `EeReplicantClientSystemImpl` to allow downstream products to define services.
* Extract `AbstractEeContextConvergerImpl` class to simplify building ee context convergers.
* Make the `converge()` method public in the classes `ContextConvergerImpl` and `ReplicantClientSystemImpl`
  to make it easier to schedule converges in subclasses.

### [v0.5.86](https://github.com/realityforge/replicant/tree/v0.5.86)

* Ensure that disowned entities still send out events by waiting til the end of the cycle before purging listeners.

### [v0.5.85](https://github.com/realityforge/replicant/tree/v0.5.85)

* Remove attributes from `ReplicantSession`.
* Inline the `org.realityforge.ssf` dependency and compress inheritance chain for any class incorporated from ssf.
* Remove the `org.realityforge.rest.field_filter` dependency and remove usage from codebase. Effectively
  unused as it only used during local debugging of replicant in which case the filters were typically left
  at default values.
* Extract `AbstractInvocationAdapter` from `AbstractReplicationInterceptor` to make functionality reusable
  in other contexts.

### [v0.5.84](https://github.com/realityforge/replicant/tree/v0.5.84)

* Extract a helper method `newSessionBasedInvocationBuilder` in `EeWebPollerDataLoaderService`.
* Ensure interfaces can be bound into `EntityRepository` and generated messages via `EntityChangeBroker`.
* Remove `ReplicantGwtClientTestModule`, `AbstractClientTest` and `AbstractGwtClientTest` as no
  downstream users make use of any of these classes.

### [v0.5.83](https://github.com/realityforge/replicant/tree/v0.5.83)

* Make `ReplicantClientTestModule` more extensible and bind some missing elements required for tests.

### [v0.5.82](https://github.com/realityforge/replicant/tree/v0.5.82)

* Update GWT module to include `AbstractFrontendContextImpl` and friends for GWT compiler.

### [v0.5.81](https://github.com/realityforge/replicant/tree/v0.5.81)

* Introduce `AreaOfInterestListenerAdapter` to simplify writing custom listeners.

### [v0.5.80](https://github.com/realityforge/replicant/tree/v0.5.80)

* Introduce `AbstractFrontendContextImpl` to simplify creation of frontend context for gwt interfaces.

### [v0.5.79](https://github.com/realityforge/replicant/tree/v0.5.79)

* Introduce `org.realityforge.replicant.client.transport.SessionContext#request()` method to simplify
  request management.
* Enhance `ReplicantSessionManagerImpl` so support delink operations between graphs.
* Rename methods on `EntitySubscriptionManager` that record subscriptions and update parameter names. The
  aim to clarify purpose of the API.
* Introduce the `AreaOfInterestService`, `ContextConverger` and `ReplicantClientSystem` classes in a
  runtime package that aims to simplify the mechanisms for setting up client environments.
* Move to using `ChannelDescriptor` to represent channels/graphs through a larger portion of the API.
* Move from mandatory subclassing of `ClientSession` to disallowing subclassing of the same.
* Remove generics from the `ClientSession` and the `DataLoaderService` classes.
* Make the `ClientSession` methods `requestSubscribe`, `requestSubscriptionUpdate` and `requestUnsubscribe`
  public so that it can be called directly from code. This will potentially enable the elimination of the
  session subclasses.
* Add `DataLoaderService#isAreaOfInterestActionPending()` methods to query the state of pending requests.
* Ensure http requests from `GwtWebPollerDataLoaderService` have a reasonable timeout specified.
* Significant refactoring of `AvbstractDataLoaderServiceImpl` to aid inheritance.
* Update `DataLoaderService` to maintain the current state of the connection. ie. CONNECTING,
  CONNECTED, DISCONNECTING, DISCONNECTED, ERROR.
* Introduce `DataLoaderListener` to generate events from `DataLoaderService`.
* Ensure the type parameter for enums in DataLoaderService is qualified correctly.
* Replace and enhance `DataLoaderService#isConnected()` with `DataLoaderService#getState()`.
* Implement `AbstractDataLoaderService#channelToGraph()` directly rather than relying on customization
  by subclasses.
* Rename `DataLoaderService#getSystemKey()` to `DataLoaderService#getKey()` and changing the way it is
  used in the context of the ChangeBroker.
* Add `DataLoaderService#getGraphType()` method to access type of graphs processed by loader.
* Update `client.ChannelDescriptor` to add method `getSystem()` that returns the class of enum.
* Increase the log level from FINE to WARNING for the `WebPoller` in `WebPollerDataLoaderService`.
* Decrease the log level from INFO to FINEST for the `AbstractDataLoaderService`.
* Decrease the log level from WARNING to FINEST for the `WebPollerLogLevel`.

### [v0.5.78](https://github.com/realityforge/replicant/tree/v0.5.78)

* Enhance the `DataLoaderService` so that interaction between the `ClientSession` instances
  and `DataLoaderService` uses a formal contract rather than protected methods.
* Make sure the asynchronous callbacks from JAXRS are correctly contextualized to ensure
  CDI operates as expected in `EeWebPollerDataLoaderService`.
* Increase the log level from FINE to INFO for the `WebPoller` in `WebPollerDataLoaderService`.

### [v0.5.77](https://github.com/realityforge/replicant/tree/v0.5.77)

* Update `ReplicantEntityCustomizer.configure` to be static.

### [v0.5.76](https://github.com/realityforge/replicant/tree/v0.5.76)

* Introduce `ReplicantEntityCustomizer` to help customize replicant entities during tests.

### [v0.5.75](https://github.com/realityforge/replicant/tree/v0.5.75)

* Restructure `ReplicantClientTestModule` so that it also exposes the `EntitySystem` service.

### [v0.5.74](https://github.com/realityforge/replicant/tree/v0.5.74)

* Introduce querying and sorting accessors to EntityRepository as default methods.
* Introduce basic locking around EeDataLoaderService operations.
* Convert EntityRepository.getByID and EntityRepository.findByID to default methods on interface.
* Throw a NoSuchEntityException rather than IllegalStateException from EntityRepository.getByID when
  no entity is present.
* Ensure correct handling of incorrect disconnects when failing to connect to server.
* Ensure a stopped WebPoller marks the DataLoaderService as disconnected.
* Generate CDI events for InvalidConnect, InvalidDisconnect and PollError conditions in EE client.
* Add explicit mechanisms for firing events for InvalidConnect, InvalidDisconnect and PollError events.
* Move propagation of SystemErrorEvent from EeWebPollerDataLoaderService to EeDataLoaderService.
* Fire ConnectEvent and DisconnectEvent from EeDataLoaderService when data loader connects and disconnects.

### [v0.5.73](https://github.com/realityforge/replicant/tree/v0.5.73)

* Add implementation EeDataLoaderService.getCacheService().
* Change log level of WebPollerDataLoaderService to INFO.

### [v0.5.72](https://github.com/realityforge/replicant/tree/v0.5.72)

* Introduce the `EntitySystem` abstraction that collectors the EntityRepository, the EntityChangeBroker and
  the EntitySubscriptionManager services into one access point.
* Move to Java8.
* Move to GWT 2.8.0.
* Refactor the AbstractDataLoader so that required services are exposed as template methods rather than
  being passed into the constructor. The aim is to enable sharing of these services for EE clients.

### [v0.5.71](https://github.com/realityforge/replicant/tree/v0.5.71)

* Introduce support interface `DataLoaderService` to make interaction with data loader generalizable.

### [v0.5.70](https://github.com/realityforge/replicant/tree/v0.5.70)

* Introduce constant `ReplicantContext.MAX_POLL_TIME_IN_SECONDS` to make it easy to determine poll
  time in both client and server code.

### [v0.5.69](https://github.com/realityforge/replicant/tree/v0.5.69)

* Change the access specifier of the class `GwtWebPollerDataLoaderService.ReplicantRequestFactory` to protected.

### [v0.5.68](https://github.com/realityforge/replicant/tree/v0.5.68)

* Introduce `AbstractSessionContextImpl` as a base class to extend as part of generation.

### [v0.5.67](https://github.com/realityforge/replicant/tree/v0.5.67)

* Add `ReplicantSessionManagerImpl.delinkDownstreamSubscriptions` and exposed to subclasses.
* Update `ReplicantSessionManagerImpl` so that `ChannelLinks` are only expanded for updates.
* Update `ReplicantSessionManagerImpl` to make `delinkSubscriptionEntries` and `linkSubscriptionEntries`
  protected access and available to subclasses.

### [v0.5.66](https://github.com/realityforge/replicant/tree/v0.5.66)

* Update the `ReplicantSessionManagerImpl` so the ChangeSet is passed into many methods rather than
  assuming the caller sessions ChangeSet.

### [v0.5.65](https://github.com/realityforge/replicant/tree/v0.5.65)

* Ensure `ChannelLink.hashcode()`, `ChannelLink.equals()` and `ChannelLink.toString()` take
  into consideration the source channel.

### [v0.5.64](https://github.com/realityforge/replicant/tree/v0.5.64)

* Major refactoring of `ReplicantSessionManagerImpl` so that the logic behind the
  subscribe, subscription updates and unsubscribe actions is not hidden in generated code.
  This has allowed the support of several new features, including automatic unsubscription
  of unreferenced subscriptions that were not explicitly subscribed
* Support bulk subscribe, subscription updates and unsubscribe actions on `ReplicantSessionManagerImpl`.
* Add helper methods to `ChangeSet` to simplify merging messages.
* Convert `ReplicantSession` from abstract to final class and update code appropriately.
* Split `ReplicantSessionManager` into an interface and an implementation.
* Add `ChannelMetaData` class that represents characteristics of each channel.
* Add `ChangeUtil.toChanges` method that directly accepts `ChannelDescriptor`.
* Introduce `JsonUtil` as a helper to convert strings and structured types to `JsonObject`
  instances.
* Make `ChannelDescriptor` implement `Comparable` to enable sorting.
* Introduce `SubscriptionEntry` to start explicitly representing subscriptions and
  make it accessible via `ReplicantSession`.

### [v0.5.63](https://github.com/realityforge/replicant/tree/v0.5.63)

* Introduced `ChainedAction` class to help when building chains of actions.
* Convert the "cache action" in the `AbstractDataLoaderService` to be a `ChainedAction`
  to allow injections of actions after cache has been injected.
* Send messages when session is connected/disconnected.
* Move generic connect/disconnection functionality into `AbstractDataLoaderService` from
  `WebPollerDataLoaderService`.

### [v0.5.62](https://github.com/realityforge/replicant/tree/v0.5.62)

* Fix bug that required that the WebPoller factory be setup prior to creating
  `GwtWebPollerDataLoaderService`.
* Introduce `GwtWebPollerDataLoaderService.newRequestBuilder()` template method for
  constructing `RequestBuilder` objects to allow subclasses to customize requests.
* Update `GwtWebPollerDataLoaderService.newRequestBuilder()` to set the "Authorization"
  http header when creating new builder if `getSessionContext().getAuthenticationToken()`
  is not null.
* Update `GwtWebPollerDataLoaderService.newRequestBuilder()` to set "Pragma: no-cache".

### [v0.5.61](https://github.com/realityforge/replicant/tree/v0.5.61)

* Add WebPollerDataLoaderService.getWebPollerLogLevel() template method to configure
  log level for WebPoller.

### [v0.5.60](https://github.com/realityforge/replicant/tree/v0.5.60)

* Break the project into several different jars to produce a cleaner dependency tree.
  This avoids scenarios where inappropriate code is included in final deployment units.
  The new packages include;
  - replicant-shared
  - replicant-server
  - replicant-client-common
  - replicant-client-ee
  - replicant-client-gwt
  - replicant-client-qa-support
* Convert `ReplicantPollResource` to an abstract class `AbstractReplicantPollResource` with template
  methods desgned for overload. Remove ReplicantPollResource now that it is no longer needed. Improve
  handling of exceptions so that the last Poll when session is being disconnected will not produce
  an error in the log. Improve the code so that `ManagedScheduledExecutorService` is used rather than
  `ScheduledExecutorService`.
* Change implementation of `ReplicantSessionManager` so that it uses a template method to retrieve
  `TransactionSynchronizationRegistry` rather than a directly injected resource.
* Change implementation of `AbstractReplicationInterceptor` so that it uses a template method to
  retrieve `TransactionSynchronizationRegistry` rather than a directly injected resource.
* Update `AbstractSessionRestService` to remove `@PostConstruct` annotation and require that
  subclasses annotate the method appropriately.

### [v0.5.59](https://github.com/realityforge/replicant/tree/v0.5.59)

* Create a new WebPoller when polling starts to avoid reusing a WebPoller as pending calls may not
  be handled correctly if they were cancelled when WebPoller was stopped but did not return until
  the WebPoller was started again.
* Define implementation of `WebPollerDataLoaderService.connect()` and `WebPollerDataLoaderService.disconnect()`
  that has template methods for subclasses to override. Ensure `disconnect()` is called before attempting
  to reconnect WebPoller.
* Use more reasonable values for `ChangesToProcessPerTick` and `LinksToProcessPerTick` for ee client.
* Use a small repeat period for repeating scheduler in ee client.
* Avoid the use of `javax.enterprise.inject.spi.CDI` as it is not always configured in EE apps.
* Defer creation of WebPoller in WebPollerDataLoaderService till actually required. This allows
  the EE client variant to be injected with resources prior to creating WebPoller.
* Fix NullPointerException in WebPollerDataLoaderService.
* Fix several NullPointerExceptions in ChannelActionDTO.

### [v0.5.58](https://github.com/realityforge/replicant/tree/v0.5.58)

* Update to `gwt-webpoller` 0.9.1.
* Introduce the package `org.realityforge.replicant.client.ee` to contain support required for client-side
  enterprise java replicant clients.
* Rename WebPollerDataLoaderService to GwtWebPollerDataLoaderService and introduce a WebPollerDataLoaderService
  that contains all the non-technology specific support for WebPoller library.
* Set the Authorization header in WebPollerDataLoaderService if the SessionContext has authentication token.
* Derive the baseURL of WebPollerDataLoaderService from SessionContext parameter rather than heuristics.
* Change access level of WebPollerDataLoaderService.ReplicantRequestFactory to private.
* Change access level of WebPollerDataLoaderService.handlePollSuccess() to private.
* Implement AbstractDataLoaderService.getSystemKey() to derive key from SessionContext.getKey().
* Correctly return false from JsoChange.getBooleanValue if no such value exists rather than null.
* Add many more Nullability annotations to make reuse of library easier.
* Remove final methods from AbstractDataLoaderService to prepare it for being a CDI bean.
* Use a template method in AbstractDataLoaderService to create the ChangeMapper to avoid the
  need to pass it in through the constructor.
* Migrate GwtDataLoaderService.scheduleDataLoad() implementation and supporting methods to
  AbstractDataLoaderService and introduce template methods for GWT specific functionality.

### [v0.5.57](https://github.com/realityforge/replicant/tree/v0.5.57)

* Rename package `org.realityforge.replicant.client.json.gwt` to `org.realityforge.replicant.client.gwt`.
* Extract the gwt specific functionality of AbstractClientTest to AbstractGwtClientTest and move
  to a separate directory.
* Move LocalCacheService and ReplicantRpcRequestBuilder to org.realityforge.replicant.client.json.gwt package.
* Move generation of DataLoadCompleteEvent event from and linkage against EventBus from
  AbstractDataLoaderService to GwtDataLoaderService and move DataLoadCompleteEvent to
  org.realityforge.replicant.client.json.gwt package.
* Remove unused org.realityforge.replicant.client.json.gwt.StringUtils.

### [v0.5.56](https://github.com/realityforge/replicant/tree/v0.5.56)

* Make it possible to store authentication token in SessionContext.
* In AbstractDataLoaderService, reorder actions so that validation of repository occurs after debug output.

### [v0.5.55](https://github.com/realityforge/replicant/tree/v0.5.55)

* Update simple-session-filter dependency to enable CDI support for session managers.
* Ensure ReplicantSessionManager and ReplicantJsonSessionManager can be CDI beans by removing final
  methods and adding tests to enforce this feature.
* Convert ReplicantPollResource to a CDI bean from an EJB.
* Rework SessionRestService to an abstract class AbstractSessionRestService with a template method
  to retrieve the SessionManager. Document how subclasses need to be defined.

### [v0.5.54](https://github.com/realityforge/replicant/tree/v0.5.54)

* Remove per request hash as the "Pragma: no-cache" header gets around caching in proxy servers.
* Fix implementation of `JsoChange.containsKey` so that the method will return true even if the value is null.

### [v0.5.53](https://github.com/realityforge/replicant/tree/v0.5.53)

* Set "Pragma: no-cache" header when polling for changes.
* Generate a per request hash added to each poll request to punch through overly zealous caching proxy servers.
* Revert to using @EJB rather than @Inject for ReplicantPollSource to work-around limitations when deploying to GlassFish.

### [v0.5.52](https://github.com/realityforge/replicant/tree/v0.5.52)

* Specify further header in CacheUtil to avoid caching.

### [v0.5.51](https://github.com/realityforge/replicant/tree/v0.5.51)

* Add some documentation to README covering the basic concepts.
* Eliminate BadSessionException and require AbstractDataLoaderService to implement ensureSession().

### [v0.5.50](https://github.com/realityforge/replicant/tree/v0.5.50)

* Update the AbstractDataLoaderService so that it only purges subscriptions that are "owned" by
  the data loader service and ignores any subscriptions owned by other data loaders.
* Add EntityChangeBroker.removeAllChangeListeners() to purge listeners for a specific entity.
* Update EntitySubscriptionImpl to remove type map when empty.
* Ensure that entities are unloaded from EntityRepository and listeners in the change broker
  are removed when subscriptions are removed as part of disconnect() method in AbstractDataLoaderService.
* Update EntityRepositoryDebugger to add methods to support debugging of subscriptions.

### [v0.5.49](https://github.com/realityforge/replicant/tree/v0.5.49)

* Add a guard in EntityMessageCacheUtil so that if EntityMessageCacheUtil is accessed outside of a
  replication context, an exception is thrown. This forces all entity modifications to occur within
  a replication context.

### [v0.5.48](https://github.com/realityforge/replicant/tree/v0.5.48)

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

### [v0.5.47](https://github.com/realityforge/replicant/tree/v0.5.47)

* When a client-side session is disconnected, delete the server-side session.
* Upgrade to compile and test against GWT 2.7.0.
* Replace TokenRestService with enhanced SessionRestService that provides status details
  about the session and other candidate sessions.
* Add field_filter dependency to expand the capabilities of the token service.
* Add a template method ReplicantSession.emitStatus() to make it possible for sub-classes
  to provide additional details about session status.

### [v0.5.46](https://github.com/realityforge/replicant/tree/v0.5.46)

* Add CollectorEntityChangeListener that records entity change events, useful during testing.
* Introduce ReplicantClientTestModule in the client-side test code that registers the standard set
  of client-side dependencies. Update AbstractClientTest to add ReplicantClientTestModule to the
  list of modules.

### [v0.5.45](https://github.com/realityforge/replicant/tree/v0.5.45)

* Remove unused EntityMessageGenerator.
* Remove ChangeRecorder as downstream projects no longer use the class and instead
  generate the entire artifact.

### [v0.5.44](https://github.com/realityforge/replicant/tree/v0.5.44)

* Re-add inadvertently removed resumeBroker() and pauseBroker() to AbstractClientTest.
* Add some minimal tests around how the WebPollerDataLoader will handle system failures

### [v0.5.43](https://github.com/realityforge/replicant/tree/v0.5.43)

* Add abstract method AbstractDataLoaderService.getSystemKey() that helps identify which replication
  system that the data loader is supporting. This is important when replicant is used with multiple
  replication data sources.
* Introduce an EntityBrokerTransaction structure that identifies the data loader that initiated the
  "transaction" (a.k.a the disable or pause actions).

### [v0.5.42](https://github.com/realityforge/replicant/tree/v0.5.42)

* Ensure TokenRestService sets HTTP headers so IE performs no caching.

### [v0.5.41](https://github.com/realityforge/replicant/tree/v0.5.41)

* Ensure ReplicantPollResource sets HTTP headers to ensure IE performs no caching.
* Extract out a constant for the query parameter used to identify the last received packet sequence.

### [v0.5.40](https://github.com/realityforge/replicant/tree/v0.5.40)

* Send a DataLoadCompleteEvent when a data load completes.
* Send a SystemErrorEvent on the EventBus when a system error occurs.
* Merge the WebPollerDataLoaderService.connect() method from downstream projects as they are all
  identical in their implementation.
* Extract TokenRestService from downstream projects. The TokenRestService simply generates a new
  session and returns the token.

### [v0.5.39](https://github.com/realityforge/replicant/tree/v0.5.39)

* Introduce EntityChangeListenerAdapter to make writing listeners easier.

### [v0.5.38](https://github.com/realityforge/replicant/tree/v0.5.38)

* Remove the ReplicationRequestManager abstraction as it implies a transactional boundary crossing
  which is not the intention. Implement the same functionality as a collection of static methods
  in the ReplicationRequestUtil utility class.

### [v0.5.37](https://github.com/realityforge/replicant/tree/v0.5.37)

* Add AbstractDataLoaderService.supportMultipleDataLoaders() template method that should return
  true if the data loader source loader should gracefully share common resources between data
  loaders. Avoid Pausing already paused loader during data load processing when this return true.

### [v0.5.36](https://github.com/realityforge/replicant/tree/v0.5.36)

* Avoid ConcurrentModificationException in AbstractDataLoaderService.unsubscribeInstanceGraphs
  by duplicating list before unsubscribing.

### [v0.5.35](https://github.com/realityforge/replicant/tree/v0.5.35)

* Extract the handling of context management from AbstractReplicationInterceptor into
  ReplicationRequestManager and AbstractReplicationRequestManager.

### [v0.5.34](https://github.com/realityforge/replicant/tree/v0.5.34)

* Fix concurrency bug triggered in AbstractDataLoaderService.updateSubscriptionForFilteredEntities
  when entities are removed from subscription.

### [v0.5.33](https://github.com/realityforge/replicant/tree/v0.5.33)

* Add Runnable parameters to the connect and disconnect methods in WebPollerDataLoaderService.

### [v0.5.32](https://github.com/realityforge/replicant/tree/v0.5.32)

* Add source channel to ChannelLink.

### [v0.5.31](https://github.com/realityforge/replicant/tree/v0.5.31)

* Default ReplicantPollResource to 30 seconds long polling before making another request.

### [v0.5.30](https://github.com/realityforge/replicant/tree/v0.5.30)

* Add support for debugging entity subscriptions and requests as well as local
  repository state.
* If repository debug output is enabled in application then print out a helper message
  when the GwtDataLoaderService is defined. Helps developers remember this feature is
  present.
* Associate a symbolic key with each session context. Prefix log messages using key and
  use it to restrict debugging to a particular GwtDataLoaderService subclass.

### [v0.5.29](https://github.com/realityforge/replicant/tree/v0.5.29)

* Update WebPollerDataLoaderService to annotate the exception in handleSystemFailure as nullable.
* Make ReplicantRpcRequestBuilder a non final class.
* Update SessionContext to have a per-session base url.
* Remove reference to unused module (com.google.gwt.rpc.RPC) that is not present in GWT 2.7.

### [v0.5.28](https://github.com/realityforge/replicant/tree/v0.5.28)

* Support the ability to debug just a single DataSourceLoader services
  changes.
* Add ReplicantDev.gwt.xml configuration that turns on all debug features.
* Exclude test classes form GWT compiler's path.

### [v0.5.27](https://github.com/realityforge/replicant/tree/v0.5.27)

* Introduce AbstractClientTest to help write client-side tests.

### [v0.5.26](https://github.com/realityforge/replicant/tree/v0.5.26)

* Remove the usage of a Synchronized map and replace with ConcurrentHashMap in
  ReplicantPollResource as the version if jersey in GlassFish 4.1.0 can result in deadlocks
  when timeouts are triggered.
* Remove final qualifier for method in ReplicantPollResource as CDI attempts to intercept
  method in GlassFish 4.1.0.
* Refactor SessionContext is not static, thus allowing multiple contexts within a single
  application.

### [v0.5.25](https://github.com/realityforge/replicant/tree/v0.5.25)

* Introduce ReplicantJsonSessionManager as all downstream projects use json as their
  transport layer.
* Rename ReplicantSessionManager.poll() to pollPacket to make it easier to sub-class.
* Backport, test and generalize ReplicantPollResource from downstream libraries. This class
  makes it easy to setup polling for replicant based systems. Derive the default poll
  url in the WebPollerDataLoaderService assuming the ReplicantPollResource implementation.

### [v0.5.24](https://github.com/realityforge/replicant/tree/v0.5.24)

* Extract utility method AbstractDataLoaderService.unsubscribeInstanceGraphs().
* Add close handler in WebPollerDataLoaderService that disconnects WebPoller
  when the windows closes.
* Correct nullability annotation for filterParameter in
  AbstractDataLoaderService.requestUpdateSubscription().

### [v0.5.23](https://github.com/realityforge/replicant/tree/v0.5.23)

* Implement WebPollerDataLoaderService to simplify construction of polling based data loaders.
* Implement purging of subscriptions when the session changes.

### [v0.5.22](https://github.com/realityforge/replicant/tree/v0.5.22)

* Move to EE7.
* Fix bug in ChangeAccumulator where change initiator can be incorrectly identified
  as having been routed to if a changeset was was accessed via getChangeSet()
  but no message was ever added to ChangeSet.

### [v0.5.21](https://github.com/realityforge/replicant/tree/v0.5.21)

* Remove BadSessionException and associated ensureSession so domgen can generate
  a customized implementation.

### [v0.5.20](https://github.com/realityforge/replicant/tree/v0.5.20)

* Move the responsibility for validating the entities in the EntityRepository to
  the EntitySubscriptionValidator.

### [v0.5.19](https://github.com/realityforge/replicant/tree/v0.5.19)

* Move the responsibility for deleting the entities from the EntityRepository from
  the EntitySubscriptionManager to the DataLoaderService.

### [v0.5.18](https://github.com/realityforge/replicant/tree/v0.5.18)

* Initial work to add debugging capability to the EntityRepository via a helper
  class EntityRepositoryDebugger.
* Extract AbstractDataLoaderService.updateSubscriptionForFilteredEntities so that
  subclasses can control the order in which types are unloaded due to a change in
  filter.
* Add EntityRepository.findAllIDs method to get ids for entities.

### [v0.5.17](https://github.com/realityforge/replicant/tree/v0.5.17)

* Support the replication of the filter between the server and client.
* Update the client to unregister entities that are filtered after a channels
  filter is updated.
* Start to record filter on EntitySubscriptionManager on the client-side.
* Move to GWT 2.6.1.
* Move to Java 7.
* Add toString to ChannelLink and ChannelDescriptor to ease debugging.
* Correct the namespace in ReplicantConfig so that the property is read correctly.
* Pass ReplicantConfig through the constructor to make it easier to test.

### [v0.5.16](https://github.com/realityforge/replicant/tree/v0.5.16)

* Associated with each change, the channel(s)  (a.k.a. subscription(s)) which resulted in
  the change replicating to the client.
* Replicate record of subscription changes to the client.

NOTE: This is a large change and further details are in the source control system.

### [v0.5.15](https://github.com/realityforge/replicant/tree/v0.5.15)

* Rework ChangeRecorder to make sub-classing easier.
* Rename SubscriptionEntry.subscriptionData to filterParameter to match domgen conventions.
* Use constructor based injection for DataLoaders.
* Extract a separate queue of actions to control subscription in the DataLoaders.

### [v0.5.14](https://github.com/realityforge/replicant/tree/v0.5.14)

* Associate a key with each request that corresponds to the operation being performed.
* Merge RequestManager and AbstractSessionManager into ClientSession.
* Add dependency on gwt-property-source library so that the choice on whether to
  validate repository after loads is controlled using a compile time configuration
  property.

### [v0.5.13](https://github.com/realityforge/replicant/tree/v0.5.13)

* Add support for Verifiable interface for entities that can validate their own state.
* Support registration of interfaces in EntityRepository.
* Add support for detecting whether an entity has been lined via Linkable.isLinked().
* Reconfigure the Linkable interface so that implementations no longer implement
  delink and instead rely on the repository invoking invalidate.
* Rework ChangeRecorder to make it easier to sub-class and customize behaviour.

### [v0.5.12](https://github.com/realityforge/replicant/tree/v0.5.12)

* Mark the EntityMessageSet class as final.
* Add EntityMessageSet.containsEntityMessage(...) to test whether the set
  contains a message.

### [v0.5.11](https://github.com/realityforge/replicant/tree/v0.5.11)

* Add support for recording arbitrary data in SubscriptionEntry.
* Add AbstractSubscriptionManager.find(Instance|Type)GraphSubscription methods.

### [v0.5.10](https://github.com/realityforge/replicant/tree/v0.5.10)

* Restore compatibility with JDK 6.

### [v0.5.9](https://github.com/realityforge/replicant/tree/v0.5.9)

* Support encoding of Longs as strings in change sets.

### [v0.5.8](https://github.com/realityforge/replicant/tree/v0.5.8)

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

### [v0.5.7](https://github.com/realityforge/replicant/tree/v0.5.7)

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

### [v0.5.6](https://github.com/realityforge/replicant/tree/v0.5.6)

* In the DataLoaderService, do not execute runnable unless the RequestEntry has
  been successfully processed.
* In the DataLoaderService, remove the RequestEntry if it has been successfully
  processed.
* Support recording on the RequestEntry that the results have arrived to counter
  the race scenario where the change set arrives prior to the the request
  returning.

### [v0.5.5](https://github.com/realityforge/replicant/tree/v0.5.5)

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

### [v0.5.4](https://github.com/realityforge/replicant/tree/v0.5.4)

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

### [v0.5.3](https://github.com/realityforge/replicant/tree/v0.5.3)

* Move EntityMessageAccumulator to the transport package and re-target it to
  deliver messages to the PacketQueue.
* Introduce the Packet and PacketQueue to support creating the transport layer.
* Add Gin module that defines EntityMessageBroker and EntityRepository instances.
* Update EntityChangeListener to add a callback method that is called when
  and entity is added to the system.
* Make EntityChangeBrokerImpl.shouldRaiseErrorOnEventHandlerError() final.

### [v0.5.2](https://github.com/realityforge/replicant/tree/v0.5.2)

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

### [v0.5.1](https://github.com/realityforge/replicant/tree/v0.5.1)

* Include source in jar file to make it easier to integrate with GWT.

### [v0.5](https://github.com/realityforge/replicant/tree/v0.5)

* Remove the Async*Callback interfaces now that they are generated by Domgen.
* Move the RDate, Date*Serializer and Date*Deserializer classes to gwt-datatypes
  library.
* Move the org.realityforge.replicant.client.json.gwt.JsoReadOnly* collections
  to org.realityforge.gwt.datatypes.client.collections in the gwt-datatypes package.
* Add GwtDataLoaderService to simplify creating a DataLoader in GWT.
* Add template method AbstractDataLoaderService.shouldValidateOnLoad() that will
  validate the entity repository on each change. Useful to override and return true
  during development or in debug mode.

### [v0.4.8](https://github.com/realityforge/replicant/tree/v0.4.8)

* Change AbstractReplicationInterceptor so that subclasses must override a template
  method to provide the EntityManager. The purpose of this change is to allow for
  the use of this library in applications that have multiple persistence contexts.
