package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when an DataLoader received an error when disconnecting from DataSource.
 */
public final class DisconnectFailureEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final Throwable _error;

  public DisconnectFailureEvent( @Nonnull final Class<?> systemType, @Nonnull final Throwable error )
  {
    _systemType = Objects.requireNonNull( systemType );
    _error = Objects.requireNonNull( error );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
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
    map.put( "type", "DataLoader.InvalidDisconnect" );
    map.put( "systemType", getSystemType().getSimpleName() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
