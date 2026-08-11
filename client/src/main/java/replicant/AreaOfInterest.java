package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.spy.AreaOfInterestDisposedEvent;
import zemeckis.Zemeckis;

/**
 * An Area of Interest declares the latest desired Subscription at a Dataset Address.
 *
 * <p>The satisfaction status describes whether the desired Subscription is established. Data Availability is
 * reported independently by {@link #isDataAvailable()} because data can remain locally available while a replacement
 * Subscription is pending.
 */
@ArezComponent(observable = Feature.ENABLE, requireId = Feature.ENABLE)
public abstract class AreaOfInterest extends ReplicantService {
    public enum Status {
        /**
         * The desired Subscription is not currently established.
         */
        PENDING,
        /**
         * An explicit Subscription with the latest desired Filter Parameter is established.
         */
        SATISFIED,
        /**
         * The Dataset Address was invalidated by the server and can never be satisfied in this Replicant Context.
         */
        INVALIDATED
    }

    @NonNull
    private final DatasetAddress _datasetAddress;

    @Nullable
    private Object _filterParameter;

    private boolean _invalidated;
    /**
     * Reference counting determines whether an AreaOfInterest is still of interest.
     * The assumption is that after the refCount reaches 0 then it is likely that there is no longer any interest
     * and it can be disposed.
     */
    private int _refCount;

    @NonNull
    static AreaOfInterest create(
            @Nullable final ReplicantContext context,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final boolean invalidated) {
        return new Arez_AreaOfInterest(context, datasetAddress, filterParameter, invalidated);
    }

    AreaOfInterest(
            @Nullable final ReplicantContext context,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final boolean invalidated) {
        super(context);
        _datasetAddress = Objects.requireNonNull(datasetAddress);
        _filterParameter = filterParameter;
        _invalidated = invalidated;
    }

    public void incRefCount() {
        _refCount++;
    }

    public int getRefCount() {
        return _refCount;
    }

    public void decRefCount() {
        assert _refCount > 0;
        _refCount--;
        if (0 >= _refCount) {
            Zemeckis.delayedTask(Zemeckis.areNamesEnabled() ? "TryDispose-" + this : null, this::tryDispose, 5);
        }
    }

    private void tryDispose() {
        if (Disposable.isNotDisposed(this) && 0 >= getRefCount()) {
            Disposable.dispose(this);
        }
    }

    @PreDispose
    void preDispose() {
        if (Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents()) {
            getReplicantContext().getSpy().reportSpyEvent(new AreaOfInterestDisposedEvent(this));
        }
    }

    @NonNull
    public DatasetAddress getDatasetAddress() {
        return _datasetAddress;
    }

    @Observable
    @Nullable
    public Object getFilterParameter() {
        return _filterParameter;
    }

    void setFilterParameter(@Nullable final Object filterParameter) {
        _filterParameter = filterParameter;
    }

    @Memoize(readOutsideTransaction = Feature.ENABLE)
    @NonNull
    public Status getStatus() {
        if (isInvalidated()) {
            return Status.INVALIDATED;
        }
        final Subscription subscription = getSubscription();
        return null != subscription
                        && SubscriptionMode.EXPLICIT == subscription.getMode()
                        && FilterParameterUtil.filterParametersEqual(
                                getFilterParameter(), subscription.getFilterParameter())
                ? Status.SATISFIED
                : Status.PENDING;
    }

    @Observable
    boolean isInvalidated() {
        return _invalidated;
    }

    void setInvalidated(final boolean invalidated) {
        _invalidated = invalidated;
    }

    @Memoize
    @Nullable
    public Subscription getSubscription() {
        return getReplicantContext().findSubscription(getDatasetAddress());
    }

    /**
     * Return whether complete data for the Dataset Address is currently usable in this Replicant Context.
     *
     * <p>This reports actual Data Availability, independently of {@link #getStatus()}. It can be {@code true} while
     * the Area of Interest is {@link Status#PENDING}, such as while an established Subscription applies a newer
     * Updatable Filter Parameter. It becomes {@code false} when no Subscription exists, including while replacing a
     * Subscription to apply a newer Fixed Filter Parameter and after disconnection. Changes become observable only
     * after the complete server Change Set has been applied atomically.
     *
     * @return true if complete data for the Dataset Address is currently usable in this Replicant Context.
     */
    @Memoize(readOutsideTransaction = Feature.ENABLE)
    public boolean isDataAvailable() {
        return getReplicantContext().isDataAvailable(getDatasetAddress());
    }

    @Override
    public String toString() {
        if (Replicant.areNamesEnabled()) {
            return "AreaOfInterest[" + _datasetAddress
                    + (null == _filterParameter
                            ? ""
                            : " Filter Parameter: " + FilterParameterUtil.filterParameterToString(_filterParameter))
                    + " Status: "
                    + getStatus() + "]";
        } else {
            return super.toString();
        }
    }
}
