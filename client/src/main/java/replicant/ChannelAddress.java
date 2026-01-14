package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A channel address is immutable reference that defines the channel address.
 * A "type" channel is addressed by the type while an "instance" channel is addressed
 * by the "type" and the instance "id". Channels that support multiple filter instances
 * include a filter instance id.
 */
public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  private final int _schemaId;
  private final int _channelId;
  @Nullable
  private final Integer _rootId;
  @Nullable
  private final String _filterInstanceId;

  public ChannelAddress( final int schemaId, final int channelId )
  {
    this( schemaId, channelId, null, null );
  }

  public ChannelAddress( final int schemaId, final int channelId, @Nullable final Integer rootId )
  {
    this( schemaId, channelId, rootId, null );
  }

  public ChannelAddress( final int schemaId,
                         final int channelId,
                         @Nullable final Integer rootId,
                         @Nullable final String filterInstanceId )
  {
    _schemaId = schemaId;
    _channelId = channelId;
    _rootId = rootId;
    _filterInstanceId = filterInstanceId;
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

  @Nullable
  public String filterInstanceId()
  {
    return _filterInstanceId;
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
    final StringBuilder sb = new StringBuilder().append( channelId() );
    if ( null != _rootId )
    {
      sb.append( "." ).append( _rootId );
    }
    if ( null != _filterInstanceId )
    {
      sb.append( "#" ).append( _filterInstanceId );
    }
    return sb.toString();
  }

  @Nonnull
  public String getCacheKey()
  {
    return "RC-" + getName();
  }

  @Nonnull
  public static ChannelAddress parse( final int schema, @Nonnull final String channel )
  {
    final int instanceOffset = channel.indexOf( '#' );
    final String channelPart = -1 == instanceOffset ? channel : channel.substring( 0, instanceOffset );
    final String filterInstanceId = -1 == instanceOffset ? null : channel.substring( instanceOffset + 1 );
    final int offset = channelPart.indexOf( ".", 1 );
    final int channelId = Integer.parseInt( -1 == offset ? channelPart : channelPart.substring( 0, offset ) );
    final Integer rootId = -1 == offset ? null : Integer.parseInt( channelPart.substring( offset + 1 ) );
    return new ChannelAddress( schema, channelId, rootId, filterInstanceId );
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
             Objects.equals( _rootId, that._rootId ) &&
             Objects.equals( _filterInstanceId, that._filterInstanceId );
    }
  }

  @Override
  public int hashCode()
  {
    int result = _schemaId;
    result = 17 * result + _channelId;
    result = 31 * result + ( _rootId != null ? _rootId.hashCode() : 0 );
    result = 31 * result + ( _filterInstanceId != null ? _filterInstanceId.hashCode() : 0 );
    return result;
  }

  @Override
  public int compareTo( @Nonnull final ChannelAddress o )
  {
    final int systemDiff = Integer.compare( schemaId(), o.schemaId() );
    if ( 0 != systemDiff )
    {
      return systemDiff;
    }
    else
    {
      final int channelDiff = Integer.compare( channelId(), o.channelId() );
      if ( 0 != channelDiff )
      {
        return channelDiff;
      }
      else
      {
        // Align ordering with equals by comparing rootId as well
        final Integer r1 = rootId();
        final Integer r2 = o.rootId();
        if ( null != r1 || null != r2 )
        {
          if ( null == r1 )
          {
            return -1;
          }
          else if ( null == r2 )
          {
            return 1;
          }
          else
          {
            final int rootDiff = Integer.compare( r1, r2 );
            if ( 0 != rootDiff )
            {
              return rootDiff;
            }
          }
        }
        final String f1 = filterInstanceId();
        final String f2 = o.filterInstanceId();
        if ( null == f1 && null == f2 )
        {
          return 0;
        }
        else if ( null == f1 )
        {
          return -1;
        }
        else if ( null == f2 )
        {
          return 1;
        }
        else
        {
          return f1.compareTo( f2 );
        }
      }
    }
  }
}
