package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector queues an Exec message.
 */
public final class ExecRequestQueuedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final String _command;

  public ExecRequestQueuedEvent( final int schemaId,
                                 @Nonnull final String schemaName,
                                 @Nonnull final String command )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _command = Objects.requireNonNull( command );
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

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.ExecRequestQueued" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "command", getCommand() );
  }
}
