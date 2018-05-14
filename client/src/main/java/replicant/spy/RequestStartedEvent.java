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
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final String _requestId;
  @Nonnull
  private final String _name;

  public RequestStartedEvent( @Nonnull final Class<?> systemType,
                              @Nonnull final String requestId,
                              @Nonnull final String name )
  {
    _systemType = Objects.requireNonNull( systemType );
    _requestId = Objects.requireNonNull( requestId );
    _name = Objects.requireNonNull( name );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
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
    map.put( "systemType", getSystemType().getSimpleName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
  }
}
