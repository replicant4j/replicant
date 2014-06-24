package org.realityforge.replicant.client.transport;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Summary describing the result of a data load action.
 */
public final class DataLoadStatus
{
  private final int _sequence;
  private final boolean _bulkLoad;
  @Nullable
  private final String _requestID;
  @Nonnull
  private final List<ChannelChangeStatus> _channelAdds;
  @Nonnull
  private final List<ChannelChangeStatus> _channelUpdates;
  @Nonnull
  private final List<ChannelChangeStatus> _channelRemoves;

  // The number of entities created or updated as part of change message
  private final int _entityUpdateCount;
  // The number of entities removed as part of change message
  private final int _entityRemoveCount;
  // The number of entities where link() was invoked
  private final int _entityLinkCount;

  public DataLoadStatus( final int sequence,
                         final boolean bulkLoad,
                         final String requestID,
                         @Nonnull final List<ChannelChangeStatus> channelAdds,
                         @Nonnull final List<ChannelChangeStatus> channelUpdates,
                         @Nonnull final List<ChannelChangeStatus> channelRemoves,
                         final int entityUpdateCount,
                         final int entityRemoveCount,
                         final int entityLinkCount )
  {
    _sequence = sequence;
    _bulkLoad = bulkLoad;
    _requestID = requestID;
    _channelAdds = channelAdds;
    _channelUpdates = channelUpdates;
    _channelRemoves = channelRemoves;
    _entityUpdateCount = entityUpdateCount;
    _entityRemoveCount = entityRemoveCount;
    _entityLinkCount = entityLinkCount;
  }

  public int getSequence()
  {
    return _sequence;
  }

  public boolean isBulkLoad()
  {
    return _bulkLoad;
  }

  @Nullable
  public String getRequestID()
  {
    return _requestID;
  }

  @Nonnull
  public List<ChannelChangeStatus> getChannelAdds()
  {
    return _channelAdds;
  }

  @Nonnull
  public List<ChannelChangeStatus> getChannelUpdates()
  {
    return _channelUpdates;
  }

  @Nonnull
  public List<ChannelChangeStatus> getChannelRemoves()
  {
    return _channelRemoves;
  }

  public int getEntityUpdateCount()
  {
    return _entityUpdateCount;
  }

  public int getEntityRemoveCount()
  {
    return _entityRemoveCount;
  }

  public int getEntityLinkCount()
  {
    return _entityLinkCount;
  }
}
