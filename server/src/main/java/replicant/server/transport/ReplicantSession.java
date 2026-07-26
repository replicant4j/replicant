package replicant.server.transport;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.SubscriptionAction;
import replicant.server.json.JsonEncoder;

public final class ReplicantSession implements Serializable, Closeable {
    @NonNull
    private static final Logger LOG = Logger.getLogger(ReplicantSession.class.getName());

    @NonNull
    private final Session _webSocketSession;

    @Nullable
    private final ReplicantSessionAuthorization _authorization;

    @NonNull
    private final Map<DatasetAddress, String> _eTags = new HashMap<>();

    @NonNull
    private final Map<DatasetAddress, SubscriptionEntry> _subscriptions = new HashMap<>();

    @NonNull
    private final Map<DatasetRootKey, Set<SubscriptionEntry>> _subscriptionsByDatasetRoot = new HashMap<>();

    @NonNull
    private final BlockingQueue<Packet> _pendingSubscriptionPackets = new LinkedBlockingQueue<>();

    @NonNull
    private final BlockingQueue<Packet> _pendingPackets = new LinkedBlockingQueue<>();

    @NonNull
    private final ReentrantLock _lock = new ReentrantLock(true);

    @Nullable
    private String _authToken;

    @Nullable
    private Object _userObject;

    private boolean _authorizationClosed;

    public ReplicantSession(@NonNull final Session webSocketSession) {
        this(webSocketSession, null);
    }

    public ReplicantSession(
            @NonNull final Session webSocketSession, @Nullable final ReplicantSessionAuthorization authorization) {
        _webSocketSession = Objects.requireNonNull(webSocketSession);
        _authorization = authorization;
        _userObject = null == authorization ? null : authorization.getPrincipal();
    }

    @SuppressWarnings("unused")
    @Nullable
    public Object getUserObject() {
        return _userObject;
    }

    @SuppressWarnings("unused")
    public void setUserObject(@Nullable final Object userObject) {
        _userObject = userObject;
    }

    public void closeDueToInterrupt() {
        close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Action interrupted"));
    }

