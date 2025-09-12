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
  private final int _schemaId;
  private final int _channelId;
  @Nullable
  private final Integer _rootId;

  public ChannelAddress( final int schemaId, final int channelId )
  {
    this( schemaId, channelId, null );
  }

  public ChannelAddress( final int schemaId, final int channelId, @Nullable final Integer rootId )
  {
    _schemaId = schemaId;
    _channelId = channelId;
    _rootId = rootId;
  }

  public int schemaId()
  {
    return _schemaId;
  }

  public int channelId()
  {
    return _channelId;
  }

  @Nullable
  public Integer rootId()
  {
    return _rootId;
  }

  @Override
  public String toString()
  {
    return Replicant.areNamesEnabled() ? getName() : super.toString();
  }

  @Nonnull
  public String getName()
  {
    return schemaId() + "." + asChannelDescriptor();
  }

  @Nonnull
  public String asChannelDescriptor()
  {
    return channelId() + ( null != _rootId ? "." + _rootId : "" );
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
    final Integer rootId = -1 == offset ? null : Integer.parseInt( channel.substring( offset + 1 ) );
    return new ChannelAddress( schema, channelId, rootId );
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
      return Objects.equals( _schemaId, that._schemaId ) &&
             Objects.equals( _channelId, that._channelId ) &&
             Objects.equals( _rootId, that._rootId );
    }
  }

  @Override
  public int hashCode()
  {
    int result = _schemaId;
    result = 17 * result + _channelId;
    result = 31 * result + ( _rootId != null ? _rootId.hashCode() : 0 );
    return result;
  }

  @Override
  public int compareTo( @Nonnull final ChannelAddress o )
  {
    final int systemDiff = Integer.compare( schemaId(), o.schemaId() );
    return 0 == systemDiff ? Integer.compare( channelId(), o.channelId() ) : systemDiff;
  }
}
