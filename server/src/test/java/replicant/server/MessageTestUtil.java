package replicant.server;

import static org.testng.Assert.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A set of utility methods for testing EntityChangeCandidate infrastructure.
 */
public final class MessageTestUtil {
    @NonNull
    static final String ROUTING_KEY1 = "ROUTING_KEY1";

    @NonNull
    static final String ROUTING_KEY2 = "ROUTING_KEY2";

    @NonNull
    public static final String ATTR_KEY1 = "ATTR_KEY1";

    @NonNull
    public static final String ATTR_KEY2 = "ATTR_KEY2";

    private MessageTestUtil() {}

    @NonNull
    public static EntityChangeCandidate createMessage(
            final int id,
            final int typeID,
            final long timestamp,
            @Nullable final String r1,
            @Nullable final String r2,
            @Nullable final String a1,
            @Nullable final String a2) {
        return createMessage(id, typeID, timestamp, null, r1, r2, a1, a2);
    }

    @NonNull
    static EntityChangeCandidate createMessage(
            final int id,
            final int typeID,
            final long timestamp,
            @Nullable final SubscriptionDependencyCandidate subscriptionDependency,
            @Nullable final String r1,
            @Nullable final String r2,
            @Nullable final String a1,
            @Nullable final String a2) {
        final var routingKeys = new HashMap<String, Serializable>();
        if (null != r1) {
            routingKeys.put(ROUTING_KEY1, r1);
        }
        if (null != r2) {
            routingKeys.put(ROUTING_KEY2, r2);
        }

        final var attributeValues = (null == a1 && null == a2) ? null : new HashMap<String, Serializable>();
        if (null != a1) {
            Objects.requireNonNull(attributeValues).put(ATTR_KEY1, a1);
        }
        if (null != a2) {
            Objects.requireNonNull(attributeValues).put(ATTR_KEY2, a2);
        }

        final HashSet<SubscriptionDependencyCandidate> subscriptionDependencyCandidates;
        if (null != subscriptionDependency) {
            subscriptionDependencyCandidates = new HashSet<>();
            subscriptionDependencyCandidates.add(subscriptionDependency);
        } else {
            subscriptionDependencyCandidates = null;
        }

        return new EntityChangeCandidate(
                id, typeID, timestamp, routingKeys, attributeValues, subscriptionDependencyCandidates);
    }

    static void assertAttributeValue(
            @NonNull final EntityChangeCandidate message, @NonNull final String key, @Nullable final String value) {
        final var values = message.getAttributeValues();
        assertNotNull(values);
        assertEquals(Objects.requireNonNull(values).get(key), value);
    }

    static void assertRouteValue(
            @NonNull final EntityChangeCandidate message, @NonNull final String key, @Nullable final String value) {
        assertEquals(message.getRoutingKeys().get(key), value);
    }
}
