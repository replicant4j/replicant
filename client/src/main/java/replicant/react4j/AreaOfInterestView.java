package replicant.react4j;

import arez.annotations.Action;
import arez.annotations.ArezComponentLike;
import arez.annotations.ComponentDependency;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import arez.annotations.SuppressArezWarnings;
// Temporary workaround until Arez propagates JSpecify nullability to generated observable setters.
import javax.annotation.Nullable;
import org.jspecify.annotations.NonNull;
import react4j.annotations.PostMountOrUpdate;
import replicant.AreaOfInterest;
import replicant.DatasetAddress;
import replicant.Replicant;
import replicant.ReplicantContext;

/**
 * An abstract React4j view that acquires and retains an Area of Interest.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ArezComponentLike
public abstract class AreaOfInterestView {
    // The warning is suppressed as reference is managed on method.
    // We can not convert this field into abstract observable because of some surgery do to work between
    // React/Arez component models.
    @SuppressArezWarnings("Arez:UnmanagedComponentReference")
    @Nullable
    private AreaOfInterest _areaOfInterest;

    @Nullable
    protected Object getFilterParameter() {
        return null;
    }

    @ComponentDependency(action = ComponentDependency.Action.SET_NULL)
    @Observable
    @Nullable
    protected AreaOfInterest getAreaOfInterest() {
        return _areaOfInterest;
    }

    protected void setAreaOfInterest(@Nullable final AreaOfInterest areaOfInterest) {
        _areaOfInterest = areaOfInterest;
    }

    @PreDispose
    protected final void preDispose() {
        if (null != _areaOfInterest) {
            _areaOfInterest.decRefCount();
            _areaOfInterest = null;
        }
    }

    @PostMountOrUpdate
    protected final void postMountOrUpdate() {
        updateAreaOfInterest();
    }

    protected final void updateAreaOfInterestOnFilterParameterChange(@Nullable final Object newFilterParameter) {
        if (null != _areaOfInterest) {
            Replicant.context().createOrUpdateAreaOfInterest(_areaOfInterest.getDatasetAddress(), newFilterParameter);
        }
    }

    @Action
    protected void updateAreaOfInterest() {
        final ReplicantContext context = Replicant.context();
        final AreaOfInterest newAreaOfInterest =
                context.createOrUpdateAreaOfInterest(getDatasetAddress(), getFilterParameter());
        if (null == _areaOfInterest) {
            newAreaOfInterest.incRefCount();
            setAreaOfInterest(newAreaOfInterest);
        }
    }

    @NonNull
    protected abstract DatasetAddress getDatasetAddress();
}
