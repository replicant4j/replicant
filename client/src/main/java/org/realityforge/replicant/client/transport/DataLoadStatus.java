package org.realityforge.replicant.client.transport;

import arez.Arez;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Summary describing the result of a data load action.
 */
public final class DataLoadStatus
{
  /**
   * A key that uniquely identifies the data source that initiated the data load.
   */
  @Nonnull
  private final String _systemKey;
  private final int _sequence;
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

  public DataLoadStatus( @Nonnull final String systemKey,
                         final int sequence,
                         @Nullable final String requestID,
                         @Nonnull final List<ChannelChangeStatus> channelAdds,
                         @Nonnull final List<ChannelChangeStatus> channelUpdates,
                         @Nonnull final List<ChannelChangeStatus> channelRemoves,
                         final int entityUpdateCount,
                         final int entityRemoveCount,
                         final int entityLinkCount )
  {
    _systemKey = systemKey;
    _sequence = sequence;
    _requestID = requestID;
    _channelAdds = channelAdds;
    _channelUpdates = channelUpdates;
    _channelRemoves = channelRemoves;
    _entityUpdateCount = entityUpdateCount;
    _entityRemoveCount = entityRemoveCount;
    _entityLinkCount = entityLinkCount;
  }

  @Nonnull
  public String getSystemKey()
  {
    return _systemKey;
  }

  public int getSequence()
  {
    return _sequence;
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

  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return "[" +
             getSystemKey() + ": ChangeSet " + getSequence() + " involved " +
             getChannelAdds().size() + " subscribes, " +
             getChannelUpdates().size() + " subscription updates, " +
             getChannelRemoves().size() + " un-subscribes, " +
             getEntityUpdateCount() + " updates, " +
             getEntityRemoveCount() + " removes and " +
             getEntityLinkCount() + " links" +
             "]";
    }
    else
    {
      return super.toString();
    }
  }
}
