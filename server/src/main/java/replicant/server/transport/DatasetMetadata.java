package replicant.server.transport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.UnmodifiableView;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public final class DatasetMetadata {
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

    public enum CacheType {
        /**
         * No caching
         */
        NONE,
        /**
         * Caching is managed internally by Replicant. If a change arrives for an Entity in the Dataset then the
         * cache is expired.
         */
        INTERNAL
    }

    private final int _datasetId;

    @NonNull
    private final String _name;

    @Nullable
    private final Integer _datasetRootEntityTypeId;

    @NonNull
    private final FilterMode _filterMode;

    @Nullable
    private final FilterParameterMode _filterParameterMode;

    private final boolean _keyed;

    @NonNull
    private final CacheType _cacheType;
    /**
     * Flag indicating whether the Dataset can be backed by an externally supplied Area of Interest.
     */
    private final boolean _external;

    @NonNull
    private final DatasetMetadata[] _requiredTypeDatasets;

    @NonNull
    private final Set<DatasetMetadata> _dependentDatasets = new HashSet<>();

    public DatasetMetadata(
            final int datasetId,
            @NonNull final String name,
            @Nullable final Integer datasetRootEntityTypeId,
            @NonNull final FilterMode filterMode,
            @Nullable final FilterParameterMode filterParameterMode,
            final boolean keyed,
            @NonNull final CacheType cacheType,
            final boolean external,
            @NonNull final DatasetMetadata... requiredTypeDatasets) {
        _datasetId = datasetId;
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
        _cacheType = Objects.requireNonNull(cacheType);
        _external = external;
        _requiredTypeDatasets = Objects.requireNonNull(requiredTypeDatasets);
        for (final var requiredTypeDataset : _requiredTypeDatasets) {
            if (requiredTypeDataset.isInstanceDataset()) {
                throw new IllegalArgumentException(
                        "Specified Required Type Dataset " + requiredTypeDataset.getName() + " is not a Type Dataset");
            }
            requiredTypeDataset._dependentDatasets.add(this);
        }
    }

    public int getDatasetId() {
        return _datasetId;
    }

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
        return CacheType.NONE != _cacheType;
    }

    @NonNull
    public CacheType getCacheType() {
        return _cacheType;
    }

    public boolean isExternal() {
        return _external;
    }

    /**
     * Return the Type Datasets that this Dataset requires unconditionally.
     *
     * @return the Required Type Datasets
     */
    @NonNull
    public DatasetMetadata[] getRequiredTypeDatasets() {
        return _requiredTypeDatasets;
    }

    /**
     * Return the Datasets that unconditionally require this Type Dataset.
     *
     * @return the dependent Datasets
     */
    @Contract(pure = true)
    @NonNull
    public @UnmodifiableView Set<DatasetMetadata> getDependentDatasets() {
        return Collections.unmodifiableSet(_dependentDatasets);
    }
}
