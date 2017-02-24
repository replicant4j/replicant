package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class ConnectEvent
  extends AbstractDataLoaderEvent
{
  public ConnectEvent( @Nonnull final String systemKey )
  {
    super( systemKey );
  }

  public String toString()
  {
    return "Connect[SystemKey=" + getSystemKey() + "]";
  }
}
