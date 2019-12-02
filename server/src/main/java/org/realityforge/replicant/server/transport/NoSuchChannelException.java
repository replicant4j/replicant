package org.realityforge.replicant.server.transport;

public class NoSuchChannelException
  extends RuntimeException
{
  private final int _channelId;

  public NoSuchChannelException( final int channelId )
  {
    _channelId = channelId;
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Override
  public String toString()
  {
    return "NoSuchChannelException[channelId=" + _channelId + "]";
  }
}
