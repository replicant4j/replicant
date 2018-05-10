package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Connector has disconnected from a DataSource.
 */
public final class DisconnectedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;

  public DisconnectedEvent( @Nonnull final Class<?> systemType )
  {
    _systemType = Objects.requireNonNull( systemType );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.Disconnect" );
    map.put( "systemType", getSystemType().getSimpleName() );
  }
}
