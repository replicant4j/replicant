package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * A record of a change in channel subscriptions.
 */
public final class ChannelAction
{
  public static enum Action
  {
    ADD, REMOVE, UPDATE
  }
  @Nonnull
  private final ChannelDescriptor _channelDescriptor;
  @Nonnull
  private final Action _action;
  @Nullable
  private final JsonObject _filter;

  public ChannelAction( @Nonnull final ChannelDescriptor descriptor,
                        @Nonnull final Action action,
                        @Nullable final JsonObject filter )
  {
    _channelDescriptor = descriptor;
    _action = action;
    _filter = filter;
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

  @Nullable
  public JsonObject getFilter()
  {
    return _filter;
  }
}
