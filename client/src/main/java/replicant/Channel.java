package replicant;

import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * The Channel object contains the address of the channel and the optional filter for the channel.
 */
@ArezComponent
public abstract class Channel
  implements Comparable<Channel>
{
  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private Object _filter;

  @Nonnull
  public static Channel create( @Nonnull final ChannelAddress address )
  {
    return create( address, null );
  }

  @Nonnull
  public static Channel create( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    return new Arez_Channel( address, filter );
  }

  Channel( @Nonnull final ChannelAddress address, @Nullable final Object filter )
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
    if ( Replicant.areNamesEnabled() )
    {
      return "Channel[" + _address + " :: Filter=" + FilterUtil.filterToString( _filter ) + "]";
    }
    else
    {
      return super.toString();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo( @NotNull final Channel o )
  {
    return getAddress().compareTo( o.getAddress() );
  }
}
