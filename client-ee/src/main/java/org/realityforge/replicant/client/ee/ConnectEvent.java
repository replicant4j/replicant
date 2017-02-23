package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ConnectEvent
{
  private final String _systemKey;

  @SuppressWarnings( "ConstantConditions" )
  public ConnectEvent( @Nonnull final String systemKey )
  {
    if ( null == systemKey )
    {
      throw new IllegalArgumentException( "systemKey is null" );
    }
    _systemKey = systemKey;
  }

  @Nonnull
  public String getSystemKey()
  {
    return _systemKey;
  }

  public String toString()
  {
    return "ConnectEvent[SystemKey=" + _systemKey + "]";
  }
}
