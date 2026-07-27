package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A server-reported transition in actual Subscription state.
 */
public record SubscriptionChange(
        @NonNull DatasetAddress datasetAddress,
        @NonNull Type type,
        @Nullable JsonObject filterParameter) {
    public enum Type {
        // The Subscription has been created.
        SUBSCRIBE,
        // The Subscription has been removed.
        UNSUBSCRIBE,
        // The Filter Parameter associated with the Subscription has been updated.
        UPDATE,
        // The Dataset Root has been deleted and its Dataset Address can no longer be subscribed to.
        INVALIDATE_DATASET_ADDRESS
    }

    @NonNull
    public static SubscriptionChange of(@NonNull final DatasetAddress datasetAddress, @NonNull final Type type) {
        return of(datasetAddress, type, null);
    }

    @NonNull
    public static SubscriptionChange of(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final Type type,
            @Nullable final JsonObject filterParameter) {
        return new SubscriptionChange(datasetAddress, type, filterParameter);
    }

    public SubscriptionChange {
        assert (Type.SUBSCRIBE == type || Type.UPDATE == type) || null == filterParameter;
    }
}
