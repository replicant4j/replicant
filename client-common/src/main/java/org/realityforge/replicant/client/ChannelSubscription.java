package org.realityforge.replicant.client;

import arez.Arez;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A ChannelSubscription descriptor describes a channel subscription.
 * It includes the address of the channel and the optional filter passed to the channel.
 */
@ArezComponent( disposeOnDeactivate = true )
public abstract class ChannelSubscription
{
  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private Object _filter;

  public static ChannelSubscription create( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    return new Arez_ChannelSubscription( address, filter );
  }

  ChannelSubscription( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    _address = Objects.requireNonNull( address );
    _filter = filter;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @Observable
  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  public void setFilter( @Nullable final Object filter )
  {
    _filter = filter;
  }

  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return "ChannelSubscription[" + _address + " :: Filter=" + FilterUtil.filterToString( _filter ) + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
