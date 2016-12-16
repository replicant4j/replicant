package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;

public final class ChannelMetaData
{
  enum FilterType
  {
    NONE, STATIC, DYNAMIC
  }

  private final int _channelID;
  @Nonnull
  private final String _name;
  @Nonnull
  private final FilterType _filterType;

  public ChannelMetaData( final int channelID,
                          @Nonnull final String name,
                          @Nonnull final FilterType filterType )
  {
    _channelID = channelID;
    _name = name;
    _filterType = filterType;
  }

  public int getChannelID()
  {
    return _channelID;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  @Nonnull
  public FilterType getFilterType()
  {
    return _filterType;
  }
}
