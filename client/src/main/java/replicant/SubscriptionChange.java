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
        INVALIDATE_DATASET_ADDRESS
    }

    @NonNull
    private final Type _type;

    @NonNull
    private final DatasetAddress _datasetAddress;

    @Nullable
    private final Object _filterParameter;

    @NonNull
    static SubscriptionChange from(final int schema, @NonNull final String subscriptionChange) {
        return from(schema, subscriptionChange, null);
    }

    @NonNull
    static SubscriptionChange from(final int schema, @NonNull final SubscriptionChangeMessage subscriptionChange) {
        return from(schema, subscriptionChange.getSubscriptionChange(), subscriptionChange.getFilterParameter());
    }

    @NonNull
    private static SubscriptionChange from(
            final int schema, @NonNull final String subscriptionChange, @Nullable final Object filterParameter) {
        try {
            final String descriptor = subscriptionChange.substring(1);
            final DatasetAddress datasetAddress = DatasetAddress.parse(schema, descriptor);
            return new SubscriptionChange(changeToType(subscriptionChange), datasetAddress, filterParameter);
        } catch (final Throwable t) {
            throw new IllegalStateException("Failed to parse Subscription Change '" + subscriptionChange + "'", t);
        }
    }

    @NonNull
    private static Type changeToType(@NonNull final String subscriptionChange) {
        assert !subscriptionChange.isEmpty();
        final char commandCode = subscriptionChange.charAt(0);
        final Type type = Messages.Update.SUBSCRIPTION_CHANGE_SUBSCRIBE == commandCode
                ? Type.SUBSCRIBE
                : Messages.Update.SUBSCRIPTION_CHANGE_UNSUBSCRIBE == commandCode
                        ? Type.UNSUBSCRIBE
                        : Messages.Update.SUBSCRIPTION_CHANGE_UPDATE == commandCode
                                ? Type.UPDATE
                                : Messages.Update.SUBSCRIPTION_CHANGE_INVALIDATE_DATASET_ADDRESS == commandCode
                                        ? Type.INVALIDATE_DATASET_ADDRESS
                                        : null;
        if (null == type) {
            throw new IllegalArgumentException("Unknown Subscription Change '" + subscriptionChange + "'");
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
