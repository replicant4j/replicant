package replicant;

import akasha.Storage;
import akasha.WindowGlobal;
import akasha.core.JSON;
import akasha.core.JsObject;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

/**
 * An implementation of the CacheService that uses LocalStorage or SessionStorage.
 * The implementation will preferentially use local storage and then session storage.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class WebStorageCacheService implements CacheService {
    @NonNull
    static final String DATASET_CACHE_VERSION_INDEX = "REPLICANT_DATASET_CACHE_VERSION_INDEX";

    @NonNull
    private final Storage _storage;

    /**
     * Install CacheService into the default context where persistence occurs in storage attached to root window.
     * The <code>localStorage</code> of window will be used if present, else the <code>sessionStorage</code> will be used.
     */
    public static void install() {
        install(Replicant.context());
    }

    /**
     * Install CacheService into specified context where persistence occurs in storage attached to root window.
     * The <code>localStorage</code> of window will be used if present, else the <code>sessionStorage</code> will be used.
     *
     * @param context the replicant context.
     */
    public static void install(@NonNull final ReplicantContext context) {
        install(context, WindowGlobal.localStorage());
    }

    /**
     * Install CacheService into specified context where persistence occurs in specified storage.
     *
     * @param context the replicant context.
     * @param storage the store used to cache data.
     */
    public static void install(@NonNull final ReplicantContext context, @NonNull final Storage storage) {
        Objects.requireNonNull(context).setCacheService(new WebStorageCacheService(storage));
    }

    WebStorageCacheService(@NonNull final Storage storage) {
        _storage = Objects.requireNonNull(storage);
    }

    @NonNull
    @Override
    public Set<DatasetAddress> keySet(final int systemSchemaId) {
        final Set<DatasetAddress> datasetAddresses = new HashSet<>();
        getIndex(systemSchemaId).forEach(v -> datasetAddresses.add(DatasetAddress.parse(systemSchemaId, v)));
        return CollectionsUtil.wrap(datasetAddresses);
    }

    @Nullable
    @Override
    public String lookupDatasetCacheVersion(@NonNull final DatasetAddress datasetAddress) {
        return getIndex(datasetAddress.systemSchemaId())
                .get(Objects.requireNonNull(datasetAddress).asDatasetAddressDescriptor());
    }

    @Nullable
    @Override
    public CacheEntry lookup(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final String datasetCacheVersion =
                getIndex(datasetAddress.systemSchemaId()).get(datasetAddress.asDatasetAddressDescriptor());
        final String changeSet = _storage.getItem(datasetAddress.getCacheKey());
        if (null != datasetCacheVersion && null != changeSet) {
            return new CacheEntry(datasetAddress, datasetCacheVersion, changeSet);
        } else {
            return null;
        }
    }

    @Override
    public boolean store(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final String datasetCacheVersion,
            @NonNull final ChangeSetMessage changeSet) {
        Objects.requireNonNull(datasetAddress);
        Objects.requireNonNull(datasetCacheVersion);
        assert null != changeSet;
        try {
            final int systemSchemaId = datasetAddress.systemSchemaId();
            final JsPropertyMap<String> index = getIndex(systemSchemaId);
            index.set(datasetAddress.asDatasetAddressDescriptor(), datasetCacheVersion);
            saveIndex(systemSchemaId, index);
            getStorage().setItem(datasetAddress.getCacheKey(), JSON.stringify(changeSet));
            return true;
        } catch (final Throwable e) {
            // This exception can occur when storage is full
            invalidate(datasetAddress);
            return false;
        }
    }

    private void saveIndex(final int systemSchemaId, @NonNull final JsPropertyMap<String> index) {
        final Storage storage = getStorage();
        final String key = indexKey(systemSchemaId);
        if (0 == JsObject.keys(index).length) {
            storage.removeItem(key);
        } else {
            storage.setItem(key, JSON.stringify(index));
        }
    }

    @Override
    public boolean invalidate(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final int systemSchemaId = datasetAddress.systemSchemaId();
        final JsPropertyMap<String> index = findIndex(systemSchemaId);
        final String key = datasetAddress.asDatasetAddressDescriptor();
        if (null == index || null == index.get(key)) {
            return false;
        } else {
            index.delete(key);
            saveIndex(systemSchemaId, index);
            getStorage().removeItem(datasetAddress.getCacheKey());
            return true;
        }
    }

    @NonNull
    Storage getStorage() {
        return _storage;
    }

    @NonNull
    private JsPropertyMap<String> getIndex(final int systemSchemaId) {
        final JsPropertyMap<String> index = findIndex(systemSchemaId);
        return null == index ? Js.uncheckedCast(JsPropertyMap.of()) : index;
    }

    @Nullable
    private JsPropertyMap<String> findIndex(final int systemSchemaId) {
        final String indexData = _storage.getItem(indexKey(systemSchemaId));
        return null == indexData ? null : Js.uncheckedCast(JSON.parse(indexData));
    }

    @NonNull
    private String indexKey(final int systemSchemaId) {
        return DATASET_CACHE_VERSION_INDEX + '-' + systemSchemaId;
    }
}
