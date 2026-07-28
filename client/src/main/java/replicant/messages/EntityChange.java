package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A Change Set member directing the client to update an Entity's Replica or remove it from specified Subscriptions.
 *
 * <p>An update carries an {@link EntityChangePayload} containing serialized Entity attribute values. The payload is
 * only one part of the Entity Change.</p>
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NullAway.Init", "NotNullFieldNotInitialized"})
public class EntityChange {
    private int entityTypeId;
    private int entityId;
    private String[] datasetAddresses;

    @Nullable
    private EntityChangePayload payload;

    /**
     * Create a "remove" Entity Change.
     *
     * @param entityTypeId     the Entity Type ID.
     * @param entityId         the Entity ID.
     * @param datasetAddresses the Dataset Addresses of Subscriptions from which to remove the Replica.
     * @return the new Entity Change.
     */
    @JsOverlay
    @NonNull
    public static EntityChange create(
            final int entityTypeId, final int entityId, @NonNull final String[] datasetAddresses) {
        final EntityChange change = new EntityChange();
        change.entityTypeId = entityTypeId;
        change.entityId = entityId;
        change.datasetAddresses = datasetAddresses;
        return change;
    }

    /**
     * Create an "update" Entity Change.
     *
     * @param entityTypeId     the Entity Type ID.
     * @param entityId         the Entity ID.
     * @param datasetAddresses the Dataset Addresses of Subscriptions containing the Replica after the update.
     * @param payload          the serialized Entity attribute values used to create or update the Replica.
     * @return the new Entity Change.
     */
    @JsOverlay
    @NonNull
    public static EntityChange create(
            final int entityTypeId,
            final int entityId,
            @NonNull final String[] datasetAddresses,
            @Nullable final EntityChangePayload payload) {
        final EntityChange change = create(entityTypeId, entityId, datasetAddresses);
        change.payload = payload;
        return change;
    }

    private EntityChange() {}

    /**
     * Return the Entity Type ID.
     *
     * @return the Entity Type ID.
     */
    @JsOverlay
    public final int getEntityTypeId() {
        return entityTypeId;
    }

    /**
     * Return the Entity ID.
     *
     * @return the Entity ID.
     */
    @JsOverlay
    public final int getEntityId() {
        return entityId;
    }

    /**
     * Return the Dataset Addresses for Subscriptions containing the Entity.
     *
     * @return the Dataset Addresses for Subscriptions containing the Entity.
     */
    @JsOverlay
    public final String[] getDatasetAddresses() {
        return datasetAddresses;
    }

    /**
     * @return true if the change is an update, false if it is a remove.
     */
    @JsOverlay
    public final boolean isUpdate() {
        return null != payload;
    }

    /**
     * @return true if the change is a remove, false if it is an update.
     */
    @JsOverlay
    public final boolean isRemove() {
        return !isUpdate();
    }

    /**
     * Return the serialized Entity attribute values carried by this update.
     *
     * <p>This payload does not include the Entity identity, target Subscription Dataset Addresses, or update
     * semantics represented by this Entity Change.</p>
     *
     * @return the Entity Change payload.
     */
    @NonNull
    @JsOverlay
    public final EntityChangePayload getPayload() {
        if (null == payload) {
            throw new AssertionError("Entity Change has no payload");
        }
        return payload;
    }
}
