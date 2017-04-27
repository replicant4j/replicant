package org.realityforge.replicant.server.transport;

public class NoSuchChannelException
  extends RuntimeException
{
  private final int _channelID;

  public NoSuchChannelException( final int channelID )
  {
    _channelID = channelID;
  }

  public int getChannelID()
  {
    return _channelID;
  }

  @Override
  public String toString()
  {
    return "NoSuchChannelException[channelID=" + _channelID + "]";
  }
}
