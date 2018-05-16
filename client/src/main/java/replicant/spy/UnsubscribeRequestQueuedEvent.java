package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;

/**
 * Notification when a Subscription removal is requested.
 */
public final class UnsubscribeRequestQueuedEvent
  implements SerializableEvent
{
  @Nonnull
  private final ChannelAddress _address;

  public UnsubscribeRequestQueuedEvent( @Nonnull final ChannelAddress address )
  {
    _address = Objects.requireNonNull( address );
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
    map.put( "type", "Connector.UnsubscribeRequestQueued" );
    final ChannelAddress address = getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
  }
}