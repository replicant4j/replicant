package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SubscriptionReference
{
  @Nullable
  private Subscription _subscription;

  public SubscriptionReference( @Nonnull final Subscription subscription )
  {
    _subscription = subscription;
  }

  @Nonnull
  public Subscription getSubscription()
  {
    if ( null == _subscription )
    {
      throw new ReferenceReleasedException();
    }
    return _subscription;
  }

  public boolean hasBeenReleased()
  {
    return !isActive();
  }

  public boolean isActive()
  {
    return null != _subscription;
  }

  public void release()
  {
    if ( null != _subscription )
    {
      _subscription.release( this );
      _subscription = null;
    }
  }
}
