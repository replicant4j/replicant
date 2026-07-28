package replicant.spy;

import org.jspecify.annotations.Nullable;
import replicant.Replicant;

/**
 * Summary describing the result of processing a server message.
 */
public final class MessageProcessingSummary {
    @Nullable
    private final Integer _requestId;
    /// The number of subscribe operations applied as a result of the Message
    private final int _subscriptionSubscribeCount;
    /// The number of Subscription update operations applied as a result of the Message
    private final int _subscriptionUpdateCount;
    /// The number of unsubscriptions or Dataset Address Invalidations applied as a result of the Message
    private final int _subscriptionUnsubscribeCount;
    // The number of entities created or updated as part of a Change Set
    private final int _entityUpdateCount;
    // The number of entities removed as part of a Change Set
    private final int _entityRemoveCount;
    // The number of entities where link() was invoked
    private final int _entityLinkCount;

    public MessageProcessingSummary(
            @Nullable final Integer requestId,
            final int subscriptionSubscribeCount,
            final int subscriptionUpdateCount,
            final int subscriptionUnsubscribeCount,
            final int entityUpdateCount,
            final int entityRemoveCount,
            final int entityLinkCount) {
        _requestId = requestId;
        _subscriptionSubscribeCount = subscriptionSubscribeCount;
        _subscriptionUpdateCount = subscriptionUpdateCount;
        _subscriptionUnsubscribeCount = subscriptionUnsubscribeCount;
        _entityUpdateCount = entityUpdateCount;
        _entityRemoveCount = entityRemoveCount;
        _entityLinkCount = entityLinkCount;
    }

    @Nullable
    public Integer getRequestId() {
        return _requestId;
    }

    public int getSubscriptionSubscribeCount() {
        return _subscriptionSubscribeCount;
    }

    public int getSubscriptionUpdateCount() {
        return _subscriptionUpdateCount;
    }

    public int getSubscriptionUnsubscribeCount() {
        return _subscriptionUnsubscribeCount;
    }

    public int getEntityUpdateCount() {
        return _entityUpdateCount;
    }

    public int getEntityRemoveCount() {
        return _entityRemoveCount;
    }

    public int getEntityLinkCount() {
        return _entityLinkCount;
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return "[Message" + (null == _requestId ? "" : " for request " + _requestId)
                    + " involved "
                    + getSubscriptionSubscribeCount()
                    + " subscribes, " + getSubscriptionUpdateCount()
                    + " subscription updates, " + getSubscriptionUnsubscribeCount()
                    + " unsubscribes, " + getEntityUpdateCount()
                    + " updates, " + getEntityRemoveCount()
                    + " removes and " + getEntityLinkCount()
                    + " links" + "]";
        } else {
            return super.toString();
        }
    }
}
