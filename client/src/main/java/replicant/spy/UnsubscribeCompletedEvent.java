package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

/**
 * Notification when a Connector completes an unsubscribe from a channel.
 */
public final class UnsubscribeCompletedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final ChannelAddress _address;

  public UnsubscribeCompletedEvent( @Nonnull final Class<?> systemType, @Nonnull final ChannelAddress address )
  {
    _systemType = Objects.requireNonNull( systemType );
    _address = Objects.requireNonNull( address );
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "DataLoader.UnsubscribeCompleted" );
    map.put( "systemType", getSystemType().getSimpleName() );
    final ChannelAddress address = getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
  }
}
