package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A representation indicating that an entity message will cause another Dataset Address to be subscribed.
 */
public record ChannelLink(
        @NonNull DatasetAddress sourceDatasetAddress,
        @NonNull DatasetAddress targetDatasetAddress,
        @Nullable JsonObject targetFilter,
        boolean partial) {
    public ChannelLink(
            @NonNull final DatasetAddress sourceDatasetAddress, @NonNull final DatasetAddress targetDatasetAddress) {
        this(sourceDatasetAddress, targetDatasetAddress, null);
    }

    public ChannelLink(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject targetFilter) {
        this(sourceDatasetAddress, targetDatasetAddress, targetFilter, false);
    }

    public ChannelLink {
        assert partial || (!sourceDatasetAddress.partial() && !targetDatasetAddress.partial());
        assert !partial || sourceDatasetAddress.partial() || targetDatasetAddress.partial() || null == targetFilter;
    }

    public boolean hasTargetFilter() {
        return null != targetFilter;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + sourceDatasetAddress
                + "=>" + targetDatasetAddress + (hasTargetFilter() ? ("~<" + targetFilter + ">") : "")
                + (partial ? "?" : "")
                + "]";
    }
}
