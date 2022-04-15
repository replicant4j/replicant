# Change Log

### [v6.122](https://github.com/replicant4j/replicant/tree/v6.122) (2022-04-15) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.121...v6.122)

Changes in this release:

* Modify `AbstractSessionContextImpl.generateTempIdTable(...)` so that it chunks the insertions into the temp id table to work around limitations in some versions of SQL server of a maximum of 1000 rows in an INSERT statement.

### [v6.121](https://github.com/replicant4j/replicant/tree/v6.121) (2022-04-07) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.120...v6.121)

Changes in this release:

* Use `tryLock()` rather than lock in implementation of `removeClosedSessions()`, `pingSessions()`, `removeAllSessions()` in `ReplicantSessionManagerImpl` to avoid deadlocks. These actions will just be skipped if they would otherwise cause blocking.
* Avoid attempting bulk subscribe attempts submitted to the server for an existing subscription a matching filter.
* Generate a `AttemptedToUpdateStaticFilterException` exception when attempting to update a channel with a static filter.
* Rework the way we expand changes for processing so that we break it down into cycles and for each cycle we:
  - collect pending channel links
  - select the highest priority channel with pending links
  - select the channel links for the selected channel and perform a bulk subscribe for all channel links with the same channel id
* Add `isExplicitSubscribe` parameter to `bulkCollectDataForSubscribe(...)` and  `bulkCollectDataForSubscriptionUpdate(...)` on `ReplicantSessionManagerImpl` to enable controlling the behaviour in generated domgen code.

### [v6.120](https://github.com/replicant4j/replicant/tree/v6.120) (2022-04-01) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.119...v6.120)

Changes in this release:

* Expose the "productionMode" compilation time parameter using `Replicant.isProductionMode()`.
* Add `SystemSchema.getInwardChannelLinks(int)`, `SystemSchema.getOutwardChannelLinks(int)`, `EntitySchema.getOutwardChannelLinks()` and `ChannelSchema.getOutwardChannelLinks()` helper methods. These are not used at runtime but are primarily intended for usage in supporting tooling and testing infrastructure

### [v6.119](https://github.com/replicant4j/replicant/tree/v6.119) (2022-04-01) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.118...v6.119)

Changes in this release:

* Change the `ChannelLinkSchema.auto` property to being a boolean rather than an int.

### [v6.118](https://github.com/replicant4j/replicant/tree/v6.118) (2022-03-31) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.117...v6.118)

Changes in this release:

* Introduce `ChannelLinkSchema` entities that are not used at runtime but are a placeholder to store additional data about a channel/graph.
* Add `EntityMessage.safeGetLinks()` helper method that initializes a links field if not present.
* Rename `linkSourceGraphToTargetGraph(...)` to `bulkLinkFromSourceGraphToTargetGraph(...)` in `AbstractSessionContextImpl` to match conventions present in domgen generated code.
* Add initial support for "bulk" loading of type graphs. While "bulk" loading is a bit of a misnomer given that type graphs have at most 1 subscription, the term is synonymous with loading using SQL queries in domgen generated code.
* Remove return value from bulk loading methods as implementations never return false, they generate an exception or perform the bulk load.
* Fix a bug in `AbstractSessionContextImpl.bulkLinkFromSourceGraphToTargetGraph(...)` where the ids used in linking from the source graph to the target graph were incorrectly inverted.

### [v6.117](https://github.com/replicant4j/replicant/tree/v6.117) (2022-03-29) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.116...v6.117)

Changes in this release:

* Pass the ChangeSet when performing bulk subscribe except on the outermost call where locks are acquired as the bulk subscribe methods may be invoked when expanding links outside the initial transaction.

### [v6.116](https://github.com/replicant4j/replicant/tree/v6.116) (2022-03-28) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.115...v6.116)

Changes in this release:

* Ensure that graphs that support bulk loads always go through the bulk loads path even when the client subscribes or updates the filter of a single instance of the graph.

### [v6.115](https://github.com/replicant4j/replicant/tree/v6.115) (2022-03-25) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.114...v6.115)

Changes in this release:

* Update the `org.realityforge.react4j` artifacts to version `0.187`.
* Update the `org.realityforge.arez` artifacts to version `0.203`.
* Update the `org.realityforge.guiceyloops` artifact to version `0.110`.
* Update the `org.realityforge.grim` artifacts to version `0.06`.
* Update the `org.realityforge.akasha` artifacts to version `0.30`.
* Add a `AbstractSessionContextImpl` base class that SessionContext implementations can extend.

### [v6.114](https://github.com/replicant4j/replicant/tree/v6.114) (2021-10-23) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.113...v6.114)

Changes in this release:

* Update the `org.realityforge.react4j` artifacts to version `0.185`.
* Update the `org.realityforge.arez` artifacts to version `0.200`.
* Update the `org.realityforge.zemeckis` artifact to version `0.13`.
* Update the `org.realityforge.akasha` artifacts to version `0.28`.

### [v6.113](https://github.com/replicant4j/replicant/tree/v6.113) (2021-09-24) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.112...v6.113)

Changes in this release:

* Rename `Entity.delinkFromInternalFilteringSubscription(Subscription)` method to `Entity.delinkFromFilteringSubscription(Subscription)` and modify the implementation to support calling with any filtered graph. The intent is to support mutable routing parameters.

### [v6.112](https://github.com/replicant4j/replicant/tree/v6.112) (2021-09-21) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.111...v6.112)

Changes in this release:

* Add an `OnEntityUpdateAction` hook to the `SystemSchema` that will be invoked for every update received from the server. The intent is to support `INTERNAL` filtering using mutable routing parameters.
* Add a `Entity.delinkFromInternalFilteringSubscription(Subscription)` method to help support mutable routing parameter management.

### [v6.111](https://github.com/replicant4j/replicant/tree/v6.111) (2021-09-21) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.110...v6.111)

Changes in this release:

* Ensure that changes that should invalidate a cache will invalidate the cache even when there are no sessions connected.

### [v6.110](https://github.com/replicant4j/replicant/tree/v6.110) (2021-09-16) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.109...v6.110)

Changes in this release:

* Update the `org.realityforge.akasha` artifacts to version `0.24`.

### [v6.109](https://github.com/replicant4j/replicant/tree/v6.109) (2021-08-25) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.108...v6.109)

Changes in this release:

* Update the `org.realityforge.akasha` artifacts to version `0.21`.
* Update the `org.realityforge.arez` artifacts to version `0.199`.
* Add support for the concept of a `UserObject` associated with a `ReplicantSession` that can be used to associate arbitrary application-specific information with a session.

### [v6.108](https://github.com/replicant4j/replicant/tree/v6.108) (2021-07-27) · [Full Changelog](https://github.com/spritz/spritz/compare/v6.107...v6.108)

Changes in this release:

* Update the `org.realityforge.react4j` artifacts to version `0.183`.
* Upgrade the `org.realityforge.akasha` artifacts to version `0.15`.
* Upgrade the `org.realityforge.braincheck` artifacts to version `1.31.0`.
* Upgrade the `org.realityforge.zemeckis` artifact to version `0.11`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.198`.

### [v6.107](https://github.com/replicant4j/replicant/tree/v6.107) (2021-03-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.106...v6.107)

* Update release process.

### [v6.106](https://github.com/replicant4j/replicant/tree/v6.106) (2021-03-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.105...v6.106)

* Upgrade the `org.realityforge.akasha` artifact to version `0.05`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.193`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.181`.
* Upgrade the `org.realityforge.zemeckis` artifact to version `0.09`.
* Upgrade the `org.realityforge.grim` artifacts to version `0.05`.
* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.17`.
* Migrate from using Elemental2 to Akasha when interacting with the Browser API.

### [v6.105](https://github.com/replicant4j/replicant/tree/v6.105) (2021-02-01) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.104...v6.105)

* Avoid crashes in `AbstractReplicantEndpoint.sendErrorAndClose(...)` and `AbstractReplicantEndpoint.getReplicantSession(...)` that can occur when the application is being un-deployed.
* Reduce log message from WARNING to FINE in `AbstractReplicantEndpoint.onClose(...)` when unable to locate replicant session associated with websocket session as this scenario can occur during normal operation of the service when the replicant session had previously aborted/errored (likely due to authentication errors).

### [v6.104](https://github.com/replicant4j/replicant/tree/v6.104) (2021-01-11) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.103...v6.104)

* Change the way `ReplicantSessionManagerImpl` determines whether a message was initiated by a session so that it takes into account sessions that have been closed.
* Upgrade the `org.realityforge.zemeckis` artifact to version `0.08`.
* Name the Zemeckis tasks.

