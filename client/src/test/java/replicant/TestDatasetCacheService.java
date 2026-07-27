package replicant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

public class TestDatasetCacheService implements DatasetCacheService {
    @NonNull
    private final Map<Integer, Map<DatasetAddress, DatasetCacheEntry>> _datasetCacheEntriesBySystemSchema =
            new HashMap<>();

    @NonNull
    private Map<DatasetAddress, DatasetCacheEntry> getDatasetCacheEntries(final int systemSchemaId) {
        return _datasetCacheEntriesBySystemSchema.computeIfAbsent(systemSchemaId, v -> new HashMap<>());
    }

    @NonNull
    @Override
    public Set<DatasetAddress> getDatasetAddresses(final int systemSchemaId) {
        return CollectionsUtil.wrap(getDatasetCacheEntries(systemSchemaId).keySet());
    }

    @Nullable
    @Override
    public String lookupDatasetCacheVersion(@NonNull final DatasetAddress datasetAddress) {
        final DatasetCacheEntry entry =
                getDatasetCacheEntries(datasetAddress.systemSchemaId()).get(datasetAddress);
        return null != entry ? entry.getDatasetCacheVersion() : null;
    }

    @Nullable
    @Override
    public DatasetCacheEntry lookupDatasetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        return getDatasetCacheEntries(datasetAddress.systemSchemaId()).get(datasetAddress);
    }

    @Override
    public boolean storeDatasetCacheEntry(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final ChangeSetMessage changeSet) {
        getDatasetCacheEntries(datasetAddress.systemSchemaId())
                .put(
                        datasetAddress,
                        new DatasetCacheEntry(datasetAddress, datasetCacheVersion, String.valueOf(changeSet)));
        return true;
    }

    @Override
    public boolean invalidateDatasetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        final Map<DatasetAddress, DatasetCacheEntry> datasetCacheEntries =
                getDatasetCacheEntries(datasetAddress.systemSchemaId());
        if (!datasetCacheEntries.containsKey(datasetAddress)) {
            return false;
        } else {
            datasetCacheEntries.remove(datasetAddress);
            return true;
        }
    }
}
