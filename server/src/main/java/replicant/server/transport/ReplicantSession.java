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
import replicant.server.SubscriptionChange;
import replicant.server.json.JsonEncoder;

public final class ReplicantSession implements Serializable, Closeable {
    @NonNull
    private static final Logger LOG = Logger.getLogger(ReplicantSession.class.getName());

    @NonNull
    private final Session _webSocketSession;

    @Nullable
    private final ReplicantSessionAuthorization _authorization;

    @NonNull
    private final Map<DatasetAddress, String> _datasetCacheVersions = new HashMap<>();

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
        if (packet.fromSubscriptionRequest()) {
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
     * Send a Change Set to the client.
     *
     * @param requestId the request id that caused these changes if this session requested the changes.
     * @param response  the response message if the packet is the result of a request that has a response,
     *                  and the request was initiated by the session.
     * @param datasetCacheVersion the opaque Dataset Cache Version, or null for a non-cacheable result.
     * @param changeSet the Change Set to send.
     */
    public void sendChangeSet(
            @Nullable final Integer requestId,
            @Nullable final JsonValue response,
            @Nullable final String datasetCacheVersion,
            @NonNull final ChangeSet changeSet) {
        assert null == response || null != requestId;
        ensureLockedByCurrentThread();
        final var encodedChangeSet = JsonEncoder.encodeChangeSet(requestId, response, datasetCacheVersion, changeSet);
        LOG.log(
                Level.FINE,
                () -> "Sending Change Set for Replicant Session " + getId() + " with payload " + encodedChangeSet);
        if (!WebSocketUtil.sendText(getWebSocketSession(), encodedChangeSet)) {
            LOG.log(
                    Level.FINE,
                    () -> "Failed to send Change Set for Replicant Session " + getId() + " with payload "
                            + encodedChangeSet);
        }
    }

    void ensureLockedByCurrentThread() {
        if (!_lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Expected session to be locked by the current thread");
        }
    }

    @Nullable
    String getDatasetCacheVersion(@NonNull final DatasetAddress datasetAddress) {
        return _datasetCacheVersions.get(datasetAddress);
    }

    public void setDatasetCacheVersions(@NonNull final Map<DatasetAddress, String> datasetCacheVersions) {
        ensureLockedByCurrentThread();
        _datasetCacheVersions.clear();
        for (final var datasetCacheVersion : datasetCacheVersions.entrySet()) {
            setDatasetCacheVersion(datasetCacheVersion.getKey(), datasetCacheVersion.getValue());
        }
    }

    void setDatasetCacheVersion(
            @NonNull final DatasetAddress datasetAddress, @Nullable final String datasetCacheVersion) {
        ensureLockedByCurrentThread();
        if (null == datasetCacheVersion) {
            _datasetCacheVersions.remove(datasetAddress);
        } else {
            _datasetCacheVersions.put(datasetAddress, datasetCacheVersion);
        }
    }

    /**
     * Return the Subscription Entry for the specified Dataset Address.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    SubscriptionEntry getSubscriptionEntry(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        final var entry = findSubscriptionEntry(datasetAddress);
        if (null == entry) {
            throw new IllegalStateException(
                    "Unable to locate subscription entry for Dataset Address " + datasetAddress);
        }
        return entry;
    }

    /**
     * Record a Dataset-scoped Subscription Dependency.
     *
     * <p>This API is intended for downstream application code that needs to record an unconditional dependency after
     * subscribing to both Dataset Addresses. The source and target Dataset Addresses must already identify concrete
     * Subscriptions in this session and the target must be a concrete Type Dataset Address.</p>
     */
    public void recordDatasetScopedSubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress, @NonNull final DatasetAddress targetDatasetAddress) {
        assert !targetDatasetAddress.hasDatasetRootId();
        recordSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, SubscriptionDependencyOwner.dataset());
    }

    /**
     * Record an Entity-scoped Subscription Dependency.
     *
     * <p>This API is intended for downstream application code that needs to record an Entity-scoped dependency after
     * subscribing to both Dataset Addresses. The source and target Dataset Addresses must already identify concrete
     * Subscriptions in this session.</p>
     *
     * <p>Entity-owned Subscription Dependencies derived from Dataset Links are managed internally and should not be
     * recorded through this API unless the developer is using post-subscribe hooks to optimize data loads.</p>
     */
    public void recordEntityScopedSubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            final int entityTypeId,
            final int entityId) {
        recordSubscriptionDependency(
                sourceDatasetAddress, targetDatasetAddress, SubscriptionDependencyOwner.entity(entityTypeId, entityId));
    }

    private void recordSubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            @NonNull final SubscriptionDependencyOwner owner) {
        final var sourceEntry = getSubscriptionEntry(sourceDatasetAddress);
        final var targetEntry = getSubscriptionEntry(targetDatasetAddress);
        recordSubscriptionDependency(sourceEntry, targetEntry, owner);
    }

    void recordSubscriptionDependency(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionEntry targetEntry,
            @NonNull final SubscriptionDependencyOwner owner) {
        assert !owner.isDatasetScoped() || !targetEntry.datasetAddress().hasDatasetRootId();
        final var added = sourceEntry.registerOutwardSubscriptionDependencies(owner, targetEntry.datasetAddress());
        if (0 != added.length) {
            targetEntry.registerInwardSubscriptionDependencies(sourceEntry.datasetAddress());
        }
    }

    public void recordSubscriptions(
            @NonNull final ChangeSet changeSet,
            @NonNull final Collection<DatasetAddress> datasetAddresses,
            @Nullable final JsonObject filterParameter,
            @NonNull final SubscriptionMode mode) {
        for (final var datasetAddress : datasetAddresses) {
            recordSubscription(changeSet, datasetAddress, filterParameter, mode);
        }
    }

    public void recordSubscription(
            @NonNull final ChangeSet changeSet,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final JsonObject filterParameter,
            @NonNull final SubscriptionMode mode) {
        final var existing = findSubscriptionEntry(datasetAddress);
        final var entry = null == existing ? createSubscriptionEntry(datasetAddress, mode) : existing;
        if (SubscriptionMode.EXPLICIT == mode) {
            entry.setMode(SubscriptionMode.EXPLICIT);
        }
        entry.setFilterParameter(filterParameter);
        changeSet.mergeSubscriptionChange(
                datasetAddress,
                null == existing ? SubscriptionChange.Type.SUBSCRIBE : SubscriptionChange.Type.UPDATE,
                filterParameter);
    }

    @Nullable
    public JsonObject getFilterParameter(@NonNull final DatasetAddress datasetAddress) {
        return getSubscriptionEntry(datasetAddress).getFilterParameter();
    }

    public void setFilterParameter(
            @NonNull final DatasetAddress datasetAddress, @Nullable final JsonObject filterParameter) {
        getSubscriptionEntry(datasetAddress).setFilterParameter(filterParameter);
    }

    /**
     * Create and return a subscription entry for the specified Dataset Address.
     *
     * @throws IllegalStateException if subscription already exists.
     */
    @NonNull
    SubscriptionEntry createSubscriptionEntry(
            @NonNull final DatasetAddress datasetAddress, @NonNull final SubscriptionMode mode) {
        if (!_subscriptions.containsKey(datasetAddress)) {
            LOG.log(
                    Level.FINE,
                    () -> "Creating subscription entry for replicant session " + getId() + " at Dataset Address "
                            + datasetAddress);
            final var entry = new SubscriptionEntry(this, datasetAddress, mode);
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
            final boolean areaOfInterestRemoved,
            final boolean invalidateDatasetAddress,
            @NonNull final ChangeSet changeSet) {
        if (areaOfInterestRemoved) {
            entry.setMode(SubscriptionMode.IMPLICIT);
        }
        if (entry.canUnsubscribe()) {
            changeSet.mergeSubscriptionChange(
                    entry.datasetAddress(),
                    invalidateDatasetAddress
                            ? SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS
                            : SubscriptionChange.Type.UNSUBSCRIBE);
            for (final var downstream : new ArrayList<>(entry.getOutwardSubscriptionDependencies())) {
                removeAllDownstreamSubscriptionDependencies(entry, downstream, changeSet);
            }
            deleteSubscriptionEntry(entry);
        }
    }

    public void removeDownstreamSubscriptionDependency(
            @NonNull final DatasetAddress upstream,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        removeDownstreamSubscriptionDependency(
                getSubscriptionEntry(upstream), SubscriptionDependencyOwner.dataset(), downstream, changeSet);
    }

    void removeDownstreamSubscriptionDependency(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        final var removed = sourceEntry.deregisterOutwardSubscriptionDependencies(owner, downstream);
        if (0 != removed.length) {
            final var downstreamEntry = findSubscriptionEntry(downstream);
            if (null != downstreamEntry) {
                downstreamEntry.deregisterInwardSubscriptionDependencies(sourceEntry.datasetAddress());
                performUnsubscribe(downstreamEntry, false, false, changeSet);
            }
        }
    }

    void removeDownstreamSubscriptionDependencies(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final ChangeSet changeSet) {
        for (final var downstream : new ArrayList<>(sourceEntry.getOwnedOutwardSubscriptionDependencies(owner))) {
            removeDownstreamSubscriptionDependency(sourceEntry, owner, downstream, changeSet);
        }
    }

    private void removeAllDownstreamSubscriptionDependencies(
            @NonNull final SubscriptionEntry sourceEntry,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        final var removed = sourceEntry.deregisterAllOutwardSubscriptionDependencies(downstream);
        if (0 != removed.length) {
            final var downstreamEntry = findSubscriptionEntry(downstream);
            if (null != downstreamEntry) {
                downstreamEntry.deregisterInwardSubscriptionDependencies(sourceEntry.datasetAddress());
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
