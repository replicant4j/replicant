package replicant.events;

/**
 * Notification when a Connector processed a message from a DataSource.
 */
public final class MessageProcessedEvent
{
  private final int _schemaId;

  public MessageProcessedEvent( final int schemaId )
  {
    _schemaId = schemaId;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }
}
