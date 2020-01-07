package replicant.events;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector processed a message from a DataSource.
 */
public final class MessageProcessedEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;

  public MessageProcessedEvent( final int schemaId, @Nonnull final String schemaName )
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
}
