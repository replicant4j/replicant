package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  private final int _channelId;
  @Nullable
  private final Integer _subChannelId;

  @Nonnull
  public static ChannelAddress parse( @Nonnull final String name )
  {
    final int offset = name.indexOf( "." );
    final int channelId = Integer.parseInt( -1 == offset ? name : name.substring( 0, offset ) );
    final Integer subChannelId = -1 == offset ? null : Integer.parseInt( name.substring( offset + 1 ) );
    return new ChannelAddress( channelId, subChannelId );
  }

  public ChannelAddress( final int channelId )
  {
    this( channelId, null );
  }

  public ChannelAddress( final int channelId, @Nullable final Integer subChannelId )
  {
    _channelId = channelId;
    _subChannelId = subChannelId;
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Nullable
  public Integer getSubChannelId()
  {
    return _subChannelId;
  }

  public boolean hasSubChannelId()
  {
    return null != _subChannelId;
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
    return _channelId == that._channelId &&
           !( _subChannelId != null ? !_subChannelId.equals( that._subChannelId ) : null != that._subChannelId );
  }

  @Override
  public int hashCode()
  {
    int result = _channelId;
    result = 31 * result + ( _subChannelId != null ? _subChannelId.hashCode() : 0 );
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
      final Integer otherSubChannelId = other.getSubChannelId();
      final Integer subChannelId = getSubChannelId();
      if ( null == otherSubChannelId && null == subChannelId )
      {
        return 0;
      }
      else if ( null == otherSubChannelId )
      {
        return -1;
      }
      else if ( null == subChannelId )
      {
        return 1;
      }
      else
      {
        return subChannelId.compareTo( otherSubChannelId );
      }
    }
  }

  @Override
  public String toString()
  {
    return _channelId + ( null == _subChannelId ? "" : "." + _subChannelId );
  }
}
