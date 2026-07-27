package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init", "unused"})
public final class DatasetCacheVersionsMessage extends ClientToServerMessage {
    @JsOverlay
    public static final String TYPE = Messages.C2S_Type.DATASET_CACHE_VERSIONS;

    @NonNull
    private DatasetCacheVersionsData datasetCacheVersions;

    @JsOverlay
    @NonNull
    public static DatasetCacheVersionsMessage create(
            final int req, @NonNull final DatasetCacheVersionsData datasetCacheVersions) {
        final DatasetCacheVersionsMessage message = new DatasetCacheVersionsMessage();
        message.type = TYPE;
        message.requestId = req;
        message.datasetCacheVersions = datasetCacheVersions;
        return message;
    }
}
