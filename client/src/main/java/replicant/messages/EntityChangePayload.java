package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import org.jspecify.annotations.NonNull;

/**
 * The serialized Entity attribute values carried by an update {@link EntityChange}.
 *
 * <p>This payload is not the complete Entity Change. Entity identity, target Subscription Dataset Addresses, and
 * update or removal semantics belong to the enclosing Entity Change.</p>
 */
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public interface EntityChangePayload {
    /**
     * Return true if a serialized value for the attribute identified by the key is present in the payload.
     *
     * @param key the attribute key.
     * @return true if the serialized value is present.
     */
    @JsOverlay
    default boolean containsKey(@NonNull final String key) {
        return Js.asPropertyMap(this).has(key);
    }

    /**
     * Return true if the serialized value for the attribute identified by the key is null.
     *
     * @param key the attribute key.
     * @return true if the serialized value is null.
     */
    @JsOverlay
    default boolean isNull(@NonNull final String key) {
        return null == Js.asPropertyMap(this).getAsAny(key);
    }

    /**
     * Return the integer value for the attribute identified by the key.
     *
     * @param key the attribute key.
     * @return the serialized integer value.
     */
    @JsOverlay
    default int getIntegerValue(@NonNull final String key) {
        return Js.asPropertyMap(this).getAsAny(key).asInt();
    }

    /**
     * Return the string value for the attribute identified by the key.
     *
     * @param key the attribute key.
     * @return the serialized string value.
     */
    @NonNull
    @JsOverlay
    default String getStringValue(@NonNull final String key) {
        return Js.asPropertyMap(this).getAsAny(key).asString();
    }

    /**
     * Return the boolean value for the attribute identified by the key.
     *
     * @param key the attribute key.
     * @return the serialized boolean value.
     */
    @JsOverlay
    default boolean getBooleanValue(@NonNull final String key) {
        return Js.asPropertyMap(this).getAsAny(key).asBoolean();
    }
}
