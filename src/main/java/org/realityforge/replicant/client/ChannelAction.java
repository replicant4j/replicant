package org.realityforge.replicant.client;

public interface ChannelAction
{
  enum Action
  {
    ADD, REMOVE
  }

  int getChannelID();

  Object getSubChannelID();

  Action getAction();
}
