package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAction;

class TestChannelAction
  implements ChannelAction
{

  private final int _channelID;
  @Nullable
  private final Object _subChannelId;
  private final Action _action;
  @Nullable
  private final Object _filter;

  private TestChannelAction( final int channelID,
                             @Nullable final Object subChannelId,
                             @Nonnull final Action action,
                             @Nullable final Object filter )
  {
    _channelID = channelID;
    _subChannelId = subChannelId;
    _action = action;
    _filter = filter;
  }

  TestChannelAction( final int channelID,
                     @Nullable final Object subChannelId,
                     @Nonnull final Action action )
  {
    this( channelID, subChannelId, action, null );
  }

  @Override
  public int getChannelId()
  {
    return _channelID;
  }

  @Nullable
  @Override
  public Object getSubChannelId()
  {
    return _subChannelId;
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
