package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.ChannelAddress;

/**
 * Notification when a Subscription is requested.
 */
public final class SubscribeRequestCreatedEvent
  implements SerializableEvent
{
  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private final Object _filter;

  public SubscribeRequestCreatedEvent( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    _address = Objects.requireNonNull( address );
    _filter = filter;
  }

  @Nonnull
  public ChannelAddress getAddress()
  {
    return _address;
  }

  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.SubscribeRequestCreated" );
    final ChannelAddress address = getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
    map.put( "channel.filter", getFilter() );
  }
}
