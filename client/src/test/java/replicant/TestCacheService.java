package replicant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

public class TestCacheService implements CacheService {
    @NonNull
    private final Map<Integer, Map<DatasetAddress, CacheEntry>> _data = new HashMap<>();

    @NonNull
    private Map<DatasetAddress, CacheEntry> getSystemCache(final int systemSchemaId) {
        return _data.computeIfAbsent(systemSchemaId, v -> new HashMap<>());
    }

    @NonNull
    @Override
    public Set<DatasetAddress> keySet(final int systemSchemaId) {
        return CollectionsUtil.wrap(getSystemCache(systemSchemaId).keySet());
    }

    @Nullable
    @Override
    public String lookupDatasetCacheVersion(@NonNull final DatasetAddress datasetAddress) {
        final CacheEntry entry = getSystemCache(datasetAddress.systemSchemaId()).get(datasetAddress);
        return null != entry ? entry.getDatasetCacheVersion() : null;
    }

    @Nullable
    @Override
    public CacheEntry lookup(@NonNull final DatasetAddress datasetAddress) {
        return getSystemCache(datasetAddress.systemSchemaId()).get(datasetAddress);
    }

    @Override
    public boolean store(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final ChangeSetMessage changeSet) {
        getSystemCache(datasetAddress.systemSchemaId())
                .put(datasetAddress, new CacheEntry(datasetAddress, datasetCacheVersion, String.valueOf(changeSet)));
        return true;
    }

    @Override
    public boolean invalidate(@NonNull final DatasetAddress datasetAddress) {
        final Map<DatasetAddress, CacheEntry> systemCache = getSystemCache(datasetAddress.systemSchemaId());
        if (!systemCache.containsKey(datasetAddress)) {
            return false;
        } else {
            systemCache.remove(datasetAddress);
            return true;
        }
    }
}
