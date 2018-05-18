package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector received an error connecting to a DataSource.
 */
public final class ConnectFailureEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final Throwable _error;

  public ConnectFailureEvent( final int schemaId, @Nonnull final String schemaName, @Nonnull final Throwable error )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _error = Objects.requireNonNull( error );
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
  public Throwable getError()
  {
    return _error;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.ConnectFailure" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
