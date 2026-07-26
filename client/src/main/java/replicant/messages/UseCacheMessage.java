package replicant.messages;

import java.util.Objects;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class UseCacheMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.USE_CACHE;

    @NonNull
    private String datasetAddress;

    @NonNull
    private String etag;

    @JsOverlay
    @NonNull
    public static UseCacheMessage create(
            @Nullable final Integer requestId, @NonNull final String datasetAddress, @NonNull final String eTag) {
        final UseCacheMessage changeSet = new UseCacheMessage();
        changeSet.type = TYPE;
        changeSet.requestId = null == requestId ? null : requestId.doubleValue();
        changeSet.datasetAddress = Objects.requireNonNull(datasetAddress);
        changeSet.etag = Objects.requireNonNull(eTag);
        return changeSet;
    }

    @JsOverlay
    @NonNull
    public final String getDatasetAddress() {
        return datasetAddress;
    }

    @JsOverlay
    @NonNull
    public final String getEtag() {
        return etag;
    }
}
