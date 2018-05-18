package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Request starts.
 */
public final class RequestStartedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final String _requestId;
  @Nonnull
  private final String _name;

  public RequestStartedEvent( final int schemaId, @Nonnull final String schemaName,
                              @Nonnull final String requestId,
                              @Nonnull final String name )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _requestId = Objects.requireNonNull( requestId );
    _name = Objects.requireNonNull( name );
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
  public String getRequestId()
  {
    return _requestId;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.RequestStarted" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
  }
}
