package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.SubscriptionChangeMessage;
import replicant.shared.Messages;

final class SubscriptionChange {
    enum Type {
        SUBSCRIBE,
        UNSUBSCRIBE,
        UPDATE,
        DELETE
    }

    @NonNull
    private final Type _type;

    @NonNull
    private final DatasetAddress _datasetAddress;

    @Nullable
    private final Object _filterParameter;

    @NonNull
    static SubscriptionChange from(final int schema, @NonNull final String subscriptionAction) {
        return from(schema, subscriptionAction, null);
    }

    @NonNull
    static SubscriptionChange from(final int schema, @NonNull final SubscriptionChangeMessage subscriptionChange) {
        return from(schema, subscriptionChange.getSubscriptionAction(), subscriptionChange.getFilterParameter());
    }

    @NonNull
    private static SubscriptionChange from(
            final int schema, @NonNull final String subscriptionAction, @Nullable final Object filterParameter) {
        try {
            final String descriptor = subscriptionAction.substring(1);
            final DatasetAddress datasetAddress = DatasetAddress.parse(schema, descriptor);
            return new SubscriptionChange(actionToType(subscriptionAction), datasetAddress, filterParameter);
        } catch (final Throwable t) {
            throw new IllegalStateException("Failed to parse Subscription action '" + subscriptionAction + "'", t);
        }
    }

    @NonNull
    private static Type actionToType(@NonNull final String subscriptionAction) {
        assert !subscriptionAction.isEmpty();
        final char commandCode = subscriptionAction.charAt(0);
        final Type type = Messages.Update.SUBSCRIPTION_ACTION_SUBSCRIBE == commandCode
                ? Type.SUBSCRIBE
                : Messages.Update.SUBSCRIPTION_ACTION_UNSUBSCRIBE == commandCode
                        ? Type.UNSUBSCRIBE
                        : Messages.Update.SUBSCRIPTION_ACTION_UPDATE == commandCode
                                ? Type.UPDATE
                                : Messages.Update.SUBSCRIPTION_ACTION_DELETE == commandCode ? Type.DELETE : null;
        if (null == type) {
            throw new IllegalArgumentException("Unknown Subscription action '" + subscriptionAction + "'");
        }
        return type;
    }

    private SubscriptionChange(
            @NonNull final Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        _type = Objects.requireNonNull(type);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _filterParameter = filterParameter;
    }

    @NonNull
    Type getType() {
        return _type;
    }

    @NonNull
    DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Nullable
    Object getFilterParameter() {
        return _filterParameter;
    }
}
