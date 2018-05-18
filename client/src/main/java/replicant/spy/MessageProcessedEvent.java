package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector processed a message from a DataSource.
 */
public final class MessageProcessedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final DataLoadStatus _dataLoadStatus;

  public MessageProcessedEvent( final int schemaId, @Nonnull final String schemaName,
                                @Nonnull final DataLoadStatus dataLoadStatus )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _dataLoadStatus = Objects.requireNonNull( dataLoadStatus );
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Nonnull
  public String getSchemaName()
  {
    return _schemaName;
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
    map.put( "type", "Connector.MessageProcess" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
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
