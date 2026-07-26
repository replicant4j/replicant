package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A record of a change in channel subscriptions.
 */
public record ChannelAction(
        @NonNull DatasetAddress datasetAddress,
        @NonNull Action action,
        @Nullable JsonObject filter) {
    public enum Action {
        // The channel has been successfully added.
        ADD,
        // The channel has been removed. This could be as a result of client request or as a result of the
        // filter excluding the graph, as the root instance being deleted
        REMOVE,
        // The filter associated with the channel has been updated
        UPDATE,
        // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
        DELETE
    }

    @NonNull
    public static ChannelAction of(@NonNull final DatasetAddress datasetAddress, @NonNull final Action action) {
        return of(datasetAddress, action, null);
    }

    @NonNull
    public static ChannelAction of(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final Action action,
            @Nullable final JsonObject filter) {
        return new ChannelAction(datasetAddress, action, filter);
    }

    public ChannelAction {
        assert (Action.ADD == action || Action.UPDATE == action) || null == filter;
    }
}
