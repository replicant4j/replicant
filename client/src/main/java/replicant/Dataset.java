package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Describes a Dataset within a System Schema.
 */
public final class Dataset {
    /**
     * The type of filtering applied to the Dataset.
     */
    public enum FilterType {
        /// No filtering
        NONE,
        // Filtering occurs but no parameter is passed to control such behaviour. Filtering rules are internal to the
        // data.
        INTERNAL,
        /// Filtering is specified when the Subscription is created and is unable to be changed
        STATIC,
        /// Filtering can be changed after the Subscription has been created
        DYNAMIC
    }

    /**
     * The id of the Dataset. This is the value used when transmitting messages across network.
     */
    private final int _id;
    /**
     * A human consumable name for the Dataset. It should be non-null if {@link Replicant#areNamesEnabled()} returns
     * true and <tt>null</tt> otherwise.
     */
    @Nullable
    private final String _name;
    /**
     * The Dataset Root Entity Type for an Instance Dataset, or null for a Type Dataset.
     */
    @Nullable
    private final Class<?> _datasetRootEntityType;
    /**
     * The filtering applied to the Dataset.
     */
    @NonNull
    private final FilterType _filterType;
    /**
     * True when independently addressable selections of this Dataset require a Dataset Key.
     */
    private final boolean _keyed;
    /**
     * The hook to filter Replica Entries when the filter changes. This should be null unless {@link #_filterType} is
     * {@link FilterType#DYNAMIC}.
     */
    @Nullable
    private final SubscriptionUpdateReplicaFilter<?> _filter;
    /**
     * A flag indicating whether the results of the Dataset can be cached.
     */
    private final boolean _cacheable;
    /**
     * Flag indicating whether the Dataset should able to be subscribed to externally.
     * i.e. Can this be explicitly subscribed.
     */
    private final boolean _external;
    /**
     * The Entity Types included within the Dataset.
     */
    private final List<EntityType> _entityTypes;

    public Dataset(
            final int id,
            @Nullable final String name,
            @Nullable final Class<?> datasetRootEntityType,
            @NonNull final FilterType filterType,
            final boolean keyed,
            @Nullable final SubscriptionUpdateReplicaFilter<?> filter,
            final boolean cacheable,
            final boolean external,
            @NonNull final List<EntityType> entityTypes) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0045: Dataset passed a name '" + name
                            + "' but Replicant.areNamesEnabled() is false");
            apiInvariant(
                    () -> FilterType.DYNAMIC != filterType || null != filter,
                    () -> "Replicant-0076: Dataset " + id + " has a DYNAMIC filterType "
                            + "but has supplied no filter.");
            apiInvariant(
                    () -> FilterType.DYNAMIC == filterType || null == filter,
                    () -> "Replicant-0077: Dataset " + id + " does not have a DYNAMIC filterType "
                            + "but has supplied a filter.");
        }
        _id = id;
        _name = Replicant.areNamesEnabled() ? Objects.requireNonNull(name) : null;
        _datasetRootEntityType = datasetRootEntityType;
        _filterType = Objects.requireNonNull(filterType);
        _keyed = keyed;
        _filter = filter;
        _cacheable = cacheable;
        _external = external;
        _entityTypes = entityTypes;
    }

    /**
     * Return the id of the Dataset.
     *
     * @return the id of the Dataset.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the name of the Dataset.
     * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
     * exception if invariant checking is enabled.
     *
     * @return the name of the Dataset.
     */
    @NonNull
    public String getName() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areNamesEnabled,
                    () -> "Replicant-0044: Dataset.getName() invoked when Replicant.areNamesEnabled() is false");
        }
        return Objects.requireNonNull(_name);
    }

    /**
     * Return true if this is a Type Dataset.
     *
     * @return true if this is a Type Dataset.
     */
    public boolean isTypeDataset() {
        return null == _datasetRootEntityType;
    }

    /**
     * Return true if this is an Instance Dataset.
     *
     * @return true if this is an Instance Dataset.
     */
    public boolean isInstanceDataset() {
        return !isTypeDataset();
    }

    /**
     * Return the Dataset Root Entity Type for an Instance Dataset.
     *
     * @return the Dataset Root Entity Type, or null for a Type Dataset.
     */
    @Nullable
    public Class<?> getDatasetRootEntityType() {
        return _datasetRootEntityType;
    }

    /**
     * Return the type of filtering applied to the Dataset.
     *
     * @return the type of filtering applied to the Dataset.
     */
    @NonNull
    public FilterType getFilterType() {
        return _filterType;
    }

    /**
     * Return true if independently addressable selections of this Dataset require a Dataset Key.
     *
     * @return true if independently addressable selections of this Dataset require a Dataset Key.
     */
    public boolean requiresDatasetKey() {
        return _keyed;
    }

    /**
     * Return the hook that filters entities when the filter changes.
     * This will not be null if and only if {@link #_filterType} is {@link FilterType#DYNAMIC}.
     *
     * @return the hook to filter entities.
     */
    @Nullable
    public SubscriptionUpdateReplicaFilter<?> getFilter() {
        return _filter;
    }

    /**
     * Return a flag indicating whether the results of the Dataset can be cached.
     *
     * @return a flag indicating whether the results of the Dataset can be cached.
     */
    public boolean isCacheable() {
        return _cacheable;
    }

    /**
     * Return the flag indicating whether the Dataset should able to be subscribed to externally.
     *
     * @return the flag indicating whether the Dataset should able to be subscribed to externally.
     */
    public boolean isExternal() {
        return _external;
    }

    /**
     * Return the Entity Types selected by the Dataset.
     *
     * @return the Entity Types selected by the Dataset.
     */
    @NonNull
    public List<EntityType> getEntityTypes() {
        return CollectionsUtil.wrap(_entityTypes);
    }

    /**
     * Return the entity type with specified id, if any.
     *
     * @param entityTypeId the id of the entity type to find.
     * @return the entity type with specified id, if any.
     */
    @Nullable
    public EntityType findEntityTypeById(final int entityTypeId) {
        return _entityTypes.stream()
                .filter(entityType -> entityType.getId() == entityTypeId)
                .findAny()
                .orElse(null);
    }

    @NonNull
    public List<DatasetLink> getOutwardDatasetLinks() {
        return getEntityTypes().stream()
                .flatMap(entityType -> Stream.of(entityType.getDatasetLinks())
                        .filter(datasetLink -> datasetLink.getSourceDatasetId() == getId()))
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
