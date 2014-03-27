package org.realityforge.replicant.server;

import javax.annotation.Nonnull;

/**
 * A record of a change in channel subscriptions.
 */
public final class ChannelAction
{
  public static enum Action
  {
    ADD, REMOVE
  }
  @Nonnull
  private final ChannelDescriptor _channelDescriptor;
  @Nonnull
  private final Action _action;

  public ChannelAction( @Nonnull final ChannelDescriptor descriptor, @Nonnull final Action action )
  {
    _channelDescriptor = descriptor;
    _action = action;
  }

  @Nonnull
  public ChannelDescriptor getChannelDescriptor()
  {
    return _channelDescriptor;
  }

  @Nonnull
  public Action getAction()
  {
    return _action;
  }
}
