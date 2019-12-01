package org.realityforge.replicant.server.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  @Nullable
  private final Class _subChannelType;
  @Nonnull
  private final FilterType _filterType;
  private final Class _filterParameterType;
  private final boolean _cacheable;

  public ChannelMetaData( final int channelID,
                          @Nonnull final String name,
                          @Nullable final Class subChannelType,
                          @Nonnull final FilterType filterType,
                          @Nullable final Class filterParameterType,
                          final boolean cacheable )
  {
    _channelID = channelID;
    _name = Objects.requireNonNull( name );
    _typeGraph = null == subChannelType;
    _subChannelType = subChannelType;
    _filterType = Objects.requireNonNull( filterType );
    _filterParameterType = filterParameterType;
    if ( FilterType.NONE == filterType && null != filterParameterType )
    {
      throw new IllegalArgumentException( "FilterParameterType specified but filterType is set to NONE" );
    }
    else if ( FilterType.NONE != filterType && null == filterParameterType )
    {
      throw new IllegalArgumentException( "FilterParameterType not specified but filterType is not set to NONE" );
    }
    _cacheable = cacheable;
  }

  public int getChannelId()
  {
    return _channelID;
  }

  @Nonnull
  public Class getSubChannelType()
  {
    if ( null == _subChannelType )
    {
      throw new IllegalStateException( "getSubChannelType invoked on type graph" );
    }
    return _subChannelType;
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

  @Nonnull
  public Class getFilterParameterType()
  {
    if ( null == _filterParameterType )
    {
      throw new IllegalStateException( "getFilterParameterType invoked on unfiltered graph" );
    }
    return _filterParameterType;
  }

  public boolean isCacheable()
  {
    return _cacheable;
  }
}
