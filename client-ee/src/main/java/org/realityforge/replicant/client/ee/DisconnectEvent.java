package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class DisconnectEvent
{
  private final String _systemKey;

  @SuppressWarnings( "ConstantConditions" )
  public DisconnectEvent( @Nonnull final String systemKey )
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
    return "DisconnectEvent[SystemKey=" + _systemKey + "]";
  }
}
