package org.realityforge.replicant.client.react4j;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;

public class SubscriptionResult<T>
{
  @Nonnull
  private final ChannelSubscriptionEntry _entry;
  @Nullable
  private final T _instanceRoot;

  SubscriptionResult( @Nonnull final ChannelSubscriptionEntry entry, @Nullable final T instanceRoot )
  {
    _entry = Objects.requireNonNull( entry );
    _instanceRoot = instanceRoot;
  }

  @Nonnull
  public ChannelSubscriptionEntry getEntry()
  {
    return _entry;
  }

  @Nullable
  public T getInstanceRoot()
  {
    return _instanceRoot;
  }
}
