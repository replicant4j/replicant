package replicant.server.transport;

import org.jspecify.annotations.Nullable;

public record SubscribeResult(
        boolean datasetRootDeleted, @Nullable String cacheKey) {}
