package replicant.messages;

import java.util.Objects;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A change to an entity.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings("NullAway.Init")
public class EntityChange {
    private String id;
    private String[] channels;

    @Nullable
    private EntityChangeData data;

    /**
     * Create a "remove" EntityChange message.
     *
     * @return the new EntityChange.
     */
    @JsOverlay
    @NonNull
    public static EntityChange create(final int type, final int id, @NonNull final String[] channels) {
        final EntityChange change = new EntityChange();
        change.id = type + "." + id;
        change.channels = channels;
        return change;
    }

    /**
     * Create an "update" EntityChange message.
     *
     * @return the new EntityChange.
     */
    @JsOverlay
    @NonNull
    public static EntityChange create(
            final int type, final int id, @NonNull final String[] channels, @Nullable final EntityChangeData data) {
        final EntityChange change = create(type, id, channels);
        change.data = data;
        return change;
    }

    private EntityChange() {}

    /**
     * @return the id of the entity.
     */
    @JsOverlay
    public final String getId() {
        return id;
    }

    /**
     * Return the channels that the entity is associated with.
     *
     * @return the channels that the entity is associated with.
     */
    @JsOverlay
    public final String[] getChannels() {
        return channels;
    }

    /**
     * @return true if the change is an update, false if it is a remove.
     */
    @JsOverlay
    public final boolean isUpdate() {
        return null != data;
    }

    /**
     * @return true if the change is a remove, false if it is an update.
     */
    @JsOverlay
    public final boolean isRemove() {
        return !isUpdate();
    }

    /**
     * Return data to update.
     *
     * @return true if the data is present.
     */
    @NonNull
    @JsOverlay
    public final EntityChangeData getData() {
        return Objects.requireNonNull(data);
    }
}
