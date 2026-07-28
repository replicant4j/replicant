package replicant.server.transport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A reusable definition of a replicable population and its filtering behaviour.
 */
@SuppressWarnings("WeakerAccess")
public final class Dataset {
    /**
     * The permitted origins of Subscriptions to the Dataset.
     */
    public enum Visibility {
        /**
         * An Area of Interest may request the Dataset directly.
         */
        EXTERNAL,
        /**
         * The Dataset may be reached through a Dataset Link or Required Type Dataset.
         */
        INTERNAL,
        /**
         * Both Area of Interest and Dataset Link or Required Type Dataset origins are permitted.
         */
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

    public enum FilterMode {
        /**
         * No filtering
         */
        UNFILTERED,
        /**
         * Filtering occurs without a Filter Parameter; the system supplies the rule and its inputs.
         */
        IMPLICIT,
        /**
         * Filtering occurs and the subscriber supplies a Filter Parameter.
         */
        PARAMETER_FILTERED
    }

    public enum FilterParameterMode {
        /**
         * The Filter Parameter cannot change while the Subscription persists.
         */
        FIXED,
        /**
         * The Filter Parameter can change while the Subscription persists.
         */
        UPDATABLE
    }

    /**
     * The compact Dataset identifier used in runtime metadata, Dataset Addresses, and transport messages.
     */
    private final int _id;

    /**
     * The human-readable Dataset name used for diagnostics and Routing Key lookup.
     */
    @NonNull
    private final String _name;

    @Nullable
    private final Integer _datasetRootEntityTypeId;

    @NonNull
    private final FilterMode _filterMode;

    @Nullable
    private final FilterParameterMode _filterParameterMode;

    private final boolean _keyed;

    /**
     * True if this is a Cacheable Dataset. The Dataset declaration asserts that one shared Change Set is equal for
     * every authorized subscriber able to reuse it and that subscriber-specific inputs participate in Dataset Cache
     * Entry identity and validation. Relevant Entity Changes invalidate the entry and its dependent entries before
     * further reuse.
     */
    private final boolean _cacheable;
    /**
     * The permitted origins of Subscriptions to the Dataset.
     */
    @NonNull
    private final Visibility _visibility;

    @NonNull
    private final Dataset[] _requiredTypeDatasets;

    @NonNull
    private final Set<Dataset> _dependentDatasets = new HashSet<>();

    public Dataset(
            final int id,
            @NonNull final String name,
            @Nullable final Integer datasetRootEntityTypeId,
            @NonNull final FilterMode filterMode,
            @Nullable final FilterParameterMode filterParameterMode,
            final boolean keyed,
            final boolean cacheable,
            @NonNull final Visibility visibility,
            @NonNull final Dataset... requiredTypeDatasets) {
        _id = id;
        _name = Objects.requireNonNull(name);
        _datasetRootEntityTypeId = datasetRootEntityTypeId;
        _filterMode = Objects.requireNonNull(filterMode);
        if (isParameterFiltered()) {
            if (null == filterParameterMode) {
                throw new IllegalArgumentException("Parameter-Filtered Dataset requires a Filter Parameter Mode");
            }
        } else if (null != filterParameterMode) {
            throw new IllegalArgumentException("Filter Parameter Mode is only valid for a Parameter-Filtered Dataset");
        }
        _filterParameterMode = filterParameterMode;
        if (keyed && !isParameterFiltered()) {
            throw new IllegalArgumentException("Only a Parameter-Filtered Dataset can be keyed");
        }
        _keyed = keyed;
        _cacheable = cacheable;
        _visibility = Objects.requireNonNull(visibility);
        _requiredTypeDatasets = Objects.requireNonNull(requiredTypeDatasets);
        for (final var requiredTypeDataset : _requiredTypeDatasets) {
            if (requiredTypeDataset.isInstanceDataset()) {
                throw new IllegalArgumentException(
                        "Specified Required Type Dataset " + requiredTypeDataset.getName() + " is not a Type Dataset");
            }
            if (!requiredTypeDataset.getVisibility().permitsDatasetLinkOrRequiredTypeDatasetOrigin()) {
                throw new IllegalArgumentException("Specified Required Type Dataset "
                        + requiredTypeDataset.getName()
                        + " has "
                        + requiredTypeDataset.getVisibility()
                        + " Dataset Visibility, which does not permit a Required Type Dataset origin");
            }
            requiredTypeDataset._dependentDatasets.add(this);
        }
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
     * Return the human-readable Dataset name.
     *
     * @return the Dataset name.
     */
    @NonNull
    public String getName() {
        return _name;
    }

    public boolean isTypeDataset() {
        return null == _datasetRootEntityTypeId;
    }

    public boolean isInstanceDataset() {
        return !isTypeDataset();
    }

    public boolean isUnfiltered() {
        return FilterMode.UNFILTERED == getFilterMode();
    }

    public boolean isImplicitlyFiltered() {
        return FilterMode.IMPLICIT == getFilterMode();
    }

    public boolean isParameterFiltered() {
        return FilterMode.PARAMETER_FILTERED == getFilterMode();
    }

    public boolean hasFixedFilterParameter() {
        return FilterParameterMode.FIXED == getFilterParameterMode();
    }

    public boolean hasUpdatableFilterParameter() {
        return FilterParameterMode.UPDATABLE == getFilterParameterMode();
    }

    public boolean isKeyed() {
        return _keyed;
    }

    @NonNull
    public Integer getDatasetRootEntityTypeId() {
        return Objects.requireNonNull(_datasetRootEntityTypeId);
    }

    @NonNull
    public FilterMode getFilterMode() {
        return _filterMode;
    }

    @Nullable
    public FilterParameterMode getFilterParameterMode() {
        return _filterParameterMode;
    }

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
     * Return the Type Datasets that this Dataset requires unconditionally.
     *
     * @return the Required Type Datasets
     */
    @NonNull
    public Dataset[] getRequiredTypeDatasets() {
        return _requiredTypeDatasets;
    }

    /**
     * Return the Datasets that unconditionally require this Type Dataset.
     *
     * @return the dependent Datasets
     */
    @Contract(pure = true)
    @NonNull
    public @UnmodifiableView Set<Dataset> getDependentDatasets() {
        return Collections.unmodifiableSet(_dependentDatasets);
    }
}
