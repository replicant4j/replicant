package replicant.messages;

import static org.realityforge.braincheck.Guards.*;

import java.util.HashSet;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import replicant.Replicant;
import replicant.shared.Messages;

/**
 * The server-to-client wire message carrying one Change Set.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
@SuppressWarnings({"NullAway.Init", "ConstantValue", "NotNullFieldNotInitialized"})
public class ChangeSetMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.CHANGE_SET;

    @Nullable
    private String datasetCacheVersion;

    @Nullable
    private String[] subscriptionChanges;

    @Nullable
    private SubscriptionChangeMessage[] filterParameterSubscriptionChanges;

    @Nullable
    private EntityChange[] entityChanges;

    @Nullable
    private Object response;

    @JsOverlay
    @NonNull
    @NullUnmarked
    public static ChangeSetMessage create(
            @Nullable final Integer requestId,
            @Nullable final String datasetCacheVersion,
            @Nullable final String[] subscriptionChanges,
            @Nullable final SubscriptionChangeMessage[] filterParameterSubscriptionChanges,
            @Nullable final EntityChange[] entityChanges,
            @Nullable final Object response) {
        final ChangeSetMessage changeSet = new ChangeSetMessage();
        changeSet.type = TYPE;
        changeSet.requestId = null == requestId ? null : requestId.doubleValue();
        changeSet.datasetCacheVersion = datasetCacheVersion;
        changeSet.subscriptionChanges = subscriptionChanges;
        changeSet.filterParameterSubscriptionChanges = filterParameterSubscriptionChanges;
        changeSet.entityChanges = entityChanges;
        changeSet.response = response;
        return changeSet;
    }

    /**
     * @return the Dataset Cache Version for this Change Set, if present.
     */
    @Nullable
    @JsOverlay
    public final String getDatasetCacheVersion() {
        return datasetCacheVersion;
    }

    /**
     * @return the exec response associated with the Change Set, if any.
     */
    @Nullable
    @JsOverlay
    public final Object getResponse() {
        return response;
    }

    /**
     * Return the compact Subscription Changes that are part of the Change Set.
     * This should only be invoked if {@link #hasSubscriptionChanges()} returns true.
     *
     * @return the compact Subscription Changes.
     */
    @NonNull
    @JsOverlay
    public final String[] getSubscriptionChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != subscriptionChanges,
                    () -> "Replicant-0013: ChangeSetMessage.getSubscriptionChanges() invoked when no changes are"
                            + " present. Should guard call with ChangeSetMessage.hasSubscriptionChanges().");
        }
        assert null != subscriptionChanges;
        return subscriptionChanges;
    }

    /**
     * Return true if this Change Set contains compact Subscription Changes.
     *
     * @return true if this Change Set contains compact Subscription Changes.
     */
    @JsOverlay
    public final boolean hasSubscriptionChanges() {
        return null != subscriptionChanges;
    }

    /**
     * Return the Subscription Changes with Filter Parameters that are part of the Change Set.
     * This should only be invoked if {@link #hasFilterParameterSubscriptionChanges()} returns true.
     *
     * @return the Subscription Changes with Filter Parameters.
     */
    @NonNull
    @JsOverlay
    public final SubscriptionChangeMessage[] getFilterParameterSubscriptionChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != filterParameterSubscriptionChanges,
                    () -> "Replicant-0030: ChangeSetMessage.getFilterParameterSubscriptionChanges() invoked when no"
                            + " changes are present. Should guard call with"
                            + " ChangeSetMessage.hasFilterParameterSubscriptionChanges().");
        }
        assert null != filterParameterSubscriptionChanges;
        return filterParameterSubscriptionChanges;
    }

    /**
     * Return true if this Change Set contains Subscription Changes with Filter Parameters.
     *
     * @return true if this Change Set contains Subscription Changes with Filter Parameters.
     */
    @JsOverlay
    public final boolean hasFilterParameterSubscriptionChanges() {
        return null != filterParameterSubscriptionChanges;
    }

    /**
     * Return the Entity Changes that are part of the Change Set.
     * This should only be invoked if {@link #hasEntityChanges()} return true.
     *
     * @return the Entity Changes.
     */
    @NonNull
    @JsOverlay
    public final EntityChange[] getEntityChanges() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    () -> null != entityChanges,
                    () -> "Replicant-0012: ChangeSetMessage.getEntityChanges() invoked when no changes are present."
                            + " Should guard call with ChangeSetMessage.hasEntityChanges().");
        }
        assert null != entityChanges;
        return entityChanges;
    }

    /**
     * Return true if this Change Set contains Entity Changes.
     *
     * @return true if this Change Set contains Entity Changes.
     */
    @JsOverlay
    public final boolean hasEntityChanges() {
        return null != entityChanges;
    }

    /**
     * Validate the Change Set when invariants are enabled.
     * The validation ensures there are not multiple Entity Changes for the same Entity and that there are not multiple
     * Subscription Changes for the same Dataset Address.
     */
    @JsOverlay
    public final void validate() {
        if (Replicant.shouldCheckApiInvariants()) {
            if (null != entityChanges) {
                final HashSet<String> existing = new HashSet<>();
                for (final EntityChange change : entityChanges) {
                    assert null != change;
                    final String id = change.getId();
                    apiInvariant(
                            () -> existing.add(id),
                            () -> "Replicant-0014: ChangeSetMessage contains multiple Entity Changes " + "with the id '"
                                    + id + "'.");
                }
            }
            final HashSet<String> existingDatasetAddresses = new HashSet<>();
            if (null != subscriptionChanges) {
                for (final String subscriptionChange : subscriptionChanges) {
                    assert null != subscriptionChange;
                    final String key = subscriptionChange.substring(1);
                    apiInvariant(
                            () -> existingDatasetAddresses.add(key),
                            () -> "Replicant-0022: ChangeSetMessage contains multiple Subscription Changes "
                                    + "for Dataset Address " + subscriptionChange.substring(1) + ".");
                }
            }
            if (null != filterParameterSubscriptionChanges) {
                for (final SubscriptionChangeMessage subscriptionChange : filterParameterSubscriptionChanges) {
                    assert null != subscriptionChange;
                    final String descriptor = subscriptionChange.getSubscriptionChange();
                    final String key = descriptor.substring(1);
                    apiInvariant(
                            () -> existingDatasetAddresses.add(key),
                            () -> "Replicant-0028: ChangeSetMessage contains multiple Subscription Changes "
                                    + "for Dataset Address " + descriptor.substring(1) + ".");
                }
            }
        }
    }
}
