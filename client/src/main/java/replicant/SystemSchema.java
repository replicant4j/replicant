package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The catalog of Dataset and Entity Type definitions belonging to one isolated replicated system.
 */
public final class SystemSchema {
    /**
     * The identifier that distinguishes the System Schema within a Replicant Context.
     */
    private final int _id;
    /**
     * A human consumable name for the System Schema. It should be non-null if {@link Replicant#areNamesEnabled()} returns
     * true and <tt>null</tt> otherwise.
     */
    @Nullable
    private final String _name;

    @Nullable
    private final OnReplicaUpdateAction _onReplicaUpdateAction;
    /**
     * The Datasets in the System Schema.
     */
    @NonNull
    private final Dataset[] _datasets;
    /**
     * The Entity Types in the System Schema.
     */
    @NonNull
    private final EntityType[] _entityTypes;

    public SystemSchema(
            final int id,
            @Nullable final String name,
            @NonNull final Dataset[] datasets,
            @NonNull final EntityType[] entityTypes) {
        this(id, name, null, datasets, entityTypes);
    }

    public SystemSchema(
            final int id,
            @Nullable final String name,
            @Nullable final OnReplicaUpdateAction onReplicaUpdateAction,
            @NonNull final Dataset[] datasets,
            @NonNull final EntityType[] entityTypes) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0051: SystemSchema passed a name '" + name
                            + "' but Replicant.areNamesEnabled() is false");
            apiInvariant(
                    () -> Arrays.stream(entityTypes).allMatch(Objects::nonNull),
                    () -> "Replicant-0053: SystemSchema named '" + (null == name ? "?" : name)
                            + "' passed an array of entity types that has a null element");
            for (int i = 0; i < entityTypes.length; i++) {
                final int index = i;
                apiInvariant(
                        () -> index == entityTypes[index].getEntityTypeId(),
                        () -> "Replicant-0054: SystemSchema named '" + (null == name ? "?" : name)
                                + "' passed an array of Entity Types where Entity Type at index "
                                + index + " does not have an Entity Type ID matching the index.");
            }
            for (int i = 0; i < datasets.length; i++) {
                final int index = i;
                apiInvariant(
                        () -> null == datasets[index] || index == datasets[index].getId(),
                        () -> "Replicant-0056: SystemSchema named '" + (null == name ? "?" : name)
                                + "' passed an array of Datasets where Dataset at index "
                                + index + " does not " + "have id matching index.");
            }
        }
        _id = id;
        _name = Replicant.areNamesEnabled() ? Objects.requireNonNull(name) : null;
        _onReplicaUpdateAction = onReplicaUpdateAction;
        _entityTypes = Objects.requireNonNull(entityTypes);
        _datasets = Objects.requireNonNull(datasets);
    }

    /**
     * Return the System Schema identifier.
     *
     * @return the System Schema identifier.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the name of the System Schema.
     * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
     * exception if invariant checking is enabled.
     *
     * @return the name of the System Schema.
     */
    @NonNull
    public String getName() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areNamesEnabled,
                    () -> "Replicant-0052: SystemSchema.getName() invoked when Replicant.areNamesEnabled() is false");
        }
        return Objects.requireNonNull(_name);
    }

    @Nullable
    public OnReplicaUpdateAction getOnReplicaUpdateAction() {
        return _onReplicaUpdateAction;
    }

    /**
     * Return the number of Entity Types in the System Schema.
     *
     * @return the number of Entity Types in the System Schema.
     */
    public int getEntityTypeCount() {
        return _entityTypes.length;
    }

    /**
     * Return the Entity Type with the specified Entity Type ID.
     * The Entity Type ID MUST be 0 or more and less than {@link #getEntityTypeCount()}.
     *
     * @param entityTypeId the Entity Type ID.
     * @return the Entity Type matching the Entity Type ID.
     */
    @NonNull
    public EntityType getEntityType(final int entityTypeId) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> entityTypeId >= 0 && entityTypeId < _entityTypes.length,
                    () -> "Replicant-0057: SystemSchema.getEntityType passed an Entity Type ID that is out of range.");
        }
        return _entityTypes[entityTypeId];
    }

    /**
     * Return the number of Datasets in the System Schema.
     *
     * @return the number of Datasets in the System Schema.
     */
    public int getDatasetCount() {
        return _datasets.length;
    }

    /**
     * Return true if the System Schema contains a Dataset with the specified datasetId.
     */
    public boolean hasDataset(final int datasetId) {
        return datasetId >= 0 && datasetId < _datasets.length && null != _datasets[datasetId];
    }

    /**
     * Return the Dataset with specified datasetId.
     * The datasetId MUST be 0 or more and less than {@link #getDatasetCount()}.
     *
     * @param datasetId the Dataset id.
     * @return the Dataset matching datasetId.
     */
    @NonNull
    public Dataset getDataset(final int datasetId) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> datasetId >= 0 && datasetId < _datasets.length,
                    () -> "Replicant-0058: SystemSchema.getDataset(id) passed an id that is out of range.");
            apiInvariant(
                    () -> null != _datasets[datasetId],
                    () -> "Replicant-0008: SystemSchema.getDataset(id) attempted to access null Dataset.");
        }
        return _datasets[datasetId];
    }

    @NonNull
    public List<DatasetLink> getInwardDatasetLinks(final int datasetId, final int entityTypeId) {
        return Stream.of(_datasets)
                .filter(Objects::nonNull)
                .flatMap(dataset -> dataset.getEntityTypes().stream()
                        .filter(entityType -> entityType.getEntityTypeId() == entityTypeId)
                        .flatMap(entityType -> Stream.of(entityType.getDatasetLinks())
                                .filter(datasetLink -> datasetLink.getTargetDatasetId() == datasetId)))
                .distinct()
                .collect(Collectors.toList());
    }

    @NonNull
    public List<DatasetLink> getInwardDatasetLinks(final int datasetId) {
        return Stream.of(_datasets)
                .filter(Objects::nonNull)
                .flatMap(dataset -> dataset.getEntityTypes().stream()
                        .flatMap(entityType -> Stream.of(entityType.getDatasetLinks())
                                .filter(datasetLink -> datasetLink.getTargetDatasetId() == datasetId)))
                .distinct()
                .collect(Collectors.toList());
    }

    @NonNull
    public List<DatasetLink> getOutwardDatasetLinks(final int datasetId) {
        return getDataset(datasetId).getEntityTypes().stream()
                .flatMap(entityType -> entityType.getOutwardDatasetLinks(datasetId).stream())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return getName();
        } else {
            return super.toString();
        }
    }
}
