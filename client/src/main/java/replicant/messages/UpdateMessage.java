package replicant.messages;

import static org.realityforge.braincheck.Guards.*;

import java.util.HashSet;
import java.util.Objects;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import replicant.Replicant;
import replicant.shared.Messages;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings("NullAway.Init")
public class UpdateMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.UPDATE;

    @Nullable
    private String etag;

    @Nullable
    private String[] subscriptionChanges;

    @Nullable
    private SubscriptionChangeMessage[] filterParameterSubscriptionChanges;

    @Nullable
    private EntityChange[] changes;

    @Nullable
    private Object response;

    @JsOverlay
    @NonNull
    @NullUnmarked
    public static UpdateMessage create(
            @Nullable final Integer requestId,
            @Nullable final String eTag,
            @Nullable final String[] subscriptionChanges,
            @Nullable final SubscriptionChangeMessage[] filterParameterSubscriptionChanges,
            @Nullable final EntityChange[] entityChanges,
            @Nullable final Object response) {
        final UpdateMessage updateMessage = new UpdateMessage();
        updateMessage.type = TYPE;
        updateMessage.requestId = null == requestId ? null : requestId.doubleValue();
        updateMessage.etag = eTag;
        updateMessage.subscriptionChanges = subscriptionChanges;
        updateMessage.filterParameterSubscriptionChanges = filterParameterSubscriptionChanges;
        updateMessage.changes = entityChanges;
        updateMessage.response = response;
        return updateMessage;
    }

    /**
     * @return the version under which this can be cached.
     */
    @Nullable
    @JsOverlay
    public final String getETag() {
        return etag;
    }

    /**
     * @return the exec response associated with the message, if any
     */
    @Nullable
    @JsOverlay
    public final Object getResponse() {
        return response;
    }

    /**
     * Return the compact Subscription changes that are part of the message.
     * This should only be invoked if {@link #hasSubscriptionChanges()} returns true.
     *
     * @return the compact Subscription changes.
     */
    @NonNull
    @JsOverlay
    public final String[] getSubscriptionChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != subscriptionChanges,
                    () -> "Replicant-0013: UpdateMessage.getSubscriptionChanges() invoked when no changes are"
                            + " present. Should guard call with UpdateMessage.hasSubscriptionChanges().");
        }
        return Objects.requireNonNull(subscriptionChanges);
    }

    /**
     * Return true if this UpdateMessage contains compact Subscription changes.
     *
     * @return true if this UpdateMessage contains compact Subscription changes.
     */
    @JsOverlay
    public final boolean hasSubscriptionChanges() {
        return null != subscriptionChanges;
    }

    /**
     * Return the Subscription changes with Filter Parameters that are part of the message.
     * This should only be invoked if {@link #hasFilterParameterSubscriptionChanges()} returns true.
     *
     * @return the Subscription changes with Filter Parameters.
     */
    @NonNull
    @JsOverlay
    public final SubscriptionChangeMessage[] getFilterParameterSubscriptionChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != filterParameterSubscriptionChanges,
                    () -> "Replicant-0030: UpdateMessage.getFilterParameterSubscriptionChanges() invoked when no"
                            + " changes are present. Should guard call with"
                            + " UpdateMessage.hasFilterParameterSubscriptionChanges().");
        }
        return Objects.requireNonNull(filterParameterSubscriptionChanges);
    }

    /**
     * Return true if this UpdateMessage contains Subscription changes with Filter Parameters.
     *
     * @return true if this UpdateMessage contains Subscription changes with Filter Parameters.
     */
    @JsOverlay
    public final boolean hasFilterParameterSubscriptionChanges() {
        return null != filterParameterSubscriptionChanges;
    }

    /**
     * Return the entity changes that are part of the message.
     * This should only be invoked if {@link #hasEntityChanges()} return true.
     *
     * @return the entity changes.
     */
    @NonNull
    @JsOverlay
    public final EntityChange[] getEntityChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != changes,
                    () -> "Replicant-0012: UpdateMessage.getEntityChanges() invoked when no changes are present."
                            + " Should guard call with UpdateMessage.hasEntityChanges().");
        }
        return Objects.requireNonNull(changes);
    }

    /**
     * Return true if this UpdateMessage contains EntityChanges.
     *
     * @return true if this UpdateMessage contains EntityChanges
     */
    @JsOverlay
    public final boolean hasEntityChanges() {
        return null != changes;
    }

    /**
     * This method will validate the UpdateMessage to make sure it is internally consistent if invariants are enabled.
     * The validation will ensure that there is not multiple EntityChange messages for the same entity and that
     * there are not multiple SubscriptionChangeMessage instances for the same Dataset Address.
     */
    @JsOverlay
    public final void validate() {
        if (Replicant.shouldCheckApiInvariants()) {
            if (null != changes) {
                final HashSet<String> existing = new HashSet<>();
                for (final EntityChange change : changes) {
                    final String id = change.getId();
                    apiInvariant(
                            () -> existing.add(id),
                            () -> "Replicant-0014: UpdateMessage contains multiple EntityChange messages "
                                    + "with the id '" + id + "'.");
                }
            }
            final HashSet<String> existingDatasetAddresses = new HashSet<>();
            if (null != subscriptionChanges) {
                for (final String subscriptionChange : subscriptionChanges) {
                    final String key = subscriptionChange.substring(1);
                    apiInvariant(
                            () -> existingDatasetAddresses.add(key),
                            () -> "Replicant-0022: UpdateMessage contains multiple Subscription changes "
                                    + "for Dataset Address " + subscriptionChange.substring(1) + ".");
                }
            }
            if (null != filterParameterSubscriptionChanges) {
                for (final SubscriptionChangeMessage subscriptionChange : filterParameterSubscriptionChanges) {
                    final String descriptor = subscriptionChange.getSubscriptionChange();
                    final String key = descriptor.substring(1);
                    apiInvariant(
                            () -> existingDatasetAddresses.add(key),
                            () -> "Replicant-0028: UpdateMessage contains multiple Subscription changes "
                                    + "for Dataset Address " + descriptor.substring(1) + ".");
                }
            }
        }
    }
}
