package replicant.messages;

import akasha.core.JsObject;
import akasha.lang.JsArray;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;
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
        return Js.asPropertyMap(this).has(datasetAddress);
    }

    /**
     * Return the Dataset Address descriptors.
     *
     * @return the Dataset Address descriptors.
     */
    @JsOverlay
    default String[] datasetAddresses() {
        final JsArray<String> keys = JsObject.keys(this);
        return keys.asArray(new String[keys.length]);
    }

    @NonNull
    @JsOverlay
    default String getDatasetCacheVersion(@NonNull final String key) {
        final Any any = Js.asPropertyMap(this).getAsAny(key);
        assert null != any;
        return any.asString();
    }
}
