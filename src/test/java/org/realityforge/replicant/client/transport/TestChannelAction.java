package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAction;

class TestChannelAction
  implements ChannelAction
{

  private final int _channelID;
  @Nullable
  private final Object _subChannelID;
  private final Action _action;

  TestChannelAction( final int channelID, final Object subChannelID, final Action action )
  {
    _channelID = channelID;
    _subChannelID = subChannelID;
    _action = action;
  }

  @Override
  public int getChannelID()
  {
    return _channelID;
  }

  @Override
  public Object getSubChannelID()
  {
    return _subChannelID;
  }

  @Override
  public Action getAction()
  {
    return _action;
  }

  @Override
  public Object getChannelFilter()
  {
    return null;
  }
}
