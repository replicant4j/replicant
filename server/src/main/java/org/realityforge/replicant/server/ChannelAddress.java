package org.realityforge.replicant.server;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings( "ClassCanBeRecord" )
public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  private final int _channelId;
  @Nullable
  private final Integer _rootId;

  @Nonnull
  public static ChannelAddress parse( @Nonnull final String name )
  {
    final int offset = name.indexOf( "." );
    final int channelId = Integer.parseInt( -1 == offset ? name : name.substring( 0, offset ) );
    final Integer rootId = -1 == offset ? null : Integer.parseInt( name.substring( offset + 1 ) );
    return new ChannelAddress( channelId, rootId );
  }

  public ChannelAddress( final int channelId )
  {
    this( channelId, null );
  }

  public ChannelAddress( final int channelId, @Nullable final Integer rootId )
  {
    _channelId = channelId;
    _rootId = rootId;
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Nullable
  public Integer getRootId()
  {
    return _rootId;
  }

  public boolean hasSubChannelId()
  {
    return null != _rootId;
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    final ChannelAddress that = (ChannelAddress) o;
    return _channelId == that._channelId && Objects.equals( _rootId, that._rootId );
  }

  @Override
  public int hashCode()
  {
    int result = _channelId;
    result = 31 * result + ( _rootId != null ? _rootId.hashCode() : 0 );
    return result;
  }

  @Override
  public int compareTo( @Nonnull final ChannelAddress other )
  {
    final int channelDiff = Integer.compare( getChannelId(), other.getChannelId() );
    if ( 0 != channelDiff )
    {
      return channelDiff;
    }
    else
    {
      final Integer otherRootId = other.getRootId();
      final Integer rootId = getRootId();
      if ( null == otherRootId && null == rootId )
      {
        return 0;
      }
      else if ( null == otherRootId )
      {
        return -1;
      }
      else if ( null == rootId )
      {
        return 1;
      }
      else
      {
        return rootId.compareTo( otherRootId );
      }
    }
  }

  @Override
  public String toString()
  {
    return _channelId + ( null == _rootId ? "" : "." + _rootId );
  }
}
