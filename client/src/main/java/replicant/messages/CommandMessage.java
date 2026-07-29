package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class CommandMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.COMMAND;

    @NonNull
    private String name;

    @Nullable
    private Object payload;

    @JsOverlay
    @NonNull
    public static CommandMessage create(final int req, @NonNull final String name, @Nullable final Object payload) {
        final CommandMessage message = new CommandMessage();
        message.type = TYPE;
        message.requestId = req;
        message.name = name;
        if (null != payload) {
            message.payload = payload;
        }
        return message;
    }

    @JsOverlay
    @NonNull
    public String getName() {
        return name;
    }

    @JsOverlay
    @Nullable
    public Object getPayload() {
        return payload;
    }
}
