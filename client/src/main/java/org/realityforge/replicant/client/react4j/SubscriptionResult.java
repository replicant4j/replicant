package org.realityforge.replicant.client.react4j;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Subscription;

public class SubscriptionResult<T>
{
  @Nonnull
  private final Subscription _entry;
  @Nullable
  private final T _instanceRoot;

  SubscriptionResult( @Nonnull final Subscription entry, @Nullable final T instanceRoot )
  {
    _entry = Objects.requireNonNull( entry );
    _instanceRoot = instanceRoot;
  }

  @Nonnull
  public Subscription getEntry()
  {
    return _entry;
  }

  @Nullable
  public T getInstanceRoot()
  {
    return _instanceRoot;
  }
}
