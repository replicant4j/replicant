package org.realityforge.replicant.server;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChannelDescriptor
  implements Comparable<ChannelDescriptor>
{
  private final int _channelID;
  @Nullable
  private final Serializable _subChannelID;

  public ChannelDescriptor( final int channelID, @Nullable final Serializable subChannelID )
  {
    _channelID = channelID;
    _subChannelID = subChannelID;
  }

  public int getChannelID()
  {
    return _channelID;
  }

  @Nullable
  public Serializable getSubChannelID()
  {
    return _subChannelID;
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

    final ChannelDescriptor that = (ChannelDescriptor) o;
    return _channelID == that._channelID &&
           !( _subChannelID != null ? !_subChannelID.equals( that._subChannelID ) : null != that._subChannelID );
  }

  @Override
  public int hashCode()
  {
    int result = _channelID;
    result = 31 * result + ( _subChannelID != null ? _subChannelID.hashCode() : 0 );
    return result;
  }

  @Override
  public int compareTo( @Nonnull final ChannelDescriptor other )
  {
    final int otherChannelID = other.getChannelID();
    final int channelID = getChannelID();

    final int channelDiff = ( channelID < otherChannelID ) ? -1 : ( ( channelID == otherChannelID ) ? 0 : 1 );
    if ( 0 != channelDiff )
    {
      return channelDiff;
    }
    else
    {
      final Serializable otherSubChannelID = other.getSubChannelID();
      final Serializable subChannelID = getSubChannelID();
      if ( null == otherSubChannelID && null == subChannelID )
      {
        return 0;
      }
      else if ( null == otherSubChannelID )
      {
        return -1;
      }
      else if ( null == subChannelID )
      {
        return 1;
      }
      else
      {
        //noinspection unchecked
        return ( (Comparable) subChannelID ).compareTo( (Comparable) otherSubChannelID );
      }
    }
  }

  @Override
  public String toString()
  {
    return "#" + _channelID + ( null == _subChannelID ? "" : "." + _subChannelID ) + "#";
  }
}
