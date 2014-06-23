package org.realityforge.replicant.client;

public interface ChannelAction
{
  enum Action
  {
    ADD, REMOVE, UPDATE
  }

  int getChannelID();

  Object getSubChannelID();

  Action getAction();

  /**
   * Get the "raw" filter object.
   * In GWT-land this may need to be cast to a jso before usage.
   *
   * @return the filter object for channel if any.
   */
  Object getChannelFilter();
}
