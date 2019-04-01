package replicant.spy;

import javax.annotation.Nullable;
import replicant.Replicant;

/**
 * Summary describing the result of a data load action.
 */
public final class DataLoadStatus
{
  @Nullable
  private final Integer _requestId;
  /// The number of channels added as a result of the Message
  private final int _channelAddCount;
  /// The number of channels updated as a result of the Message
  private final int _channelUpdateCount;
  /// The number of channels removed as a result of the Message
  private final int _channelRemoveCount;
  // The number of entities created or updated as part of change message
  private final int _entityUpdateCount;
  // The number of entities removed as part of change message
  private final int _entityRemoveCount;
  // The number of entities where link() was invoked
  private final int _entityLinkCount;

  public DataLoadStatus( @Nullable final Integer requestId,
                         final int channelAddCount,
                         final int channelUpdateCount,
                         final int channelRemoveCount,
                         final int entityUpdateCount,
                         final int entityRemoveCount,
                         final int entityLinkCount )
  {
    _requestId = requestId;
    _channelAddCount = channelAddCount;
    _channelUpdateCount = channelUpdateCount;
    _channelRemoveCount = channelRemoveCount;
    _entityUpdateCount = entityUpdateCount;
    _entityRemoveCount = entityRemoveCount;
    _entityLinkCount = entityLinkCount;
  }

  @Nullable
  public Integer getRequestId()
  {
    return _requestId;
  }

  public int getChannelAddCount()
  {
    return _channelAddCount;
  }

  public int getChannelUpdateCount()
  {
    return _channelUpdateCount;
  }

  public int getChannelRemoveCount()
  {
    return _channelRemoveCount;
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
    if ( Replicant.areNamesEnabled() )
    {
      return "[Message" +
             ( null == _requestId ? "" : " for request " + _requestId ) +
             " involved " +
             getChannelAddCount() + " subscribes, " +
             getChannelUpdateCount() + " subscription updates, " +
             getChannelRemoveCount() + " un-subscribes, " +
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
