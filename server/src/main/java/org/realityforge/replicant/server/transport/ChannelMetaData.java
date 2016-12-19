package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;

public final class ChannelMetaData
{
  public enum FilterType
  {
    NONE, STATIC, DYNAMIC
  }

  private final int _channelID;
  @Nonnull
  private final String _name;
  private final boolean _typeGraph;
  @Nonnull
  private final FilterType _filterType;
  private final boolean _cacheable;

  public ChannelMetaData( final int channelID,
                          @Nonnull final String name,
                          final boolean typeGraph,
                          @Nonnull final FilterType filterType,
                          final boolean cacheable )
  {
    _channelID = channelID;
    _name = name;
    _typeGraph = typeGraph;
    _filterType = filterType;
    _cacheable = cacheable;
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

  public boolean isTypeGraph()
  {
    return _typeGraph;
  }

  public boolean isInstanceGraph()
  {
    return !isTypeGraph();
  }

  @Nonnull
  public FilterType getFilterType()
  {
    return _filterType;
  }

  public boolean isCacheable()
  {
    return _cacheable;
  }
}
