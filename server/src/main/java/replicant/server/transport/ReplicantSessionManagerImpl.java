package replicant.server.transport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelLink;
import replicant.server.DatasetAddress;
import replicant.server.EntityMessage;
import replicant.server.FilterUtil;
import replicant.server.ServerConstants;
import replicant.server.json.JsonEncoder;
import replicant.server.runtime.EntityMessageCacheUtil;
import replicant.server.runtime.ReplicantContextHolder;
import replicant.server.runtime.ReplicantSystem;

@SuppressWarnings("DuplicatedCode")
@ApplicationScoped
@Transactional
@Typed(ReplicantSessionManager.class)
public class ReplicantSessionManagerImpl implements ReplicantSessionManager {
    @NonNull
    private static final Logger LOG = Logger.getLogger(ReplicantSessionManagerImpl.class.getName());

    @NonNull
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();

    @NonNull
    private final Map<String, ReplicantSession> _sessions = new HashMap<>();

    @NonNull
    private final ReadWriteLock _cacheLock = new ReentrantReadWriteLock();

    @NonNull
    private final Map<DatasetAddress, DatasetCacheEntry> _cache = new HashMap<>();

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private ReplicantSessionContext _context;

    @Inject
    @ReplicantSystem
    private TransactionSynchronizationRegistry _registry;

    @Inject
    private ReplicantMessageBroker _broker;

    @Inject
    @ReplicantSystem("ScheduledExecutorService")
    private ScheduledExecutorService _scheduledExecutorService;

    @Nullable
    private ScheduledFuture<?> _removeClosedSessionsFuture;

    @Nullable
    private ScheduledFuture<?> _pingSessionsFuture;

