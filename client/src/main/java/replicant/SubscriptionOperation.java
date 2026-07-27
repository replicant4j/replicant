package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class SubscriptionOperation {
    public enum Type {
        SUBSCRIBE,
        UNSUBSCRIBE,
        UPDATE
    }

    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    private final Type _type;

    @Nullable
    private final Object _filterParameter;

    private int _requestId;

    SubscriptionOperation(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final Type type,
            @Nullable final Object filterParameter) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> type != Type.UNSUBSCRIBE || null == filterParameter,
                    () -> "Replicant-0027: SubscriptionOperation constructor passed an UNSUBSCRIBE operation for "
                            + "Dataset Address '" + datasetAddress
                            + "' with a non-null Filter Parameter '" + filterParameter
                            + "'.");
        }
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _type = Objects.requireNonNull(type);
        _filterParameter = filterParameter;
        _requestId = -1;
    }

    @NonNull
    DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @NonNull
    Type getType() {
        return _type;
    }

    @Nullable
    Object getFilterParameter() {
        return _filterParameter;
    }

    boolean isInProgress() {
        return -1 != _requestId;
    }

    int getRequestId() {
        return _requestId;
    }

    void markAsInProgress(final int requestId) {
        _requestId = requestId;
    }

    void markAsComplete() {
        _requestId = -1;
    }

    boolean match(
            @NonNull final Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        return getType().equals(type)
                && getDatasetAddress().equals(datasetAddress)
                && (Type.UNSUBSCRIBE == type
                        || FilterParameterUtil.filterParametersEqual(filterParameter, getFilterParameter()));
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            final DatasetAddress datasetAddress = getDatasetAddress();
            return "SubscriptionOperation[" + "Type="
                    + _type + " Address="
                    + datasetAddress
                    + (null == _filterParameter
                            ? ""
                            : " Filter Parameter=" + FilterParameterUtil.filterParameterToString(_filterParameter))
                    + "]" + (-1 != _requestId ? "(InProgress)" : "");
        } else {
            return super.toString();
        }
    }
}
