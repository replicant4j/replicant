package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.ChannelAddress;
import replicant.Subscription;

/**
 * Notification when an Subscription is created.
 */
public final class SubscriptionCreatedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Subscription _subscription;

  public SubscriptionCreatedEvent( @Nonnull final Subscription subscription )
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
    map.put( "type", "Subscription.Created" );
    final ChannelAddress address = getSubscription().getAddress();
    map.put( "channel.systemId", address.getSystemId() );
    map.put( "channel.channelId", address.getChannelId() );
    map.put( "channel.id", address.getId() );
    map.put( "channel.filter", getSubscription().getFilter() );
    map.put( "explicitSubscription", getSubscription().isExplicitSubscription() );
  }
}
