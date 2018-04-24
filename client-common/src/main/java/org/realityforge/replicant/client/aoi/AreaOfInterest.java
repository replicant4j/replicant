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
import org.realityforge.replicant.client.ChannelSubscriptionEntry;

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
  @Nullable
  private ChannelSubscriptionEntry _entry;
  @Nullable
  private Throwable _error;

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
  public Channel getChannel()
  {
    return _channel;
  }

  @Observable
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
  public Throwable getError()
  {
    return _error;
  }

  public void setError( @Nullable final Throwable error )
  {
    _error = error;
  }

  @Observable
  @Nullable
  public ChannelSubscriptionEntry getEntry()
  {
    return _entry;
  }

  public void setEntry( @Nullable final ChannelSubscriptionEntry entry )
  {
    _entry = entry;
  }

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
