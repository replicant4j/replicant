package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class AreaOfInterestRequest {
    public enum Type {
        ADD,
        REMOVE,
        UPDATE
    }

    @NonNull
    private final DatasetAddress _datasetAddress;

    @NonNull
    private final Type _type;

    @Nullable
    private final Object _filterParameter;

    private int _requestId;

    AreaOfInterestRequest(
            @NonNull final DatasetAddress datasetAddress,
            @NonNull final Type type,
            @Nullable final Object filterParameter) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> type != Type.REMOVE || null == filterParameter,
                    () -> "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE "
                            + "request for Dataset Address '" + datasetAddress
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
            @NonNull final Type action,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter) {
        return getType().equals(action)
                && getDatasetAddress().equals(datasetAddress)
                && (Type.REMOVE == action
                        || FilterParameterUtil.filterParametersEqual(filterParameter, getFilterParameter()));
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            final DatasetAddress datasetAddress = getDatasetAddress();
            return "AreaOfInterestRequest[" + "Type="
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
