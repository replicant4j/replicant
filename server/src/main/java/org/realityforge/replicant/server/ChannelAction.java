package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * A record of a change in channel subscriptions.
 */
public final class ChannelAction
{
  public enum Action
  {
    // The channel has been successfully added.
    ADD,
    // The channel has been removed. This could be as a result of client request or as a result of the
    // filter excluding the graph, as the root instance being deleted
    REMOVE,
    // The filter associated with the channel has been updated
    UPDATE,
    // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
    DELETE
  }

  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final Action _action;
  @Nullable
  private final JsonObject _filter;

  public ChannelAction( @Nonnull final ChannelAddress address,
                        @Nonnull final Action action,
                        @Nullable final JsonObject filter )
  {
    _address = address;
    _action = action;
    _filter = filter;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
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
