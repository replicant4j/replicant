package org.realityforge.replicant.client;

public interface ChannelAction
{
  enum Action
  {
    ADD, REMOVE
  }

  int getChannelID();

  String getSubChannelIDAsString();

  int getSubChannelIDAsInt();

  Action getAction();
}
