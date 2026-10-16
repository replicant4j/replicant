package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;
import jsinterop.base.JsArrayLike;
import jsinterop.base.JsPropertyMap;
import org.jspecify.annotations.NonNull;

/**
 * A mapping from Dataset Address descriptors to opaque Dataset Cache Versions.
 */
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public interface DatasetCacheVersionsData {
    /**
     * Return true if a Dataset Cache Version for the Dataset Address is present.
     *
     * @param datasetAddress the Dataset Address descriptor.
     * @return true if the Dataset Cache Version is present.
     */
    @JsOverlay
    default boolean containsDatasetAddress(@NonNull final String datasetAddress) {
        final JsPropertyMap<?> properties = Js.cast(this);
        return properties.has(datasetAddress);
    }

    /**
     * Return the Dataset Address descriptors.
     *
     * @return the Dataset Address descriptors.
     */
    @JsOverlay
    default String[] datasetAddresses() {
        final JsArrayLike<String> keys = BrowserObject.keys(this);
        final int length = keys.getLength();
        final String[] datasetAddresses = new String[length];
        for (int i = 0; i < length; i++) {
            final String datasetAddress = keys.getAt(i);
            assert null != datasetAddress;
            datasetAddresses[i] = datasetAddress;
        }
        return datasetAddresses;
    }

    @NonNull
    @JsOverlay
    default String getDatasetCacheVersion(@NonNull final String datasetAddress) {
        final JsPropertyMap<?> properties = Js.cast(this);
        final Any any = properties.getAsAny(datasetAddress);
        assert null != any;
        return any.asString();
    }
}