    public void close(@NonNull final CloseReason closeReason) {
        releaseAuthorization();
        if (isOpen()) {
            LOG.log(Level.FINE, () -> "Closing websocket for replicant session " + getId() + " with " + closeReason);
            try {
                _webSocketSession.close(closeReason);
            } catch (final IOException ioe) {
                LOG.log(
                        Level.FINE,
                        () -> "Websocket close for replicant session " + getId() + " generated error " + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket close requested for replicant session " + getId() + " with " + closeReason
                            + " but the websocket is already closed");
        }
    }

    @Override
    public void close() {
        releaseAuthorization();
        if (isOpen()) {
            LOG.log(Level.FINE, () -> "Closing websocket for replicant session " + getId());
            try {
                _webSocketSession.close();
            } catch (final IOException ioe) {
                LOG.log(
                        Level.FINE,
                        () -> "Websocket close for replicant session " + getId() + " generated error " + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket close requested for replicant session " + getId()
                            + " but the websocket is already closed");
        }
    }

    /**
     * Send a ping at the network level to ensure the connection is kept alive.
     *
     * <p>This is required to keep connection alive when passing through some load balancers
     * that proxy non-ssl websockets and close the socket after an idle period.</p>
     */
    public void pingTransport() {
        if (isOpen()) {
            LOG.log(Level.FINE, () -> "Pinging websocket for replicant session " + getId());
            try {
                _webSocketSession.getBasicRemote().sendPing(null);
            } catch (final IOException ioe) {
                // All scenarios we can envision imply the session is shutting down, and thus can be ignored
                LOG.log(
                        Level.FINER,
                        () -> "Websocket ping for replicant session " + getId() + " generated error " + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket ping requested for replicant session " + getId()
                            + " but the websocket is already closed");
        }
    }

    public boolean isOpen() {
        return _webSocketSession.isOpen();
    }

    @NonNull
    public Session getWebSocketSession() {
        return _webSocketSession;
    }

    public void setAuthToken(@Nullable final String authToken) {
        _authToken = authToken;
    }

    /**
     * @return a token used for authentication, if any.
     */
    @SuppressWarnings("unused")
    @Nullable
    public String getAuthToken() {
        return _authToken;
    }

    /**
     * @return an opaque ID representing session.
     */
    @NonNull
    public String getId() {
        return getWebSocketSession().getId();
    }

    @NonNull
    public ReentrantLock getLock() {
        return _lock;
    }

    public boolean runIfValid(final ReplicantSessionAuthorization.@NonNull Action action) throws IOException {
        if (null == _authorization) {
            action.run();
            return true;
        }
        return _authorization.runIfValid(action);
    }

    public void touchActivity() {
        if (null != _authorization) {
            _authorization.touchActivity();
        }
    }

    private synchronized void releaseAuthorization() {
        if (!_authorizationClosed && null != _authorization) {
            _authorizationClosed = true;
            _authorization.close();
        }
    }

    void queuePacket(@NonNull final Packet packet) {
        if (packet.altersExplicitSubscriptions()) {
            _pendingSubscriptionPackets.add(packet);
        } else {
            _pendingPackets.add(packet);
        }
    }

    @Nullable
    Packet popPendingPacket() {
        /*
         * We prioritize subscription packets ahead of other packets.
         * As the subscription data on the session object has already been
         * updated, we need to tell the client that these subscription changes
         * have occurred before we try and route other messages to the client.
         *
         * Only after the client has been updated with all subscription changing
         * packets do we send other packets.
         */
        final var packet = _pendingSubscriptionPackets.poll();
        return null == packet ? _pendingPackets.poll() : packet;
    }

    boolean hasPendingPackets() {
        return !_pendingSubscriptionPackets.isEmpty() || !_pendingPackets.isEmpty();
    }

    /**
     * Send a packet to the client.
     *
     * @param requestId the request id that caused these changes if this session requested the changes.
     * @param response  the response message if the packet is the result of a request that has a response,
     *                  and the request was initiated by the session.
     * @param etag      the opaque identifier identifying the version. May be null if packet is not cache-able
     * @param changeSet the changeSet to create packet from.
     */
    public void sendPacket(
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @Nullable final String etag,
            @NonNull final ChangeSet changeSet) {
        assert null == response || null != requestId;
        ensureLockedByCurrentThread();
        final var message = JsonEncoder.encodeChangeSet(requestId, response, etag, changeSet);
        LOG.log(Level.FINE, () -> "Sending text message for replicant session " + getId() + " with payload " + message);
        if (!WebSocketUtil.sendText(getWebSocketSession(), message)) {
            LOG.log(
                    Level.FINE,
                    () -> "Failed to send text message for replicant session " + getId() + " with payload " + message);
        }
    }

    void ensureLockedByCurrentThread() {
        if (!_lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Expected session to be locked by the current thread");
        }
    }

    @Nullable
    String getETag(@NonNull final DatasetAddress datasetAddress) {
        assert datasetAddress.concrete();
        return _eTags.get(datasetAddress);
    }

    public void setETags(@NonNull final Map<DatasetAddress, String> etags) {
        ensureLockedByCurrentThread();
        _eTags.clear();
        for (final var etag : etags.entrySet()) {
            setETag(etag.getKey(), etag.getValue());
        }
    }

    void setETag(@NonNull final DatasetAddress datasetAddress, @Nullable final String eTag) {
        ensureLockedByCurrentThread();
        assert datasetAddress.concrete();
        if (null == eTag) {
            _eTags.remove(datasetAddress);
        } else {
            _eTags.put(datasetAddress, eTag);
        }
    }

    /**
     * Return the Subscription Entry for the specified Dataset Address.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    SubscriptionEntry getSubscriptionEntry(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        assert datasetAddress.concrete();
        final var entry = findSubscriptionEntry(datasetAddress);
        if (null == entry) {
            throw new IllegalStateException(
                    "Unable to locate subscription entry for Dataset Address " + datasetAddress);
        }
        return entry;
    }

    /**
     * Configure the subscription entries to reflect a graph-scoped downstream dependency.
     *
     * <p>This API is intended for downstream application code that needs to record a graph-level dependency after
     * subscribing to both Dataset Addresses. The source and target Dataset Addresses must already be concrete subscriptions in this
     * session and the target must be a concrete Type Dataset Address.</p>
     */
    public void recordGraphScopedGraphLink(
            @NonNull final DatasetAddress sourceDatasetAddress, @NonNull final DatasetAddress targetDatasetAddress) {
        assert !targetDatasetAddress.hasDatasetRootId();
        recordGraphLink(sourceDatasetAddress, targetDatasetAddress, LinkOwner.graph());
    }

    /**
     * Configure the subscription entries to reflect an entity-scoped downstream dependency.
     *
     * <p>This API is intended for downstream application code that needs to record an entity-scoped dependency after
     * subscribing to both Dataset Addresses. The source and target Dataset Addresses must already be concrete subscriptions in this
     * session.</p>
     *
     * <p>Entity-owned links, including links that resolve to instance graphs, are managed internally by Replicant's
     * follow-link processing and should not be recorded through this API unless the developer is using postSubscribe
     * hooks to optimize data loads.</p>
     */
    public void recordEntityScopedGraphLink(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            final int entityTypeId,
            final int entityId) {
        recordGraphLink(sourceDatasetAddress, targetDatasetAddress, LinkOwner.entity(entityTypeId, entityId));
    }

    private void recordGraphLink(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            @NonNull final LinkOwner owner) {
        InvariantUtil.assertConcreteDatasetAddress(sourceDatasetAddress);
        InvariantUtil.assertConcreteDatasetAddress(targetDatasetAddress);
        final var sourceEntry = getSubscriptionEntry(sourceDatasetAddress);
        final var targetEntry = getSubscriptionEntry(targetDatasetAddress);
        recordGraphLink(sourceEntry, targetEntry, owner);
    }

    void recordGraphLink(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionEntry targetEntry,
            @NonNull final LinkOwner owner) {
        InvariantUtil.assertConcreteDatasetAddress(sourceEntry.datasetAddress());
        InvariantUtil.assertConcreteDatasetAddress(targetEntry.datasetAddress());
        assert !owner.isGraphScoped() || !targetEntry.datasetAddress().hasDatasetRootId();
        final var added = sourceEntry.registerOutwardSubscriptions(owner, targetEntry.datasetAddress());
        if (0 != added.length) {
            targetEntry.registerInwardSubscriptions(sourceEntry.datasetAddress());
        }
    }

    public void recordSubscriptions(
            @NonNull final ChangeSet changeSet,
            @NonNull final Collection<DatasetAddress> datasetAddresses,
            @Nullable final JsonObject filter,
            final boolean explicitSubscribe) {
        for (final var datasetAddress : datasetAddresses) {
            recordSubscription(changeSet, datasetAddress, filter, explicitSubscribe);
        }
    }

    public void recordSubscription(
            @NonNull final ChangeSet changeSet,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final JsonObject filter,
            final boolean explicitSubscribe) {
        assert datasetAddress.concrete();
        final var existing = findSubscriptionEntry(datasetAddress);
        final var entry = null == existing ? createSubscriptionEntry(datasetAddress) : existing;
        if (explicitSubscribe) {
            entry.setExplicitlySubscribed(true);
        }
        entry.setFilter(filter);
        changeSet.mergeSubscriptionAction(
                datasetAddress,
                null == existing ? SubscriptionAction.Action.SUBSCRIBE : SubscriptionAction.Action.UPDATE,
                filter);
    }

    @Nullable
    public JsonObject getFilter(@NonNull final DatasetAddress datasetAddress) {
        assert datasetAddress.concrete();
        return getSubscriptionEntry(datasetAddress).getFilter();
    }

    public void setFilter(@NonNull final DatasetAddress datasetAddress, @Nullable final JsonObject filter) {
        assert datasetAddress.concrete();
        getSubscriptionEntry(datasetAddress).setFilter(filter);
    }

    /**
     * Create and return a subscription entry for the specified Dataset Address.
     *
     * @throws IllegalStateException if subscription already exists.
     */
    @NonNull
    SubscriptionEntry createSubscriptionEntry(@NonNull final DatasetAddress datasetAddress) {
        assert datasetAddress.concrete();
        if (!_subscriptions.containsKey(datasetAddress)) {
            LOG.log(
                    Level.FINE,
                    () -> "Creating subscription entry for replicant session " + getId() + " at Dataset Address "
                            + datasetAddress);
            final var entry = new SubscriptionEntry(this, datasetAddress);
            _subscriptions.put(datasetAddress, entry);
            _subscriptionsByDatasetRoot
                    .computeIfAbsent(
                            new DatasetRootKey(datasetAddress.datasetId(), datasetAddress.datasetRootId()),
                            key -> new HashSet<>())
                    .add(entry);
            return entry;
        } else {
            throw new IllegalStateException(
                    "SubscriptionEntry for Dataset Address " + datasetAddress + " already exists");
        }
    }

    /**
     * Return the subscription entry for the specified Dataset Address.
     */
    @Nullable
    SubscriptionEntry findSubscriptionEntry(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        assert datasetAddress.concrete();
        return _subscriptions.get(datasetAddress);
    }

    /**
     * Return true if the specified Dataset Address is present.
     */
    public boolean isSubscriptionEntryPresent(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        return null != findSubscriptionEntry(datasetAddress);
    }

    @NonNull
    List<SubscriptionEntry> findSubscriptionEntries(final int datasetId, @Nullable final Integer datasetRootId) {
        ensureLockedByCurrentThread();
        final var entries = _subscriptionsByDatasetRoot.get(new DatasetRootKey(datasetId, datasetRootId));
        return null == entries ? Collections.emptyList() : entries.stream().toList();
    }

    void bulkUnsubscribe(
            @NonNull final List<DatasetAddress> datasetAddresses, @NonNull final ChangeSet sessionChanges) {
        for (final var datasetAddress : datasetAddresses) {
            assert datasetAddress.concrete();
            unsubscribe(datasetAddress, sessionChanges);
        }
    }

    private void unsubscribe(@NonNull final DatasetAddress datasetAddress, @NonNull final ChangeSet changeSet) {
        final var entry = findSubscriptionEntry(datasetAddress);
        if (null != entry) {
            performUnsubscribe(entry, true, false, changeSet);
        }
    }

    void performUnsubscribe(
            @NonNull final SubscriptionEntry entry,
            final boolean explicitUnsubscribe,
            final boolean delete,
            @NonNull final ChangeSet changeSet) {
        assert entry.datasetAddress().concrete();
        if (explicitUnsubscribe) {
            entry.setExplicitlySubscribed(false);
        }
        if (entry.canUnsubscribe()) {
            changeSet.mergeSubscriptionAction(
                    entry.datasetAddress(),
                    delete ? SubscriptionAction.Action.DELETE : SubscriptionAction.Action.UNSUBSCRIBE);
            for (final var downstream : new ArrayList<>(entry.getOutwardSubscriptions())) {
                delinkAllDownstreamSubscription(entry, downstream, changeSet);
            }
            deleteSubscriptionEntry(entry);
        }
    }

    public void delinkDownstreamSubscription(
            @NonNull final DatasetAddress upstream,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        assert upstream.concrete();
        assert downstream.concrete();
        delinkDownstreamSubscription(getSubscriptionEntry(upstream), LinkOwner.graph(), downstream, changeSet);
    }

    void delinkDownstreamSubscription(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final LinkOwner owner,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        assert sourceEntry.datasetAddress().concrete();
        assert downstream.concrete();
        final var removed = sourceEntry.deregisterOutwardSubscriptions(owner, downstream);
        if (0 != removed.length) {
            final var downstreamEntry = findSubscriptionEntry(downstream);
            if (null != downstreamEntry) {
                downstreamEntry.deregisterInwardSubscriptions(sourceEntry.datasetAddress());
                performUnsubscribe(downstreamEntry, false, false, changeSet);
            }
        }
    }

    void delinkDownstreamSubscriptions(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final LinkOwner owner,
            @NonNull final ChangeSet changeSet) {
        assert sourceEntry.datasetAddress().concrete();
        for (final var downstream : new ArrayList<>(sourceEntry.getOwnedOutwardSubscriptions(owner))) {
            delinkDownstreamSubscription(sourceEntry, owner, downstream, changeSet);
        }
    }

    private void delinkAllDownstreamSubscription(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        assert sourceEntry.datasetAddress().concrete();
        assert downstream.concrete();
        final var removed = sourceEntry.deregisterAllOutwardSubscriptions(downstream);
        if (0 != removed.length) {
            final var downstreamEntry = findSubscriptionEntry(downstream);
            if (null != downstreamEntry) {
                downstreamEntry.deregisterInwardSubscriptions(sourceEntry.datasetAddress());
                performUnsubscribe(downstreamEntry, false, false, changeSet);
            }
        }
    }

    /**
     * Delete specified subscription entry.
     */
    boolean deleteSubscriptionEntry(@NonNull final SubscriptionEntry entry) {
        ensureLockedByCurrentThread();
        final var datasetAddress = entry.datasetAddress();
        final var removed = null != _subscriptions.remove(datasetAddress);
        if (removed) {
            final var key = new DatasetRootKey(datasetAddress.datasetId(), datasetAddress.datasetRootId());
            final var entries = _subscriptionsByDatasetRoot.get(key);
            if (null != entries) {
                entries.remove(entry);
                if (entries.isEmpty()) {
                    _subscriptionsByDatasetRoot.remove(key);
                }
            }
            LOG.log(
                    Level.FINE,
                    () -> "Removed subscription entry for replicant session " + getId() + " at Dataset Address "
                            + datasetAddress);
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Attempted to remove subscription entry for replicant session " + getId()
                            + " at Dataset Address " + datasetAddress + " but no such subscription existed");
        }
        return removed;
    }

    private record DatasetRootKey(int datasetId, @Nullable Integer datasetRootId) {}
}
