package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector received an error disconnecting from a DataSource.
 */
public final class SyncFailureEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @Nonnull
  private final Throwable _error;

  public SyncFailureEvent( final int schemaId, @Nonnull final Throwable error )
  {
    _schemaId = schemaId;
    _error = Objects.requireNonNull( error );
  }

  public int getSchemaId()
  {
    return _schemaId;
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
    map.put( "type", "Connector.SyncFailure" );
    map.put( "schema.id", getSchemaId() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
