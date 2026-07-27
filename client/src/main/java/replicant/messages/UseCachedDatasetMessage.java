package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.Messages;

/**
 * A server instruction to apply the complete cached representation for a Dataset Address.
 */
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class UseCachedDatasetMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.USE_CACHED_DATASET;

    @NonNull
    private String datasetAddress;

    @NonNull
    private String datasetCacheVersion;

    @JsOverlay
    @NonNull
    public static UseCachedDatasetMessage create(
            @Nullable final Integer requestId,
            @NonNull final String datasetAddress,
            @NonNull final String datasetCacheVersion) {
        final UseCachedDatasetMessage message = new UseCachedDatasetMessage();
        assert null != datasetAddress;
        assert null != datasetCacheVersion;
        message.type = TYPE;
        message.requestId = null == requestId ? null : requestId.doubleValue();
        message.datasetAddress = datasetAddress;
        message.datasetCacheVersion = datasetCacheVersion;
        return message;
    }

    @JsOverlay
    @NonNull
    public final String getDatasetAddress() {
        return datasetAddress;
    }

    @JsOverlay
    @NonNull
    public final String getDatasetCacheVersion() {
        return datasetCacheVersion;
    }
}
