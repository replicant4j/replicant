package org.realityforge.replicant.client.aoi;

import arez.Arez;
import arez.annotations.ArezComponent;
import arez.annotations.ComponentId;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.subscription.Subscription;

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

  @Nonnull
  private final Channel _channel;
  @Nonnull
  private Status _status = Status.NOT_ASKED;

  @Nonnull
  public static AreaOfInterest create( @Nonnull final Channel channel )
  {
    return new Arez_AreaOfInterest( channel );
  }

  AreaOfInterest( @Nonnull final Channel channel )
  {
    _channel = Objects.requireNonNull( channel );
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
  public abstract Subscription getEntry();

  public abstract void setEntry( @Nullable Subscription entry );

  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return "AreaOfInterest[" + _channel + " Status: " + _status + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