### [v6.103](https://github.com/replicant4j/replicant/tree/v6.103) (2021-01-07) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.102...v6.103)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.179`.
* Upgrade the `org.realityforge.org.jetbrains.annotations` artifact to version `1.7.0`.
* Upgrade the `org.realityforge.arez.testng` artifact to version `0.24`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.192`.

### [v6.102](https://github.com/replicant4j/replicant/tree/v6.102) (2021-01-06) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.101...v6.102)

* Adopt the `org.realityforge.zemeckis:zemeckis-core` artifact to provide scheduling primitives.

### [v6.101](https://github.com/replicant4j/replicant/tree/v6.101) (2020-12-15) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.100...v6.101)

* Avoid error when attempting to close and already closed websocket in `AbstractReplicantEndpoint`.

### [v6.100](https://github.com/replicant4j/replicant/tree/v6.100) (2020-12-15) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.99...v6.100)

* Avoid log spam when WebSocket connections are closed after the application has undeployed.

### [v6.99](https://github.com/replicant4j/replicant/tree/v6.99) (2020-11-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.98...v6.99)

* Workaround concurrency bug in Payara/Tyrus/Catalina by catching `NullPointerException` in `WebSocketUtil` and ignoring the exception. This change just removes the logging of these exceptions to production logs and lets Payara/Tyrus/Catalina fail silently and recover normally.
* Avoid processing the same session multiple times within `ReplicantMessageBrokerImpl` across multiple calls to `processPendingSessions()`. This stops a single blocked session from locking the entire thread pool responsible for invoking `processPendingSessions()`.

### [v6.98](https://github.com/replicant4j/replicant/tree/v6.98) (2020-11-25) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.97...v6.98)

* Add `ReplicantSessionManager.deleteAllCacheEntries()` so that bulk change operations that are done without using JPA entities can trigger a cache reset.
* Ensure that a deletion of an entity will result in any caches that contain the entity as being reset. Previously only entity changes caused cache resets.
* Abort the `subscribe`/`unsubscribe`/`bulkSubscribe`/`bulkUnsubscribe` actions if the associated session is closed.
* Support arbitrary degrees of concurrency in `ReplicantMessageBroker.processPendingSessions()` so that if a session is taking a long time in `expandLinks()`, other sessions can make progress.

### [v6.97](https://github.com/replicant4j/replicant/tree/v6.97) (2020-11-16) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.96...v6.97)

* Upgrade the `org.realityforge.org.jetbrains.annotations` artifact to version `1.6.0`.
* Upgrade the `javax` artifact to version `8.0`.
* Add `final` modifier to the `SecuredReplicantRpcRequestBuilder` class.
* Generate an exception if the `SecuredReplicantRpcRequestBuilder` attempts to build a request but no keycloak token is available.

### [v6.96](https://github.com/replicant4j/replicant/tree/v6.96) (2020-08-10) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.95...v6.96)

* Ensure that the rest endpoints in `AbstractSessionRestService` acquire the session lock prior to accessing session data.

### [v6.95](https://github.com/replicant4j/replicant/tree/v6.95) (2020-08-03) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.94...v6.95)

* Upgrade the `org.realityforge.arez.testng` artifact to version `0.20`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.188`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.173`.
* Update `ReplicantSubscription.getStatus()` implementation to avoid exception when an `AreaOfInterest` is disposed before the associated view has been removed from the react component hierarchy.

### [v6.94](https://github.com/replicant4j/replicant/tree/v6.94) (2020-06-08) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.93...v6.94)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.162`.
* Upgrade the `org.realityforge.arez.testng` artifact to version `0.14`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.182`.
* Upgrade the `org.realityforge.braincheck` artifact to version `1.29.0`.

### [v6.93](https://github.com/replicant4j/replicant/tree/v6.93) (2020-06-05) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.92...v6.93)

* Avoid reporting the parameters for the action `Connector.completeAreaOfInterestRequests(...)` to the spy subsystem.
* Pause in the debugger when superdevmode is enabled and an error is generated in the `ApplicationEventBroker` when invoking `onApplicationEvent(...)` method for any `ApplicationEventHandler` listener.

### [v6.92](https://github.com/replicant4j/replicant/tree/v6.92) (2020-06-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.91...v6.92)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.160`.

### [v6.91](https://github.com/replicant4j/replicant/tree/v6.91) (2020-05-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.90...v6.91)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.158`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.181`.
* Upgrade the `org.realityforge.arez.testng` artifact to version `0.13`.
* Explicitly specify `requireId = Feature.DISABLE` parameter on `@ArezComponent` annotations as the default behaviour will change in the next version of arez.

### [v6.90](https://github.com/replicant4j/replicant/tree/v6.90) (2020-05-26) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.89...v6.90)

* Avoid crash then `Connector` callback triggers to process messages but the connection is in the process of disconnecting.

### [v6.89](https://github.com/replicant4j/replicant/tree/v6.89) (2020-05-21) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.88...v6.89)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.157`.
* Upgrade the `org.realityforge.arez.testng` artifact to version `0.11`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.179`.

### [v6.88](https://github.com/replicant4j/replicant/tree/v6.88) (2020-05-15) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.87...v6.88)

* Upgrade the `org.realityforge.braincheck` artifact to version `1.28.0`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.156`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.177`.
* Upgrade the `org.realityforge.arez.testng` artifacts to version `0.09`.

### [v6.87](https://github.com/replicant4j/replicant/tree/v6.87) (2020-04-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.86...v6.87)

* Upgrade the `org.realityforge.arez` artifacts to version `0.175`.
* Upgrade the `org.realityforge.arez.testng` artifacts to version `0.07`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.154`.
* Rework the way `EntityBrokerLock` so that multiple locks can be acquired by different connectors simultaneously. Only when all locks are released will the broker re-enable or resume.

### [v6.86](https://github.com/replicant4j/replicant/tree/v6.86) (2020-04-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.85...v6.86)

* Make the `AreaOfInterest.getRefCount()` method public. This makes it easier to integrate into systems that used earlier versions of replicant.

### [v6.85](https://github.com/replicant4j/replicant/tree/v6.85) (2020-04-23) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.84...v6.85)

* Correct the return type of `ReplicantContext.getInstanceSubscriptionIds()` to return a set of integer ids rather than a set of object ids.
* Remove `SubscriptionUtil.instanceSubscriptionToValues()` method as it is not used and no longer useful in the current architecture.
* Correct the typing of `SubscriptionUtil.convergeCrossDataSourceSubscriptions(...)` method as ids must be integers in replicant v6.

### [v6.84](https://github.com/replicant4j/replicant/tree/v6.84) (2020-04-16) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.83...v6.84)

* Upgrade the `org.realityforge.arez` artifacts to version `0.173`.
* Change `setFilter(...)`, `registerInwardSubscriptions(...)` and `registerOutwardSubscriptions(...)` on `SubscriptionEntry` back to public as the methods can be invoked if an application is implementing a bulk subscriptions.
* Change `ReplicantSessionManagerImpl.subscribe(...)` to be public access so that propagation of filters on subscription updates can be implemented in downstream projects.
* Introduce `ReplicantSessionManagerImpl.propagateSubscriptionFilterUpdate(...)` template method that can be overidden to implement filter propagation.

### [v6.83](https://github.com/replicant4j/replicant/tree/v6.83) (2020-04-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.82...v6.83)

* Change the routing infrastructure so an entity message can be associated with multiple instance graphs of the same type. i.e. An entity can belong to the `Event/1` and `Event/2` channel.

### [v6.82](https://github.com/replicant4j/replicant/tree/v6.82) (2020-03-31) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.81...v6.82)

* Fix "concurrent modification of collection" bug when removing manually linked graph links.

### [v6.81](https://github.com/replicant4j/replicant/tree/v6.81) (2020-03-26) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.80...v6.81)

* Generate an error in production builds when there is an unexpected error processing a network message.

### [v6.80](https://github.com/replicant4j/replicant/tree/v6.80) (2020-03-26) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.79...v6.80)

* Reduce access levels on several methods on `SubscriptionEntry` as they are not expected to be called outside of the framework.
* Change the API of `ReplicantSessionManagerImpl.collectDataForSubscriptionUpdate` to accept a session so that the collection process can also inspect session to determine what code managed graph links need to be updated.
* Expose `ReplicantSession.isSubscriptionEntryPresent(...)`
* Refactor the way `ReplicantSessionManagerImpl.updateSubscription(...)` works to support updates that signal that code-managed graph-link links should be unsubscribed when the soure entity is filtered out of graph.

### [v6.79](https://github.com/replicant4j/replicant/tree/v6.79) (2020-03-25) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.78...v6.79)

* Upgrade the `org.realityforge.arez` artifacts to version `0.172`.
* Fix a bug exhibited when filtered subscriptions update the filters and the filter methods use traverses across entities to determine whether an entity should be omitted from the updated subscription. If the entities traversed occurred lexicographically earlier than the entity and could be filtered out then they may already be disposed when later entities are attempting to determine whether they should remain in the graph which can cause crashes or unexpected behaviour. This has been fixed by deferring the unlinking till after the set of filtered out entities is determined.

### [v6.78](https://github.com/replicant4j/replicant/tree/v6.78) (2020-03-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.77...v6.78)

* Rework the initialization sequence in `ReplicantContext` to eliminate the need for the `deferScheduler=true` parameter to be applied to the `@ArezComponent` annotation in `replicant.Converger` and `replicant.ReplicantRuntime`. This produces a more reliable mechanism for deferring observer reactions.
* Remove the need to cache the `ArezContext` in `ReplicantContext` by moving the `requestSync` logic to `replicant.ReplicantRuntime`.
* Refactor the initialization sequences when `Zone`'s are enabled so that zone creation is always contained within an Arez scheduler lock to avoid access of data from observers before the data has been prepared.

### [v6.77](https://github.com/replicant4j/replicant/tree/v6.77) (2020-03-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.76...v6.77)

* Upgrade the `org.realityforge.arez` artifacts to version `0.171`.
* Simplify some of the test infrastructure by adopting the `org.realityforge.arez.testng:arez-testng` dependency.
* Fix an error that occurs as the server was not locking the session when it updated the eTags.
* Fix bug that would trigger an invariant failure in clients when a cached response was returned. The cause was a duplicate ok message for the initiating request.

### [v6.76](https://github.com/replicant4j/replicant/tree/v6.76) (2020-03-05) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.75...v6.76)

* Fix concurrency bug that results from routing a message to a client before the client is aware that they are subscribed to a channel. This can occur if the message is in a packet queued prior to the subscription update packet.

### [v6.75](https://github.com/replicant4j/replicant/tree/v6.75) (2020-03-04) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.74...v6.75)

* Upgrade the `org.realityforge.braincheck` artifact to version `1.26.0`.
* Fix bug where a request that caused changes replicated to the user could result in a hang in the client due to failing to send back a answer over replicant.

### [v6.74](https://github.com/replicant4j/replicant/tree/v6.74) (2020-03-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.73...v6.74)

* Upgrade the `org.realityforge.arez` artifacts to version `0.170`.
* Add `ReplicantSession.maybeSendPacket(...)` helper method that will only send a packet for `ChangeSet` if it is non-empty or it is marked as required.
* Refactor `ReplicantSessionManagerImpl.saveEntityMessages(...)` so that each session is processed in succession rather than processing all sessions simultaneously an phase-by-phase, event-by-event. This is preparing for a future mechanism via which sessions can be locked during processing to avoid concurrency issues as outlined in stocksoftware/rose#716 and replicant4j/replicant#4.
* Remove unused `ChangeAccumulator`.
* Expose `ChangeSet.merge(Collection<Change>)` and `ChangeSet.mergeActions(Collection<Change>)` as public methods.
* Remove invariant guard that is no longer needed post `v6.71` when invoking `Replicant.context().request(...)`.
* Expose `ReplicantSession.isOpen()` helper method.
* Add `ChangeSet.hasContent()` helper method that is used to determine whether a `ChangeSet` is required to be sent to the client.
* Add `FINE` and `FINER` level logging to `ReplicantSession` when events of interest occur. The intent is to make it easy to turn logging on trace problems during development and production.
* Remove the `AbstractSecuredSessionRestService.getAuthService()` method as it is unused.
* Refactor the way processing is performed on the server. The entity change messages are still collected within the transaction but they are now packaged up and posted to separate queues for each session. Another task in a separate thread will then process the change messages for each session. The selection/routing of each message to a session occurs in this separate thread as does the expansion of `GraphLink` messages. This fixes several dead-lock scenarios that could occur as described in #16
* Add locking to `ReplicantSession` so that routing messages and altering of subscriptions requires the lock acquisition. This avoids several crashes related to concurrency issues such as those described in #4 and several issues within rose.

