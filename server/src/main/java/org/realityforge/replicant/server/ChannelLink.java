package org.realityforge.replicant.server;

import javax.annotation.Nonnull;

/**
 * A representation indicating that an entity message will cause another channel to be subscribed.
 */
public final class ChannelLink
{
  @Nonnull
  private final ChannelDescriptor _sourceChannel;
  @Nonnull
  private final ChannelDescriptor _targetChannel;

  public ChannelLink( @Nonnull final ChannelDescriptor sourceChannel,
                      @Nonnull final ChannelDescriptor targetChannel )
  {
    _sourceChannel = sourceChannel;
    _targetChannel = targetChannel;
  }

  @Nonnull
  public ChannelDescriptor getSourceChannel()
  {
    return _sourceChannel;
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
    return _sourceChannel.equals( that._sourceChannel ) && _targetChannel.equals( that._targetChannel );
  }

  @Override
  public int hashCode()
  {
    return _sourceChannel.hashCode() + _targetChannel.hashCode();
  }

  @Override
  public String toString()
  {
    return "[" + _sourceChannel.toString() + "=>" + _targetChannel.toString() + "]";
  }
}
