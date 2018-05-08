package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import replicant.Channel;
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Subscription.Disposed" );
    final Channel channel = getSubscription().getChannel();
    final ChannelAddress address = channel.getAddress();
    map.put( "channel.type", address.getChannelType().name() );
    map.put( "channel.id", address.getId() );
    map.put( "channel.filter", channel.getFilter() );
    map.put( "explicitSubscription", getSubscription().isExplicitSubscription() );
  }
}
