package replicant.server.transport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record SubscriptionDependencyOwner(@Nullable EntityReference entityReference) {
    @NonNull
    private static final SubscriptionDependencyOwner DATASET = new SubscriptionDependencyOwner(null);

    @NonNull
    static SubscriptionDependencyOwner dataset() {
        return DATASET;
    }

    @NonNull
    static SubscriptionDependencyOwner entity(final int entityTypeId, final int entityId) {
        return new SubscriptionDependencyOwner(new EntityReference(entityTypeId, entityId));
    }

    boolean isDatasetScoped() {
        return null == entityReference();
    }
}
