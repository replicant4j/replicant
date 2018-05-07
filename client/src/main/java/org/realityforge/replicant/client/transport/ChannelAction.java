package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ChannelAction
{
  enum Action
  {
    ADD, REMOVE, UPDATE
  }

  int getChannelId();

  @Nullable
  Integer getSubChannelId();

  @Nonnull
  Action getAction();

  /**
   * Get the "raw" filter object.
   * In GWT-land this may need to be cast to a jso before usage.
   *
   * @return the filter object for channel if any.
   */
  @Nullable
  Object getChannelFilter();
}
