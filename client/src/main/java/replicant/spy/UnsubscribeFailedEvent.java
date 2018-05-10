package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

/**
 * Notification when a Connector fails to unsubscribe from a channel.
 */
public final class UnsubscribeFailedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final ChannelAddress _address;
  @Nonnull
  private final Throwable _error;

  public UnsubscribeFailedEvent( @Nonnull final Class<?> systemType,
                                 @Nonnull final ChannelAddress address,
                                 @Nonnull final Throwable error )
  {
    _systemType = Objects.requireNonNull( systemType );
    _address = Objects.requireNonNull( address );
    _error = Objects.requireNonNull( error );
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
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
    map.put( "type", "Connector.UnsubscribeFailed" );
    map.put( "systemType", getSystemType().getSimpleName() );
    final ChannelAddress address = getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
    final Throwable throwable = getError();
    map.put( "message", null == throwable.getMessage() ? throwable.toString() : throwable.getMessage() );
  }
}
