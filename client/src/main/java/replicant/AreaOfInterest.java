package replicant;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentId;
import arez.annotations.Observable;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.AreaOfInterestDisposedEvent;

/**
 * The channel description declares a desired channel subscription and also
 * includes data on the current status of the subscription.
 */
@ArezComponent
public abstract class AreaOfInterest
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

  /**
   * Reference to the container that created AreaOfInterest.
   * In the future this reference should be eliminated when there is a way to get to the singleton
   * AreaOfInterestService. (Similar to the way we have Arez.context().X we should have Replicant.context().X)
   * This will save memory resources on the client.
   */
  @Nonnull
  private final AreaOfInterestService _areaOfInterestService;
  @Nonnull
  private final Channel _channel;
  @Nonnull
  private Status _status = Status.NOT_ASKED;

  @Nonnull
  static AreaOfInterest create( @Nonnull final AreaOfInterestService areaOfInterestService,
                                @Nonnull final Channel channel )
  {
    return new Arez_AreaOfInterest( areaOfInterestService, channel );
  }

  AreaOfInterest( @Nonnull final AreaOfInterestService areaOfInterestService, @Nonnull final Channel channel )
  {
    _areaOfInterestService = Objects.requireNonNull( areaOfInterestService );
    _channel = Objects.requireNonNull( channel );
  }

  @PreDispose
  final void preDispose()
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestDisposedEvent( this ) );
    }
  }

  @ComponentId
  @Nonnull
  public final ChannelAddress getAddress()
  {
    return getChannel().getAddress();
  }

  @Nonnull
  public final Channel getChannel()
  {
    return _channel;
  }

  @Observable
  @Nonnull
  public Status getStatus()
  {
    return _status;
  }

  public void setStatus( @Nonnull final Status status )
  {
    _status = Objects.requireNonNull( status );
  }

  @Observable
  @Nullable
  public abstract Throwable getError();

  public abstract void setError( @Nullable Throwable error );

  @Observable
  @Nullable
  public abstract Subscription getSubscription();

  public abstract void setSubscription( @Nullable Subscription subscription );

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "AreaOfInterest[" + _channel + " Status: " + _status + "]";
    }
    else
    {
      return super.toString();
    }
  }

  @Nonnull
  private ReplicantContext getReplicantContext()
  {
    return getAreaOfInterestService().getReplicantContext();
  }

  @Nonnull
  final AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }
}
