package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector requested a sync with a datasource.
 */
public final class SyncRequestEvent
  implements SerializableEvent
{
  private final int _schemaId;

  public SyncRequestEvent( final int schemaId )
  {
    _schemaId = schemaId;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.SyncRequest" );
    map.put( "schema.id", getSchemaId() );
  }
}
