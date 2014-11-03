package org.realityforge.replicant.server;

import javax.annotation.Nonnull;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public final class ChannelLink
{
  @Nonnull
  private final ChannelDescriptor _targetChannel;

  public ChannelLink( @Nonnull final ChannelDescriptor targetChannel )
  {
    if ( null == targetChannel.getSubChannelID() )
    {
      throw new IllegalArgumentException( "Channel Link sub-channel id should not be null" );
    }
    _targetChannel = targetChannel;
  }

  @Nonnull
  public ChannelDescriptor getTargetChannel()
  {
    return _targetChannel;
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

    final ChannelLink that = (ChannelLink) o;
    return _targetChannel.equals( that._targetChannel );
  }

  @Override
  public int hashCode()
  {
    return _targetChannel.hashCode();
  }

  @Override
  public String toString()
  {
    return _targetChannel.toString();
  }
}
