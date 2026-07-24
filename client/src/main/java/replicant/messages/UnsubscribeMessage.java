package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class UnsubscribeMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.UNSUB;

    @NonNull
    private String channel;

    @JsOverlay
    @NonNull
    public static UnsubscribeMessage create(final int req, @NonNull final String ch) {
        final UnsubscribeMessage message = new UnsubscribeMessage();
        message.type = TYPE;
        message.requestId = req;
        message.channel = ch;
        return message;
    }
}
