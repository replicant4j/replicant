package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Request starts.
 */
public final class RequestStartedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;
  private final int _requestId;
  @NonNull
  private final String _name;

  public RequestStartedEvent( final int schemaId,
                              @NonNull final String schemaName,
                              final int requestId,
                              @NonNull final String name )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _requestId = requestId;
    _name = Objects.requireNonNull( name );
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

  public int getRequestId()
  {
    return _requestId;
  }

  @NonNull
  public String getName()
  {
    return _name;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.RequestStarted" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
  }
}
