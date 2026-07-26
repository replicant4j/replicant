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
    public enum FilterType {
        /**
         * No filtering
         */
        NONE,
        /**
         * Filtering occurs but no parameter is passed to control such behaviour. Filtering rules are internal to the data.
         */
        INTERNAL,
        /**
         * Filtering occurs and the client passes a filter parameter but can never change the filter parameter without unsubscribing and resubscribing to the Dataset.
         */
        STATIC,
        /**
         * Filtering occurs and the client passes a filter parameter and can change the filter parameter.
         */
        DYNAMIC;

        /**
         * Return true if the filter is a dynamic parameter that can be updated.
         */
        public boolean isDynamicFilter() {
            return this == DYNAMIC;
        }

        /**
         * Return true if the filter is a static parameter.
         */
        public boolean isStaticFilter() {
            return this == STATIC;
        }

        /**
         * Return true if the filter requires a parameter to be passed to it.
         */
        public boolean hasFilterParameter() {
            return isDynamicFilter() || isStaticFilter();
        }
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
    private final FilterType _filterType;

    private final boolean _keyed;

    @NonNull
    private final CacheType _cacheType;
    /**
     * Flag indicating whether the Dataset can be subscribed to externally.
     * i.e. Can this be explicitly subscribed.
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
            @NonNull final FilterType filterType,
            final boolean keyed,
            @NonNull final CacheType cacheType,
            final boolean external,
            @NonNull final DatasetMetadata... requiredTypeDatasets) {
        _datasetId = datasetId;
        _name = Objects.requireNonNull(name);
        _datasetRootEntityTypeId = datasetRootEntityTypeId;
        _filterType = Objects.requireNonNull(filterType);
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

    public boolean requiresFilterParameter() {
        return filterType().hasFilterParameter();
    }

    public boolean requiresDatasetKey() {
        return _keyed;
    }

    @NonNull
    public Integer getDatasetRootEntityTypeId() {
        return Objects.requireNonNull(_datasetRootEntityTypeId);
    }

    @NonNull
    public FilterType filterType() {
        return _filterType;
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
