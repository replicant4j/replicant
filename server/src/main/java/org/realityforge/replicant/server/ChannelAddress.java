package org.realityforge.replicant.server;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChannelAddress
  implements Comparable<ChannelAddress>
{
  private final int _channelId;
  @Nullable
  private final Serializable _subChannelId;

  public ChannelAddress( final int channelId )
  {
    this( channelId, null );
  }

  public ChannelAddress( final int channelId, @Nullable final Serializable subChannelId )
  {
    _channelId = channelId;
    _subChannelId = subChannelId;
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Nullable
  public Serializable getSubChannelId()
  {
    return _subChannelId;
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
    final int otherChannelId = other.getChannelId();
    final int channelID = getChannelId();

    final int channelDiff = ( channelID < otherChannelId ) ? -1 : ( ( channelID == otherChannelId ) ? 0 : 1 );
    if ( 0 != channelDiff )
    {
      return channelDiff;
    }
    else
    {
      final Serializable otherSubChannelId = other.getSubChannelId();
      final Serializable subChannelId = getSubChannelId();
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
        //noinspection unchecked
        return ( (Comparable) subChannelId ).compareTo( otherSubChannelId );
      }
    }
  }

  @Override
  public String toString()
  {
    return "#" + _channelId + ( null == _subChannelId ? "" : "." + _subChannelId ) + "#";
  }
}
