package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A runtime relationship indicating that the Subscription at the source Dataset Address requires the Subscription at
 * the target Dataset Address.
 */
public record SubscriptionDependency(
        @NonNull DatasetAddress sourceDatasetAddress,
        @NonNull DatasetAddress targetDatasetAddress,
        @Nullable JsonObject targetFilter,
        boolean partial) {
    public SubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress, @NonNull final DatasetAddress targetDatasetAddress) {
        this(sourceDatasetAddress, targetDatasetAddress, null);
    }

    public SubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject targetFilter) {
        this(sourceDatasetAddress, targetDatasetAddress, targetFilter, false);
    }

    public SubscriptionDependency {
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
