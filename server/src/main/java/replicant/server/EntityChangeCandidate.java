package replicant.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class EntityChangeCandidate {
    private final int _id;
    private final int _typeId;
    /**
     * Routing keys contain values used to select affected Dataset Addresses. They include configured routing values
     * for this Entity and Entities on an Instance Dataset traversal path, plus identifiers for Dataset Roots reached
     * through Dataset Links.
     */
    @NonNull
    private final Map<String, Serializable> _routingKeys;

    @Nullable
    private Set<SubscriptionDependencyCandidate> _subscriptionDependencyCandidates;

    @Nullable
    private Map<String, Serializable> _attributeValues;

    private long _timestamp;

    public EntityChangeCandidate(
            final int id,
            final int typeId,
            final long timestamp,
            @NonNull final Map<String, Serializable> routingKeys,
            @Nullable final Map<String, Serializable> attributeValues) {
        this(id, typeId, timestamp, routingKeys, attributeValues, null);
    }

    public EntityChangeCandidate(
            final int id,
            final int typeId,
            final long timestamp,
            @NonNull final Map<String, Serializable> routingKeys,
            @Nullable final Map<String, Serializable> attributeValues,
            @Nullable final Set<SubscriptionDependencyCandidate> subscriptionDependencyCandidates) {
        _id = id;
        _typeId = typeId;
        _timestamp = timestamp;
        _routingKeys = Objects.requireNonNull(routingKeys);
        _attributeValues = attributeValues;
        _subscriptionDependencyCandidates = subscriptionDependencyCandidates;
        assertInvariants();
    }

    public int getTypeId() {
        return _typeId;
    }

    public int getId() {
        return _id;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public boolean isUpdate() {
        return null != getAttributeValues();
    }

    public boolean isDelete() {
        return !isUpdate();
    }

    @Nullable
    public Map<String, Serializable> getAttributeValues() {
        return _attributeValues;
    }

    @NonNull
    public Map<String, Serializable> getRoutingKeys() {
        return _routingKeys;
    }

    @Nullable
    public Set<SubscriptionDependencyCandidate> getSubscriptionDependencyCandidates() {
        return _subscriptionDependencyCandidates;
    }

    @NonNull
    public EntityChangeCandidate duplicate() {
        final var candidate =
                new EntityChangeCandidate(getId(), getTypeId(), getTimestamp(), new HashMap<>(), new HashMap<>());
        candidate.merge(this);
        return candidate;
    }

    @NonNull
    public EntityChangeCandidate toReplicaRemoval() {
        final var candidate = duplicate();
        candidate.merge(this);
        candidate._attributeValues = null;
        candidate._subscriptionDependencyCandidates = null;
        candidate.assertInvariants();
        return candidate;
    }

    @NonNull
    @Override
    public String toString() {
        return (isUpdate() ? "U" : "D") + "(Type="
                + getTypeId() + ",ID="
                + getId() + ",RoutingKeys="
                + getRoutingKeys() + (!isDelete() ? ",Data=" + getAttributeValues() : "")
                + ",Subscription Dependency Candidates="
                + getSubscriptionDependencyCandidates() + ")";
    }

    public void merge(@NonNull final EntityChangeCandidate other) {
        mergeTimestamp(other);
        mergeRoutingKeys(other);
        mergeAttributeValues(other);
        if (other.isDelete()) {
            _subscriptionDependencyCandidates = null;
        } else {
            mergeSubscriptionDependencyCandidates(other);
        }
        assertInvariants();
    }

    private void mergeTimestamp(@NonNull final EntityChangeCandidate other) {
        if (other.getTimestamp() > getTimestamp()) {
            _timestamp = other.getTimestamp();
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeRoutingKeys(@NonNull final EntityChangeCandidate other) {
        final var routingKeys = other.getRoutingKeys();
        for (final var entry : routingKeys.entrySet()) {
            final var value = entry.getValue();
            if (value instanceof List) {
                final var existing =
                        (List<Integer>) getRoutingKeys().computeIfAbsent(entry.getKey(), k -> new ArrayList<Integer>());
                final var toMerge = (List<Integer>) entry.getValue();
                for (final var id : toMerge) {
                    if (!existing.contains(id)) {
                        existing.add(id);
                    }
                }
            } else {
                getRoutingKeys().put(entry.getKey(), value);
            }
        }
    }

    private void mergeAttributeValues(@NonNull final EntityChangeCandidate other) {
        final var attributeValues = other.getAttributeValues();
        if (null == attributeValues) {
            _attributeValues = null;
        } else {
            if (null == _attributeValues) {
                _attributeValues = new HashMap<>();
            }
            _attributeValues.putAll(attributeValues);
        }
    }

    private void mergeSubscriptionDependencyCandidates(@NonNull final EntityChangeCandidate other) {
        final var subscriptionDependencyCandidates = other.getSubscriptionDependencyCandidates();
        if (null != subscriptionDependencyCandidates) {
            if (null == _subscriptionDependencyCandidates) {
                _subscriptionDependencyCandidates = new HashSet<>();
            }
            _subscriptionDependencyCandidates.addAll(subscriptionDependencyCandidates);
        }
    }

    private void assertInvariants() {
        assert null != _attributeValues || null == _subscriptionDependencyCandidates
                : "Delete EntityChangeCandidate must not contain Subscription Dependency Candidates";
    }
}
