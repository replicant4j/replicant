package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class InvalidConnectEvent
  extends AbstractDataLoaderErrorEvent
{
  public InvalidConnectEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
  }

  public String toString()
  {
    return "InvalidConnect[systemKey=" + getSystemKey() + ", Throwable=" + getThrowable() + "]";
  }
}
