package replicant.messages;

import java.util.Objects;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class BulkSubscribeMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.BULK_SUB;

    @NonNull
    private String[] channels;

    @Nullable
    private Object filter;

    @JsOverlay
    @NonNull
    public static BulkSubscribeMessage create(
            final int req, @NonNull final String[] channels, @Nullable final Object filter) {
        final BulkSubscribeMessage message = new BulkSubscribeMessage();
        message.type = TYPE;
        message.requestId = req;
        message.channels = Objects.requireNonNull(channels);
        message.filter = filter;
        return message;
    }
}
