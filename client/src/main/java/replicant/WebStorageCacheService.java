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

/**
 * An implementation of the CacheService that uses LocalStorage or SessionStorage.
 * The implementation will preferentially use local storage and then session storage.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class WebStorageCacheService implements CacheService {
    @NonNull
    static final String ETAG_INDEX = "REPLICANT_ETAG_INDEX";

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
    public Set<DatasetAddress> keySet(final int schemaId) {
        final Set<DatasetAddress> datasetAddresses = new HashSet<>();
        getIndex(schemaId).forEach(v -> datasetAddresses.add(DatasetAddress.parse(schemaId, v)));
        return CollectionsUtil.wrap(datasetAddresses);
    }

    @Nullable
    @Override
    public String lookupEtag(@NonNull final DatasetAddress datasetAddress) {
        return getIndex(datasetAddress.schemaId())
                .get(Objects.requireNonNull(datasetAddress).asDatasetAddressDescriptor());
    }

    @Nullable
    @Override
    public CacheEntry lookup(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final String eTag = getIndex(datasetAddress.schemaId()).get(datasetAddress.asDatasetAddressDescriptor());
        final String content = _storage.getItem(datasetAddress.getCacheKey());
        if (null != eTag && null != content) {
            return new CacheEntry(datasetAddress, eTag, content);
        } else {
            return null;
        }
    }

    @Override
    public boolean store(
            @NonNull final DatasetAddress datasetAddress, @NonNull final String eTag, @NonNull final Object content) {
        Objects.requireNonNull(datasetAddress);
        Objects.requireNonNull(eTag);
        Objects.requireNonNull(content);
        try {
            final int schemaId = datasetAddress.schemaId();
            final JsPropertyMap<String> index = getIndex(schemaId);
            index.set(datasetAddress.asDatasetAddressDescriptor(), eTag);
            saveIndex(schemaId, index);
            getStorage().setItem(datasetAddress.getCacheKey(), JSON.stringify(content));
            return true;
        } catch (final Throwable e) {
            // This exception can occur when storage is full
            invalidate(datasetAddress);
            return false;
        }
    }

    private void saveIndex(final int schemaId, @NonNull final JsPropertyMap<String> index) {
        final Storage storage = getStorage();
        final String key = indexKey(schemaId);
        if (0 == JsObject.keys(index).length) {
            storage.removeItem(key);
        } else {
            storage.setItem(key, JSON.stringify(index));
        }
    }

    @Override
    public boolean invalidate(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final int schemaId = datasetAddress.schemaId();
        final JsPropertyMap<String> index = findIndex(schemaId);
        final String key = datasetAddress.asDatasetAddressDescriptor();
        if (null == index || null == index.get(key)) {
            return false;
        } else {
            index.delete(key);
            saveIndex(schemaId, index);
            getStorage().removeItem(datasetAddress.getCacheKey());
            return true;
        }
    }

    @NonNull
    Storage getStorage() {
        return _storage;
    }

    @NonNull
    private JsPropertyMap<String> getIndex(final int schemaId) {
        final JsPropertyMap<String> index = findIndex(schemaId);
        return null == index ? Js.uncheckedCast(JsPropertyMap.of()) : index;
    }

    @Nullable
    private JsPropertyMap<String> findIndex(final int schemaId) {
        final String indexData = _storage.getItem(indexKey(schemaId));
        return null == indexData ? null : Js.uncheckedCast(JSON.parse(indexData));
    }

    @NonNull
    private String indexKey(final int schemaId) {
        return ETAG_INDEX + '-' + schemaId;
    }
}
