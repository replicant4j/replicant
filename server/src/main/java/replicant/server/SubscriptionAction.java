package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A record of a Subscription lifecycle operation.
 */
public record SubscriptionAction(
        @NonNull DatasetAddress datasetAddress,
        @NonNull Action action,
        @Nullable JsonObject filterParameter) {
    public enum Action {
        // The Subscription has been created.
        SUBSCRIBE,
        // The Subscription has been removed.
        UNSUBSCRIBE,
        // The Filter Parameter associated with the Subscription has been updated.
        UPDATE,
        // The Dataset Root has been deleted and its Dataset Address can no longer be subscribed to.
        DELETE
    }

    @NonNull
    public static SubscriptionAction of(@NonNull final DatasetAddress datasetAddress, @NonNull final Action action) {
        return of(datasetAddress, action, null);
    }

    @NonNull
    public static SubscriptionAction of(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final Action action,
            @Nullable final JsonObject filterParameter) {
        return new SubscriptionAction(datasetAddress, action, filterParameter);
    }

    public SubscriptionAction {
        assert (Action.SUBSCRIBE == action || Action.UPDATE == action) || null == filterParameter;
    }
}
