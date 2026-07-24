package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Connector queues an Exec message.
 */
public final class ExecRequestQueuedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;
  @NonNull
  private final String _command;

  public ExecRequestQueuedEvent( final int schemaId,
                                 @NonNull final String schemaName,
                                 @NonNull final String command )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _command = Objects.requireNonNull( command );
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
  public String getCommand()
  {
    return _command;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.ExecRequestQueued" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "command", getCommand() );
  }
}
