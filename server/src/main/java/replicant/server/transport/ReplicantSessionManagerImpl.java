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
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressCandidate;
import replicant.server.DatasetAddressTemplate;
import replicant.server.EntityChange;
import replicant.server.EntityChangeCandidate;
import replicant.server.FilterParameterUtil;
import replicant.server.ServerConstants;
import replicant.server.SubscriptionChange;
import replicant.server.SubscriptionDependencyCandidate;
import replicant.server.json.JsonEncoder;
import replicant.server.runtime.EntityChangeCandidateCacheUtil;
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
            final var messageSet = EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet(_registry);
            final var changeSet = EntityChangeCandidateCacheUtil.removeSessionChanges(_registry);
            if (null != messageSet || null != changeSet || null != requestId) {
                final var messages = null == messageSet
                        ? Collections.<EntityChangeCandidate>emptySet()
                        : messageSet.getEntityChangeCandidates();
                if (null != changeSet || !messages.isEmpty() || null != requestId) {
                    requestComplete = !saveEntityChangeCandidates(sessionId, requestId, response, messages, changeSet);
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

    private boolean saveEntityChangeCandidates(
            @Nullable final String sessionId,
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @NonNull final Collection<EntityChangeCandidate> messages,
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
                        changeSet.merge(sessionChanges.getEntityChanges());
                        changeSet.mergeSubscriptionChanges(sessionChanges.getSubscriptionChanges());
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
                final var fromSubscriptionRequest =
                        null != _registry.getResource(ServerConstants.SUBSCRIPTION_REQUEST_KEY);
                _broker.queueChangeMessage(
                        session,
                        fromSubscriptionRequest,
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
                packet.messages().size() + packet.changeSet().getEntityChanges().size();
        final var incomingSubscriptionDependencies = packet.messages().stream()
                        .map(EntityChangeCandidate::getSubscriptionDependencyCandidates)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .distinct()
                        .count()
                + packet.changeSet().getEntityChanges().stream()
                        .map(change -> change.getEntityChangeCandidate().getSubscriptionDependencyCandidates())
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
                                + incomingEntityCount + " incomingSubscriptionDependencyCount="
                                + incomingSubscriptionDependencies + " fromSubscriptionRequest="
                                + packet.fromSubscriptionRequest());
            }
            return false;
        }
        final var hasDeletes = messages.stream().anyMatch(EntityChangeCandidate::isDelete);
        final var datasetRootDeletedDatasetAddresses =
                hasDeletes ? collectRootDeletedEntries(messages, session) : Collections.<DatasetAddress>emptySet();
        if (hasDeletes) {
            preserveOwnedSubscriptionDependenciesBeforeDelete(
                    messages, session, changeSet, datasetRootDeletedDatasetAddresses);
        }
        processEntityChangeCandidates(messages, session, changeSet);

        // ChangeSets that occur during a subscription that result in a use-cache message
        // being sent to the client will still come through here. The hasContent() should
        // return false as there are no changes for in ChangeSet and the _required flag is unset.
        if (changeSet.hasContent()) {
            final var start = System.nanoTime();

            final var expandCycleCount =
                    completeMessageProcessing(session, changeSet, datasetRootDeletedDatasetAddresses);
            final var end = System.nanoTime();
            final var expansionDuration = (end - start) / 1000000;

            // This log level should be fine but leaving it here as INFO to make it easy to assess current production
            // issues.
            final var level = expansionDuration > 1000 ? Level.SEVERE : Level.INFO;
            if (LOG.isLoggable(level)) {
                final var outgoingEntityCount = changeSet.getEntityChanges().size();
                final var outgoingSubscriptionDependencies = changeSet.getEntityChanges().stream()
                        .map(change -> change.getEntityChangeCandidate().getSubscriptionDependencyCandidates())
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .distinct()
                        .count();
                final var actions = changeSet.getSubscriptionChanges().stream()
                        .map(JsonEncoder::toDescriptor)
                        .toList();
                LOG.log(
                        level,
                        "event=session.change.send sessionId=" + session.getId() + " requestId="
                                + requestId + " etag="
                                + etag + " fromSubscriptionRequest="
                                + packet.fromSubscriptionRequest() + " incomingEntityCount="
                                + incomingEntityCount + " incomingSubscriptionDependencyCount="
                                + incomingSubscriptionDependencies + " outgoingEntityCount="
                                + outgoingEntityCount + " outgoingSubscriptionDependencyCount="
                                + outgoingSubscriptionDependencies + " expandCycleCount="
                                + expandCycleCount + " expandTimeMs="
                                + expansionDuration + " subscriptionChanges="
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
                                + etag + " fromSubscriptionRequest="
                                + packet.fromSubscriptionRequest() + " incomingEntityCount="
                                + incomingEntityCount + " incomingSubscriptionDependencyCount="
                                + incomingSubscriptionDependencies + " messageCount="
                                + messages.size() + " changeCount="
                                + changeSet.getEntityChanges().size() + " subscriptionChangeCount="
                                + changeSet.getSubscriptionChanges().size());
            }
            return false;
        }
    }

    private int completeMessageProcessing(
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<DatasetAddress> datasetRootDeletedDatasetAddresses) {
        var expandCycleCount = 0;
        try {
            final var pending = new HashSet<PendingSubscriptionDependency>();

            while (true) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(
                            Level.FINE,
                            "event=session.change.send.expand sessionId=" + session.getId()
                                    + " cycle="
                                    + expandCycleCount
                                    + " changes="
                                    + changeSet.getEntityChanges().size()
                                    + " subscriptionChanges="
                                    + changeSet.getSubscriptionChanges().stream()
                                            .map(JsonEncoder::toDescriptor)
                                            .toList()
                                    + " pending="
                                    + pending.stream()
                                            .map(e -> e.targetDatasetAddress().toString())
                                            .toList());
                }
                expandCycleCount++;
                collectSubscriptionDependenciesToFollow(
                        session, changeSet, pending, datasetRootDeletedDatasetAddresses);
                if (pending.isEmpty()) {
                    break;
                }
                final var entry = pending.stream()
                        .min(Comparator.comparing(PendingSubscriptionDependency::targetDatasetAddress))
                        .orElseThrow();
                final var targetDatasetAddress = entry.targetDatasetAddress();
                final var toSubscribe = targetDatasetAddress.hasDatasetRootId()
                        ? pending.stream()
                                .filter(a -> a.targetDatasetAddress().datasetId() == targetDatasetAddress.datasetId()
                                        && Objects.equals(a.filterParameter(), entry.filterParameter()))
                                .toList()
                        : Collections.singletonList(entry);
                final var datasetAddresses = toSubscribe.stream()
                        .map(PendingSubscriptionDependency::targetDatasetAddress)
                        .toList();
                doSubscribe(session, datasetAddresses, entry.filterParameter(), changeSet, SubscriptionMode.IMPLICIT);
                toSubscribe.forEach(pending::remove);
                for (final var e : toSubscribe) {
                    final var sourceEntry = session.getSubscriptionEntry(e.sourceDatasetAddress());
                    final var targetEntry = session.getSubscriptionEntry(e.targetDatasetAddress());
                    InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceEntry.datasetAddress());
                    InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetEntry.datasetAddress());
                    session.recordSubscriptionDependency(sourceEntry, targetEntry, e.owner());
                }
            }
        } catch (final Exception e) {
            // This can occur when there is an error accessing the database
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Error invoking expandSubscriptionDependencies for session " + session.getId(), e);
            }
            session.close(new CloseReason(
                    CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Expanding Subscription Dependencies failed"));
        }
        return expandCycleCount;
    }

    /**
     * Collect a list of Subscription Dependencies in change set that may need to be followed.
     */
    private void collectSubscriptionDependenciesToFollow(
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<PendingSubscriptionDependency> targets,
            @NonNull final Set<DatasetAddress> datasetRootDeletedDatasetAddresses) {
        for (final var change : changeSet.getEntityChanges()) {
            final var entityChangeCandidate = change.getEntityChangeCandidate();
            if (entityChangeCandidate.isUpdate()) {
                final var owner = SubscriptionDependencyOwner.entity(
                        entityChangeCandidate.getTypeId(), entityChangeCandidate.getId());
                for (final var sourceDatasetAddress : change.getDatasetAddresses()) {
                    final var sourceEntry = session.findSubscriptionEntry(sourceDatasetAddress);
                    if (null != sourceEntry) {
                        final var desiredTargets =
                                resolveDesiredSubscriptionDependencyTargets(entityChangeCandidate, sourceEntry);
                        desiredTargets.keySet().removeAll(datasetRootDeletedDatasetAddresses);
                        reconcileOwnedSubscriptionDependencies(
                                session, sourceEntry, owner, desiredTargets, changeSet, targets);
                    }
                }
            }
        }
    }

    private boolean matchesSourceDatasetAddress(
            @NonNull final DatasetAddressCandidate candidate, @NonNull final DatasetAddress datasetAddress) {
        if (candidate instanceof DatasetAddressTemplate template) {
            return template.matches(datasetAddress);
        } else {
            return candidate.equals(datasetAddress);
        }
    }

    @NonNull
    private DatasetAddress resolveTargetDatasetAddress(
            @NonNull final EntityChangeCandidate entityChangeCandidate,
            @NonNull final DatasetAddress sourceDatasetAddress,
            @Nullable final JsonObject sourceFilterParameter,
            @NonNull final DatasetAddressCandidate targetDatasetAddressCandidate,
            @Nullable final JsonObject targetFilterParameter) {
        if (targetDatasetAddressCandidate instanceof DatasetAddressTemplate targetDatasetAddressTemplate) {
            assert entityChangeCandidate.isUpdate();
            final var datasetKey = _context.deriveTargetDatasetKey(
                    entityChangeCandidate,
                    sourceDatasetAddress,
                    sourceFilterParameter,
                    targetDatasetAddressTemplate,
                    targetFilterParameter);
            final var concreteTargetDatasetAddress = DatasetAddress.of(
                    targetDatasetAddressTemplate.datasetId(), targetDatasetAddressTemplate.datasetRootId(), datasetKey);
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), concreteTargetDatasetAddress);
            return concreteTargetDatasetAddress;
        } else {
            final var targetDatasetAddress = (DatasetAddress) targetDatasetAddressCandidate;
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetDatasetAddress);
            return targetDatasetAddress;
        }
    }

    /**
     * Resolve the desired downstream targets for the source entry from the entity-owned Subscription Dependency
     * Candidates in the Entity Change Candidate.
     */
    @NonNull
    private Map<DatasetAddress, JsonObject> resolveDesiredSubscriptionDependencyTargets(
            @NonNull final EntityChangeCandidate entityChangeCandidate, @NonNull final SubscriptionEntry sourceEntry) {
        final var desiredTargets = new LinkedHashMap<DatasetAddress, JsonObject>();
        final var subscriptionDependencies = entityChangeCandidate.getSubscriptionDependencyCandidates();
        if (null != subscriptionDependencies) {
            for (final var subscriptionDependency : subscriptionDependencies) {
                InvariantUtil.assertSubscriptionDependencyCandidate(getSchemaMetaData(), subscriptionDependency);
                if (matchesSourceDatasetAddress(
                        subscriptionDependency.sourceDatasetAddressCandidate(), sourceEntry.datasetAddress())) {
                    final var resolved = resolveSubscriptionDependencyIfRequired(
                            entityChangeCandidate, sourceEntry, subscriptionDependency);
                    if (null != resolved) {
                        final var existing =
                                desiredTargets.putIfAbsent(resolved.targetDatasetAddress(), resolved.filterParameter());
                        assert null == existing || Objects.equals(existing, resolved.filterParameter());
                    }
                }
            }
        }
        return desiredTargets;
    }

    private void reconcileOwnedSubscriptionDependencies(
            @NonNull final ReplicantSession session,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final Map<DatasetAddress, JsonObject> desiredTargets,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<PendingSubscriptionDependency> targets) {
        final var existingTargets = new HashSet<>(sourceEntry.getOwnedOutwardSubscriptionDependencies(owner));
        for (final var existingTarget : existingTargets) {
            if (!desiredTargets.containsKey(existingTarget)) {
                session.removeDownstreamSubscriptionDependency(sourceEntry, owner, existingTarget, changeSet);
            }
        }

        for (final var entry : desiredTargets.entrySet()) {
            final var pending = createOrUpdatePendingSubscriptionDependency(
                    session, owner, sourceEntry, entry.getKey(), entry.getValue(), changeSet);
            if (null != pending) {
                targets.add(pending);
            }
        }
    }

    @Nullable
    private ResolvedSubscriptionDependency resolveSubscriptionDependencyIfRequired(
            @NonNull final EntityChangeCandidate entityChangeCandidate,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionDependencyCandidate subscriptionDependency) {
        final var sourceDatasetAddress = sourceEntry.datasetAddress();
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceDatasetAddress);
        final var sourceFilterParameter = sourceEntry.getFilterParameter();
        final var targetDatasetAddress = resolveTargetDatasetAddress(
                entityChangeCandidate,
                sourceDatasetAddress,
                sourceFilterParameter,
                subscriptionDependency.targetDatasetAddressCandidate(),
                subscriptionDependency.targetFilterParameter());
        InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetDatasetAddress);
        final var dataset = getSchemaMetaData().getDatasetMetadata(targetDatasetAddress);
        if (dataset.isParameterFiltered()) {
            final var filterParameter = subscriptionDependency.hasTargetFilterParameter()
                    ? subscriptionDependency.targetFilterParameter()
                    : _context.deriveTargetFilterParameter(
                            entityChangeCandidate, sourceDatasetAddress, sourceFilterParameter, targetDatasetAddress);
            return _context.shouldFollowDatasetLink(
                            sourceDatasetAddress, sourceFilterParameter, targetDatasetAddress, filterParameter)
                    ? new ResolvedSubscriptionDependency(targetDatasetAddress, filterParameter)
                    : null;
        } else {
            return new ResolvedSubscriptionDependency(targetDatasetAddress, null);
        }
    }

    @Nullable
    private PendingSubscriptionDependency createOrUpdatePendingSubscriptionDependency(
            @NonNull final ReplicantSession session,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject filterParameter,
            @NonNull final ChangeSet changeSet) {
        final var targetEntry = session.findSubscriptionEntry(targetDatasetAddress);
        if (null == targetEntry) {
            return new PendingSubscriptionDependency(
                    owner, sourceEntry.datasetAddress(), targetDatasetAddress, filterParameter);
        } else {
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), sourceEntry.datasetAddress());
            InvariantUtil.assertConcreteDatasetAddress(getSchemaMetaData(), targetEntry.datasetAddress());
            if (FilterParameterUtil.filterParametersEqual(filterParameter, targetEntry.getFilterParameter())) {
                session.recordSubscriptionDependency(sourceEntry, targetEntry, owner);
                return null;
            } else if (getSchemaMetaData()
                    .getDatasetMetadata(targetDatasetAddress)
                    .hasUpdatableFilterParameter()) {
                return new PendingSubscriptionDependency(
                        owner, sourceEntry.datasetAddress(), targetDatasetAddress, filterParameter);
            } else {
                session.removeDownstreamSubscriptionDependency(sourceEntry, owner, targetDatasetAddress, changeSet);
                if (session.isSubscriptionEntryPresent(targetDatasetAddress)) {
                    throw fixedFilterParameterUpdateException(
                            getSchemaMetaData().getDatasetMetadata(targetDatasetAddress),
                            targetDatasetAddress,
                            targetEntry.getFilterParameter(),
                            filterParameter);
                }
                return new PendingSubscriptionDependency(
                        owner, sourceEntry.datasetAddress(), targetDatasetAddress, filterParameter);
            }
        }
    }

    private void processEntityChangeCandidates(
            @NonNull final Collection<EntityChangeCandidate> messages,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet) {
        for (final var message : messages) {
            processDeleteMessages(message, session, changeSet);
        }

        for (final var message : messages) {
            processUpdateMessages(message, session, changeSet);
        }
    }

    private void preserveOwnedSubscriptionDependenciesBeforeDelete(
            @NonNull final Collection<EntityChangeCandidate> messages,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            @NonNull final Set<DatasetAddress> datasetRootDeletedDatasetAddresses) {
        for (final var message : messages) {
            if (message.isUpdate()) {
                preserveOwnedSubscriptionDependenciesFromPacketMessage(
                        message, session, datasetRootDeletedDatasetAddresses);
            }
        }
        for (final var change : changeSet.getEntityChanges()) {
            final var message = change.getEntityChangeCandidate();
            if (message.isUpdate()) {
                final var owner = SubscriptionDependencyOwner.entity(message.getTypeId(), message.getId());
                for (final var sourceDatasetAddress : change.getDatasetAddresses()) {
                    final var sourceEntry = session.findSubscriptionEntry(sourceDatasetAddress);
                    if (null != sourceEntry) {
                        preserveOwnedSubscriptionDependenciesForSourceEntry(
                                message, session, sourceEntry, owner, datasetRootDeletedDatasetAddresses);
                    }
                }
            }
        }
    }

    @NonNull
    private Set<DatasetAddress> collectRootDeletedEntries(
            @NonNull final Collection<EntityChangeCandidate> messages, @NonNull final ReplicantSession session) {
        final var datasetRootDeletedDatasetAddresses = new HashSet<DatasetAddress>();
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
                            final var hasFilter = !dataset.isUnfiltered();
                            for (final var entry : session.findSubscriptionEntries(
                                    datasetAddress.datasetId(), datasetAddress.datasetRootId())) {
                                final var entryDatasetAddress = entry.datasetAddress();
                                final var m = hasFilter
                                        ? _context.filterEntityChangeCandidate(session, entryDatasetAddress, message)
                                        : message;
                                if (null != m && m.isDelete()) {
                                    if (isEntityChangeCandidateDatasetRoot(entry, datasetAddress, m)) {
                                        datasetRootDeletedDatasetAddresses.add(entryDatasetAddress);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return datasetRootDeletedDatasetAddresses;
    }

    private void preserveOwnedSubscriptionDependenciesFromPacketMessage(
            @NonNull final EntityChangeCandidate message,
            @NonNull final ReplicantSession session,
            @NonNull final Set<DatasetAddress> datasetRootDeletedDatasetAddresses) {
        final var schema = getSchemaMetaData();
        final var datasetCount = schema.getDatasetCount();
        final var owner = SubscriptionDependencyOwner.entity(message.getTypeId(), message.getId());
        for (var i = 0; i < datasetCount; i++) {
            if (schema.hasDatasetMetadata(i)) {
                final var dataset = schema.getDatasetMetadata(i);
                final var datasetAddresses = extractDatasetAddressesFromMessage(dataset, message);
                if (null != datasetAddresses) {
                    final var hasFilter = !dataset.isUnfiltered();
                    for (final var datasetAddress : datasetAddresses) {
                        for (final var entry : session.findSubscriptionEntries(
                                datasetAddress.datasetId(), datasetAddress.datasetRootId())) {
                            final var entryDatasetAddress = entry.datasetAddress();
                            final var m = hasFilter
                                    ? _context.filterEntityChangeCandidate(session, entryDatasetAddress, message)
                                    : message;
                            if (null != m) {
                                preserveOwnedSubscriptionDependenciesForSourceEntry(
                                        message, session, entry, owner, datasetRootDeletedDatasetAddresses);
                            }
                        }
                    }
                }
            }
        }
    }

    private void preserveOwnedSubscriptionDependenciesForSourceEntry(
            @NonNull final EntityChangeCandidate message,
            @NonNull final ReplicantSession session,
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final Set<DatasetAddress> datasetRootDeletedDatasetAddresses) {
        if (!datasetRootDeletedDatasetAddresses.contains(sourceEntry.datasetAddress())) {
            final var desiredTargets = resolveDesiredSubscriptionDependencyTargets(message, sourceEntry);
            for (final var entry : desiredTargets.entrySet()) {
                final var targetEntry = session.findSubscriptionEntry(entry.getKey());
                if (null != targetEntry) {
                    // An update can point at a Dataset whose Dataset Address is invalidated by the same packet;
                    // Dataset Address Invalidation semantics must win.
                    if (!datasetRootDeletedDatasetAddresses.contains(targetEntry.datasetAddress())
                            && FilterParameterUtil.filterParametersEqual(
                                    entry.getValue(), targetEntry.getFilterParameter())) {
                        session.recordSubscriptionDependency(sourceEntry, targetEntry, owner);
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
            @Nullable final JsonObject filterParameter) {
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
                final var sessionChanges = EntityChangeCandidateCacheUtil.getSessionChanges();
                sessionChanges.setRequired(true);
                datasetAddresses.forEach(
                        datasetAddress -> _context.preSubscribe(session, datasetAddress, filterParameter));
                doSubscribe(session, datasetAddresses, filterParameter, sessionChanges, SubscriptionMode.EXPLICIT);
            }
        });
    }

    private void doSubscribe(
            @NonNull final ReplicantSession session,
            @NonNull final List<DatasetAddress> datasetAddresses,
            @Nullable final JsonObject filterParameter,
            @NonNull final ChangeSet changeSet,
            @NonNull final SubscriptionMode mode) {
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

        subscribeToRequiredTypeDatasets(session, dataset);

        final var newDatasetAddresses = new ArrayList<DatasetAddress>();
        // Original Filter Parameter => Dataset Addresses
        final var datasetAddressesToUpdate = new HashMap<JsonObject, List<DatasetAddress>>();

        for (final var datasetAddress : uniqueDatasetAddresses) {
            assert datasetAddress.datasetId() == datasetId;
            if (dataset.isTypeDataset()) {
                assert !datasetAddress.hasDatasetRootId();
            } else {
                assert datasetAddress.hasDatasetRootId();
            }

            final var entry = session.findSubscriptionEntry(datasetAddress);
            if (null == entry) {
                newDatasetAddresses.add(datasetAddress);
            } else {
                if (SubscriptionMode.EXPLICIT == mode) {
                    entry.setMode(SubscriptionMode.EXPLICIT);
                }
                final var existingFilterParameter = entry.getFilterParameter();
                if (!FilterParameterUtil.filterParametersEqual(filterParameter, existingFilterParameter)) {
                    datasetAddressesToUpdate
                            .computeIfAbsent(existingFilterParameter, k -> new ArrayList<>())
                            .add(datasetAddress);
                }
            }
        }

        if (!newDatasetAddresses.isEmpty()) {
            if (dataset.isCacheable()) {
                // Only Type Datasets are cached at present; caching Instance Datasets requires additional plumbing.
                assert dataset.isTypeDataset();
                // Only Unfiltered Datasets are currently supported as cache targets.
                assert dataset.isUnfiltered();
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
                            // cacheChangeSet.mergeSubscriptionChange(
                            //     newDatasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter );
                            queueCachedChangeSet(session, cacheChangeSet);
                            changeSet.setRequired(false);
                        }

                        final var entry = session.createSubscriptionEntry(newDatasetAddress, mode);
                        entry.setFilterParameter(filterParameter);
                    } else {
                        // If we get here then we have requested a cacheable Instance Dataset
                        // whose Dataset Root has been removed
                        assert newDatasetAddress.hasDatasetRootId();
                        final var cacheChangeSet = new ChangeSet();
                        cacheChangeSet.mergeSubscriptionChange(
                                newDatasetAddress, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
                        queueCachedChangeSet(session, cacheChangeSet);
                        changeSet.setRequired(false);
                    }
                }
            } else {
                _context.collectSubscriptionData(session, newDatasetAddresses, filterParameter, changeSet, mode);
            }
        }
        if (!datasetAddressesToUpdate.isEmpty()) {
            assert !dataset.isCacheable();
            for (final var update : datasetAddressesToUpdate.entrySet()) {
                final var originalFilterParameter = update.getKey();
                final var updateDatasetAddresses = update.getValue();

                if (dataset.hasUpdatableFilterParameter()) {
                    _context.collectSubscriptionDataForFilterParameterChange(
                            session,
                            updateDatasetAddresses,
                            originalFilterParameter,
                            Objects.requireNonNull(filterParameter),
                            changeSet);
                } else {
                    throw fixedFilterParameterUpdateException(
                            dataset, updateDatasetAddresses.get(0), originalFilterParameter, filterParameter);
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
            @NonNull final SubscriptionMode mode,
            @Nullable final JsonObject filterParameter,
            @NonNull final ChangeSet changeSet) {
        final var datasetMetadata = getSchemaMetaData().getDatasetMetadata(datasetAddress);

        if (session.isSubscriptionEntryPresent(datasetAddress)) {
            final var entry = session.getSubscriptionEntry(datasetAddress);
            if (SubscriptionMode.EXPLICIT == mode) {
                entry.setMode(SubscriptionMode.EXPLICIT);
            }
            if (datasetMetadata.hasUpdatableFilterParameter()) {
                doSubscribe(session, Collections.singletonList(datasetAddress), filterParameter, changeSet, mode);
            } else if (datasetMetadata.hasFixedFilterParameter()) {
                final var existingFilterParameter = entry.getFilterParameter();
                if (!FilterParameterUtil.filterParametersEqual(filterParameter, existingFilterParameter)) {
                    throw fixedFilterParameterUpdateException(
                            datasetMetadata, entry.datasetAddress(), existingFilterParameter, filterParameter);
                }
            }
        } else {
            doSubscribe(session, Collections.singletonList(datasetAddress), filterParameter, changeSet, mode);
        }
    }

    private void subscribeToRequiredTypeDatasets(
            @NonNull final ReplicantSession session, @NonNull final DatasetMetadata datasetMetadata) {
        final var requiredTypeDatasets = datasetMetadata.getRequiredTypeDatasets();
        if (LOG.isLoggable(Level.FINE) && requiredTypeDatasets.length > 0) {
            LOG.log(
                    Level.FINE,
                    "Subscribing to " + datasetMetadata.getName()
                            + " which has "
                            + requiredTypeDatasets.length
                            + " Required Type Datasets. "
                            + Arrays.stream(requiredTypeDatasets)
                                    .map(DatasetMetadata::getName)
                                    .collect(Collectors.joining(",")));
        }
        for (final var requiredTypeDataset : requiredTypeDatasets) {
            assert requiredTypeDataset.isTypeDataset();
            // At the moment we propagate no Filter Parameters.
            assert requiredTypeDataset.isUnfiltered();
            final var datasetAddress = DatasetAddress.of(requiredTypeDataset.getDatasetId());

            // This check is sufficient because Required Type Datasets are retained in Implicit Subscription Mode and
            // have no Filter Parameters that can change.
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
                subscribe(session, datasetAddress, SubscriptionMode.IMPLICIT, null, changeSet);
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
                // If we expire the cache then any dependent Datasets must also be expired. This is
                // required as when a cache is on a client then we send back a "use-cache" message immediately
                // whereas if a message for a cached has to be loaded and sent back then we queue it on
                // ReplicantSession._pendingSubscriptionPackets and will be sent back. Unfortunately as we chain
                // up Required Type Datasets when sending cached results this may cause the later "use-cached" to arrive
                // before cache response and thus causing a failure on client. The "fix" is to queue the use-cache
                // on _pendingSubscriptionPackets but until that is implemented when we invalidate a cache we
                // invalidate all dependent cached Datasets to avoid this scenario.
                for (final var dataset : metaData.getDependentDatasets()) {
                    if (dataset.isTypeDataset() && dataset.isCacheable()) {
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
        // We have not implemented the ability to cache filtered Datasets. When it has been implemented, we can remove
        // this assertion.
        assert metaData.isUnfiltered();
        // We have not implemented the ability to cache Instance Datasets. When it has been implemented we can remove
        // this
        // assertion.
        assert metaData.isTypeDataset();
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
            _context.collectSubscriptionData(
                    null, Collections.singletonList(datasetAddress), null, changeSet, SubscriptionMode.IMPLICIT);
            final var cacheKey = changeSet.getETag();
            final var subscriptionChange = changeSet.getSubscriptionChanges().stream()
                    .filter(a -> a.datasetAddress().equals(datasetAddress))
                    .findFirst()
                    .orElse(null);
            final var action = Objects.requireNonNull(subscriptionChange).type();
            // Delete indicates the Dataset Root has been deleted and the Instance Dataset's Dataset Address will never
            // be valid to subscribe to again.
            if (SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS == action) {
                assert null == cacheKey;
                return null;
            } else {
                // The action can only be a subscribe as we supplied no Filter Parameter and are not attempting to
                // unsubscribe.
                assert SubscriptionChange.Type.SUBSCRIBE == action;
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
                final var sessionChanges = EntityChangeCandidateCacheUtil.getSessionChanges();
                sessionChanges.setRequired(true);
                session.bulkUnsubscribe(datasetAddresses, sessionChanges);
            }
        });
    }

    private void processCachePurge(@NonNull final EntityChangeCandidate message) {
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
            @NonNull final EntityChangeCandidate message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet) {
        final var schema = getSchemaMetaData();
        final var datasetCount = schema.getDatasetCount();
        for (var i = 0; i < datasetCount; i++) {
            if (schema.hasDatasetMetadata(i)) {
                final var dataset = schema.getDatasetMetadata(i);
                final var datasetAddresses = extractDatasetAddressesFromMessage(dataset, message);
                if (null != datasetAddresses) {
                    final var hasFilter = !dataset.isUnfiltered();
                    for (final var datasetAddress : datasetAddresses) {
                        processUpdateMessage(datasetAddress, message, session, changeSet, hasFilter);
                    }
                }
            }
        }
    }

    @Nullable
    private List<DatasetAddress> extractDatasetAddressesFromMessage(
            @NonNull final DatasetMetadata dataset, @NonNull final EntityChangeCandidate message) {
        if (dataset.isInstanceDataset()) {
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
            @NonNull final EntityChangeCandidate message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            final boolean hasFilter) {
        final var entries = session.findSubscriptionEntries(datasetAddress.datasetId(), datasetAddress.datasetRootId());
        for (final var entry : entries) {
            final var entryDatasetAddress = entry.datasetAddress();
            final var m =
                    hasFilter ? _context.filterEntityChangeCandidate(session, entryDatasetAddress, message) : message;

            // Process any messages that are in scope for session
            if (null != m) {
                changeSet.merge(new EntityChange(message, entryDatasetAddress));
            }
        }
    }

    private void processDeleteMessages(
            @NonNull final EntityChangeCandidate message,
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
                    final var hasFilter = !schema.getInstanceDatasetByIndex(i).isUnfiltered();
                    processDeleteMessage(datasetAddress, message, session, changeSet, hasFilter);
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
     * @param hasFilter true if the Dataset has a Filter.
     */
    private void processDeleteMessage(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final EntityChangeCandidate message,
            @NonNull final ReplicantSession session,
            @NonNull final ChangeSet changeSet,
            final boolean hasFilter) {
        final var entries = session.findSubscriptionEntries(datasetAddress.datasetId(), datasetAddress.datasetRootId());
        for (final var entry : entries) {
            final var entryDatasetAddress = entry.datasetAddress();
            final var m =
                    hasFilter ? _context.filterEntityChangeCandidate(session, entryDatasetAddress, message) : message;

            // Process any deleted messages that are in scope for session
            if (null != m && m.isDelete()) {
                var datasetRootDeleted = false;

                // If the deletion message is for the Dataset Root, unsubscribe from the Instance Dataset.
                if (isEntityChangeCandidateDatasetRoot(entry, datasetAddress, m)) {
                    session.performUnsubscribe(entry, true, true, changeSet);
                    datasetRootDeleted = null == session.findSubscriptionEntry(entryDatasetAddress);
                }
                if (!datasetRootDeleted) {
                    session.removeDownstreamSubscriptionDependencies(
                            entry, SubscriptionDependencyOwner.entity(m.getTypeId(), m.getId()), changeSet);
                }
            }
        }
    }

    private boolean isEntityChangeCandidateDatasetRoot(
            @NonNull final SubscriptionEntry entry,
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final EntityChangeCandidate message) {
        final var dataset = getSchemaMetaData().getDatasetMetadata(entry.datasetAddress());
        return dataset.isInstanceDataset()
                && dataset.getDatasetRootEntityTypeId() == message.getTypeId()
                && Objects.equals(datasetAddress.datasetRootId(), message.getId());
    }

    @NonNull
    private AttemptedToUpdateFixedFilterParameterException fixedFilterParameterUpdateException(
            @NonNull final DatasetMetadata dataset,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final JsonObject existingFilterParameter,
            @Nullable final JsonObject newFilterParameter) {
        return new AttemptedToUpdateFixedFilterParameterException(
                "Attempted to update the Fixed Filter Parameter for Dataset Address " + datasetAddress + " from "
                        + existingFilterParameter + " to "
                        + newFilterParameter + " on Dataset "
                        + dataset.getName()
                        + ". Unsubscribe and resubscribe to replace the Subscription.");
    }

    private record ResolvedSubscriptionDependency(
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject filterParameter) {}
}
