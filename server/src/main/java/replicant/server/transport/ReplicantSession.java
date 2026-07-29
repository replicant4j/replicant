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
    private final Map<DatasetAddress, Subscription> _subscriptions = new HashMap<>();

    @NonNull
    private final Map<DatasetRootKey, Set<Subscription>> _subscriptionsByDatasetRoot = new HashMap<>();

    @NonNull
    private final BlockingQueue<Packet> _pendingSubscriptionPackets = new LinkedBlockingQueue<>();

    @NonNull
    private final BlockingQueue<Packet> _pendingPackets = new LinkedBlockingQueue<>();

    @NonNull
    private final ReentrantLock _lock = new ReentrantLock(true);

    @Nullable
    private String _authToken;

    @Nullable
    private Object _principal;

    private boolean _authorizationClosed;

    public ReplicantSession(@NonNull final Session webSocketSession) {
        this(webSocketSession, null);
    }

    public ReplicantSession(
            @NonNull final Session webSocketSession, @Nullable final ReplicantSessionAuthorization authorization) {
        _webSocketSession = Objects.requireNonNull(webSocketSession);
        _authorization = authorization;
        _principal = null == authorization ? null : authorization.getPrincipal();
    }

    /**
     * Return the Principal associated with this Replicant Session.
     *
     * @return the Principal, or null if no Principal is associated with the session.
     */
    @Nullable
    public Object getPrincipal() {
        return _principal;
    }

    /**
     * Associate a Principal with this Replicant Session.
     *
     * @param principal the Principal, or null to remove the current Principal.
     */
    public void setPrincipal(@Nullable final Object principal) {
        _principal = principal;
    }

    public void closeDueToInterrupt() {
        close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Action interrupted"));
    }

    public void close(@NonNull final CloseReason closeReason) {
        releaseAuthorization();
        if (isOpen()) {
            LOG.log(
                    Level.FINE,
                    () -> "Closing websocket for Replicant Session ID " + getReplicantSessionId() + " with "
                            + closeReason);
            try {
                _webSocketSession.close(closeReason);
            } catch (final IOException ioe) {
                LOG.log(
                        Level.FINE,
                        () -> "Websocket close for Replicant Session ID " + getReplicantSessionId()
                                + " generated error " + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket close requested for Replicant Session ID " + getReplicantSessionId() + " with "
                            + closeReason + " but the websocket is already closed");
        }
    }

    @Override
    public void close() {
        releaseAuthorization();
        if (isOpen()) {
            LOG.log(Level.FINE, () -> "Closing websocket for Replicant Session ID " + getReplicantSessionId());
            try {
                _webSocketSession.close();
            } catch (final IOException ioe) {
                LOG.log(
                        Level.FINE,
                        () -> "Websocket close for Replicant Session ID " + getReplicantSessionId()
                                + " generated error " + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket close requested for Replicant Session ID " + getReplicantSessionId()
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
            LOG.log(Level.FINE, () -> "Pinging websocket for Replicant Session ID " + getReplicantSessionId());
            try {
                _webSocketSession.getBasicRemote().sendPing(null);
            } catch (final IOException ioe) {
                // All scenarios we can envision imply the session is shutting down, and thus can be ignored
                LOG.log(
                        Level.FINER,
                        () -> "Websocket ping for Replicant Session ID " + getReplicantSessionId() + " generated error "
                                + ioe);
            }
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Websocket ping requested for Replicant Session ID " + getReplicantSessionId()
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
     * @return the Replicant Session ID.
     */
    @NonNull
    public String getReplicantSessionId() {
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
         * have occurred before we send other packets to the client.
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
     * @param requestId the Request ID that caused these changes if this session requested the changes.
     * @param commandResult the Command Result if the Change Set completes a Command initiated by this session.
     * @param datasetCacheVersion the opaque Dataset Cache Version, or null when the Change Set does not represent a
     *                            Cacheable Dataset.
     * @param changeSet the Change Set to send.
     */
    public void sendChangeSet(
            @Nullable final Integer requestId,
            @Nullable final JsonValue commandResult,
            @Nullable final String datasetCacheVersion,
            @NonNull final ChangeSet changeSet) {
        assert null == commandResult || null != requestId;
        ensureLockedByCurrentThread();
        final var encodedChangeSet =
                JsonEncoder.encodeChangeSet(requestId, commandResult, datasetCacheVersion, changeSet);
        LOG.log(
                Level.FINE,
                () -> "Sending Change Set for Replicant Session ID " + getReplicantSessionId() + " with payload "
                        + encodedChangeSet);
        if (!WebSocketUtil.sendText(getWebSocketSession(), encodedChangeSet)) {
            LOG.log(
                    Level.FINE,
                    () -> "Failed to send Change Set for Replicant Session ID " + getReplicantSessionId()
                            + " with payload " + encodedChangeSet);
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
     * Return the Subscription for the specified Dataset Address.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    Subscription getSubscription(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        final var subscription = findSubscription(datasetAddress);
        if (null == subscription) {
            throw new IllegalStateException("Unable to locate Subscription for Dataset Address " + datasetAddress);
        }
        return subscription;
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
     * recorded through this API unless the developer is using post-collection hooks to optimize Subscription
     * Collection.</p>
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
        final var sourceSubscription = getSubscription(sourceDatasetAddress);
        final var targetSubscription = getSubscription(targetDatasetAddress);
        recordSubscriptionDependency(sourceSubscription, targetSubscription, owner);
    }

    void recordSubscriptionDependency(
            @NonNull final Subscription sourceSubscription,
            @NonNull final Subscription targetSubscription,
            @NonNull final SubscriptionDependencyOwner owner) {
        assert !owner.isDatasetScoped() || !targetSubscription.datasetAddress().hasDatasetRootId();
        final var added =
                sourceSubscription.registerOutwardSubscriptionDependencies(owner, targetSubscription.datasetAddress());
        if (0 != added.length) {
            targetSubscription.registerInwardSubscriptionDependencies(sourceSubscription.datasetAddress());
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
        final var existing = findSubscription(datasetAddress);
        final var subscription = null == existing ? createSubscription(datasetAddress, mode) : existing;
        if (SubscriptionMode.EXPLICIT == mode) {
            subscription.setMode(SubscriptionMode.EXPLICIT);
        }
        subscription.setFilterParameter(filterParameter);
        changeSet.mergeSubscriptionChange(
                datasetAddress,
                null == existing ? SubscriptionChange.Type.SUBSCRIBE : SubscriptionChange.Type.UPDATE,
                filterParameter);
    }

    @Nullable
    public JsonObject getFilterParameter(@NonNull final DatasetAddress datasetAddress) {
        return getSubscription(datasetAddress).getFilterParameter();
    }

    public void setFilterParameter(
            @NonNull final DatasetAddress datasetAddress, @Nullable final JsonObject filterParameter) {
        getSubscription(datasetAddress).setFilterParameter(filterParameter);
    }

    /**
     * Create and return a Subscription for the specified Dataset Address.
     *
     * @throws IllegalStateException if subscription already exists.
     */
    @NonNull
    Subscription createSubscription(
            @NonNull final DatasetAddress datasetAddress, @NonNull final SubscriptionMode mode) {
        if (!_subscriptions.containsKey(datasetAddress)) {
            LOG.log(
                    Level.FINE,
                    () -> "Creating Subscription for Replicant Session ID " + getReplicantSessionId()
                            + " at Dataset Address " + datasetAddress);
            final var subscription = new Subscription(this, datasetAddress, mode);
            _subscriptions.put(datasetAddress, subscription);
            _subscriptionsByDatasetRoot
                    .computeIfAbsent(
                            new DatasetRootKey(datasetAddress.datasetId(), datasetAddress.datasetRootId()),
                            key -> new HashSet<>())
                    .add(subscription);
            return subscription;
        } else {
            throw new IllegalStateException("Subscription for Dataset Address " + datasetAddress + " already exists");
        }
    }

    /**
     * Return the Subscription for the specified Dataset Address.
     */
    @Nullable
    Subscription findSubscription(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        return _subscriptions.get(datasetAddress);
    }

    /**
     * Return true if the specified Dataset Address is present.
     */
    public boolean isSubscriptionPresent(@NonNull final DatasetAddress datasetAddress) {
        ensureLockedByCurrentThread();
        return null != findSubscription(datasetAddress);
    }

    @NonNull
    List<Subscription> findSubscriptions(final int datasetId, @Nullable final Integer datasetRootId) {
        ensureLockedByCurrentThread();
        final var subscriptions = _subscriptionsByDatasetRoot.get(new DatasetRootKey(datasetId, datasetRootId));
        return null == subscriptions
                ? Collections.emptyList()
                : subscriptions.stream().toList();
    }

    void bulkUnsubscribe(
            @NonNull final List<DatasetAddress> datasetAddresses, @NonNull final ChangeSet initiatingSessionChangeSet) {
        for (final var datasetAddress : datasetAddresses) {
            unsubscribe(datasetAddress, initiatingSessionChangeSet);
        }
    }

    private void unsubscribe(@NonNull final DatasetAddress datasetAddress, @NonNull final ChangeSet changeSet) {
        final var subscription = findSubscription(datasetAddress);
        if (null != subscription) {
            performUnsubscribe(subscription, true, false, changeSet);
        }
    }

    void performUnsubscribe(
            @NonNull final Subscription subscription,
            final boolean areaOfInterestRemoved,
            final boolean invalidateDatasetAddress,
            @NonNull final ChangeSet changeSet) {
        if (areaOfInterestRemoved) {
            subscription.setMode(SubscriptionMode.IMPLICIT);
        }
        if (subscription.canUnsubscribe()) {
            changeSet.mergeSubscriptionChange(
                    subscription.datasetAddress(),
                    invalidateDatasetAddress
                            ? SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS
                            : SubscriptionChange.Type.UNSUBSCRIBE);
            for (final var downstream : new ArrayList<>(subscription.getOutwardSubscriptionDependencies())) {
                removeAllDownstreamSubscriptionDependencies(subscription, downstream, changeSet);
            }
            deleteSubscription(subscription);
        }
    }

    public void removeDownstreamSubscriptionDependency(
            @NonNull final DatasetAddress upstream,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        removeDownstreamSubscriptionDependency(
                getSubscription(upstream), SubscriptionDependencyOwner.dataset(), downstream, changeSet);
    }

    void removeDownstreamSubscriptionDependency(
            @NonNull final Subscription sourceSubscription,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        final var removed = sourceSubscription.deregisterOutwardSubscriptionDependencies(owner, downstream);
        if (0 != removed.length) {
            final var downstreamSubscription = findSubscription(downstream);
            if (null != downstreamSubscription) {
                downstreamSubscription.deregisterInwardSubscriptionDependencies(sourceSubscription.datasetAddress());
                performUnsubscribe(downstreamSubscription, false, false, changeSet);
            }
        }
    }

    void removeDownstreamSubscriptionDependencies(
            @NonNull final Subscription sourceSubscription,
            @NonNull final SubscriptionDependencyOwner owner,
            @NonNull final ChangeSet changeSet) {
        for (final var downstream :
                new ArrayList<>(sourceSubscription.getOwnedOutwardSubscriptionDependencies(owner))) {
            removeDownstreamSubscriptionDependency(sourceSubscription, owner, downstream, changeSet);
        }
    }

    private void removeAllDownstreamSubscriptionDependencies(
            @NonNull final Subscription sourceSubscription,
            @NonNull final DatasetAddress downstream,
            @NonNull final ChangeSet changeSet) {
        final var removed = sourceSubscription.deregisterAllOutwardSubscriptionDependencies(downstream);
        if (0 != removed.length) {
            final var downstreamSubscription = findSubscription(downstream);
            if (null != downstreamSubscription) {
                downstreamSubscription.deregisterInwardSubscriptionDependencies(sourceSubscription.datasetAddress());
                performUnsubscribe(downstreamSubscription, false, false, changeSet);
            }
        }
    }

    /**
     * Delete the specified Subscription.
     */
    boolean deleteSubscription(@NonNull final Subscription subscription) {
        ensureLockedByCurrentThread();
        final var datasetAddress = subscription.datasetAddress();
        final var removed = null != _subscriptions.remove(datasetAddress);
        if (removed) {
            final var key = new DatasetRootKey(datasetAddress.datasetId(), datasetAddress.datasetRootId());
            final var subscriptions = _subscriptionsByDatasetRoot.get(key);
            if (null != subscriptions) {
                subscriptions.remove(subscription);
                if (subscriptions.isEmpty()) {
                    _subscriptionsByDatasetRoot.remove(key);
                }
            }
            LOG.log(
                    Level.FINE,
                    () -> "Removed Subscription for Replicant Session ID " + getReplicantSessionId()
                            + " at Dataset Address " + datasetAddress);
        } else {
            LOG.log(
                    Level.FINE,
                    () -> "Attempted to remove Subscription for Replicant Session ID "
                            + getReplicantSessionId()
                            + " at Dataset Address " + datasetAddress + " but no such subscription existed");
        }
        return removed;
    }

    private record DatasetRootKey(int datasetId, @Nullable Integer datasetRootId) {}
}
