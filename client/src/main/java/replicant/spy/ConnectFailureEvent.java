package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector received an error connecting to a DataSource.
 */
public final class ConnectFailureEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;

  public ConnectFailureEvent( final int schemaId, @NonNull final String schemaName )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @NonNull
  public String getSchemaName()
  {
    return _schemaName;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.ConnectFailure" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
  }
}
