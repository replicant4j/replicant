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
        @Nullable JsonObject targetFilterParameter,
        boolean partial) {
    public SubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress, @NonNull final DatasetAddress targetDatasetAddress) {
        this(sourceDatasetAddress, targetDatasetAddress, null);
    }

    public SubscriptionDependency(
            @NonNull final DatasetAddress sourceDatasetAddress,
            @NonNull final DatasetAddress targetDatasetAddress,
            @Nullable final JsonObject targetFilterParameter) {
        this(sourceDatasetAddress, targetDatasetAddress, targetFilterParameter, false);
    }

    public SubscriptionDependency {
        assert partial || (!sourceDatasetAddress.partial() && !targetDatasetAddress.partial());
        assert !partial
                || sourceDatasetAddress.partial()
                || targetDatasetAddress.partial()
                || null == targetFilterParameter;
    }

    public boolean hasTargetFilterParameter() {
        return null != targetFilterParameter;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + sourceDatasetAddress
                + "=>" + targetDatasetAddress
                + (hasTargetFilterParameter() ? ("~<" + targetFilterParameter + ">") : "")
                + (partial ? "?" : "")
                + "]";
    }
}