    @PostConstruct
    void postConstruct() {
        _removeClosedSessionsFuture =
                _scheduledExecutorService.scheduleAtFixedRate(this::removeClosedSessions, 2, 1, TimeUnit.MINUTES);
        _pingSessionsFuture = _scheduledExecutorService.scheduleAtFixedRate(this::pingSessions, 2, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    void preDestroy() {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(
                    Level.INFO,
                    "event=session.manager.stop sessionCount=" + getSessions().size());
        }
        if (null != _removeClosedSessionsFuture) {
            _removeClosedSessionsFuture.cancel(true);
            _removeClosedSessionsFuture = null;
        }
        if (null != _pingSessionsFuture) {
            _pingSessionsFuture.cancel(true);
            _pingSessionsFuture = null;
        }
        removeAllSessions();
    }

    @Override
    public <T> T runRequest(
            @NonNull final String invocationKey,
            @Nullable final ReplicantSession session,
            @Nullable final Integer requestId,
            @NonNull final Callable<T> action)
            throws Exception {
        startReplication(invocationKey, session, requestId);
        try {
            return action.call();
        } finally {
            completeReplication(invocationKey);
        }
    }

    private void sessionLockingRequest(
            @NonNull final String invocationKey,
            @NonNull final ReplicantSession session,
            @Nullable final Integer requestId,
            @NonNull final Runnable action) {
        final var lock = session.getLock();
        try {
            lock.lockInterruptibly();
            startReplication(invocationKey, session, requestId);
            try {
                action.run();
            } finally {
                completeReplication(invocationKey);
            }
        } catch (final InterruptedException ie) {
            session.closeDueToInterrupt();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isAuthorized(@NonNull final ReplicantSession session) {
        return _context.isAuthorized(session);
    }

    @Override
    public void execCommand(
            @NonNull final ReplicantSession session,
            @NonNull final String command,
            final int requestId,
            @Nullable final JsonObject payload) {
        _context.execCommand(session, command, requestId, payload);
    }

    private void sessionUpdateRequest(
            @NonNull final String invocationKey,
            @NonNull final ReplicantSession session,
            final int requestId,
            @NonNull final Runnable action) {
        sessionLockingRequest(invocationKey, session, requestId, () -> {
            _registry.putResource(ServerConstants.SUBSCRIPTION_REQUEST_KEY, "1");
            action.run();
        });
    }

    /**
     * Start a replication context.
     *
     * @param invocationKey the identifier of the element that is initiating replication. (i.e. Method name).
     * @param session       the session that initiated change if any.
     * @param requestId     the id of the request in the session that initiated change..
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void startReplication(
            @NonNull final String invocationKey,
            @Nullable final ReplicantSession session,
            @Nullable final Integer requestId) {
        // Clear the context completely, in case the caller is not a GwtRpcServlet or does not reset the state.
        final var existingKey = _registry.getResource(ServerConstants.REPLICATION_INVOCATION_KEY);
        if (null != existingKey) {
            final var message = "Attempted to invoke service method '" + invocationKey
                    + "' while there is an active replication '" + existingKey + "'";
            throw new IllegalStateException(message);
        }

        _registry.putResource(ServerConstants.REPLICATION_INVOCATION_KEY, invocationKey);
        if (null != session) {
            _registry.putResource(ServerConstants.SESSION_ID_KEY, session.getId());
        } else {
            _registry.putResource(ServerConstants.SESSION_ID_KEY, null);
        }
        _registry.putResource(ServerConstants.REQUEST_ID_KEY, requestId);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Starting invocation of " + invocationKey + " Thread: "
                    + Thread.currentThread().getId());
        }
    }

    /**
     * Complete a replication context and submit changes for replication.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private void completeReplication(@NonNull final String invocationKey) {
        if (Status.STATUS_ACTIVE == _registry.getTransactionStatus()
                && !_registry.getRollbackOnly()
                && _context.flushOpenEntityManager()) {
            final var sessionId = (String) _registry.getResource(ServerConstants.SESSION_ID_KEY);
            final var requestId = (Integer) _registry.getResource(ServerConstants.REQUEST_ID_KEY);
            final var response = (JsonValue) _registry.getResource(ServerConstants.REQUEST_RESPONSE_KEY);
            var requestComplete = true;
            final var messageSet = EntityMessageCacheUtil.removeEntityMessageSet(_registry);
            final var changeSet = EntityMessageCacheUtil.removeSessionChanges(_registry);
            if (null != messageSet || null != changeSet || null != requestId) {
                final var messages =
                        null == messageSet ? Collections.<EntityMessage>emptySet() : messageSet.getEntityMessages();
                if (null != changeSet || !messages.isEmpty() || null != requestId) {
                    requestComplete = !saveEntityMessages(sessionId, requestId, response, messages, changeSet);
                }
            }
            final var complete = (String) _registry.getResource(ServerConstants.REQUEST_COMPLETE_KEY);
            // Clear all state in case there is multiple replication contexts started in one transaction
            _registry.putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);
            _registry.putResource(ServerConstants.SESSION_ID_KEY, null);
            _registry.putResource(ServerConstants.REQUEST_ID_KEY, null);
            _registry.putResource(ServerConstants.REQUEST_COMPLETE_KEY, null);
            _registry.putResource(ServerConstants.REQUEST_RESPONSE_KEY, null);
            _registry.putResource(ServerConstants.CACHED_RESULT_HANDLED_KEY, null);
            _registry.putResource(ServerConstants.SUBSCRIPTION_REQUEST_KEY, null);

            final var isComplete = !(null != complete && !"1".equals(complete)) && requestComplete;
            ReplicantContextHolder.put(ServerConstants.REQUEST_COMPLETE_KEY, isComplete ? "1" : "0");
            ReplicantContextHolder.put(ServerConstants.REQUEST_RESPONSE_KEY, response);
        } else {
            ReplicantContextHolder.put(ServerConstants.REQUEST_COMPLETE_KEY, "1");
            ReplicantContextHolder.put(ServerConstants.REQUEST_RESPONSE_KEY, null);
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Completed invocation of " + invocationKey + " Thread: "
                    + Thread.currentThread().getId());
        }
    }

    @NonNull
    @Override
    public SchemaMetaData getSchemaMetaData() {
        return _context.getSchemaMetaData();
    }

    @SuppressWarnings("resource")
    @Override
    public void invalidateSession(@NonNull final ReplicantSession session) {
        var removed = false;
        _lock.writeLock().lock();
        try {
            if (null != _sessions.remove(session.getId())) {
                removed = true;
                session.close();
            }
        } finally {
            _lock.writeLock().unlock();
        }
        if (LOG.isLoggable(removed ? Level.INFO : Level.FINE)) {
            LOG.log(
                    removed ? Level.INFO : Level.FINE,
                    "event=session.invalidate sessionId=" + session.getId() + " removed="
                            + removed + " sessionCount="
                            + getSessions().size());
        }
    }

    @Override
    @Nullable
    public ReplicantSession getSession(@NonNull final String sessionId) {
        _lock.readLock().lock();
        try {
            return _sessions.get(sessionId);
        } finally {
            _lock.readLock().unlock();
        }
    }

    @NonNull
    Set<ReplicantSession> getSessions() {
        _lock.readLock().lock();
        try {
            return new HashSet<>(_sessions.values());
        } finally {
            _lock.readLock().unlock();
        }
    }

    @Override
    @NonNull
    public ReplicantSession createSession(
            @NonNull final Session webSocketSession, @NonNull final ReplicantSessionAuthorization authorization) {
        final var session = new ReplicantSession(webSocketSession, authorization);
        var sessionCount = 0;
        _lock.writeLock().lock();
        try {
            _sessions.put(session.getId(), session);
            sessionCount = _sessions.size();
        } finally {
            _lock.writeLock().unlock();
        }
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(
                    Level.INFO,
                    "event=session.create sessionId=" + session.getId() + " webSocketSessionId="
                            + webSocketSession.getId() + " sessionCount="
                            + sessionCount);
        }
        return session;
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    public void pingSessions() {
        if (_lock.readLock().tryLock()) {
            try {
                for (final var session : _sessions.values()) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("Pinging websocket for session " + session.getId());
                    }
                    session.pingTransport();
                }
            } finally {
                _lock.readLock().unlock();
            }
        }
    }

    /**
     * Remove all sessions and force them to reconnect.
     */
    @SuppressWarnings("WeakerAccess")
    public void removeAllSessions() {
        var removedCount = 0;
        if (_lock.writeLock().tryLock()) {
            try {
                removedCount = _sessions.size();
                new ArrayList<>(_sessions.values()).forEach(ReplicantSession::close);
                _sessions.clear();
            } finally {
                _lock.writeLock().unlock();
            }
        }
        if (LOG.isLoggable(Level.INFO) && removedCount > 0) {
            LOG.log(Level.INFO, "event=session.removeAll removedCount=" + removedCount + " sessionCount=0");
        }
    }

    /**
     * Remove sessions that are associated with a closed WebSocket.
     */
    @SuppressWarnings("WeakerAccess")
    public void removeClosedSessions() {
        var removedCount = 0;
        var sessionCount = 0;
        if (_lock.writeLock().tryLock()) {
            try {
                final var iterator = _sessions.entrySet().iterator();
                while (iterator.hasNext()) {
                    final var session = iterator.next().getValue();
                    if (!session.getWebSocketSession().isOpen()) {
                        iterator.remove();
                        removedCount++;
                    }
                }
                sessionCount = _sessions.size();
            } finally {
                _lock.writeLock().unlock();
            }
        }
        if (LOG.isLoggable(Level.FINE) && removedCount > 0) {
            LOG.log(
                    Level.FINE,
                    "event=session.removeClosed removedCount=" + removedCount + " sessionCount=" + sessionCount);
        }
    }

    /**
     * Send message to the specified session in response to a cacheable Dataset Subscription request.
     * The requesting service must NOT have made any other changes that will be sent to the
     * client, otherwise this message will be discarded.
     * This can also be sent if the cache request resulted in deleted dataset in which case the eTag will be null.
     *
     * @param session   the session.
     * @param changeSet the messages to be sent along to the client.
     */
    private void queueCachedChangeSet(@NonNull final ReplicantSession session, @NonNull final ChangeSet changeSet) {
        final var requestId = (Integer) _registry.getResource(ServerConstants.REQUEST_ID_KEY);
        _registry.putResource(ServerConstants.REQUEST_COMPLETE_KEY, "0");
        _registry.putResource(ServerConstants.CACHED_RESULT_HANDLED_KEY, "1");
        _broker.queueChangeMessage(
                session, true, requestId, null, changeSet.getETag(), Collections.emptyList(), changeSet);
    }

    private boolean saveEntityMessages(
            @Nullable final String sessionId,
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @NonNull final Collection<EntityMessage> messages,
            @Nullable final ChangeSet sessionChanges) {
        var impactsInitiator = false;

        // Make sure if the message relates to an existing cache message then the cache is busted
        for (final var message : messages) {
            processCachePurge(message);
        }

        // TODO: Rewrite this so that we add clients to indexes rather than searching through everyone for each change!
        for (final var session : getSessions()) {
            final var isInitiator = Objects.equals(session.getId(), sessionId);
            if (isInitiator) {
                // The initiator has been impacted, even if the underlying session has been closed
                // so bring this logic outside of the session.isOpen() guard.
                impactsInitiator = true;
            }
            if (session.isOpen()) {
                final var changeSet = new ChangeSet();
                if (isInitiator) {
                    if (null != sessionChanges) {
                        changeSet.setRequired(sessionChanges.isRequired());
                        changeSet.merge(sessionChanges.getChanges());
                        changeSet.mergeActions(sessionChanges.getChannelActions());
                    }

                    /*
                     * We mark this as required and as impacting the initiator because we no longer know whether the
                     * action did result in a message that needs to be sent to the client as routing occurs in a separate
                     * thread. This change here now means every rpc will be paired with a replicant message even if it
                     * is an empty ok message. This is acceptable in the short term as we expect to remove external rpc
                     * at a later stage and move all rpc onto replicant channel.
                     */
                    if (null == _registry.getResource(ServerConstants.CACHED_RESULT_HANDLED_KEY)) {
                        // We skip scenario when we have already sent a cached result
                        changeSet.setRequired(true);
                    }
                }
                final var altersExplicitSubscriptions =
                        null != _registry.getResource(ServerConstants.SUBSCRIPTION_REQUEST_KEY);
                _broker.queueChangeMessage(
                        session,
                        altersExplicitSubscriptions,
                        isInitiator ? requestId : null,
                        isInitiator ? response : null,
                        null,
                        messages,
                        changeSet);
            }
        }

        return impactsInitiator;
    }

    @Override
    public boolean sendChangeMessage(@NonNull final ReplicantSession session, @NonNull final Packet packet) {
        final var sent = new AtomicBoolean();
        try {
            return session.runIfValid(() -> sent.set(sendAuthorizedChangeMessage(session, packet))) && sent.get();
        } catch (final java.io.IOException e) {
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Authorization gate failed"));
            return false;
        }
    }

    private boolean sendAuthorizedChangeMessage(@NonNull final ReplicantSession session, @NonNull final Packet packet) {
        final var incomingEntityCount =
                packet.messages().size() + packet.changeSet().getChanges().size();
        final var incomingChannelLinks = packet.messages().stream()
                        .map(EntityMessage::getLinks)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .distinct()
                        .count()
                + packet.changeSet().getChanges().stream()
                        .map(change -> change.getEntityMessage().getLinks())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .distinct()
                        .count();

        _context.preSendChangeMessage(session, packet);

        final var requestId = packet.requestId();
        final var response = packet.response();
        final var etag = packet.etag();
        final var messages = packet.messages();
        final var changeSet = packet.changeSet();

        assert null == response || null != requestId;
        if (!session.isOpen()) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(
                        Level.FINE,
                        "event=session.change.skip reason=sessionClosed sessionId=" + session.getId() + " requestId="
                                + requestId + " incomingEntityCount="
                                + incomingEntityCount + " incomingChannelLinkCount="
                                + incomingChannelLinks + " altersExplicitSubscriptions="
                                + packet.altersExplicitSubscriptions());
            }
            return false;
        }
        final var hasDeletes = messages.stream().anyMatch(EntityMessage::isDelete);
        final var rootDeletedDatasetAddresses =
                hasDeletes ? collectRootDeletedEntries(messages, session) : Collections.<DatasetAddress>emptySet();
        if (hasDeletes) {
            preserveOwnedChannelLinksBeforeDelete(messages, session, changeSet, rootDeletedDatasetAddresses);
        }
        processMessages(messages, session, changeSet);

        // ChangeSets that occur during a subscription that result in a use-cache message
        // being sent to the client will still come through here. The hasContent() should
        // return false as there are no changes for in ChangeSet and the _required flag is unset.
        if (changeSet.hasContent()) {
            final var start = System.nanoTime();

            final var expandCycleCount = completeMessageProcessing(session, changeSet, rootDeletedDatasetAddresses);
            final var end = System.nanoTime();
            final var expansionDuration = (end - start) / 1000000;

            // This log level should be fine but leaving it here as INFO to make it easy to assess current production
            // issues.
            final var level = expansionDuration > 1000 ? Level.SEVERE : Level.INFO;
            if (LOG.isLoggable(level)) {
                final var outgoingEntityCount = changeSet.getChanges().size();
                final var outgoingChannelLinks = changeSet.getChanges().stream()
                        .map(change -> change.getEntityMessage().getLinks())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .distinct()
                        .count();
                final var actions = changeSet.getChannelActions().stream()
                        .map(JsonEncoder::toDescriptor)
                        .toList();
                LOG.log(
                        level,
                        "event=session.change.send sessionId=" + session.getId() + " requestId="
                                + requestId + " etag="
                                + etag + " altersExplicitSubscriptions="
                                + packet.altersExplicitSubscriptions() + " incomingEntityCount="
                                + incomingEntityCount + " incomingChannelLinkCount="
                                + incomingChannelLinks + " outgoingEntityCount="
                                + outgoingEntityCount + " outgoingChannelLinkCount="
                                + outgoingChannelLinks + " expandCycleCount="
                                + expandCycleCount + " expandTimeMs="
                                + expansionDuration + " channelActions="
                                + actions);
            }
            session.sendPacket(requestId, response, etag, changeSet);
            return true;
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(
                        Level.FINE,
                        "event=session.change.skip reason=noContent sessionId=" + session.getId() + " requestId="
                                + requestId + " etag="
                                + etag + " altersExplicitSubscriptions="
                                + packet.altersExplicitSubscriptions() + " incomingEntityCount="
                                + incomingEntityCount + " incomingChannelLinkCount="
                                + incomingChannelLinks + " messageCount="
                                + messages.size() + " changeCount="
                                + changeSet.getChanges().size() + " channelActionCount="
                                + changeSet.getChannelActions().size());
            }
            return false;
        }
    }

