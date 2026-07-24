package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class EtagsMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.ETAGS;

    @NonNull
    private EtagsData etags;

    @JsOverlay
    @NonNull
    public static EtagsMessage create(final int req, @NonNull final EtagsData etags) {
        final EtagsMessage message = new EtagsMessage();
        message.type = TYPE;
        message.requestId = req;
        message.etags = etags;
        return message;
    }
}
