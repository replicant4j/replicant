package org.realityforge.replicant.server;

import java.io.Serializable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A record of a change in channel subscriptions.
 */
public class ChannelAction
{
  public static enum Action
  {
    ADD, REMOVE
  }
  private final int _channelID;
  @Nullable
  private final Serializable _subChannelID;
  @Nonnull
  private final Action _action;

  public ChannelAction( final int channelID,
                        @Nullable final Serializable subChannelID,
                        @Nonnull final Action action )
  {
    _channelID = channelID;
    _subChannelID = subChannelID;
    _action = action;
  }

  public int getID()
  {
    return _channelID;
  }

  @Nullable
  public Serializable getSubChannelID()
  {
    return _subChannelID;
  }

  @Nonnull
  public Action getAction()
  {
    return _action;
  }
}
