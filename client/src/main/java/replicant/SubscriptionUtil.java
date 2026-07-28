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
     * All Subscriptions to the source Dataset within the scope are collected.
     * The supplied function generates the expected Dataset Root identifiers for the target Dataset.
     * Missing Areas of Interest are added and additional Areas of Interest are released.
     */
    public static void synchronizeCrossDataSourceSubscriptions(
            final int sourceSystemSchemaId,
            final int sourceDatasetId,
            final int targetSystemSchemaId,
            final int targetDatasetId,
            @Nullable final Object filterParameter,
            @NonNull final Function<Integer, Stream<Integer>> sourceIdToTargetIds) {
        // Need to check both subscription and Filter Parameters are identical.
        // If they are not the next step will either update the Filter Parameters or add subscriptions
        final ReplicantContext context = Replicant.context();
        final Map<Integer, AreaOfInterest> existing = context.getAreasOfInterest().stream()
                .filter(s -> s.getDatasetAddress().systemSchemaId() == targetSystemSchemaId
                        && s.getDatasetAddress().datasetId() == targetDatasetId)
                .filter(subscription ->
                        FilterParameterUtil.filterParametersEqual(subscription.getFilterParameter(), filterParameter))
                .collect(Collectors.toMap(s -> s.getDatasetAddress().datasetRootId(), Function.identity()));

        context.getAreasOfInterest().stream()
                .filter(s -> s.getDatasetAddress().systemSchemaId() == sourceSystemSchemaId
                        && s.getDatasetAddress().datasetId() == sourceDatasetId)
                .map(s -> s.getDatasetAddress().datasetRootId())
                .flatMap(sourceIdToTargetIds)
                .filter(Objects::nonNull)
                .filter(id -> null == existing.remove(id))
                .forEach(id -> context.createOrUpdateAreaOfInterest(
                        new DatasetAddress(targetSystemSchemaId, targetDatasetId, id), filterParameter));

        context.getSubscribedDatasetRootIds(sourceSystemSchemaId, sourceDatasetId).stream()
                .flatMap(sourceIdToTargetIds)
                .filter(Objects::nonNull)
                .filter(id -> null == existing.remove(id))
                .forEach(id -> context.createOrUpdateAreaOfInterest(
                        new DatasetAddress(targetSystemSchemaId, targetDatasetId, id), filterParameter));

        existing.values().forEach(Disposable::dispose);
    }
}
