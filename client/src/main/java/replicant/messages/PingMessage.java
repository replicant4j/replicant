package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"unused"})
public final class PingMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.PING;

    @JsOverlay
    @NonNull
    public static PingMessage create(final int req) {
        final PingMessage message = new PingMessage();
        message.type = TYPE;
        message.requestId = req;
        return message;
    }
}
