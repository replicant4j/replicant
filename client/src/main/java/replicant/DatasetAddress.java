package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The immutable identity of a subscribable Dataset selection.
 *
 * <p>A Dataset Address combines the System Schema and Dataset identifiers with a Dataset Root identifier and Dataset
 * Key when required.
 */
public final class DatasetAddress implements Comparable<DatasetAddress> {
    private final int _systemSchemaId;
    private final int _datasetId;

    @Nullable
    private final Integer _datasetRootId;

    @Nullable
    private final String _datasetKey;

    public DatasetAddress(final int systemSchemaId, final int datasetId) {
        this(systemSchemaId, datasetId, null, null);
    }

    public DatasetAddress(final int systemSchemaId, final int datasetId, @Nullable final Integer datasetRootId) {
        this(systemSchemaId, datasetId, datasetRootId, null);
    }

    public DatasetAddress(
            final int systemSchemaId,
            final int datasetId,
            @Nullable final Integer datasetRootId,
            @Nullable final String datasetKey) {
        _systemSchemaId = systemSchemaId;
        _datasetId = datasetId;
        _datasetRootId = datasetRootId;
        _datasetKey = datasetKey;
    }

    public int systemSchemaId() {
        return _systemSchemaId;
    }

    public int datasetId() {
        return _datasetId;
    }

    @Nullable
    public Integer datasetRootId() {
        return _datasetRootId;
    }

    @Nullable
    public String datasetKey() {
        return _datasetKey;
    }

    @Override
    public String toString() {
        return Replicant.areNamesEnabled() ? getName() : super.toString();
    }

    @NonNull
    public String getName() {
        return systemSchemaId() + "." + asDatasetAddressDescriptor();
    }

    @NonNull
    public String asDatasetAddressDescriptor() {
        final StringBuilder sb = new StringBuilder().append(datasetId());
        if (null != _datasetRootId) {
            sb.append(".").append(_datasetRootId);
        }
        if (null != _datasetKey) {
            sb.append("#").append(_datasetKey);
        }
        return sb.toString();
    }

    @NonNull
    public String getDatasetCacheEntryStorageKey() {
        return "RC-" + getName();
    }

    @NonNull
    public static DatasetAddress parse(final int systemSchemaId, @NonNull final String datasetAddressDescriptor) {
        final int datasetKeyOffset = datasetAddressDescriptor.indexOf('#');
        final String datasetAddressPart = -1 == datasetKeyOffset
                ? datasetAddressDescriptor
                : datasetAddressDescriptor.substring(0, datasetKeyOffset);
        final String datasetKey =
                -1 == datasetKeyOffset ? null : datasetAddressDescriptor.substring(datasetKeyOffset + 1);
        final int offset = datasetAddressPart.indexOf(".", 1);
        final int datasetId =
                Integer.parseInt(-1 == offset ? datasetAddressPart : datasetAddressPart.substring(0, offset));
        final Integer datasetRootId = -1 == offset ? null : Integer.parseInt(datasetAddressPart.substring(offset + 1));
        return new DatasetAddress(systemSchemaId, datasetId, datasetRootId, datasetKey);
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final DatasetAddress that = (DatasetAddress) o;
            return Objects.equals(_systemSchemaId, that._systemSchemaId)
                    && Objects.equals(_datasetId, that._datasetId)
                    && Objects.equals(_datasetRootId, that._datasetRootId)
                    && Objects.equals(_datasetKey, that._datasetKey);
        }
    }

    @Override
    public int hashCode() {
        int result = _systemSchemaId;
        result = 17 * result + _datasetId;
        result = 31 * result + (_datasetRootId != null ? _datasetRootId.hashCode() : 0);
        result = 31 * result + (_datasetKey != null ? _datasetKey.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NonNull final DatasetAddress o) {
        final int systemSchemaDiff = Integer.compare(systemSchemaId(), o.systemSchemaId());
        if (0 != systemSchemaDiff) {
            return systemSchemaDiff;
        } else {
            final int datasetDiff = Integer.compare(datasetId(), o.datasetId());
            if (0 != datasetDiff) {
                return datasetDiff;
            } else {
                // Align ordering with equals by comparing datasetRootId as well
                final Integer r1 = datasetRootId();
                final Integer r2 = o.datasetRootId();
                if (null != r1 || null != r2) {
                    if (null == r1) {
                        return -1;
                    } else if (null == r2) {
                        return 1;
                    } else {
                        final int rootDiff = Integer.compare(r1, r2);
                        if (0 != rootDiff) {
                            return rootDiff;
                        }
                    }
                }
                final String f1 = datasetKey();
                final String f2 = o.datasetKey();
                if (null == f1 && null == f2) {
                    return 0;
                } else if (null == f1) {
                    return -1;
                } else if (null == f2) {
                    return 1;
                } else {
                    return f1.compareTo(f2);
                }
            }
        }
    }
}
