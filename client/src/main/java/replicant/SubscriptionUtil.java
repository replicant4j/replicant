package replicant;

import arez.Disposable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Utilities for integration across different datasources.
 */
public class SubscriptionUtil {
    private SubscriptionUtil() {}

    /**
     * Synchronize subscriptions across data sources.
     * All instances of the subscriptions to the source channelType within the scope are collected.
     * The supplied function is used to generate a stream of expected subscriptions to the target channelType
     * that are reachable from the source channelTypes. If an expected subscription is missing it is added,
     * if an additional subscription is present then it is released.
     */
    public static void synchronizeCrossDataSourceSubscriptions(
            final int sourceSystemId,
            final int sourceDatasetId,
            final int targetSystemId,
            final int targetDatasetId,
            @Nullable final Object filter,
            @NonNull final Function<Integer, Stream<Integer>> sourceIdToTargetIds) {
        // Need to check both subscription and filters are identical.
        // If they are not the next step will either update the filters or add subscriptions
        final ReplicantContext context = Replicant.context();
        final Map<Integer, AreaOfInterest> existing = context.getAreasOfInterest().stream()
                .filter(s -> s.getDatasetAddress().schemaId() == targetSystemId
                        && s.getDatasetAddress().datasetId() == targetDatasetId)
                .filter(subscription -> FilterUtil.filtersEqual(subscription.getFilter(), filter))
                .collect(Collectors.toMap(s -> s.getDatasetAddress().datasetRootId(), Function.identity()));

        context.getAreasOfInterest().stream()
                .filter(s -> s.getDatasetAddress().schemaId() == sourceSystemId
                        && s.getDatasetAddress().datasetId() == sourceDatasetId)
                .map(s -> s.getDatasetAddress().datasetRootId())
                .flatMap(sourceIdToTargetIds)
                .filter(Objects::nonNull)
                .filter(id -> null == existing.remove(id))
                .forEach(id -> context.createOrUpdateAreaOfInterest(
                        new DatasetAddress(targetSystemId, targetDatasetId, id), filter));

        context.getInstanceSubscriptionIds(sourceSystemId, sourceDatasetId).stream()
                .flatMap(sourceIdToTargetIds)
                .filter(Objects::nonNull)
                .filter(id -> null == existing.remove(id))
                .forEach(id -> context.createOrUpdateAreaOfInterest(
                        new DatasetAddress(targetSystemId, targetDatasetId, id), filter));

        existing.values().forEach(Disposable::dispose);
    }
}
