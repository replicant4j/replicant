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

public final class EntityMessage {
    private final int _id;
    private final int _typeId;
    /**
     * Routing keys contain two types of values.
     * For every Dataset that the Entity Type is contained within, the map will
     * contain "filter_in_graphs" attributes for this entity and any entity on
     * the path to the root of the instance graph. The map will also contain the
     * id of any instance roots for Datasets with a Dataset Link to this Entity.
     */
    @NonNull
    private final Map<String, Serializable> _routingKeys;

    @Nullable
    private Set<SubscriptionDependency> _subscriptionDependencies;

    @Nullable
    private Map<String, Serializable> _attributeValues;

    private long _timestamp;

    public EntityMessage(
            final int id,
            final int typeId,
            final long timestamp,
            @NonNull final Map<String, Serializable> routingKeys,
            @Nullable final Map<String, Serializable> attributeValues) {
        this(id, typeId, timestamp, routingKeys, attributeValues, null);
    }

    public EntityMessage(
            final int id,
            final int typeId,
            final long timestamp,
            @NonNull final Map<String, Serializable> routingKeys,
            @Nullable final Map<String, Serializable> attributeValues,
            @Nullable final Set<SubscriptionDependency> subscriptionDependencies) {
        _id = id;
        _typeId = typeId;
        _timestamp = timestamp;
        _routingKeys = Objects.requireNonNull(routingKeys);
        _attributeValues = attributeValues;
        _subscriptionDependencies = subscriptionDependencies;
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
    public Set<SubscriptionDependency> getSubscriptionDependencies() {
        return _subscriptionDependencies;
    }

    @NonNull
    public EntityMessage duplicate() {
        final var message = new EntityMessage(getId(), getTypeId(), getTimestamp(), new HashMap<>(), new HashMap<>());
        message.merge(this);
        return message;
    }

    @NonNull
    public EntityMessage toDelete() {
        final var message = duplicate();
        message.merge(this);
        message._attributeValues = null;
        message._subscriptionDependencies = null;
        message.assertInvariants();
        return message;
    }

    @NonNull
    @Override
    public String toString() {
        return (isUpdate() ? "U" : "D") + "(Type="
                + getTypeId() + ",ID="
                + getId() + ",RoutingKeys="
                + getRoutingKeys() + (!isDelete() ? ",Data=" + getAttributeValues() : "")
                + ",Subscription Dependencies="
                + getSubscriptionDependencies() + ")";
    }

    public void merge(@NonNull final EntityMessage message) {
        mergeTimestamp(message);
        mergeRoutingKeys(message);
        mergeAttributeValues(message);
        if (message.isDelete()) {
            _subscriptionDependencies = null;
        } else {
            mergeSubscriptionDependencies(message);
        }
        assertInvariants();
    }

    private void mergeTimestamp(@NonNull final EntityMessage message) {
        if (message.getTimestamp() > getTimestamp()) {
            _timestamp = message.getTimestamp();
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeRoutingKeys(@NonNull final EntityMessage message) {
        final var routingKeys = message.getRoutingKeys();
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

    private void mergeAttributeValues(@NonNull final EntityMessage message) {
        final var attributeValues = message.getAttributeValues();
        if (null == attributeValues) {
            _attributeValues = null;
        } else {
            if (null == _attributeValues) {
                _attributeValues = new HashMap<>();
            }
            _attributeValues.putAll(attributeValues);
        }
    }

    private void mergeSubscriptionDependencies(@NonNull final EntityMessage message) {
        final var subscriptionDependencies = message.getSubscriptionDependencies();
        if (null != subscriptionDependencies) {
            if (null == _subscriptionDependencies) {
                _subscriptionDependencies = new HashSet<>();
            }
            _subscriptionDependencies.addAll(subscriptionDependencies);
        }
    }

    private void assertInvariants() {
        assert null != _attributeValues || null == _subscriptionDependencies
                : "Delete EntityMessage must not contain Subscription Dependencies";
    }
}