### [v6.73](https://github.com/replicant4j/replicant/tree/v6.73) (2020-02-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.72...v6.73)

* Add `WebSocketConfig.create(...)` helper method and make the constructor private access to simplify creating the config object.

### [v6.72](https://github.com/replicant4j/replicant/tree/v6.72) (2020-02-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.71...v6.72)

* Refactor `ReplicantRpcRequestBuilder` to accept the base url during construction.
* Add `SecuredReplicantRpcRequestBuilder` to simplify generation of secure request builders.

### [v6.71](https://github.com/replicant4j/replicant/tree/v6.71) (2020-02-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.70...v6.71)

* Upgrade the `org.realityforge.arez` artifacts to version `0.169`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.151`.
* Upgrade the `org.realityforge.org.jetbrains.annotations` artifact to version `1.5.0`.
* Refactor the way requests are generated so that if a request occurs before a replicant connection has been established then the request is added to a pending queue which is processed the next time connection is established.

### [v6.70](https://github.com/replicant4j/replicant/tree/v6.70) (2020-02-11) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.69...v6.70)

* Generate a runtime exception when `EntityChangeBroker` is instantiated but has been compiled with the compiler flag `-XdisableClassMetadata` as the broker will fail in suprising ways due to lack og `Class.getSuperClass()` support.

### [v6.69](https://github.com/replicant4j/replicant/tree/v6.69) (2020-02-10) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.68...v6.69)

* Add template methods into `AbstractReplicantEndpoint` so that the application can get notification when a command is processed by the websocket handler and/or a websocket is closed.
* Introduce `AbstractEeReplicantEndpoint` to simplify writing endpoints in an enterprise java environment.
* Replace template method `AbstractReplicantEndpoint.getLogger()` with a static field to reduce the amount of code generated by downstream consumers.
* Generate an invariant failure if `ReplicantContext.newRequest` is invoked before replicant has established a connection with the underlying server.
* Add `ReplicantContext.findConnectionId(...)` method to expose the underlying connection/session id to client code.

### [v6.68](https://github.com/replicant4j/replicant/tree/v6.68) (2020-02-07) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.67...v6.68)

* Fix bug introduced in `v6.67` that resulted in potentially incorrect filters being used for instance graphs.

### [v6.67](https://github.com/replicant4j/replicant/tree/v6.67) (2020-02-06) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.66...v6.67)

* Optimize access to channels from `SystemMetaData` and remove unused `SystemMetaData.getChannels()` method.
* Replace `ChannelMetaData._typeGraph` boolean flag with `ChannelMetaData._instanceRootEntityTypeId` to make it easier to drive business logic and to write tests for subscription logic.
* Extract generic `ReplicantSessionManagerImpl.processDeleteMessages()` from downstream libraries to make it easier to refactor logic in subscription managers.
* Expose instance channels by index in `SystemMetaData`. The ultimate goal is to create a fast mechanism for accessing metadata in the subscription manager.
* Implement previously abstract `ReplicantSessionManagerImpl.shouldFollowLink()` and generate an exception if called without being overriden.
* Remove `NoSuchChannelException` as never caught, handled distinctly and nor does it provide more useful data.

### [v6.66](https://github.com/replicant4j/replicant/tree/v6.66) (2020-02-05) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.65...v6.66)

* Import `ReplicantSession.pingTransport()` from downstream consumers that is used to keep alive the web socket connection when passing through some intermediaries that close websockets when idle.
* Add `ReplicantSessionManagerImpl.pingSessions()` method that pings all sessions to keep them alive. This is expected to be called periodically to ensure websockets are not closed by intermediaries such as firewalls and load balancers.

### [v6.65](https://github.com/replicant4j/replicant/tree/v6.65) (2020-01-31) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.64...v6.65)

* Refactor the `replicant.TransportContext` interface to make it public and to ensure all methods are callable from outside the package.
* Extract `replicant.AbstractTransport` class to facilitate the construction of alternative transport implementations.

### [v6.64](https://github.com/replicant4j/replicant/tree/v6.64) (2020-01-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.63...v6.64)

* Change `SubscriptionService.findInstanceSubscription(...)` so that it is effectively `readOutsideTransaction=ENABLED` to allow it to be invoked by imperative code outside an Arez action. Typically this is done via code such as `Replicant.context().findInstanceSubscription(...)` and is often used to detect whether a subscription is present locally.

### [v6.63](https://github.com/replicant4j/replicant/tree/v6.63) (2020-01-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.62...v6.63)

* Make `ReplicantRuntime.getState()` a `@Memoize(readOutsideTransaction=ENABLED)` method so that it can be read in imperative code without the need to explicitly wrap it in an Arez action. Typically this is done via code such as `Replicant.context().getState()`.
* Catch exceptions in server code when calling `expandLinks(...)` and rather than aborting the entire request, just close the session that generated the error and continue processing the change set for other sessions.

### [v6.62](https://github.com/replicant4j/replicant/tree/v6.62) (2020-01-24) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.61...v6.62)

* Add logging to `AbstractReplicantEndpoint` when web socket actions occur. This mandates that downstream libraries implement a template method to retrieve the `Logger` to log to.

### [v6.61](https://github.com/replicant4j/replicant/tree/v6.61) (2020-01-22) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.60...v6.61)

* Upgrade the `org.realityforge.grim` artifacts to version `0.04`.
* Upgrade the `org.realityforge.guiceyloops` artifact to version `0.106`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.148`.
* Remove the `schemaName` field from all the events defined in the package `replicant.events`. The `schemaName` value is only present when the `replicant.enable_names` compile time constant is set to `true` which is rarely the case in production environments. The field was not used in any downstream projects.

### [v6.60](https://github.com/replicant4j/replicant/tree/v6.60) (2020-01-10) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.59...v6.60)

* Decouple from `arez.component.CollectionsUtil` which will be removed from Arez in the near future.

### [v6.59](https://github.com/replicant4j/replicant/tree/v6.59) (2020-01-10) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.58...v6.59)

* Upgrade the `org.realityforge.arez` artifacts to version `0.165`.
* Avoid crash scenario where a delayed dispose in response to `decRef` reducing the reference count to `0` triggers after `AreaOfInterest` was explicitly disposed. In most applications this should not be a problem but applications that include reference counting as well as explicit dispose can trigger this bug.
* Rework `AreaOfInterestService` to avoid depending on the arez internal class `arez.component.internal.AbstractContainer` as the API and behaviour of this type can change between Arez releases, potentially breaking replicant.
* When `ReplicantContext.activate()` is invoked, attempt to send synchronization messages for any connectors where relevant. This catches an error scenario where a sync attempt should be made when the context is deactivated but is instead discarded.
* Make sure a sync is requested if an `AreaOfInterest` is removed and there is a local `Subscription` present for the `AreaOfInterest`.

### [v6.58](https://github.com/replicant4j/replicant/tree/v6.58) (2020-01-08) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.57...v6.58)

* Re-introduce an event based notification mechanism for legacy applications when significant state changing events occur. The events emitted are introduced on an as-needed basis and currently include; `SubscribeCompletedEvent`, `SubscribeStartedEvent`, `SubscriptionUpdateCompletedEvent`, `SubscriptionUpdateStartedEvent`, `MessageProcessedEvent`. This code is omitted unless the `replicant.enable_events` compile time setting is set to `true`. The code will likely be removed in the future as the legacy applications are decommissioned.

### [v6.57](https://github.com/replicant4j/replicant/tree/v6.57) (2020-01-06) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.56...v6.57)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.147`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.164`.
* Expose the `EntityChangeBroker.isPaused()` method for use by downstream libraries.

### [v6.56](https://github.com/replicant4j/replicant/tree/v6.56) (2019-12-19) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.55...v6.56)

* Add the ability for the user to explicitly pause and resume the change broker by exposing the pause method and enabling resume by calling `release()` on the lock.

### [v6.55](https://github.com/replicant4j/replicant/tree/v6.55) (2019-12-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.54...v6.55)

* Fix incorrect constant key in `ReplicantConfig` which made it impossible to enable the `enable_change_broker` compile time setting.

### [v6.54](https://github.com/replicant4j/replicant/tree/v6.54) (2019-12-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.53...v6.54)

* Upgrade the `org.realityforge.arez` artifacts to version `0.159`.

### [v6.53](https://github.com/replicant4j/replicant/tree/v6.53) (2019-12-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.52...v6.53)

* Consistently use `String` values when setting `REQUEST_COMPLETE_KEY` rather than using `String` in some contexts and `Boolean` in other contexts.
* Upgrade the `org.realityforge.arez` artifacts to version `0.158`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.144`.
* Import and simplify the `EntityChangeBroker` and related code from replicant version 5.x. The intent is to support replicant 5 change monitoring strategies aswell as Arez change based detection within the same codebase.

### [v6.52](https://github.com/replicant4j/replicant/tree/v6.52) (2019-11-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.51...v6.52)

* Simplify `ReplicantSubscription` to ease subclassing by reducing the number of template methods that need to be implemented.

### [v6.51](https://github.com/replicant4j/replicant/tree/v6.51) (2019-11-21) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.50...v6.51)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.142`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.156`.
* Reduce access levels of methods annotated with `@ObservableValueRef` and `@ContextRef` avo avoid warnings from the new Arez version.

### [v6.50](https://github.com/replicant4j/replicant/tree/v6.50) (2019-11-12) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.49...v6.50)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `2.27`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.141`.

### [v6.49](https://github.com/replicant4j/replicant/tree/v6.49) (2019-11-11) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.48...v6.49)

* Upgrade the `org.realityforge.arez` artifacts to version `0.154`.
* Upgrade the `org.realityforge.org.jetbrains.annotations` artifact to version `1.2.0`.
* Upgrade the `org.realityforge.react4j` artifacts to version `0.140`.
* Remove some unused methods from `EntityMessageCacheUtil`.
* Remove unused method `ReplicantContextHolder.contains(...)`.
* Reduce access of `ReplicantContextHolder.clean()` to package access.
* Reduce access of `ReplicantContextHolder.putAll(...)` to package access.
* Avoid intersection types that trigger bugs in recent versions of JDT used by post-2.8.2 versions of GWT.

