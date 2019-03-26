package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A channel address is immutable reference that defines the channel address.
 * A "type" channel is addressed by the type while an "instance" channel is addressed
 * by the "type" and the instance "id"
 */
public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  private final int _systemId;
  private final int _channelId;
  @Nullable
  private final Integer _id;

  public ChannelAddress( final int systemId, final int channelId )
  {
    this( systemId, channelId, null );
  }

  public ChannelAddress( final int systemId, final int channelId, @Nullable final Integer id )
  {
    _systemId = systemId;
    _channelId = channelId;
    _id = id;
  }

  public int getSystemId()
  {
    return _systemId;
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Nullable
  public Integer getId()
  {
    return _id;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }

  @Nonnull
  public String getName()
  {
    return getSystemId() + "." + asChannelDescriptor();
  }

  @Nonnull
  public String asChannelDescriptor()
  {
    return getChannelId() + ( null != _id ? "." + _id : "" );
  }

  @Nonnull
  public String getCacheKey()
  {
    return "RC-" + getName();
  }

  @Nonnull
  public static ChannelAddress parse( final int schema, @Nonnull final String channel )
  {
    final int offset = channel.indexOf( ".", 1 );
    final int channelId =
      Integer.parseInt( -1 == offset ? channel : channel.substring( 0, offset ) );
    final Integer subChannelId = -1 == offset ? null : Integer.parseInt( channel.substring( offset + 1 ) );
    return new ChannelAddress( schema, channelId, subChannelId );
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    else if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    else
    {
      final ChannelAddress that = (ChannelAddress) o;
      return Objects.equals( _systemId, that._systemId ) &&
             Objects.equals( _channelId, that._channelId ) &&
             Objects.equals( _id, that._id );
    }
  }

  @Override
  public int hashCode()
  {
    int result = _systemId;
    result = 17 * result + _channelId;
    result = 31 * result + ( _id != null ? _id.hashCode() : 0 );
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings( "unchecked" )
  @Override
  public int compareTo( @Nonnull final ChannelAddress o )
  {
    final int systemDiff = Integer.compare( getSystemId(), o.getSystemId() );
    return 0 == systemDiff ? Integer.compare( getChannelId(), o.getChannelId() ) : systemDiff;
  }
}
