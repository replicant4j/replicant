package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAction;

class TestChannelAction
  implements ChannelAction
{

  private final int _channelId;
  @Nullable
  private final Object _subChannelId;
  private final Action _action;
  @Nullable
  private final Object _filter;

  private TestChannelAction( final int channelId,
                             @Nullable final Object subChannelId,
                             @Nonnull final Action action,
                             @Nullable final Object filter )
  {
    _channelId = channelId;
    _subChannelId = subChannelId;
    _action = action;
    _filter = filter;
  }

  TestChannelAction( final int channelId,
                     @Nullable final Object subChannelId,
                     @Nonnull final Action action )
  {
    this( channelId, subChannelId, action, null );
  }

  @Override
  public int getChannelId()
  {
    return _channelId;
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
