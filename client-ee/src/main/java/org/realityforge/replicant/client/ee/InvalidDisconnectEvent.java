package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class InvalidDisconnectEvent
  extends AbstractDataLoaderErrorEvent
{
  public InvalidDisconnectEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
  }

  public String toString()
  {
    return "InvalidDisconnect[systemKey=" + getSystemKey() + ", Throwable=" + getThrowable() + "]";
  }
}
