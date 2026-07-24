package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"unused"})
public final class OkMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.OK;

    @JsOverlay
    @NonNull
    public static OkMessage create(final int requestId) {
        final OkMessage message = new OkMessage();
        message.type = TYPE;
        message.requestId = (double) requestId;
        return message;
    }
}
