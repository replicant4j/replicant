package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;
import replicant.Subscription;

/**
 * Notification when an Subscription is disposed.
 */
public final class SubscriptionDisposedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Subscription _subscription;

  public SubscriptionDisposedEvent( @Nonnull final Subscription subscription )
  {
    _subscription = Objects.requireNonNull( subscription );
  }

  @Nonnull
  public Subscription getSubscription()
  {
    return _subscription;
  }

  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Subscription.Disposed" );
    final ChannelAddress address = getSubscription().address();
    map.put( "channel.schemaId", address.schemaId() );
    map.put( "channel.channelId", address.channelId() );
    map.put( "channel.rootId", address.rootId() );
    map.put( "channel.filter", getSubscription().getFilter() );
    map.put( "explicitSubscription", getSubscription().isExplicitSubscription() );
  }
}
