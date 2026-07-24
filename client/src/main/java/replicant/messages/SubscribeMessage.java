package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class SubscribeMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.SUB;

    @NonNull
    private String channel;

    @Nullable
    private Object filter;

    @JsOverlay
    @NonNull
    public static SubscribeMessage create(final int req, @NonNull final String ch, @Nullable final Object filter) {
        final SubscribeMessage message = new SubscribeMessage();
        message.type = TYPE;
        message.requestId = req;
        message.channel = ch;
        if (null != filter) {
            message.filter = filter;
        }
        return message;
    }
}
