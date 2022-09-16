package org.realityforge.replicant.server.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@SuppressWarnings( "WeakerAccess" )
public final class ChannelMetaData
{
  public enum FilterType
  {
    /**
     * No filtering
     */
    NONE,
    /**
     * Filtering occurs but no parameter is passed to control such behaviour. Filtering rules are internal to the data.
     */
    INTERNAL,
    /**
     * Filtering occurs and the client passes a filter parameter but can never change the filter parameter without unsubscribing and resubscribing to graph.
     */
    STATIC,
    /**
     * Filtering occurs and the client passes a filter parameter and can change the filter parameter.
     */
    DYNAMIC
  }

  public enum CacheType
  {
    /**
     * No caching
     */
    NONE,
    /**
     * Caching is managed internally by replicant. If a change arrives for an entity in the graph then the
     * cache is expired.
     */
    INTERNAL,
    /**
     * Application code is responsible for managing the cache and expiring data when it is stale.
     */
    EXTERNAL
  }

  private final int _channelId;
  @Nonnull
  private final String _name;
  @Nullable
  private final Integer _instanceRootEntityTypeId;
  @Nonnull
  private final FilterType _filterType;
  private final Class<?> _filterParameterType;
  @Nonnull
  private final CacheType _cacheType;
  /**
   * Flag indicating whether it is valid to attempt to perform bulk loads for channel.
   */
  private final boolean _bulkLoadsSupported;
  /**
   * Flag indicating whether the channel can be subscribed to, externally.
   * i.e. Can this be explicitly subscribed.
   */
  private final boolean _external;
  @Nonnull
  private final ChannelMetaData[] _requiredTypeChannels;

  public ChannelMetaData( final int channelId,
                          @Nonnull final String name,
                          @Nullable final Integer instanceRootEntityTypeId,
                          @Nonnull final FilterType filterType,
                          @Nullable final Class<?> filterParameterType,
                          @Nonnull final CacheType cacheType,
                          final boolean bulkLoadsSupported,
                          final boolean external,
                          @Nonnull final ChannelMetaData... requiredTypeGraphs )
  {
    _channelId = channelId;
    _name = Objects.requireNonNull( name );
    _instanceRootEntityTypeId = instanceRootEntityTypeId;
    _filterType = Objects.requireNonNull( filterType );
    _filterParameterType = filterParameterType;
    _cacheType = Objects.requireNonNull( cacheType );
    _bulkLoadsSupported = bulkLoadsSupported;
    _external = external;
    _requiredTypeChannels = Objects.requireNonNull( requiredTypeGraphs );
    if ( !hasFilterParameter() && null != filterParameterType )
    {
      throw new IllegalArgumentException( "FilterParameterType specified but filterType is set to " + filterType );
    }
    else if ( hasFilterParameter() && null == filterParameterType )
    {
      throw new IllegalArgumentException( "FilterParameterType not specified but filterType is set to " + filterType );
    }
    for ( final ChannelMetaData requiredTypeChannel : _requiredTypeChannels )
    {
      if ( requiredTypeChannel.isInstanceGraph() )
      {
        throw new IllegalArgumentException( "Specified RequiredTypeChannel " + requiredTypeChannel.getName() +
                                            " is not a type channel" );
      }
    }
  }

  public int getChannelId()
  {
    return _channelId;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  public boolean isTypeGraph()
  {
    return null == _instanceRootEntityTypeId;
  }

  public boolean isInstanceGraph()
  {
    return !isTypeGraph();
  }

  @Nonnull
  public Integer getInstanceRootEntityTypeId()
  {
    assert null != _instanceRootEntityTypeId;
    return _instanceRootEntityTypeId;
  }

  @Nonnull
  public FilterType getFilterType()
  {
    return _filterType;
  }

  public boolean hasFilterParameter()
  {
    return FilterType.DYNAMIC == _filterType || FilterType.STATIC == _filterType;
  }

  @Nonnull
  public Class<?> getFilterParameterType()
  {
    assert null != _filterParameterType;
    return _filterParameterType;
  }

  public boolean isCacheable()
  {
    return CacheType.NONE != _cacheType;
  }

  @Nonnull
  public CacheType getCacheType()
  {
    return _cacheType;
  }

  public boolean areBulkLoadsSupported()
  {
    return _bulkLoadsSupported;
  }

  public boolean isExternal()
  {
    return _external;
  }

  @Nonnull
  public ChannelMetaData[] getRequiredTypeChannels()
  {
    return _requiredTypeChannels;
  }
}
