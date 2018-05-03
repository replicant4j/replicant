package replicant;

import arez.Disposable;
import arez.Observer;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Structure to contain reference to Subscription and it's monitor from within SubscriptionService.
 * The monitor is Disposable representing a "when" observer that triggers removal of
 * Subscription from SubscriptionService
 */
final class SubscriptionEntry
  implements Disposable
{
  /**
   * The underlying Subscription.
   */
  @Nonnull
  private final Subscription _subscription;
  /**
   * The monitor observer that will remove entity from repository if it is independent disposed.
   */
  @Nullable
  private Observer _monitor;

  /**
   * Create entry for subscription.
   *
   * @param subscription the subscription created in SubscriptionService.
   */
  SubscriptionEntry( @Nonnull final Subscription subscription )
  {
    _subscription = Objects.requireNonNull( subscription );
  }

  /**
   * Return the Subscription the entry represents.
   *
   * @return the Subscription the entry represents.
   */
  @Nonnull
  Subscription getSubscription()
  {
    return _subscription;
  }

  /**
   * Set the monitor that will remove Subscription from SubscriptionService when entity is disposed.
   *
   * @param monitor the monitor that will remove Subscription from SubscriptionService when entity is disposed.
   */
  void setMonitor( @Nonnull final Observer monitor )
  {
    _monitor = Objects.requireNonNull( monitor );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose()
  {
    if ( null != _monitor )
    {
      _monitor.dispose();
      _monitor = null;
    }
    Disposable.dispose( _subscription );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisposed()
  {
    return Disposable.isDisposed( _subscription );
  }
}
