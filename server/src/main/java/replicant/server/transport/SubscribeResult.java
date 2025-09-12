package replicant.server.transport;

import javax.annotation.Nullable;

public record SubscribeResult(boolean channelRootDeleted, @Nullable String cacheKey)
{
}
