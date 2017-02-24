package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class DisconnectEvent
  extends AbstractDataLoaderEvent
{
  public DisconnectEvent( @Nonnull final String systemKey )
  {
    super( systemKey );
  }

  public String toString()
  {
    return "Disconnect[SystemKey=" + getSystemKey() + "]";
  }
}
