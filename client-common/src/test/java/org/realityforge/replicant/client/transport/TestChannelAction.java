package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class TestChannelAction
  implements ChannelAction
{

  private final int _channelID;
  @Nullable
  private final Object _subChannelID;
  private final Action _action;
  @Nullable
  private final Object _filter;

  private TestChannelAction( final int channelID,
                             @Nullable final Object subChannelID,
                             @Nonnull final Action action,
                             @Nullable final Object filter )
  {
    _channelID = channelID;
    _subChannelID = subChannelID;
    _action = action;
    _filter = filter;
  }

  TestChannelAction( final int channelID,
                     @Nullable final Object subChannelID,
                     @Nonnull final Action action )
  {
    this( channelID, subChannelID, action, null );
  }

  @Override
  public int getChannelID()
  {
    return _channelID;
  }

  @Nullable
  @Override
  public Object getSubChannelID()
  {
    return _subChannelID;
  }

  @Nonnull
  @Override
  public Action getAction()
  {
    return _action;
  }

  @Nullable
  @Override
  public Object getChannelFilter()
  {
    return _filter;
  }
}
