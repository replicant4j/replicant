package replicant;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TestCacheService implements CacheService {
    @NonNull
    private final Map<Integer, Map<DatasetAddress, CacheEntry>> _data = new HashMap<>();

    @NonNull
    private Map<DatasetAddress, CacheEntry> getSystemCache(final int schemaId) {
        return _data.computeIfAbsent(schemaId, v -> new HashMap<>());
    }

    @NonNull
    @Override
    public Set<DatasetAddress> keySet(final int schemaId) {
        return CollectionsUtil.wrap(getSystemCache(schemaId).keySet());
    }

    @Nullable
    @Override
    public String lookupDatasetCacheVersion(@NonNull final DatasetAddress datasetAddress) {
        final CacheEntry entry = getSystemCache(datasetAddress.schemaId()).get(datasetAddress);
        return null != entry ? entry.getDatasetCacheVersion() : null;
    }

    @Nullable
    @Override
    public CacheEntry lookup(@NonNull final DatasetAddress datasetAddress) {
        return getSystemCache(datasetAddress.schemaId()).get(datasetAddress);
    }

    @Override
    public boolean store(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final Object content) {
        getSystemCache(datasetAddress.schemaId())
                .put(datasetAddress, new CacheEntry(datasetAddress, datasetCacheVersion, String.valueOf(content)));
        return true;
    }

    @Override
    public boolean invalidate(@NonNull final DatasetAddress datasetAddress) {
        final Map<DatasetAddress, CacheEntry> systemCache = getSystemCache(datasetAddress.schemaId());
        if (!systemCache.containsKey(datasetAddress)) {
            return false;
        } else {
            systemCache.remove(datasetAddress);
            return true;
        }
    }
}