### [v6.48](https://github.com/replicant4j/replicant/tree/v6.48) (2019-10-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.47...v6.48)

* Upgrade the `org.realityforge.arez` artifacts to version `0.151`.
* Suppress or eliminate rawtypes warnings and turn on linting to avoid their return.

### [v6.47](https://github.com/replicant4j/replicant/tree/v6.47) (2019-10-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.46...v6.47)

* Upgrade the `org.realityforge.react4j` artifacts to version `0.134`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.150`.
* Upgrade the `org.realityforge.org.jetbrains.annotations` artifact to version `1.1.0`.
* Remove the `jul` (a.k.a. `java.util.logging`) strategy available when configuring the `ReplicantLogger` via the compile-time property `replicant.logger`. This strategy was never used in practice.
* Rework the way `ReplicantLogger` is implemented to consolidate the JRE and javascript based console loggers into the class `ConsoleLogger`. The involved renaming the `console_js` value to `console` for the compile-time property `replicant.logger`.
* Upgrade the `org.realityforge.braincheck` artifact to version `1.25.0`.
* Cleanup the pom generated for the `client` module.
* Add the `org.realityforge.grim` dependency required for arez.

### [v6.46](https://github.com/replicant4j/replicant/tree/v6.46) (2019-09-16) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.45...v6.46)

* Upgrade the `org.realityforge.javax.annotation` artifact to version `1.0.1`.
* Migrate `@ComponentDependency` to fields where possible to avoid warnings in the next version of Arez.
* Upgrade the `org.realityforge.arez` artifacts to version `0.145`.

### [v6.45](https://github.com/replicant4j/replicant/tree/v6.45) (2019-09-03) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.44...v6.45)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `2.25`.
* Add the `org.realityforge.replicant.server.graphql.ReplicantEnabledDataFetcher` class to simplify writing replicant enabled GraphQL operations using `graphql-java`.

### [v6.44](https://github.com/replicant4j/replicant/tree/v6.44) (2019-07-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.43...v6.44)

* Upgrade the `org.realityforge.guiceyloops` artifact to version `0.102`.
* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.14`.
* Upgrade the `org.realityforge.braincheck` artifact to version `1.20.0`.
* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `2.24`.
* Update the `org.realityforge.react4j` dependencies to version `0.132`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.143`.
* Remove unused `spritz` dependency.

### [v6.43](https://github.com/replicant4j/replicant/tree/v6.43) (2019-04-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.42...v6.43)

* Remove `Converger.allConnectorsSynchronized()` as it was unused outside of tests and could return an incorrect
  result as not all dependencies were reactive.
* Update the `org.realityforge.react4j` dependencies to version `0.126`.

### [v6.42](https://github.com/replicant4j/replicant/tree/v6.42) (2019-04-25) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.41...v6.42)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `2.23`.
* Remove `{@inheritDoc}` as it only explicitly indicates that the default behaviour at the expense of significant visual clutter.
* Remove unused parameters and cleanup TODO in `WebSocketTransport`

### [v6.41](https://github.com/replicant4j/replicant/tree/v6.41) (2019-04-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.40...v6.41)

* Defer `WebSocket.close()` invocation on web socket that is still `CONNECTING` until it has connected
  to avoid an error.
* Remove error parameter from several spy events as it is no longer available within a `WebSocket` system.
* Avoid attempting attempts to set connection in `Connector` if the same value ias passed as the setter has the side-effect of generating spy messages which can be confusing.
* Rename constants in `ConnectorEntry` to be named `*_REGEN_PER_SECOND` rather than `*_REGEN_PER_MILLISECOND` to reflect actual behaviour.
* When `Connector` reconnection attempts are rate limited, schedule another attempt in the future.

### [v6.40](https://github.com/replicant4j/replicant/tree/v6.40) (2019-04-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.39...v6.40)

* Upgrade the `org.realityforge.braincheck` artifact to version `1.16.0`.
* Upgrade the `org.realityforge.arez` artifacts to version `0.136`.
* Update the `org.realityforge.react4j` dependencies to version `0.125`.
* Explicitly invoke `Converger.removeOrphanSubscriptions()` method when the session is synchronized rather than relying on `@Observer` being triggered on changes. This is more explicitly behaviour and simpler to understand. This fixes a problem introduced in the `v6.37` which would result in no channels being unsubscribed as they observer would no longer activate.

### [v6.39](https://github.com/replicant4j/replicant/tree/v6.39) (2019-04-16) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.38...v6.39)

* Change security of `AbstractSecuredSessionRestService` so sessions are not allowed to access their individual
  details as no longer required.
* Remove `userID` from session as no longer populated or used.
* Remove `ReplicantSecuredSessionManagerImpl` as no longer used.
* Add the ability to specify and update an authentication token over WebSocket transport. Add a hook to `AbstractReplicantEndpoint` to enable downstream projects to implement authentication as required.
* Return to using `setTimeout` to schedule network activity to avoid background timer throttling.
* If the browser page is not visible then do all message processing within the message handler callback to avoid suffering under the vagaries of the background timer throttling.

### [v6.38](https://github.com/replicant4j/replicant/tree/v6.38) (2019-04-12) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.37...v6.38)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `1.0.0-b21-6a027d2`.
* Upgrade the `org.realityforge.braincheck` artifact to version `1.15.0`.
* Omit attributes not relevant to WebSocket based sessions from the the rest API describing sessions.
* Upgrade the `org.realityforge.arez` artifacts to version `0.135`.
* Refactor the `replicant.Transport` interface to remove methods 4-arg `requestSubscribe()`, `requestSubscriptionUpdate()`
  and `requestBulkSubscriptionUpdate()` that can be reimplemented as calls to 6-arg `requestSubscribe()`.
* Refactor the serverside code to mandate the use of `WebSocket` transport layer. As a result of this there is no
  need to maintain infrastructure for representing packets, queuing and retrying packets. Thus the `Packet` class
  and `PacketQueue` have been removed. The endpoints designed for manipulation of sessions (i.e. creating,
  destroying, subscribe, unsubscribe etc) has also been removed.
* `ReplicantSessionManager.invalidateSession(...)` now accepts a `ReplicantSession session` as a parameter rather
  than `String sessionId` as all callers have a session available.
* `ReplicantSessionManager.subscribe(...)` no longer returns `CacheStatus` parameter as it is no longer used.
* Remove the dependency on the `org.realityforge.gwt.webpoller:gwt-webpoller:jar` artifact.
* Move the `InvalidHttpResponseException` exception to the package `org.realityforge.replicant.client.gwt` as that
  is the only remaining place where it is used.
* Remove `Transport.onMessageProcessed()` as flow control is no longer part of the application layer and is instead
  managed by the `WebSocket` implementation.
* Removed sequences from the server-to-client message as messages can never be transmitted out of sequence.
* Refactor `Connector.state` to be readable outside a transaction.
* Remove the need to pass `onDisconnectionError` handler to the `Transport.disconnect(...)` method and instead treat
  any error while disconnecting as a disconnect error.
* Remove the need to pass `onConnectionError` handler to the `Transport.connect(...)` method and instead treat any
  error while connecting as a connect error.
* Remove the need to pass `onDisconnection` handler to the `Transport.disconnect(...)` method and directly invoke
  callback from `Transport`.
* Decouple the `ChangeSetParser` class from the javaee infrastructure and remove code used for testing
  (i.e. `EntityChangeDataImpl`) from the main source tree into the test source tree.
* Rename the `Transport.disconnect()` method to `Transport.requestDisconnect()` to align with intent and existing
  patterns in the `Transport` interface.
* Add "type" field to the `ChangeSet` message sent from the server to client in preparation for allowing multiple
  different types of messages to be emitted from the server. Refactor the client side representation to have
  `AbstractMessage` and `ServerToClientMessage` parent classes to allow some code sharing.
* Add `SubscribeMessage` and `UnsubscribeMessage` classes to represent the requests that are sent to the server.
* Rename `ChangeSet` to `ChangeSetMessage` to match the naming conventions of other messages.
* Significant rework of the entire network layer to enable `WebsocketTransport`.

### [v6.37](https://github.com/replicant4j/replicant/tree/v6.37) (2019-03-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.36...v6.37)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `1.0.0-b20-bfe6e22`.
* Stop tracking last access time of the `ReplicantSession` as it is no longer used. The sessions expire
  after the associated web socket is closed rather than after a specified idle period.
* Stop tracking `createdAt` time of the `ReplicantSession` as it has never been used.
* Replace `ReplicantSessionManagerImpl.removeIdleSessions()` with `ReplicantSessionManagerImpl.removeClosedSessions()`
  as not expecting to need to maintain infrastructure for polling clients.
* Change the return type of `ReplicantSessionManagerImpl.sendPacket(..)` to void rather than returning a
  `Packet` as the return value was only ever used in tests.
* Change the return type of `PacketQueue.addPacket(..)` to void rather than returning a
  `Packet` as the return value was only ever used in tests.
* Rename `PacketQueue.getPacket(int)` to `PacketQueue.findPacketBySequence(int)` to align with existing
  conventions.
* Add `ReplicantSession.sendMessage(...)` helper method that delegates to internal queue.
* Add the `org.realityforge.spritz:spritz-core:jar` dependency in preparation for client-side websockets.
* Allow the creation of a `ReplicantSession` to be based of a WebSocket session.
* Add a method `ReplicantSessionManagerImpl.removeAllSessions()` that can forcibly close all connected clients.
* Add a `ReplicantSession.ack(...)` that delegates to the underlying queue and may send next packet on WebSocket
  connection.
* Remove the `explicitSubscribe` parameter from the `ReplicationSessionManager.subscribe(...)`,
  `ReplicationSessionManager.bulkSubscribe(...)`, `ReplicationSessionManager.unsubscribe(...)` and
  `ReplicationSessionManager.bulkUnsubscribe(...)` methods as the value of the parameter is only ever expected
  to be true.
* Remove the `changeSet` parameter from the `ReplicationSessionManager.subscribe(...)`, `ReplicationSessionManager.bulkSubscribe(...)`, `ReplicationSessionManager.unsubscribe(...)` and
  `ReplicationSessionManager.bulkUnsubscribe(...)` methods as the value is always the result of the call
  `EntityMessageCacheUtil.getSessionChanges()`.
* Ensure that `ReplicantSessionManagerImpl` explicitly expires sessions when the component is destroyed.
* Align the `ChannelAddress.toString()` output with the format used to serialize channel across the wire.
* Add a `ChannelAddress.parse(String)` method to parse the channel from the wire format.
* Remove the `ReplicationSessionManager.updateSubscription(...)` and `ReplicationSessionManager.bulkUpdateSubscription(...)`
  methods that have not been used since the move to "modern" replicant.
* Remove several methods from `ReplicationSessionManager` not used outside of tests. These are remnants of
  replicant's evolution and include:
  - `getSessionKey()`
  - `delinkSubscription(...)`
  - `bulkDelinkSubscription(...)`
* Remove or reduce the access level of several methods in `ReplicationSessionManagerImpl` that are only used
  by tests.
* Remove the local cache key during subscribe if the cache key is stale.

### [v6.36](https://github.com/replicant4j/replicant/tree/v6.36) (2019-03-05) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.35...v6.36)

* Remove `AbstractSessionContextImpl` as it was effectively unused. While subclasses exist, none of
  the subclasses invoked any of the `AbstractSessionContextImpl` methods.
* Add `ChannelAddress.hasSubChannelId()` helper method.
* Add support for a separate `DELETED` channel action message that indicates that the root of an
  instance graph has been deleted and will not be coming back. This allows the client-side to respond
  appropriately and differs from `REMOVED` that may just indicates that has been removed from the area
  of interest.
* Emit the `SubscribeRequestQueuedEvent` before triggering scheduler so that the spy message is emitted
  prior to performing action which improves traceability.
* Avoid converging any actions for `DELETED` `AreaOfInterest` instances.

### [v6.35](https://github.com/replicant4j/replicant/tree/v6.35) (2019-03-01) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.34...v6.35)

* Add `ChannelMetaData.hasFilterParameter()` helper method to simplify the code that works differently
  depending on the presence or non-presence of thefilter parameter.
* Add filtering type of `INTERNAL` that indicates that a graph is filtered but there is no parameter
  that controls the filtering behaviour and instead the filtering is due to internal structure of the
  data or the server.

### [v6.34](https://github.com/replicant4j/replicant/tree/v6.34) (2019-02-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.33...v6.34)

* Caching fixed as the synthesis of the cache key had diverged between the code that added data to the
  cache and the code that retrieved data from the cache. To avoid a similar problem in the future the
  generation of the cache key has been moved to the `ChannelAddress` class.

### [v6.33](https://github.com/replicant4j/replicant/tree/v6.33) (2019-02-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.32...v6.33)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `1.0.0-b19-fb227e3`.
* Change the serialized representation of links from an entity to a channel from an array
  of json objects of the form `[{"cid": 20, "scid": 1},{"cid": 20, "scid": 2},{"cid": 1}]`
  to a more succinct representation as an array of strings of the form `["20.1","20.2","1"]`.
  This involved several internal changes as well as removal of the `replicant.messages.EntityChannel`
  data transport class.
* Change the serialized representation of entity references from `{"typeId":42,"id":23,...}` to
  a more compact `{"id":"42.23",,...}`.
* Omit the `etag` property and the `requestId` property if they are null rather than transmitting nulls.
* Upgrade the `org.realityforge.braincheck` artifact to version `1.13.0`.
* Support `ChannelAddress.getName()` even when `Replicant.areNamesEnabled()` returns false as that behaviour
  is required and was implemented in other places.
* Change the serialized representation of channels from `"channel_types":[{"cid": 20, "scid": 1, "action":"add"}]`
  to a more compact `"channels":["+20.1"]`. Filtered channels actions were optimized in a similar fashion.

### [v6.32](https://github.com/replicant4j/replicant/tree/v6.32) (2019-02-24) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.31...v6.32)

* Upgrade the `org.realityforge.com.google.jsinterop` artifact to version `1.0.0-b2-e6d791f`.
* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `1.0.0-b18-f3472e7`.
* Update the `org.realityforge.react4j` dependencies to version `0.121`.

### [v6.31](https://github.com/replicant4j/replicant/tree/v6.31) (2019-02-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.30...v6.31)

* Upgrade the `org.realityforge.com.google.elemental2` artifacts to version `1.0.0-b17-6897368`.
* Add explicit reference counting to `AreaOfInterest` to ensure that object is disposed only if there
  is no explicitly interested parties. Also added in a small delay for `AreaOfInterest` so that
  a dangling `AreaOfInterest` can persist for a short period of time without unloading such as when
  changing pages.
* Update `ReplicantSubscription` to use explicit reference counting so that AreaOfInterests are correctly
  released when no longer used.
* If the server removes a subscription from the client then remove the associated `AreaOfInterest` if any.
  This typically occurs when the root entity in an instance graph is removed and the instance graph is
  subsequently removed.

### [v6.30](https://github.com/replicant4j/replicant/tree/v6.30) (2019-02-14) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.29...v6.30)

* Update the `org.realityforge.arez` dependencies to version `0.130`.
* Update the `org.realityforge.react4j` dependencies to version `0.119`.
* Remove the unused `@*Ref` annotated methods `getContext()`, `getComponentName()`
  and `component()` from `SubscriptionService` and `ReplicantRuntime`.

### [v6.29](https://github.com/replicant4j/replicant/tree/v6.29) (2019-02-12) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.28...v6.29)

* Fix pom for `replicant-client` so that it includes `react4j` dependency.
* Update project to reflect migration to `replicant4j/replicant` project.
* Emit the name of the graph in the session rest service to aid debugging.
* Fix bug in `ReplicantSubscription` that resulted in `@Action` triggering during dispose.

### [v6.28](https://github.com/replicant4j/replicant/tree/v6.28) (2019-02-07) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.27...v6.28)

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

### [v6.27](https://github.com/replicant4j/replicant/tree/v6.27) (2019-02-04) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.26...v6.27)

* Remove the prop `onNotAsked` from the `ReplicantSubscription` component as it represents
  a state that is never presented to the user and is followed in quick succession by the
  `OnLoading` state and can thus be replaced by the `onLoading` prop.

### [v6.26](https://github.com/replicant4j/replicant/tree/v6.26) (2019-02-04) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.25...v6.26)

* Work around bug resulting from react4j upgrade that merged the `componentWillUnmount` and
  dispose steps for react4j components. Previously we were able to invoke `@Action` annotated
  methods in `componentWillUnmount()` to release resources but this is no longer possible. Arez
  should allow the use of `@CascadeDispose` in this context but due to current limitations in
  Arez this is not possible. A workaround until this limitation has been addressed, has been
  added to the `ReplicantSubscription` class.
* Upgrade Elemental2 artifacts to groupId `org.realityforge.com.google.elemental2`
  and version `1.0.0-b14-2f97dbe`. This makes it possible to use a newer version of the
  Elemental2 library in downstream products.

### [v6.25](https://github.com/replicant4j/replicant/tree/v6.25) (2019-01-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.24...v6.25)

* Update the `org.realityforge.arez` dependencies to version `0.127`.
* Update the `org.realityforge.react4j` dependencies to version `0.114`.

### [v6.24](https://github.com/replicant4j/replicant/tree/v6.24) (2019-01-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.23...v6.24)

* Fix access modifiers on `ReplicantSubscription.postRender()` so that subclasses can be in
  different packages.

### [v6.23](https://github.com/replicant4j/replicant/tree/v6.23) (2019-01-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.22...v6.23)

* Fix the release process to push release to staging repository and Maven Central.

### [v6.22](https://github.com/replicant4j/replicant/tree/v6.22) (2019-01-18) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.21...v6.22)

* Rename some react lifecycle methods in `ReplicantSubscription` so that they align with the names of
  the annotations rather than the names of the methods that needed to be overridden in the past.

### [v6.21](https://github.com/replicant4j/replicant/tree/v6.21) (2019-01-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.20...v6.21)

* Remove deployment from TravisCI infrastructure as it is no longer feasible.
* Update the `org.realityforge.arez` dependencies to version `0.122`.
* Update the `org.realityforge.react4j` dependencies to version `0.110`.

### [v6.20](https://github.com/replicant4j/replicant/tree/v6.20) (2018-11-20) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.19...v6.20)

* Update the `org.realityforge.arez` dependencies to version `0.115`.
* Update the `org.realityforge.react4j` dependencies to version `0.107`.

### [v6.19](https://github.com/replicant4j/replicant/tree/v6.19) (2018-11-08) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.18...v6.19)

* Update the `org.realityforge.arez` dependencies to version `0.114`.
* Update the `org.realityforge.react4j` dependencies to version `0.106`.

### [v6.18](https://github.com/replicant4j/replicant/tree/v6.18) (2018-11-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.17...v6.18)

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

### [v6.17](https://github.com/replicant4j/replicant/tree/v6.17) (2018-10-16) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.16...v6.17)

* Update the `org.realityforge.react4j` dependencies to version `0.102`.

### [v6.16](https://github.com/replicant4j/replicant/tree/v6.16) (2018-10-09) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.15...v6.16)

* Fix bug in `ReplicantSubscription` react4j component was incorrectly comparing a primitive id
  and a boxed id in `componentDidUpdate()` lifecycle method.

### [v6.15](https://github.com/replicant4j/replicant/tree/v6.15) (2018-10-09) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.14...v6.15)

* Correct bug where id was set as `0` for type graphs.

### [v6.14](https://github.com/replicant4j/replicant/tree/v6.14) (2018-10-09) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.13...v6.14)

* Update the `org.realityforge.arez` dependencies to version `0.109`.
* Update the `org.realityforge.react4j` dependencies to version `0.100`.

### [v6.13](https://github.com/replicant4j/replicant/tree/v6.13) (2018-10-04) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.12...v6.13)

* Change `ReplicantSubscription.getId()` to return a primitive integer rather than a boxed `Integer`.

### [v6.12](https://github.com/replicant4j/replicant/tree/v6.12) (2018-09-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.11...v6.12)

* Upgrade the `org.realityforge.gwt.webpoller:gwt-webpoller:jar` artifact to version `0.9.8`.

### [v6.11](https://github.com/replicant4j/replicant/tree/v6.11) (2018-09-25) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.10...v6.11)

* Remove `super.componentDidMount()` and `super.componentDidUpdate( prevProps, prevState )` calls from
  the `ReplicantSubscription` react component as not needed as parent methods are empty as of react4j
  version `0.96`
* Fix bug where converger will not re-converge if multiple AreaOfInterest are added simultaneously and the
  the later `AreaOfInterest` instances can not be grouped into the first `AreaOfInterest` instance. The
  converger would previously incorrectly halt after the first action completed.

### [v6.10](https://github.com/replicant4j/replicant/tree/v6.10) (2018-09-21) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.09...v6.10)

* Update the `org.realityforge.arez` dependencies to version `0.107`.
* Update the `org.realityforge.react4j` dependencies to version `0.96`.
* Update the `org.realityforge.braincheck` dependencies to version `1.12.0`.

### [v6.09](https://github.com/replicant4j/replicant/tree/v6.09) (2018-08-24) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.08...v6.09)

* During code-optimization the `Converger.converge()` method stopped observing filters when converging
  filters which mean that if the filter changed it would no longer re-converge the state of the world.
  This bug has been fixed by an explicit observe of the filter field on `AreaOfInterest`.

### [v6.08](https://github.com/replicant4j/replicant/tree/v6.08) (2018-08-23) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.07...v6.08)

* Update the `org.realityforge.arez` dependencies to version `0.105`.
* Update the `org.realityforge.react4j` dependencies to version `0.93`.
* Remove the interface `org.realityforge.replicant.client.EntityRegistry` and related infrastructure.
  The code is currently unused and unlikely to be adopted in the near future.
* Remove the interface `org.realityforge.replicant.client.EntityLocator` and related infrastructure.
  This has been replaced by `arez.Locator` interface.
* Replace the interface `replicant.Linkable` with the `arez.component.Linkable` interface.
* Replace the interface `replicant.Verifiable` with the `arez.component.Verifiable` interface.

### [v6.07](https://github.com/replicant4j/replicant/tree/v6.07) (2018-07-30) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.06...v6.07)

* Change the scope of the dependencies enlisted in the `@Autorun` actions on the `Converger`
  to eliminate monitoring of entities in `preConverge()` and to re-add dependencies on
  `AreaOfInterest` collection.

### [v6.06](https://github.com/replicant4j/replicant/tree/v6.06) (2018-07-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.05...v6.06)

* Update the `org.realityforge.arez` dependencies to version `0.102`.
* Explicitly limit the scope of the dependencies enlisted in the `@Autorun` actions on the `Converger`
  so that only the data required to trigger changes are monitored.

### [v6.05](https://github.com/replicant4j/replicant/tree/v6.05) (2018-07-26) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.04...v6.05)

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

### [v6.04](https://github.com/replicant4j/replicant/tree/v6.04) (2018-07-24) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.03...v6.04)

* In the `ConsoleSpyEventProcessor` class, correct the log message when a `SubscriptionUpdateCompleted`
  event is generated.
* Fix a concurrency bug where the WebPoller could be left paused on receipt of a "ping" message.
* Add tasks to cleanup artifacts from staging repositories as part of the release process.

### [v6.03](https://github.com/replicant4j/replicant/tree/v6.03) (2018-07-17) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.02...v6.03)

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

### [v6.02](https://github.com/replicant4j/replicant/tree/v6.02) (2018-07-03) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.01...v6.02)

* Fix `Connector.completeAreaOfInterestRequest()` to handle scenario where the area of interest
  request completes after a connection disconnects. This can happen as a result of errors during
  area of interest request or during normal overlapping requests.

### [v6.01](https://github.com/replicant4j/replicant/tree/v6.01) (2018-07-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v6.00...v6.01)

* Link `EntitySchema` instances associated with channel to `ChannelSchema` to simplify validation
  of graph when or if needed.

### [v6.00](https://github.com/replicant4j/replicant/tree/v6.00) (2018-07-02) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v5.99...v6.00)

* Stop using replicant specific mechanisms for managing state and move to Arez for state management.
  This is a major rewrite of the library and involved large scale changes. See the git history for
  full details.

### [v5.99](https://github.com/replicant4j/replicant/tree/v5.99) (2018-04-26) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v5.98...v5.99)

* Make AreaOfInterest public to fix problem when deployed into EE container that attempts to proxy
  package access method that returns AreaOfInterest.  Submitted by James Walker.

### [v5.98](https://github.com/replicant4j/replicant/tree/v5.98) (2018-03-27) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v5.97...v5.98)

* Increase timeout during replication server-to-server session establishment, to handle
  very large data sets. Bit of a hack.  Submitted by James Walker.

### [v5.97](https://github.com/replicant4j/replicant/tree/v5.97) (2017-11-29) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v5.96...v5.97)

* Exposed more information in the `status` endpoint of `AbstractDataLoaderServiceRestService`.
  Add details of the timing, errors, and properties to the connection.   Submitted by James Walker.

### [v5.96](https://github.com/replicant4j/replicant/tree/v5.96) (2017-11-21) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v0.06...v5.96)

* Add ability to query the `ContextConverger` to see if it is idle. Submitted by James Walker.

### [v0.06](https://github.com/replicant4j/replicant/tree/v0.06) (2017-11-14) · [Full Changelog](https://github.com/replicant4j/replicant/compare/v0.5.94...v0.06)

* Updated the `AbstractDataLoaderService` to support bulk loads. Aggregated consecutive AOI actions that can be
  grouped into a single bulk load. Submitted by James Walker.
* Updated the `ContextConvergerImpl` to schedule multiple AOI actions where they are compatible with bulk
  loading. Submitted by James Walker.

### [v0.5.94](https://github.com/replicant4j/replicant/tree/v0.5.94) · [Full Changelog](https://github.com/realityforge/react4j/compare/v0.5.93...v0.5.94)

* in `AbstractSecuredSessionRestService`, check the `PreferredUsename` claim against the UserID associated with the
  Replicant Session, rather than the TokenID. Works with the change on 0.5.93. Submitted by James Walker.

### [v0.5.93](https://github.com/replicant4j/replicant/tree/v0.5.93)

* in `ReplicantSecuredSessionManagerImpl`, use the `PreferredUsename` claim as the UserID associated with the
  Replicant Session, rather than the TokenID. The TokenID will change each time the token refreshes. An
  alternative is the `Subject` token but everywhere this is deployed also adds the claim `PreferredUsename`
  which is easier to read. Submitted by James Walker.

### [v0.5.92](https://github.com/replicant4j/replicant/tree/v0.5.92)

* Use GWT super-source feature to replace `FilterUtil`.

### [v0.5.91](https://github.com/replicant4j/replicant/tree/v0.5.91)

* Made all variants of `ReplicationRequestUtil.runRequest` public.

### [v0.5.90](https://github.com/replicant4j/replicant/tree/v0.5.90)

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

### [v0.5.89](https://github.com/replicant4j/replicant/tree/v0.5.89)

* Move all subscription actions in `WebPollerDataLoaderService` to work via http actions rather than gwt_rpc.
* Introduce `ActionCallbackAdapter` to help managing replicant requests outside gwtrpc
* Support omitting `RequestEntry.RequestKey` during production builds. Removes a large number of strings in GWT
  compilation output. Controlled by use of setting in `ReplicantConfig`.
* Enhance server-side rest session service to accept optional `requestID` query parameter.

### [v0.5.88](https://github.com/replicant4j/replicant/tree/v0.5.88)

* Remove `ReplicantConfig` and dependency on `gwt-property-source` and replace with simple access to property
  via `System.getProperty(...)`

### [v0.5.87](https://github.com/replicant4j/replicant/tree/v0.5.87)

* Remove `EeContextConvergerImpl` and `EeReplicantClientSystemImpl` to allow downstream products to define services.
* Extract `AbstractEeContextConvergerImpl` class to simplify building ee context convergers.
* Make the `converge()` method public in the classes `ContextConvergerImpl` and `ReplicantClientSystemImpl`
  to make it easier to schedule converges in subclasses.

### [v0.5.86](https://github.com/replicant4j/replicant/tree/v0.5.86)

* Ensure that disowned entities still send out events by waiting til the end of the cycle before purging listeners.

### [v0.5.85](https://github.com/replicant4j/replicant/tree/v0.5.85)

* Remove attributes from `ReplicantSession`.
* Inline the `org.realityforge.ssf` dependency and compress inheritance chain for any class incorporated from ssf.
* Remove the `org.realityforge.rest.field_filter` dependency and remove usage from codebase. Effectively
  unused as it only used during local debugging of replicant in which case the filters were typically left
  at default values.
* Extract `AbstractInvocationAdapter` from `AbstractReplicationInterceptor` to make functionality reusable
  in other contexts.

### [v0.5.84](https://github.com/replicant4j/replicant/tree/v0.5.84)

* Extract a helper method `newSessionBasedInvocationBuilder` in `EeWebPollerDataLoaderService`.
* Ensure interfaces can be bound into `EntityRepository` and generated messages via `EntityChangeBroker`.
* Remove `ReplicantGwtClientTestModule`, `AbstractClientTest` and `AbstractGwtClientTest` as no
  downstream users make use of any of these classes.

### [v0.5.83](https://github.com/replicant4j/replicant/tree/v0.5.83)

* Make `ReplicantClientTestModule` more extensible and bind some missing elements required for tests.

### [v0.5.82](https://github.com/replicant4j/replicant/tree/v0.5.82)

* Update GWT module to include `AbstractFrontendContextImpl` and friends for GWT compiler.

### [v0.5.81](https://github.com/replicant4j/replicant/tree/v0.5.81)

* Introduce `AreaOfInterestListenerAdapter` to simplify writing custom listeners.

### [v0.5.80](https://github.com/replicant4j/replicant/tree/v0.5.80)

* Introduce `AbstractFrontendContextImpl` to simplify creation of frontend context for gwt interfaces.

### [v0.5.79](https://github.com/replicant4j/replicant/tree/v0.5.79)

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

### [v0.5.78](https://github.com/replicant4j/replicant/tree/v0.5.78)

* Enhance the `DataLoaderService` so that interaction between the `ClientSession` instances
  and `DataLoaderService` uses a formal contract rather than protected methods.
* Make sure the asynchronous callbacks from JAXRS are correctly contextualized to ensure
  CDI operates as expected in `EeWebPollerDataLoaderService`.
* Increase the log level from FINE to INFO for the `WebPoller` in `WebPollerDataLoaderService`.

### [v0.5.77](https://github.com/replicant4j/replicant/tree/v0.5.77)

* Update `ReplicantEntityCustomizer.configure` to be static.

### [v0.5.76](https://github.com/replicant4j/replicant/tree/v0.5.76)

* Introduce `ReplicantEntityCustomizer` to help customize replicant entities during tests.

### [v0.5.75](https://github.com/replicant4j/replicant/tree/v0.5.75)

* Restructure `ReplicantClientTestModule` so that it also exposes the `EntitySystem` service.

### [v0.5.74](https://github.com/replicant4j/replicant/tree/v0.5.74)

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

### [v0.5.73](https://github.com/replicant4j/replicant/tree/v0.5.73)

* Add implementation EeDataLoaderService.getCacheService().
* Change log level of WebPollerDataLoaderService to INFO.

### [v0.5.72](https://github.com/replicant4j/replicant/tree/v0.5.72)

* Introduce the `EntitySystem` abstraction that collectors the EntityRepository, the EntityChangeBroker and
  the EntitySubscriptionManager services into one access point.
* Move to Java8.
* Move to GWT 2.8.0.
* Refactor the AbstractDataLoader so that required services are exposed as template methods rather than
  being passed into the constructor. The aim is to enable sharing of these services for EE clients.

### [v0.5.71](https://github.com/replicant4j/replicant/tree/v0.5.71)

* Introduce support interface `DataLoaderService` to make interaction with data loader generalizable.

### [v0.5.70](https://github.com/replicant4j/replicant/tree/v0.5.70)

* Introduce constant `ReplicantContext.MAX_POLL_TIME_IN_SECONDS` to make it easy to determine poll
  time in both client and server code.

### [v0.5.69](https://github.com/replicant4j/replicant/tree/v0.5.69)

* Change the access specifier of the class `GwtWebPollerDataLoaderService.ReplicantRequestFactory` to protected.

### [v0.5.68](https://github.com/replicant4j/replicant/tree/v0.5.68)

* Introduce `AbstractSessionContextImpl` as a base class to extend as part of generation.

### [v0.5.67](https://github.com/replicant4j/replicant/tree/v0.5.67)

* Add `ReplicantSessionManagerImpl.delinkDownstreamSubscriptions` and exposed to subclasses.
* Update `ReplicantSessionManagerImpl` so that `ChannelLinks` are only expanded for updates.
* Update `ReplicantSessionManagerImpl` to make `delinkSubscriptionEntries` and `linkSubscriptionEntries`
  protected access and available to subclasses.

### [v0.5.66](https://github.com/replicant4j/replicant/tree/v0.5.66)

* Update the `ReplicantSessionManagerImpl` so the ChangeSet is passed into many methods rather than
  assuming the caller sessions ChangeSet.

### [v0.5.65](https://github.com/replicant4j/replicant/tree/v0.5.65)

* Ensure `ChannelLink.hashcode()`, `ChannelLink.equals()` and `ChannelLink.toString()` take
  into consideration the source channel.

### [v0.5.64](https://github.com/replicant4j/replicant/tree/v0.5.64)

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

### [v0.5.63](https://github.com/replicant4j/replicant/tree/v0.5.63)

* Introduced `ChainedAction` class to help when building chains of actions.
* Convert the "cache action" in the `AbstractDataLoaderService` to be a `ChainedAction`
  to allow injections of actions after cache has been injected.
* Send messages when session is connected/disconnected.
* Move generic connect/disconnection functionality into `AbstractDataLoaderService` from
  `WebPollerDataLoaderService`.

### [v0.5.62](https://github.com/replicant4j/replicant/tree/v0.5.62)

* Fix bug that required that the WebPoller factory be setup prior to creating
  `GwtWebPollerDataLoaderService`.
* Introduce `GwtWebPollerDataLoaderService.newRequestBuilder()` template method for
  constructing `RequestBuilder` objects to allow subclasses to customize requests.
* Update `GwtWebPollerDataLoaderService.newRequestBuilder()` to set the "Authorization"
  http header when creating new builder if `getSessionContext().getAuthenticationToken()`
  is not null.
* Update `GwtWebPollerDataLoaderService.newRequestBuilder()` to set "Pragma: no-cache".

### [v0.5.61](https://github.com/replicant4j/replicant/tree/v0.5.61)

* Add WebPollerDataLoaderService.getWebPollerLogLevel() template method to configure
  log level for WebPoller.

### [v0.5.60](https://github.com/replicant4j/replicant/tree/v0.5.60)

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

### [v0.5.59](https://github.com/replicant4j/replicant/tree/v0.5.59)

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

### [v0.5.58](https://github.com/replicant4j/replicant/tree/v0.5.58)

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

### [v0.5.57](https://github.com/replicant4j/replicant/tree/v0.5.57)

* Rename package `org.realityforge.replicant.client.json.gwt` to `org.realityforge.replicant.client.gwt`.
* Extract the gwt specific functionality of AbstractClientTest to AbstractGwtClientTest and move
  to a separate directory.
* Move LocalCacheService and ReplicantRpcRequestBuilder to org.realityforge.replicant.client.json.gwt package.
* Move generation of DataLoadCompleteEvent event from and linkage against EventBus from
  AbstractDataLoaderService to GwtDataLoaderService and move DataLoadCompleteEvent to
  org.realityforge.replicant.client.json.gwt package.
* Remove unused org.realityforge.replicant.client.json.gwt.StringUtils.

### [v0.5.56](https://github.com/replicant4j/replicant/tree/v0.5.56)

* Make it possible to store authentication token in SessionContext.
* In AbstractDataLoaderService, reorder actions so that validation of repository occurs after debug output.

### [v0.5.55](https://github.com/replicant4j/replicant/tree/v0.5.55)

* Update simple-session-filter dependency to enable CDI support for session managers.
* Ensure ReplicantSessionManager and ReplicantJsonSessionManager can be CDI beans by removing final
  methods and adding tests to enforce this feature.
* Convert ReplicantPollResource to a CDI bean from an EJB.
* Rework SessionRestService to an abstract class AbstractSessionRestService with a template method
  to retrieve the SessionManager. Document how subclasses need to be defined.

### [v0.5.54](https://github.com/replicant4j/replicant/tree/v0.5.54)

* Remove per request hash as the "Pragma: no-cache" header gets around caching in proxy servers.
* Fix implementation of `JsoChange.containsKey` so that the method will return true even if the value is null.

### [v0.5.53](https://github.com/replicant4j/replicant/tree/v0.5.53)

* Set "Pragma: no-cache" header when polling for changes.
* Generate a per request hash added to each poll request to punch through overly zealous caching proxy servers.
* Revert to using @EJB rather than @Inject for ReplicantPollSource to work-around limitations when deploying to GlassFish.

### [v0.5.52](https://github.com/replicant4j/replicant/tree/v0.5.52)

* Specify further header in CacheUtil to avoid caching.

### [v0.5.51](https://github.com/replicant4j/replicant/tree/v0.5.51)

* Add some documentation to README covering the basic concepts.
* Eliminate BadSessionException and require AbstractDataLoaderService to implement ensureSession().

### [v0.5.50](https://github.com/replicant4j/replicant/tree/v0.5.50)

* Update the AbstractDataLoaderService so that it only purges subscriptions that are "owned" by
  the data loader service and ignores any subscriptions owned by other data loaders.
* Add EntityChangeBroker.removeAllChangeListeners() to purge listeners for a specific entity.
* Update EntitySubscriptionImpl to remove type map when empty.
* Ensure that entities are unloaded from EntityRepository and listeners in the change broker
  are removed when subscriptions are removed as part of disconnect() method in AbstractDataLoaderService.
* Update EntityRepositoryDebugger to add methods to support debugging of subscriptions.

### [v0.5.49](https://github.com/replicant4j/replicant/tree/v0.5.49)

* Add a guard in EntityMessageCacheUtil so that if EntityMessageCacheUtil is accessed outside of a
  replication context, an exception is thrown. This forces all entity modifications to occur within
  a replication context.

### [v0.5.48](https://github.com/replicant4j/replicant/tree/v0.5.48)

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

### [v0.5.47](https://github.com/replicant4j/replicant/tree/v0.5.47)

* When a client-side session is disconnected, delete the server-side session.
* Upgrade to compile and test against GWT 2.7.0.
* Replace TokenRestService with enhanced SessionRestService that provides status details
  about the session and other candidate sessions.
* Add field_filter dependency to expand the capabilities of the token service.
* Add a template method ReplicantSession.emitStatus() to make it possible for sub-classes
  to provide additional details about session status.

### [v0.5.46](https://github.com/replicant4j/replicant/tree/v0.5.46)

* Add CollectorEntityChangeListener that records entity change events, useful during testing.
* Introduce ReplicantClientTestModule in the client-side test code that registers the standard set
  of client-side dependencies. Update AbstractClientTest to add ReplicantClientTestModule to the
  list of modules.

### [v0.5.45](https://github.com/replicant4j/replicant/tree/v0.5.45)

* Remove unused EntityMessageGenerator.
* Remove ChangeRecorder as downstream projects no longer use the class and instead
  generate the entire artifact.

### [v0.5.44](https://github.com/replicant4j/replicant/tree/v0.5.44)

* Re-add inadvertently removed resumeBroker() and pauseBroker() to AbstractClientTest.
* Add some minimal tests around how the WebPollerDataLoader will handle system failures

### [v0.5.43](https://github.com/replicant4j/replicant/tree/v0.5.43)

* Add abstract method AbstractDataLoaderService.getSystemKey() that helps identify which replication
  system that the data loader is supporting. This is important when replicant is used with multiple
  replication data sources.
* Introduce an EntityBrokerTransaction structure that identifies the data loader that initiated the
  "transaction" (a.k.a the disable or pause actions).

### [v0.5.42](https://github.com/replicant4j/replicant/tree/v0.5.42)

* Ensure TokenRestService sets HTTP headers so IE performs no caching.

### [v0.5.41](https://github.com/replicant4j/replicant/tree/v0.5.41)

* Ensure ReplicantPollResource sets HTTP headers to ensure IE performs no caching.
* Extract out a constant for the query parameter used to identify the last received packet sequence.

### [v0.5.40](https://github.com/replicant4j/replicant/tree/v0.5.40)

* Send a DataLoadCompleteEvent when a data load completes.
* Send a SystemErrorEvent on the EventBus when a system error occurs.
* Merge the WebPollerDataLoaderService.connect() method from downstream projects as they are all
  identical in their implementation.
* Extract TokenRestService from downstream projects. The TokenRestService simply generates a new
  session and returns the token.

### [v0.5.39](https://github.com/replicant4j/replicant/tree/v0.5.39)

* Introduce EntityChangeListenerAdapter to make writing listeners easier.

### [v0.5.38](https://github.com/replicant4j/replicant/tree/v0.5.38)

* Remove the ReplicationRequestManager abstraction as it implies a transactional boundary crossing
  which is not the intention. Implement the same functionality as a collection of static methods
  in the ReplicationRequestUtil utility class.

### [v0.5.37](https://github.com/replicant4j/replicant/tree/v0.5.37)

* Add AbstractDataLoaderService.supportMultipleDataLoaders() template method that should return
  true if the data loader source loader should gracefully share common resources between data
  loaders. Avoid Pausing already paused loader during data load processing when this return true.

### [v0.5.36](https://github.com/replicant4j/replicant/tree/v0.5.36)

* Avoid ConcurrentModificationException in AbstractDataLoaderService.unsubscribeInstanceGraphs
  by duplicating list before unsubscribing.

### [v0.5.35](https://github.com/replicant4j/replicant/tree/v0.5.35)

* Extract the handling of context management from AbstractReplicationInterceptor into
  ReplicationRequestManager and AbstractReplicationRequestManager.

### [v0.5.34](https://github.com/replicant4j/replicant/tree/v0.5.34)

* Fix concurrency bug triggered in AbstractDataLoaderService.updateSubscriptionForFilteredEntities
  when entities are removed from subscription.

### [v0.5.33](https://github.com/replicant4j/replicant/tree/v0.5.33)

* Add Runnable parameters to the connect and disconnect methods in WebPollerDataLoaderService.

### [v0.5.32](https://github.com/replicant4j/replicant/tree/v0.5.32)

* Add source channel to ChannelLink.

### [v0.5.31](https://github.com/replicant4j/replicant/tree/v0.5.31)

* Default ReplicantPollResource to 30 seconds long polling before making another request.

### [v0.5.30](https://github.com/replicant4j/replicant/tree/v0.5.30)

* Add support for debugging entity subscriptions and requests as well as local
  repository state.
* If repository debug output is enabled in application then print out a helper message
  when the GwtDataLoaderService is defined. Helps developers remember this feature is
  present.
* Associate a symbolic key with each session context. Prefix log messages using key and
  use it to restrict debugging to a particular GwtDataLoaderService subclass.

### [v0.5.29](https://github.com/replicant4j/replicant/tree/v0.5.29)

* Update WebPollerDataLoaderService to annotate the exception in handleSystemFailure as nullable.
* Make ReplicantRpcRequestBuilder a non final class.
* Update SessionContext to have a per-session base url.
* Remove reference to unused module (com.google.gwt.rpc.RPC) that is not present in GWT 2.7.

### [v0.5.28](https://github.com/replicant4j/replicant/tree/v0.5.28)

* Support the ability to debug just a single DataSourceLoader services
  changes.
* Add ReplicantDev.gwt.xml configuration that turns on all debug features.
* Exclude test classes form GWT compiler's path.

### [v0.5.27](https://github.com/replicant4j/replicant/tree/v0.5.27)

* Introduce AbstractClientTest to help write client-side tests.

### [v0.5.26](https://github.com/replicant4j/replicant/tree/v0.5.26)

* Remove the usage of a Synchronized map and replace with ConcurrentHashMap in
  ReplicantPollResource as the version if jersey in GlassFish 4.1.0 can result in deadlocks
  when timeouts are triggered.
* Remove final qualifier for method in ReplicantPollResource as CDI attempts to intercept
  method in GlassFish 4.1.0.
* Refactor SessionContext is not static, thus allowing multiple contexts within a single
  application.

### [v0.5.25](https://github.com/replicant4j/replicant/tree/v0.5.25)

* Introduce ReplicantJsonSessionManager as all downstream projects use json as their
  transport layer.
* Rename ReplicantSessionManager.poll() to pollPacket to make it easier to sub-class.
* Backport, test and generalize ReplicantPollResource from downstream libraries. This class
  makes it easy to setup polling for replicant based systems. Derive the default poll
  url in the WebPollerDataLoaderService assuming the ReplicantPollResource implementation.

### [v0.5.24](https://github.com/replicant4j/replicant/tree/v0.5.24)

* Extract utility method AbstractDataLoaderService.unsubscribeInstanceGraphs().
* Add close handler in WebPollerDataLoaderService that disconnects WebPoller
  when the windows closes.
* Correct nullability annotation for filterParameter in
  AbstractDataLoaderService.requestUpdateSubscription().

### [v0.5.23](https://github.com/replicant4j/replicant/tree/v0.5.23)

* Implement WebPollerDataLoaderService to simplify construction of polling based data loaders.
* Implement purging of subscriptions when the session changes.

### [v0.5.22](https://github.com/replicant4j/replicant/tree/v0.5.22)

* Move to EE7.
* Fix bug in ChangeAccumulator where change initiator can be incorrectly identified
  as having been routed to if a changeset was was accessed via getChangeSet()
  but no message was ever added to ChangeSet.

### [v0.5.21](https://github.com/replicant4j/replicant/tree/v0.5.21)

* Remove BadSessionException and associated ensureSession so domgen can generate
  a customized implementation.

### [v0.5.20](https://github.com/replicant4j/replicant/tree/v0.5.20)

* Move the responsibility for validating the entities in the EntityRepository to
  the EntitySubscriptionValidator.

### [v0.5.19](https://github.com/replicant4j/replicant/tree/v0.5.19)

* Move the responsibility for deleting the entities from the EntityRepository from
  the EntitySubscriptionManager to the DataLoaderService.

### [v0.5.18](https://github.com/replicant4j/replicant/tree/v0.5.18)

* Initial work to add debugging capability to the EntityRepository via a helper
  class EntityRepositoryDebugger.
* Extract AbstractDataLoaderService.updateSubscriptionForFilteredEntities so that
  subclasses can control the order in which types are unloaded due to a change in
  filter.
* Add EntityRepository.findAllIDs method to get ids for entities.

### [v0.5.17](https://github.com/replicant4j/replicant/tree/v0.5.17)

* Support the replication of the filter between the server and client.
* Update the client to unregister entities that are filtered after a channels
  filter is updated.
* Start to record filter on EntitySubscriptionManager on the client-side.
* Move to GWT 2.6.1.
* Move to Java 7.
* Add toString to ChannelLink and ChannelDescriptor to ease debugging.
* Correct the namespace in ReplicantConfig so that the property is read correctly.
* Pass ReplicantConfig through the constructor to make it easier to test.

### [v0.5.16](https://github.com/replicant4j/replicant/tree/v0.5.16)

* Associated with each change, the channel(s)  (a.k.a. subscription(s)) which resulted in
  the change replicating to the client.
* Replicate record of subscription changes to the client.

NOTE: This is a large change and further details are in the source control system.

### [v0.5.15](https://github.com/replicant4j/replicant/tree/v0.5.15)

* Rework ChangeRecorder to make sub-classing easier.
* Rename SubscriptionEntry.subscriptionData to filterParameter to match domgen conventions.
* Use constructor based injection for DataLoaders.
* Extract a separate queue of actions to control subscription in the DataLoaders.

### [v0.5.14](https://github.com/replicant4j/replicant/tree/v0.5.14)

* Associate a key with each request that corresponds to the operation being performed.
* Merge RequestManager and AbstractSessionManager into ClientSession.
* Add dependency on gwt-property-source library so that the choice on whether to
  validate repository after loads is controlled using a compile time configuration
  property.

### [v0.5.13](https://github.com/replicant4j/replicant/tree/v0.5.13)

* Add support for Verifiable interface for entities that can validate their own state.
* Support registration of interfaces in EntityRepository.
* Add support for detecting whether an entity has been lined via Linkable.isLinked().
* Reconfigure the Linkable interface so that implementations no longer implement
  delink and instead rely on the repository invoking invalidate.
* Rework ChangeRecorder to make it easier to sub-class and customize behaviour.

### [v0.5.12](https://github.com/replicant4j/replicant/tree/v0.5.12)

* Mark the EntityMessageSet class as final.
* Add EntityMessageSet.containsEntityMessage(...) to test whether the set
  contains a message.

### [v0.5.11](https://github.com/replicant4j/replicant/tree/v0.5.11)

* Add support for recording arbitrary data in SubscriptionEntry.
* Add AbstractSubscriptionManager.find(Instance|Type)GraphSubscription methods.

### [v0.5.10](https://github.com/replicant4j/replicant/tree/v0.5.10)

* Restore compatibility with JDK 6.

### [v0.5.9](https://github.com/replicant4j/replicant/tree/v0.5.9)

* Support encoding of Longs as strings in change sets.

### [v0.5.8](https://github.com/replicant4j/replicant/tree/v0.5.8)

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

### [v0.5.7](https://github.com/replicant4j/replicant/tree/v0.5.7)

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

### [v0.5.6](https://github.com/replicant4j/replicant/tree/v0.5.6)

* In the DataLoaderService, do not execute runnable unless the RequestEntry has
  been successfully processed.
* In the DataLoaderService, remove the RequestEntry if it has been successfully
  processed.
* Support recording on the RequestEntry that the results have arrived to counter
  the race scenario where the change set arrives prior to the the request
  returning.

### [v0.5.5](https://github.com/replicant4j/replicant/tree/v0.5.5)

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

### [v0.5.4](https://github.com/replicant4j/replicant/tree/v0.5.4)

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

### [v0.5.3](https://github.com/replicant4j/replicant/tree/v0.5.3)

* Move EntityMessageAccumulator to the transport package and re-target it to
  deliver messages to the PacketQueue.
* Introduce the Packet and PacketQueue to support creating the transport layer.
* Add Gin module that defines EntityMessageBroker and EntityRepository instances.
* Update EntityChangeListener to add a callback method that is called when
  and entity is added to the system.
* Make EntityChangeBrokerImpl.shouldRaiseErrorOnEventHandlerError() final.

### [v0.5.2](https://github.com/replicant4j/replicant/tree/v0.5.2)

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

### [v0.5.1](https://github.com/replicant4j/replicant/tree/v0.5.1)

* Include source in jar file to make it easier to integrate with GWT.

### [v0.5](https://github.com/replicant4j/replicant/tree/v0.5)

* Remove the Async*Callback interfaces now that they are generated by Domgen.
* Move the RDate, Date*Serializer and Date*Deserializer classes to gwt-datatypes
  library.
* Move the org.realityforge.replicant.client.json.gwt.JsoReadOnly* collections
  to org.realityforge.gwt.datatypes.client.collections in the gwt-datatypes package.
* Add GwtDataLoaderService to simplify creating a DataLoader in GWT.
* Add template method AbstractDataLoaderService.shouldValidateOnLoad() that will
  validate the entity repository on each change. Useful to override and return true
  during development or in debug mode.

### [v0.4.8](https://github.com/replicant4j/replicant/tree/v0.4.8)

* Change AbstractReplicationInterceptor so that subclasses must override a template
  method to provide the EntityManager. The purpose of this change is to allow for
  the use of this library in applications that have multiple persistence contexts.
