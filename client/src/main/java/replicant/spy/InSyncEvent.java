package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector is synchronized with the backend.
 */
public final class InSyncEvent
  implements SerializableEvent
{
  private final int _schemaId;

  public InSyncEvent( final int schemaId )
  {
    _schemaId = schemaId;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.InSync" );
    map.put( "schema.id", getSchemaId() );
  }
}
