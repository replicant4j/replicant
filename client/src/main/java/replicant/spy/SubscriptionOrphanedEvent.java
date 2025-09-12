package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;
import replicant.Subscription;

/**
 * Notification when an Subscription is "orphaned".
 * An "orphaned" subscription no longer has an explicit subscription to it. This may result in it being
 * unsubscribed if there is no implicit subscription within the system. The process that converges between
 * desired state (i.e. AreaOfInterest) and actual state (i.e. Subscription) is responsible for identifying
 * orphaned subscriptions.
 */
public final class SubscriptionOrphanedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Subscription _subscription;

  public SubscriptionOrphanedEvent( @Nonnull final Subscription subscription )
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
    map.put( "type", "Subscription.Orphaned" );
    final ChannelAddress address = getSubscription().getAddress();
    map.put( "channel.schemaId", address.getSchemaId() );
    map.put( "channel.channelId", address.getChannelId() );
    map.put( "channel.rootId", address.getRootId() );
    map.put( "channel.filter", getSubscription().getFilter() );
  }
}
