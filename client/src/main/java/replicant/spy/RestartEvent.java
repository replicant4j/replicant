package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector attempts to disconnect connection to force a restart.
 */
public final class RestartEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;

  public RestartEvent( final int schemaId, @Nonnull final String schemaName )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
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

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.Restart" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
  }
}
