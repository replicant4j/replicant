package replicant.messages;

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
    private String[] datasetAddresses;

    @Nullable
    private Object filterParameter;

    @JsOverlay
    @NonNull
    public static BulkSubscribeMessage create(
            final int req, @NonNull final String[] datasetAddresses, @Nullable final Object filterParameter) {
        final BulkSubscribeMessage message = new BulkSubscribeMessage();
        assert null != datasetAddresses;
        message.type = TYPE;
        message.requestId = req;
        message.datasetAddresses = datasetAddresses;
        message.filterParameter = filterParameter;
        return message;
    }
}