    private int completeMessageProcessing(
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<DatasetAddress> rootDeletedDatasetAddresses) {
        var expandCycleCount = 0;
        try {
            final var pending = new HashSet<ChannelLinkEntry>();

            while (true) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(
                            Level.FINE,
                            "event=session.change.send.expand sessionId=" + session.getId()
                                    + " cycle="
                                    + expandCycleCount
                                    + " changes="
                                    + changeSet.getChanges().size()
                                    + " channelActions="
                                    + changeSet.getChannelActions().stream()
                                            .map(JsonEncoder::toDescriptor)
                                            .toList()
                                    + " pending="
                                    + pending.stream()
                                            .map(e -> e.targetDatasetAddress().toString())
                                            .toList());
                }
                expandCycleCount++;
                collectChannelLinksToFollow(session, changeSet, pending, rootDeletedDatasetAddresses);
                if (pending.isEmpty()) {
                    break;
                }
                final var entry = pending.stream()
                        .min(Comparator.comparing(ChannelLinkEntry::targetDatasetAddress))
                        .orElseThrow();
                final var targetDatasetAddress = entry.targetDatasetAddress();
                final var toSubscribe = targetDatasetAddress.hasDatasetRootId()
                        ? pending.stream()
                                .filter(a -> a.targetDatasetAddress().datasetId() == targetDatasetAddress.datasetId()
                                        && Objects.equals(a.filter(), entry.filter()))
                                .toList()
                        : Collections.singletonList(entry);
                final var datasetAddresses = toSubscribe.stream()
                        .map(ChannelLinkEntry::targetDatasetAddress)
                        .toList();
                doSubscribe(session, datasetAddresses, entry.filter(), changeSet, false);
                toSubscribe.forEach(pending::remove);
                for (final var e : toSubscribe) {
                    final var sourceEntry = session.getSubscriptionEntry(e.sourceDatasetAddress());
                    final var targetEntry = session.getSubscriptionEntry(e.targetDatasetAddress());
                    InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceEntry.datasetAddress());
                    InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetEntry.datasetAddress());
                    session.recordGraphLink(sourceEntry, targetEntry, e.owner());
                }
            }
        } catch (final Exception e) {
            // This can occur when there is an error accessing the database
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Error invoking expandLinks for session " + session.getId(), e);
            }
            session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Expanding links failed"));
        }
        return expandCycleCount;
    }

    /**
     * Collect a list of ChannelLinks in change set that may need to be followed.
     */
    private void collectChannelLinksToFollow(
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<ChannelLinkEntry> targets,
            @NonNull final Set<DatasetAddress> rootDeletedDatasetAddresses) {
        for (final var change : changeSet.getChanges()) {
            final var entityMessage = change.getEntityMessage();
            if (entityMessage.isUpdate()) {
                final var owner = LinkOwner.entity(entityMessage.getTypeId(), entityMessage.getId());
                for (final var sourceDatasetAddress : change.getDatasetAddresses()) {
                    final var sourceEntry = session.findSubscriptionEntry(sourceDatasetAddress);
                    if (null != sourceEntry) {
                        final var desiredTargets = resolveDesiredChannelLinkTargets(entityMessage, sourceEntry);
                        desiredTargets.keySet().removeAll(rootDeletedDatasetAddresses);
                        reconcileOwnedChannelLinks(session, sourceEntry, owner, desiredTargets, changeSet, targets);
                    }
                }
            }
        }
    }

    private boolean matchesSourceDatasetAddress(
            @NonNull final DatasetAddress template, @NonNull final DatasetAddress datasetAddress) {
        if (template.partial()) {
            return template.datasetId() == datasetAddress.datasetId()
                    && Objects.equals(template.datasetRootId(), datasetAddress.datasetRootId());
        } else {
            return template.equals(datasetAddress);
        }
    }

    @NonNull
    private DatasetAddress resolveTargetDatasetAddress(
            @NonNull final EntityMessage entityMessage,
            @NonNull final DatasetAddress sourceDatasetAddress,
            @Nullable final JsonObject sourceFilter,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject targetFilter) {
        if (targetDatasetAddress.partial()) {
            assert entityMessage.isUpdate();
            final var datasetKey = _context.deriveTargetDatasetKey(
                    entityMessage, sourceDatasetAddress, sourceFilter, targetDatasetAddress, targetFilter);
            final var concreteTargetDatasetAddress = DatasetAddress.of(
                    targetDatasetAddress.datasetId(), targetDatasetAddress.datasetRootId(), datasetKey);
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), concreteTargetDatasetAddress);
            return concreteTargetDatasetAddress;
        } else {
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetDatasetAddress);
            return targetDatasetAddress;
        }
    }

    /**
     * Resolve the desired downstream targets for the source entry from the entity-owned links in the message.
     */
    @NonNull
    private Map<DatasetAddress, JsonObject> resolveDesiredChannelLinkTargets(
            @NonNull final EntityMessage entityMessage, @NonNull final SubscriptionEntry sourceEntry) {
        final var desiredTargets = new LinkedHashMap<DatasetAddress, JsonObject>();
        final var links = entityMessage.getLinks();
        if (null != links) {
            for (final var link : links) {
                InvariantUtil.assertLink(getSchemaMetaData(), link);
                if (matchesSourceDatasetAddress(link.sourceDatasetAddress(), sourceEntry.datasetAddress())) {
                    final var resolved = resolveChannelLinkIfRequired(entityMessage, sourceEntry, link);
                    if (null != resolved) {
                        final var existing =
                                desiredTargets.putIfAbsent(resolved.targetDatasetAddress(), resolved.filter());
                        assert null == existing || Objects.equals(existing, resolved.filter());
                    }
                }
            }
        }
        return desiredTargets;
    }

    private void reconcileOwnedChannelLinks(
            @NonNull final ReplicantSession session,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final LinkOwner owner,
            @NonNull final Map<DatasetAddress, JsonObject> desiredTargets,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<ChannelLinkEntry> targets) {
        final var existingTargets = new HashSet<>(sourceEntry.getOwnedOutwardSubscriptions(owner));
        for (final var existingTarget : existingTargets) {
            if (!desiredTargets.containsKey(existingTarget)) {
                session.delinkDownstreamSubscription(sourceEntry, owner, existingTarget, changeSet);
            }
        }

        for (final var entry : desiredTargets.entrySet()) {
            final var pending =
                    createOrUpdateChannelLinkEntry(session, owner, sourceEntry, entry.getKey(), entry.getValue());
            if (null != pending) {
                targets.add(pending);
            }
        }
    }

    @Nullable
    private ResolvedChannelLink resolveChannelLinkIfRequired(
            @NonNull final EntityMessage entityMessage,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final ChannelLink link) {
        final var sourceDatasetAddress = sourceEntry.datasetAddress();
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceDatasetAddress);
        final var sourceFilter = sourceEntry.getFilter();
        final var targetDatasetAddress = resolveTargetDatasetAddress(
                entityMessage, sourceDatasetAddress, sourceFilter, link.targetDatasetAddress(), link.targetFilter());
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetDatasetAddress);
        final var dataset = getSchemaMetaData().getDatasetMetadata(targetDatasetAddress);
        if (dataset.requiresFilterParameter()) {
            final var filter = link.hasTargetFilter()
                    ? link.targetFilter()
                    : _context.deriveTargetFilter(
                            entityMessage, sourceDatasetAddress, sourceFilter, targetDatasetAddress);
            return _context.shouldFollowLink(sourceDatasetAddress, sourceFilter, targetDatasetAddress, filter)
                    ? new ResolvedChannelLink(targetDatasetAddress, filter)
                    : null;
        } else {
            return new ResolvedChannelLink(targetDatasetAddress, null);
        }
    }

    @Nullable
    private ChannelLinkEntry createOrUpdateChannelLinkEntry(
            @NonNull final ReplicantSession session,
            @NonNull final LinkOwner owner,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject filter) {
        final var targetEntry = session.findSubscriptionEntry(targetDatasetAddress);
        if (null == targetEntry) {
            return new ChannelLinkEntry(owner, sourceEntry.datasetAddress(), targetDatasetAddress, filter);
        } else {
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceEntry.datasetAddress());
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetEntry.datasetAddress());
            session.recordGraphLink(sourceEntry, targetEntry, owner);
            targetEntry.setFilter(filter);
            return null;
        }
    }

    private void processMessages(
            @NonNull final Collection<EntityMessage> messages,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet) {
        for (final var message : messages) {
            processDeleteMessages(message, session, changeSet);
        }

        for (final var message : messages) {
            processUpdateMessages(message, session, changeSet);
        }
    }

    private void preserveOwnedChannelLinksBeforeDelete(
            @NonNull final Collection<EntityMessage> messages,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<DatasetAddress> rootDeletedDatasetAddresses) {
        for (final var message : messages) {
            if (message.isUpdate()) {
                preserveOwnedChannelLinksFromPacketMessage(message, session, rootDeletedDatasetAddresses);
            }
        }
        for (final var change : changeSet.getChanges()) {
            final var message = change.getEntityMessage();
            if (message.isUpdate()) {
                final var owner = LinkOwner.entity(message.getTypeId(), message.getId());
                for (final var sourceDatasetAddress : change.getDatasetAddresses()) {
                    final var sourceEntry = session.findSubscriptionEntry(sourceDatasetAddress);
                    if (null != sourceEntry) {
                        preserveOwnedChannelLinksForSourceEntry(
                                message, session, sourceEntry, owner, rootDeletedDatasetAddresses);
                    }
                }
            }
        }
    }

    @NonNull
    private Set<DatasetAddress> collectRootDeletedEntries(
            @NonNull final Collection<EntityMessage> messages, @NonNull final ReplicantSession session) {
        final var rootDeletedDatasetAddresses = new HashSet<DatasetAddress>();
        final var schema = getSchemaMetaData();
        final var instanceDatasetCount = schema.getInstanceDatasetCount();
        for (final var message : messages) {
            if (message.isDelete()) {
                for (var i = 0; i < instanceDatasetCount; i++) {
                    final var dataset = schema.getInstanceDatasetByIndex(i);
                    @SuppressWarnings("unchecked")
                    final var datasetRootIds =
                            (List<Integer>) message.getRoutingKeys().get(dataset.getName());
                    if (null != datasetRootIds) {
                        for (final var datasetRootId : datasetRootIds) {
                            final var datasetAddress = DatasetAddress.of(dataset.getDatasetId(), datasetRootId);
                            final var isFiltered = DatasetMetadata.FilterType.NONE != dataset.filterType();
                            for (final var entry : session.findSubscriptionEntries(
                                    datasetAddress.datasetId(), datasetAddress.datasetRootId())) {
                                final var entryDatasetAddress = entry.datasetAddress();
                                final var m = isFiltered
                                        ? _context.filterEntityMessage(session, entryDatasetAddress, message)
                                        : message;
                                if (null != m && m.isDelete()) {
                                    if (isEntityMessageChannelRoot(entry, datasetAddress, m)) {
                                        rootDeletedDatasetAddresses.add(entryDatasetAddress);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return rootDeletedDatasetAddresses;
    }

    private void preserveOwnedChannelLinksFromPacketMessage(
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final Set<DatasetAddress> rootDeletedDatasetAddresses) {
        final var schema = getSchemaMetaData();
        final var datasetCount = schema.getDatasetCount();
        final var owner = LinkOwner.entity(message.getTypeId(), message.getId());
        for (var i = 0; i < datasetCount; i++) {
            if (schema.hasDatasetMetadata(i)) {
                final var dataset = schema.getDatasetMetadata(i);
                final var datasetAddresses = extractDatasetAddressesFromMessage(dataset, message);
                if (null != datasetAddresses) {
                    final var isFiltered = DatasetMetadata.FilterType.NONE != dataset.filterType();
                    for (final var datasetAddress : datasetAddresses) {
                        for (final var entry : session.findSubscriptionEntries(
                                datasetAddress.datasetId(), datasetAddress.datasetRootId())) {
                            final var entryDatasetAddress = entry.datasetAddress();
                            final var m = isFiltered
                                    ? _context.filterEntityMessage(session, entryDatasetAddress, message)
                                    : message;
                            if (null != m) {
                                preserveOwnedChannelLinksForSourceEntry(
                                        message, session, entry, owner, rootDeletedDatasetAddresses);
                            }
                        }
                    }
                }
            }
        }
    }

    private void preserveOwnedChannelLinksForSourceEntry(
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final LinkOwner owner,
            @NonNull final Set<DatasetAddress> rootDeletedDatasetAddresses) {
        if (!rootDeletedDatasetAddresses.contains(sourceEntry.datasetAddress())) {
            final var desiredTargets = resolveDesiredChannelLinkTargets(message, sourceEntry);
            for (final var entry : desiredTargets.entrySet()) {
                final var targetEntry = session.findSubscriptionEntry(entry.getKey());
                if (null != targetEntry) {
                    // An update can point at a graph whose root is deleted by the same packet; DELETE semantics must
                    // win.
                    if (!rootDeletedDatasetAddresses.contains(targetEntry.datasetAddress())
                            && FilterUtil.filtersEqual(entry.getValue(), targetEntry.getFilter())) {
                        session.recordGraphLink(sourceEntry, targetEntry, owner);
                    }
                }
            }
        }
    }

    @Override
    public void subscribe(
            @NonNull final ReplicantSession session,
            final int requestId,
            @NonNull final List<DatasetAddress> datasetAddresses,
            @Nullable final JsonObject filter) {
        if (InvariantUtil.isInvariantCheckingEnabled()) {
            datasetAddresses.forEach(
                    datasetAddress -> InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress));
        }

        final var key = "Subscribe("
                + (datasetAddresses.isEmpty()
                        ? "empty"
                        : datasetAddresses.get(0).datasetId()) + ")";
        sessionUpdateRequest(key, session, requestId, () -> {
            if (session.isOpen()) {
                final var sessionChanges = EntityMessageCacheUtil.getSessionChanges();
                sessionChanges.setRequired(true);
                datasetAddresses.forEach(datasetAddress -> _context.preSubscribe(session, datasetAddress, filter));
                doSubscribe(session, datasetAddresses, filter, sessionChanges, true);
            }
        });
    }

    private void doSubscribe(
            @NonNull final ReplicantSession session,
            @NonNull final List<DatasetAddress> datasetAddresses,
            @Nullable final JsonObject filter,
            @NonNull final ChangeSet changeSet,
            final boolean isExplicitSubscribe) {
        final var uniqueDatasetAddresses = datasetAddresses.stream().distinct().toList();
        if (uniqueDatasetAddresses.isEmpty()) {
            return;
        }
        if (InvariantUtil.isInvariantCheckingEnabled()) {
            uniqueDatasetAddresses.forEach(
                    datasetAddress -> InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress));
        }
        final var datasetId = uniqueDatasetAddresses.get(0).datasetId();
        final var dataset = getSchemaMetaData().getDatasetMetadata(datasetId);

        subscribeToRequiredTypeChannels(session, dataset);

        final var newDatasetAddresses = new ArrayList<DatasetAddress>();
        // OriginalFilter => Channels
        final var datasetAddressesToUpdate = new HashMap<JsonObject, List<DatasetAddress>>();

        for (final var datasetAddress : uniqueDatasetAddresses) {
            assert datasetAddress.datasetId() == datasetId;
            if (dataset.isTypeGraph()) {
                assert !datasetAddress.hasDatasetRootId();
            } else {
                assert datasetAddress.hasDatasetRootId();
            }

            final var entry = session.findSubscriptionEntry(datasetAddress);
            if (null == entry) {
                newDatasetAddresses.add(datasetAddress);
            } else {
                final var existingFilter = entry.getFilter();
                if (!FilterUtil.filtersEqual(filter, existingFilter)) {
                    datasetAddressesToUpdate
                            .computeIfAbsent(existingFilter, k -> new ArrayList<>())
                            .add(datasetAddress);
                } else if (!entry.isExplicitlySubscribed() && isExplicitSubscribe) {
                    entry.setExplicitlySubscribed(true);
                }
            }
        }

        if (!newDatasetAddresses.isEmpty()) {
            if (dataset.isCacheable()) {
                // Only type graphs are cached atm, need to add extra plumbing to cache instance graphs
                assert dataset.isTypeGraph();
                // Only unfiltered graphs currently supported as cache targets, although static or internal
                // caching would be possible if we wanted to add support
                assert DatasetMetadata.FilterType.NONE == dataset.filterType();
                for (var newDatasetAddress : newDatasetAddresses) {
                    final var cacheEntry = tryGetCacheEntry(newDatasetAddress);
                    if (null != cacheEntry) {
                        final var eTag = cacheEntry.getCacheKey();
                        if (eTag.equals(session.getETag(newDatasetAddress))) {
                            if (session.getWebSocketSession().isOpen()) {
                                final var requestId = (Integer) _registry.getResource(ServerConstants.REQUEST_ID_KEY);
                                WebSocketUtil.sendText(
                                        session.getWebSocketSession(),
                                        JsonEncoder.encodeUseCacheMessage(newDatasetAddress, eTag, requestId));
                                changeSet.setRequired(false);
                                // We need to mark this as handled otherwise the wrapper will attempt to send
                                // another ok message with same requestId
                                // TODO: We really need to be able to handle multiple cached results for a single
                                // request
                                _registry.putResource(ServerConstants.CACHED_RESULT_HANDLED_KEY, "1");
                            }
                        } else {
                            session.setETag(newDatasetAddress, null);
                            final var cacheChangeSet = new ChangeSet();
                            cacheChangeSet.merge(cacheEntry.getChangeSet());
                            // cacheChangeSet.mergeAction( newDatasetAddress, ChannelAction.Action.ADD, filter );
                            queueCachedChangeSet(session, cacheChangeSet);
                            changeSet.setRequired(false);
                        }

                        final var entry = session.createSubscriptionEntry(newDatasetAddress);
                        if (isExplicitSubscribe) {
                            entry.setExplicitlySubscribed(true);
                        }
                        entry.setFilter(filter);
                    } else {
                        // If we get here then we have requested a cacheable instance dataset
                        // where the root has been removed
                        assert newDatasetAddress.hasDatasetRootId();
                        final var cacheChangeSet = new ChangeSet();
                        cacheChangeSet.mergeAction(newDatasetAddress, ChannelAction.Action.DELETE);
                        queueCachedChangeSet(session, cacheChangeSet);
                        changeSet.setRequired(false);
                    }
                }
            } else {
                _context.collectChannelData(session, newDatasetAddresses, filter, changeSet, isExplicitSubscribe);
            }
        }
        if (!datasetAddressesToUpdate.isEmpty()) {
            assert !dataset.isCacheable();
            for (final var update : datasetAddressesToUpdate.entrySet()) {
                final var originalFilter = update.getKey();
                final var updateDatasetAddresses = update.getValue();

                if (dataset.filterType().isDynamicFilter()) {
                    _context.collectChannelDataForFilterChange(
                            session, updateDatasetAddresses, originalFilter, Objects.requireNonNull(filter), changeSet);
                } else {
                    final var message = "Attempted to update filter on Dataset " + dataset.getName() + " to " + filter
                            + " but the Dataset has a static filter. Unsubscribe and resubscribe to the Dataset.";
                    throw new AttemptedToUpdateStaticFilterException(message);
                }
            }
        }
    }

    @Override
    public void setETags(@NonNull final ReplicantSession session, @NonNull final Map<DatasetAddress, String> eTags) {
        sessionLockingRequest("setEtags()", session, null, () -> session.setETags(eTags));
    }

    @SuppressWarnings("SameParameterValue")
    private void subscribe(
            @NonNull final ReplicantSession session,
            @NonNull final DatasetAddress datasetAddress,
            final boolean explicitlySubscribe,
            @Nullable final JsonObject filter,
            @NonNull final ChangeSet changeSet) {
        final var datasetMetadata = getSchemaMetaData().getDatasetMetadata(datasetAddress);

        if (session.isSubscriptionEntryPresent(datasetAddress)) {
            final var entry = session.getSubscriptionEntry(datasetAddress);
            if (explicitlySubscribe) {
                entry.setExplicitlySubscribed(true);
            }
            if (datasetMetadata.filterType().isDynamicFilter()) {
                doSubscribe(session, Collections.singletonList(datasetAddress), filter, changeSet, true);
            } else if (datasetMetadata.filterType().isStaticFilter()) {
                final var existingFilter = entry.getFilter();
                if (!FilterUtil.filtersEqual(filter, existingFilter)) {
                    final var message = "Attempted to update filter for Dataset Address " + entry.datasetAddress()
                            + " from " + existingFilter + " to " + filter
                            + " for a Dataset that has a static filter. Unsubscribe and resubscribe to the Dataset.";
                    throw new AttemptedToUpdateStaticFilterException(message);
                }
            }
        } else {
            doSubscribe(session, Collections.singletonList(datasetAddress), filter, changeSet, true);
        }
    }

    private void subscribeToRequiredTypeChannels(
            @NonNull final ReplicantSession session, @NonNull final DatasetMetadata datasetMetadata) {
        final var requiredTypeChannels = datasetMetadata.getRequiredTypeChannels();
        if (LOG.isLoggable(Level.FINE) && requiredTypeChannels.length > 0) {
            LOG.log(
                    Level.FINE,
                    "Subscribing to " + datasetMetadata.getName()
                            + " which has "
                            + requiredTypeChannels.length
                            + " required channels. "
                            + Arrays.stream(requiredTypeChannels)
                                    .map(DatasetMetadata::getName)
                                    .collect(Collectors.joining(",")));
        }
        for (final var requiredTypeChannel : requiredTypeChannels) {
            assert requiredTypeChannel.isTypeGraph();
            // At the moment we propagate no filters ... which is fine
            assert DatasetMetadata.FilterType.NONE == requiredTypeChannel.filterType();
            final var datasetAddress = DatasetAddress.of(requiredTypeChannel.getDatasetId());

            // This check is sufficient as it is not an explicit subscribe and there are no filters that can change
            if (!session.isSubscriptionEntryPresent(datasetAddress)) {
                final var requestId = (Integer) _registry.getResource(ServerConstants.REQUEST_ID_KEY);
                final var requestComplete = (String) _registry.getResource(ServerConstants.REQUEST_COMPLETE_KEY);
                final var requestResponse = (String) _registry.getResource(ServerConstants.REQUEST_RESPONSE_KEY);
                final var requestCachedResultHandled =
                        (String) _registry.getResource(ServerConstants.CACHED_RESULT_HANDLED_KEY);

                _registry.putResource(ServerConstants.REQUEST_ID_KEY, null);
                _registry.putResource(ServerConstants.REQUEST_COMPLETE_KEY, null);
                _registry.putResource(ServerConstants.REQUEST_RESPONSE_KEY, null);
                _registry.putResource(ServerConstants.CACHED_RESULT_HANDLED_KEY, null);

                final var changeSet = new ChangeSet();
                subscribe(session, datasetAddress, false, null, changeSet);
                if (changeSet.hasContent()) {
                    // In this scenario we have a non-cached changeset, so we send it along
                    _broker.queueChangeMessage(session, true, null, null, null, Collections.emptyList(), changeSet);
                }

                _registry.putResource(ServerConstants.REQUEST_ID_KEY, requestId);
                _registry.putResource(ServerConstants.REQUEST_COMPLETE_KEY, requestComplete);
                _registry.putResource(ServerConstants.REQUEST_RESPONSE_KEY, requestResponse);
                _registry.putResource(ServerConstants.CACHED_RESULT_HANDLED_KEY, requestCachedResultHandled);
            }
        }
    }

    private void deleteCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress);
        _cacheLock.writeLock().lock();
        try {
            final var metaData = getSchemaMetaData().getDatasetMetadata(datasetAddress);
            if (null != _cache.remove(datasetAddress)) {
                // If we expire the cache then any dependent type graphs must also be expired. This is
                // required as when a cache is on a client then we send back a "use-cache" message immediately
                // whereas if a message for a cached has to be loaded and sent back then we queue it on
                // ReplicantSession._pendingSubscriptionPackets and will be sent back. Unfortunately as we chain
                // up required graphs when sending cached results this may cause the later "use-cached" to arrive
                // before cache response and thus causing a failure on client. The "fix" is to queue the use-cache
                // on _pendingSubscriptionPackets but until that is implemented when we invalidate a cache we
                // invalidate all dependent cached type graphs to avoid this scenario.
                for (final var dataset : metaData.getDependentChannels()) {
                    if (dataset.isTypeGraph() && dataset.isCacheable()) {
                        _cache.remove(DatasetAddress.of(dataset.getDatasetId()));
                    }
                }
            }
        } finally {
            _cacheLock.writeLock().unlock();
        }
    }

    /**
     * Return a CacheEntry for a specific dataset. When this method returns the cache
     * data will have already been loaded. The cache data is loaded using a separate lock for
     * each dataset cached.
     */
    @Nullable
    private DatasetCacheEntry tryGetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress);
        final var metaData = getSchemaMetaData().getDatasetMetadata(datasetAddress);
        assert metaData.isCacheable();
        // We have not implemented the ability to cache filtered graphs. When it has been implemented, we can remove
        // this assertion.
        assert !metaData.requiresFilterParameter();
        // We have not implemented the ability to cache instance graphs. When it has been implemented we can remove this
        // assertion.
        assert metaData.isTypeGraph();
        final var entry = getCacheEntry(datasetAddress);
        entry.getLock().readLock().lock();
        try {
            if (entry.isInitialized()) {
                return entry;
            }
        } finally {
            entry.getLock().readLock().unlock();
        }
        entry.getLock().writeLock().lock();
        try {
            // Make sure check again once we re-acquire the lock
            if (entry.isInitialized()) {
                return entry;
            }
            final var changeSet = new ChangeSet();
            _context.collectChannelData(null, Collections.singletonList(datasetAddress), null, changeSet, false);
            final var cacheKey = changeSet.getETag();
            final var channelAction = changeSet.getChannelActions().stream()
                    .filter(a -> a.datasetAddress().equals(datasetAddress))
                    .findFirst()
                    .orElse(null);
            final var action = Objects.requireNonNull(channelAction).action();
            // Delete indicates the instance dataset has been deleted and will never be a valid dataset to subscribe to.
            if (ChannelAction.Action.DELETE == action) {
                assert null == cacheKey;
                return null;
            } else {
                // action can only be an update as we have supplied no filter and we are not attemptint to unsubscribe
                assert ChannelAction.Action.ADD == action;
                entry.init(Objects.requireNonNull(cacheKey), changeSet);
                return entry;
            }
        } finally {
            entry.getLock().writeLock().unlock();
        }
    }

    /**
     * Clear entire cache
     */
    @Override
    public void clearCache() {
        _cacheLock.writeLock().lock();
        try {
            _cache.clear();
        } finally {
            _cacheLock.writeLock().unlock();
        }
    }

    /**
     * Get the CacheEntry for specified dataset. Note that the cache is not necessarily
     * loaded at this stage. This is done to avoid using a global lock while loading data for a
     * particular cache entry.
     */
    private DatasetCacheEntry getCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress);
        _cacheLock.readLock().lock();
        try {
            final var entry = _cache.get(datasetAddress);
            if (null != entry) {
                return entry;
            }
        } finally {
            _cacheLock.readLock().unlock();
        }
        _cacheLock.writeLock().lock();
        try {
            // Try again in case it has since been created
            var entry = _cache.get(datasetAddress);
            if (null != entry) {
                return entry;
            }
            entry = new DatasetCacheEntry(datasetAddress);
            _cache.put(datasetAddress, entry);
            return entry;
        } finally {
            _cacheLock.writeLock().unlock();
        }
    }

    @Override
    public void unsubscribe(
            @NonNull final ReplicantSession session,
            final int requestId,
            @NonNull final List<DatasetAddress> datasetAddresses) {
        if (InvariantUtil.isInvariantCheckingEnabled()) {
            datasetAddresses.forEach(
                    datasetAddress -> InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), datasetAddress));
        }
        final var invocationKey = datasetAddresses.isEmpty()
                ? "BulkUnsubscribe(empty)"
                : "BulkUnsubscribe(" + datasetAddresses.get(0).datasetId() + ")";
        sessionUpdateRequest(invocationKey, session, requestId, () -> {
            if (session.isOpen()) {
                final var sessionChanges = EntityMessageCacheUtil.getSessionChanges();
                sessionChanges.setRequired(true);
                session.bulkUnsubscribe(datasetAddresses, sessionChanges);
            }
        });
    }

    private void processCachePurge(@NonNull final EntityMessage message) {
        final var schema = getSchemaMetaData();
        final var datasetCount = schema.getDatasetCount();
        for (var i = 0; i < datasetCount; i++) {
            if (schema.hasDatasetMetadata(i)) {
                final var dataset = schema.getDatasetMetadata(i);
                if (DatasetMetadata.CacheType.INTERNAL == dataset.getCacheType()) {
                    final var datasetAddresses = extractDatasetAddressesFromMessage(dataset, message);
                    if (null != datasetAddresses) {
                        for (final var datasetAddress : datasetAddresses) {
                            deleteCacheEntry(datasetAddress);
                        }
                    }
                }
            }
        }
    }

    private void processUpdateMessages(
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet) {
        final var schema = getSchemaMetaData();
        final var datasetCount = schema.getDatasetCount();
        for (var i = 0; i < datasetCount; i++) {
            if (schema.hasDatasetMetadata(i)) {
                final var dataset = schema.getDatasetMetadata(i);
                final var datasetAddresses = extractDatasetAddressesFromMessage(dataset, message);
                if (null != datasetAddresses) {
                    final var isFiltered = DatasetMetadata.FilterType.NONE != dataset.filterType();
                    for (final var datasetAddress : datasetAddresses) {
                        processUpdateMessage(datasetAddress, message, session, changeSet, isFiltered);
                    }
                }
            }
        }
    }

    @Nullable
    private List<DatasetAddress> extractDatasetAddressesFromMessage(
            @NonNull final DatasetMetadata dataset, @NonNull final EntityMessage message) {
        if (dataset.isInstanceGraph()) {
            @SuppressWarnings("unchecked")
            final var datasetRootIds = (List<Integer>) message.getRoutingKeys().get(dataset.getName());
            if (null != datasetRootIds) {
                return datasetRootIds.stream()
                        .map(datasetRootId -> DatasetAddress.of(dataset.getDatasetId(), datasetRootId))
                        .collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            if (message.getRoutingKeys().containsKey(dataset.getName())) {
                return Collections.singletonList(DatasetAddress.of(dataset.getDatasetId()));
            } else {
                return null;
            }
        }
    }

    private void processUpdateMessage(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            final boolean isFiltered) {
        final var entries = session.findSubscriptionEntries(datasetAddress.datasetId(), datasetAddress.datasetRootId());
        for (final var entry : entries) {
            final var entryDatasetAddress = entry.datasetAddress();
            final var m = isFiltered ? _context.filterEntityMessage(session, entryDatasetAddress, message) : message;

            // Process any messages that are in scope for session
            if (null != m) {
                changeSet.merge(new Change(message, entryDatasetAddress));
            }
        }
    }

    private void processDeleteMessages(
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet) {
        final var schema = getSchemaMetaData();
        final var instanceDatasetCount = schema.getInstanceDatasetCount();
        for (var i = 0; i < instanceDatasetCount; i++) {
            final var dataset = schema.getInstanceDatasetByIndex(i);
            @SuppressWarnings("unchecked")
            final var datasetRootIds = (List<Integer>) message.getRoutingKeys().get(dataset.getName());
            if (null != datasetRootIds) {
                for (final var datasetRootId : datasetRootIds) {
                    final var datasetAddress = DatasetAddress.of(dataset.getDatasetId(), datasetRootId);
                    final var isFiltered = DatasetMetadata.FilterType.NONE
                            != schema.getInstanceDatasetByIndex(i).filterType();
                    processDeleteMessage(datasetAddress, message, session, changeSet, isFiltered);
                }
            }
        }
    }

    /**
     * Process message handling any logical deletes.
     *
     * @param datasetAddress the Dataset Address
     * @param message    the message to process
     * @param session    the session that message is being processed for.
     * @param changeSet  for changeSet for session.
     * @param isFiltered a flag indicating that the graph is filtered.
     */
    private void processDeleteMessage(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final EntityMessage message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            final boolean isFiltered) {
        final var entries = session.findSubscriptionEntries(datasetAddress.datasetId(), datasetAddress.datasetRootId());
        for (final var entry : entries) {
            final var entryDatasetAddress = entry.datasetAddress();
            final var m = isFiltered ? _context.filterEntityMessage(session, entryDatasetAddress, message) : message;

            // Process any deleted messages that are in scope for session
            if (null != m && m.isDelete()) {
                var rootDeleted = false;

                // if the deletion message is for the root of the graph then perform an unsubscribe on the graph
                if (isEntityMessageChannelRoot(entry, datasetAddress, m)) {
                    session.performUnsubscribe(entry, true, true, changeSet);
                    rootDeleted = null == session.findSubscriptionEntry(entryDatasetAddress);
                }
                if (!rootDeleted) {
                    session.delinkDownstreamSubscriptions(entry, LinkOwner.entity(m.getTypeId(), m.getId()), changeSet);
                }
            }
        }
    }

    private boolean isEntityMessageChannelRoot(
            @NonNull final SubscriptionEntry entry,
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final EntityMessage message) {
        final var dataset = getSchemaMetaData().getDatasetMetadata(entry.datasetAddress());
        return dataset.isInstanceGraph()
                && dataset.getInstanceRootEntityTypeId() == message.getTypeId()
                && Objects.equals(datasetAddress.datasetRootId(), message.getId());
    }

    private record ResolvedChannelLink(
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject filter) {}
}
