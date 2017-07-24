package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import javax.annotation.Nonnull;

@SuppressWarnings( "GwtInconsistentSerializableClass" )
public class SubscriptionInactiveException
  extends RuntimeException
{
   @Nonnull
  private final Subscription _subscription;

  public SubscriptionInactiveException( @Nonnull final Subscription subscription )
  {
    _subscription = Objects.requireNonNull( subscription );
  }

  @Nonnull
  public Subscription getSubscription()
  {
    return _subscription;
  }

  @Override
  public String toString()
  {
    return "SubscriptionInactiveException[subscription=" + _subscription + ']';
  }
}
