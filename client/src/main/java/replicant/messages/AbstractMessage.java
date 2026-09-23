package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

/**
 * The abstract message that messages conform to.
 */
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public abstract class AbstractMessage {
    @JsProperty(name = Messages.Common.TYPE)
    @NonNull
    String type;

    /**
     * Return the type of the message.
     *
     * @return the type of the message.
     */
    @NonNull
    @JsOverlay
    public final String getType() {
        return type;
    }
}
