package replicant.server;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A possible Subscription Dependency emitted before Dataset Address Templates and Filter Parameters are resolved.
 */
public record SubscriptionDependencyCandidate(
        @NonNull DatasetAddressCandidate sourceDatasetAddressCandidate,
        @NonNull DatasetAddressCandidate targetDatasetAddressCandidate,
        @Nullable JsonObject targetFilterParameter) {
    public SubscriptionDependencyCandidate(
            @NonNull final DatasetAddressCandidate sourceDatasetAddressCandidate,
            @NonNull final DatasetAddressCandidate targetDatasetAddressCandidate) {
        this(sourceDatasetAddressCandidate, targetDatasetAddressCandidate, null);
    }

    public SubscriptionDependencyCandidate(
            @NonNull final DatasetAddressCandidate sourceDatasetAddressCandidate,
            @NonNull final DatasetAddressCandidate targetDatasetAddressCandidate,
            @Nullable final JsonObject targetFilterParameter) {
        this.sourceDatasetAddressCandidate = sourceDatasetAddressCandidate;
        this.targetDatasetAddressCandidate = targetDatasetAddressCandidate;
        this.targetFilterParameter = targetFilterParameter;
    }

    public boolean hasTargetFilterParameter() {
        return null != targetFilterParameter;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + sourceDatasetAddressCandidate
                + "=>" + targetDatasetAddressCandidate
                + (hasTargetFilterParameter() ? ("~<" + targetFilterParameter + ">") : "")
                + "]";
    }
}
