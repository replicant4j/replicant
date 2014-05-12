package org.realityforge.replicant.server;

import java.io.Serializable;
import javax.annotation.Nullable;

public final class ChannelDescriptor
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
  public String toString()
  {
    return "#" + _channelID + ( null == _subChannelID ? "" : "." + _subChannelID ) + "#";
  }
}
