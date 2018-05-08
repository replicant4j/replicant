package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when an DataLoader generated an error processing a message from the datasource.
 */
public final class DataLoaderMessageProcessedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final DataLoadStatus _dataLoadStatus;

  public DataLoaderMessageProcessedEvent( @Nonnull final Class<?> systemType,
                                          @Nonnull final DataLoadStatus dataLoadStatus )
  {
    _systemType = Objects.requireNonNull( systemType );
    _dataLoadStatus = Objects.requireNonNull( dataLoadStatus );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
  }

  @Nonnull
  public DataLoadStatus getDataLoadStatus()
  {
    return _dataLoadStatus;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "DataLoader.MessageProcess" );
    map.put( "systemType", getSystemType().getSimpleName() );
    final DataLoadStatus status = getDataLoadStatus();
    map.put( "sequence", status.getSequence() );
    map.put( "requestId", status.getRequestId() );
    map.put( "channelAddCount", status.getChannelAddCount() );
    map.put( "channelRemoveCount", status.getChannelRemoveCount() );
    map.put( "channelUpdateCount", status.getChannelUpdateCount() );
    map.put( "entityUpdateCount", status.getEntityUpdateCount() );
    map.put( "entityRemoveCount", status.getEntityRemoveCount() );
    map.put( "entityLinkCount", status.getEntityLinkCount() );
  }
}
