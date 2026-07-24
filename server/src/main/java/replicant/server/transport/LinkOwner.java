package replicant.server.transport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record LinkOwner(@Nullable EntityReference entityReference) {
    @NonNull
    private static final LinkOwner GRAPH = new LinkOwner(null);

    @NonNull
    static LinkOwner graph() {
        return GRAPH;
    }

    @NonNull
    static LinkOwner entity(final int entityTypeId, final int entityId) {
        return new LinkOwner(new EntityReference(entityTypeId, entityId));
    }

    boolean isGraphScoped() {
        return null == entityReference();
    }
}
