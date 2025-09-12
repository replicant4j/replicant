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
public final class SubscribeRequestQueuedEvent
  implements SerializableEvent
{
  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private final Object _filter;

  public SubscribeRequestQueuedEvent( @Nonnull final ChannelAddress address, @Nullable final Object filter )
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

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.SubscribeRequestQueued" );
    final ChannelAddress address = getAddress();
    map.put( "channel.schemaId", address.schemaId() );
    map.put( "channel.channelId", address.channelId() );
    map.put( "channel.rootId", address.rootId() );
    map.put( "channel.filter", getFilter() );
  }
}
