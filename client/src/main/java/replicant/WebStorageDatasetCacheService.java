package replicant;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ChangeSetMessage;

/**
 * An implementation of the Dataset Cache Service that uses local storage.
 */
@SuppressWarnings({"unused", "ClassCanBeRecord"})
public final class WebStorageDatasetCacheService implements DatasetCacheService {
    @NonNull
    static final String DATASET_CACHE_VERSION_INDEX = "REPLICANT_DATASET_CACHE_VERSION_INDEX";

    @NonNull
    private final BrowserStorage _storage;

    /**
     * Install the Dataset Cache Service into the default Replicant Context where persistence occurs in storage
     * attached to the root window.
     * The <code>localStorage</code> of window will be used.
     */
    public static void install() {
        install(Replicant.context());
    }

    /**
     * Install a distinct Dataset Cache Service into the specified Replicant Context where persistence occurs in
     * storage attached to the root window.
     * The <code>localStorage</code> of window will be used.
     *
     * @param context the Replicant Context that owns the service association.
     */
    public static void install(@NonNull final ReplicantContext context) {
        Objects.requireNonNull(context)
                .setDatasetCacheService(new WebStorageDatasetCacheService(BrowserGlobal.localStorage()));
    }

    WebStorageDatasetCacheService(@NonNull final BrowserStorage storage) {
        _storage = Objects.requireNonNull(storage);
    }

    @NonNull
    @Override
    public Set<DatasetAddress> getDatasetAddresses(final int systemSchemaId) {
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
    public DatasetCacheEntry lookupDatasetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final String datasetCacheVersion =
                getIndex(datasetAddress.systemSchemaId()).get(datasetAddress.asDatasetAddressDescriptor());
        final String changeSet = _storage.getItem(datasetAddress.getDatasetCacheEntryStorageKey());
        if (null != datasetCacheVersion && null != changeSet) {
            return new DatasetCacheEntry(datasetAddress, datasetCacheVersion, changeSet);
        } else {
            return null;
        }
    }

    @Override
    public boolean storeDatasetCacheEntry(
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
            getStorage()
                    .setItem(
                            datasetAddress.getDatasetCacheEntryStorageKey(),
                            BrowserJson.stringify(Js.asAny(changeSet)));
            return true;
        } catch (final Throwable e) {
            // This exception can occur when storage is full
            invalidateDatasetCacheEntry(datasetAddress);
            return false;
        }
    }

    private void saveIndex(final int systemSchemaId, @NonNull final JsPropertyMap<String> index) {
        final BrowserStorage storage = getStorage();
        final String key = indexKey(systemSchemaId);
        if (0 == BrowserObject.keys(index).getLength()) {
            storage.removeItem(key);
        } else {
            storage.setItem(key, BrowserJson.stringify(Js.asAny(index)));
        }
    }

    @Override
    public boolean invalidateDatasetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        Objects.requireNonNull(datasetAddress);
        final int systemSchemaId = datasetAddress.systemSchemaId();
        final JsPropertyMap<String> index = findIndex(systemSchemaId);
        final String key = datasetAddress.asDatasetAddressDescriptor();
        if (null == index || null == index.get(key)) {
            return false;
        } else {
            index.delete(key);
            saveIndex(systemSchemaId, index);
            getStorage().removeItem(datasetAddress.getDatasetCacheEntryStorageKey());
            return true;
        }
    }

    @NonNull
    BrowserStorage getStorage() {
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
        return null == indexData ? null : Js.uncheckedCast(BrowserJson.parse(indexData));
    }

    @NonNull
    private String indexKey(final int systemSchemaId) {
        return DATASET_CACHE_VERSION_INDEX + '-' + systemSchemaId;
    }

    @JsType(isNative = true, name = "globalThis", namespace = JsPackage.GLOBAL)
    private static final class BrowserGlobal {
        @NonNull
        @JsProperty(name = "localStorage")
        static native BrowserStorage localStorage();

        private BrowserGlobal() {}
    }

    @JsType(isNative = true, name = "Storage", namespace = JsPackage.GLOBAL)
    static final class BrowserStorage {
        @Nullable
        native String getItem(String key);

        native void setItem(String key, String value);

        native void removeItem(String key);
    }
}
