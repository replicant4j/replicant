package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector requested synchronized but is out of synchronization with the backend.
 */
public final class OutOfSyncEvent
  implements SerializableEvent
{
  private final int _schemaId;

  public OutOfSyncEvent( final int schemaId )
  {
    _schemaId = schemaId;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.OutOfSync" );
    map.put( "schema.id", getSchemaId() );
  }
}
