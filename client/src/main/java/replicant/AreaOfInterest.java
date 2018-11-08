package replicant;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * The channel description declares a desired channel subscription and also
 * includes data on the current status of the subscription.
 */
@ArezComponent( observable = Feature.ENABLE, requireId = Feature.ENABLE )
public abstract class AreaOfInterest
  extends ReplicantService
{
  public enum Status
  {
    NOT_ASKED,
    LOADING,
    LOADED,
    LOAD_FAILED,
    UPDATING,
    UPDATED,
    UPDATE_FAILED,
    UNLOADING,
    UNLOADED
  }

  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private Object _filter;
  @Nonnull
  private Status _status = Status.NOT_ASKED;

  @Nonnull
  static AreaOfInterest create( @Nullable final ReplicantContext context,
                                @Nonnull final ChannelAddress address,
                                @Nullable final Object filter )
  {
    return new Arez_AreaOfInterest( context, address, filter );
  }

  AreaOfInterest( @Nullable final ReplicantContext context,
                  @Nonnull final ChannelAddress address,
                  @Nullable final Object filter )
  {
    super( context );
    _address = Objects.requireNonNull( address );
    _filter = filter;
  }

  @PreDispose
  final void preDispose()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestDisposedEvent( this ) );
    }
  }

  @Nonnull
  public final ChannelAddress getAddress()
  {
    return _address;
  }

  @Observable
  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  void setFilter( @Nullable final Object filter )
  {
    _filter = filter;
  }

  @Observable( readOutsideTransaction = true )
  @Nonnull
  public Status getStatus()
  {
    return _status;
  }

  void setStatus( @Nonnull final Status status )
  {
    _status = Objects.requireNonNull( status );
  }

  @Observable
  @Nullable
  public abstract Throwable getError();

  abstract void setError( @Nullable Throwable error );

  @Memoize
  @Nullable
  public Subscription getSubscription()
  {
    final boolean expectSubscription = shouldExpectSubscription( getStatus() );
    return expectSubscription ? getReplicantContext().findSubscription( getAddress() ) : null;
  }

  /**
   * Update the status of the AreaOfInterest.
   */
  @Action
  void updateAreaOfInterest( @Nonnull final Status status, @Nullable final Throwable error )
  {
    final boolean expectSubscription = shouldExpectSubscription( status );
    if ( Replicant.shouldCheckApiInvariants() )
    {
      final boolean expectError = Status.LOAD_FAILED == status || Status.UPDATE_FAILED == status;
      final Subscription subscription = getReplicantContext().findSubscription( getAddress() );

      final ChannelAddress address = getAddress();
      apiInvariant( () -> !expectError || null != error,
                    () -> "Replicant-0016: Invoked updateAreaOfInterest for channel at address " +
                          address + " with status " + status + " but failed to supply " +
                          "the expected error." );
      apiInvariant( () -> expectError || null == error,
                    () -> "Replicant-0017: Invoked updateAreaOfInterest for channel at address " +
                          address + " with status " + status + " and supplied an unexpected error." );
      apiInvariant( () -> expectSubscription || null == subscription,
                    () -> "Replicant-0019: Invoked updateAreaOfInterest for channel at address " +
                          address + " with status " + status + " and found unexpected subscription in the context." );
    }

    setStatus( status );
    setError( error );
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestStatusUpdatedEvent( this ) );
    }
  }

  private boolean shouldExpectSubscription( @Nonnull final Status status )
  {
    return Status.LOADED == status ||
           Status.UPDATED == status ||
           Status.UPDATING == status ||
           Status.UPDATE_FAILED == status ||
           Status.UNLOADING == status;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "AreaOfInterest[" + _address +
             ( null == _filter ? "" : " Filter: " + FilterUtil.filterToString( _filter ) ) +
             " Status: " + _status + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
