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
 * A simple abstraction for key-value etag pairs.
 */
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public interface EtagsData {
    /**
     * Return true if etag for channel is present.
     *
     * @param key the channel key.
     * @return true if the etag is present.
     */
    @JsOverlay
    default boolean containsChannel(@NonNull final String key) {
        return Js.asPropertyMap(this).has(key);
    }

    /**
     * Return the channels.
     *
     * @return eturn the channels.
     */
    @JsOverlay
    default String[] channels() {
        final JsArray<String> keys = JsObject.keys(this);
        return keys.asArray(new String[keys.length]);
    }

    @NonNull
    @JsOverlay
    default String getEtag(@NonNull final String key) {
        final Any any = Js.asPropertyMap(this).getAsAny(key);
        assert null != any;
        return any.asString();
    }
}
