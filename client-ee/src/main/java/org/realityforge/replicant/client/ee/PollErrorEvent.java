package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class PollErrorEvent
  extends AbstractDataLoaderErrorEvent
{
  public PollErrorEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
  }

  public String toString()
  {
    return "PollError[systemKey=" + getSystemKey() + ", Throwable=" + getThrowable() + "]";
  }
}
