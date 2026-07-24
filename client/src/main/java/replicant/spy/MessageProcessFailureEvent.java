package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector generated an error processing a message from a DataSource.
 */
public final class MessageProcessFailureEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;
  @NonNull
  private final Throwable _error;

  public MessageProcessFailureEvent( final int schemaId,
                                     @NonNull final String schemaName,
                                     @NonNull final Throwable error )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _error = Objects.requireNonNull( error );
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

  @NonNull
  public Throwable getError()
  {
    return _error;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.MessageProcessFailure" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
