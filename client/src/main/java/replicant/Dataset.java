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
     * The permitted origins of Subscriptions to the Dataset.
     */
    public enum Visibility {
        /// An Area of Interest may request the Dataset directly.
        EXTERNAL,
        /// The Dataset may be reached through a Dataset Link or Required Type Dataset.
        INTERNAL,
        /// Both Area of Interest and Dataset Link or Required Type Dataset origins are permitted.
        UNIVERSAL;

        /**
         * Return whether an Area of Interest may request the Dataset directly.
         *
         * @return true when an Area of Interest origin is permitted.
         */
        public boolean permitsAreaOfInterestOrigin() {
            return INTERNAL != this;
        }

        /**
         * Return whether the Dataset may be reached through a Dataset Link or Required Type Dataset.
         *
         * @return true when a Dataset Link or Required Type Dataset origin is permitted.
         */
        public boolean permitsDatasetLinkOrRequiredTypeDatasetOrigin() {
            return EXTERNAL != this;
        }
    }

    /**
     * The type of filtering applied to the Dataset.
     */
    public enum FilterMode {
        /// No filtering.
        UNFILTERED,
        /// Filtering with system-supplied rules and inputs.
        IMPLICIT,
        /// Filtering that consumes a subscriber-supplied Filter Parameter.
        PARAMETER_FILTERED
    }

    /**
     * The mutability of the Filter Parameter.
     */
    public enum FilterParameterMode {
        /// The Filter Parameter can not change while the Subscription persists.
        FIXED,
        /// The Filter Parameter can change while the Subscription persists.
        UPDATABLE
    }

    /**
     * The compact Dataset identifier used in runtime metadata, Dataset Addresses, and transport messages.
     */
    private final int _id;
    /**
     * A human-readable Dataset name used for diagnostics. It should be non-null if
     * {@link Replicant#areNamesEnabled()} returns true and <tt>null</tt> otherwise.
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
    private final FilterMode _filterMode;
    /**
     * The mutability of the Filter Parameter, or null when the Dataset is not parameter-filtered.
     */
    @Nullable
    private final FilterParameterMode _filterParameterMode;
    /**
     * True when independently addressable selections of this Dataset require a Dataset Key.
     */
    private final boolean _keyed;
    /**
     * The hook to re-evaluate Replica membership after an updatable Filter Parameter changes.
     */
    @Nullable
    private final FilterParameterUpdateReplicaMatcher<?> _filterParameterUpdateReplicaMatcher;
    /**
     * A flag indicating whether this is a Cacheable Dataset.
     */
    private final boolean _cacheable;
    /**
     * The permitted origins of Subscriptions to the Dataset.
     */
    @NonNull
    private final Visibility _visibility;
    /**
     * The Entity Types included within the Dataset.
     */
    private final List<EntityType> _entityTypes;

    public Dataset(
            final int id,
            @Nullable final String name,
            @Nullable final Class<?> datasetRootEntityType,
            @NonNull final FilterMode filterMode,
            @Nullable final FilterParameterMode filterParameterMode,
            final boolean keyed,
            @Nullable final FilterParameterUpdateReplicaMatcher<?> filterParameterUpdateReplicaMatcher,
            final boolean cacheable,
            @NonNull final Visibility visibility,
            @NonNull final List<EntityType> entityTypes) {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> Replicant.areNamesEnabled() || null == name,
                    () -> "Replicant-0045: Dataset passed a name '" + name
                            + "' but Replicant.areNamesEnabled() is false");
            apiInvariant(
                    () -> FilterMode.PARAMETER_FILTERED == filterMode || null == filterParameterMode,
                    () -> "Replicant-0100: Dataset " + id + " is not parameter-filtered "
                            + "but has supplied a Filter Parameter mode.");
            apiInvariant(
                    () -> FilterMode.PARAMETER_FILTERED != filterMode || null != filterParameterMode,
                    () -> "Replicant-0101: Dataset " + id + " is parameter-filtered "
                            + "but has supplied no Filter Parameter mode.");
            apiInvariant(
                    () -> !keyed || FilterMode.PARAMETER_FILTERED == filterMode,
                    () -> "Replicant-0102: Dataset " + id + " is keyed but is not parameter-filtered.");
            apiInvariant(
                    () -> FilterParameterMode.UPDATABLE != filterParameterMode
                            || null != filterParameterUpdateReplicaMatcher,
                    () -> "Replicant-0076: Dataset " + id + " has an updatable Filter Parameter "
                            + "but has supplied no Filter Parameter update Replica matcher.");
            apiInvariant(
                    () -> FilterParameterMode.UPDATABLE == filterParameterMode
                            || null == filterParameterUpdateReplicaMatcher,
                    () -> "Replicant-0077: Dataset " + id + " does not have an updatable Filter Parameter "
                            + "but has supplied a Filter Parameter update Replica matcher.");
        }
        _id = id;
        _name = Replicant.areNamesEnabled() ? Objects.requireNonNull(name) : null;
        _datasetRootEntityType = datasetRootEntityType;
        _filterMode = Objects.requireNonNull(filterMode);
        _filterParameterMode = filterParameterMode;
        _keyed = keyed;
        _filterParameterUpdateReplicaMatcher = filterParameterUpdateReplicaMatcher;
        _cacheable = cacheable;
        _visibility = Objects.requireNonNull(visibility);
        _entityTypes = entityTypes;
    }

    /**
     * Return the compact runtime ID of the Dataset definition.
     *
     * @return the Dataset ID.
     */
    public int getId() {
        return _id;
    }

    /**
     * Return the human-readable name of the Dataset.
     * This method should NOT be invoked unless {@link Replicant#areNamesEnabled()} returns true and will throw an
     * exception if invariant checking is enabled.
     *
     * @return the Dataset name.
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
     * Return the mode of filtering applied to the Dataset.
     *
     * @return the mode of filtering applied to the Dataset.
     */
    @NonNull
    public FilterMode getFilterMode() {
        return _filterMode;
    }

    public boolean isUnfiltered() {
        return FilterMode.UNFILTERED == _filterMode;
    }

    public boolean isImplicitlyFiltered() {
        return FilterMode.IMPLICIT == _filterMode;
    }

    public boolean isParameterFiltered() {
        return FilterMode.PARAMETER_FILTERED == _filterMode;
    }

    @Nullable
    public FilterParameterMode getFilterParameterMode() {
        return _filterParameterMode;
    }

    public boolean hasFixedFilterParameter() {
        return FilterParameterMode.FIXED == _filterParameterMode;
    }

    public boolean hasUpdatableFilterParameter() {
        return FilterParameterMode.UPDATABLE == _filterParameterMode;
    }

    /**
     * Return true if independently addressable selections of this Dataset require a Dataset Key.
     *
     * @return true if independently addressable selections of this Dataset require a Dataset Key.
     */
    public boolean isKeyed() {
        return _keyed;
    }

    /**
     * Return the hook that re-evaluates Replica membership after an updatable Filter Parameter changes.
     *
     * @return the Replica membership matcher.
     */
    @Nullable
    public FilterParameterUpdateReplicaMatcher<?> getFilterParameterUpdateReplicaMatcher() {
        return _filterParameterUpdateReplicaMatcher;
    }

    /**
     * Return true if this is a Cacheable Dataset.
     *
     * @return true if this is a Cacheable Dataset.
     */
    public boolean isCacheable() {
        return _cacheable;
    }

    /**
     * Return the Dataset Visibility that controls how Subscriptions may originate.
     *
     * @return the Dataset Visibility.
     */
    @NonNull
    public Visibility getVisibility() {
        return _visibility;
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
    public EntityType findEntityTypeByEntityTypeId(final int entityTypeId) {
        return _entityTypes.stream()
                .filter(entityType -> entityType.getEntityTypeId() == entityTypeId)
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
