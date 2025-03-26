package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector receives a response to an Exec message.
 */
public final class ExecCompletedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final String _command;
  private final int _requestId;

  public ExecCompletedEvent( final int schemaId,
                             @Nonnull final String schemaName,
                             @Nonnull final String command,
                             final int requestId )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _command = Objects.requireNonNull( command );
    _requestId = requestId;
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
  public String getCommand()
  {
    return _command;
  }

  public int getRequestId()
  {
    return _requestId;
  }

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.ExecCompleted" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "command", getCommand() );
    map.put( "requestId", getRequestId() );
  }
}
