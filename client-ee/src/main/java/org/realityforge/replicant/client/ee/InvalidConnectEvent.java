package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InvalidConnectEvent
{
  private final String _systemKey;
  private final Throwable _throwable;

  @SuppressWarnings( "ConstantConditions" )
  public InvalidConnectEvent( @Nonnull final String systemKey,
                              @Nullable final Throwable throwable )
  {
    if ( null == systemKey )
    {
      throw new IllegalArgumentException( "systemKey is null" );
    }
    _systemKey = systemKey;
    _throwable = throwable;
  }

  @Nonnull
  public String getSystemKey()
  {
    return _systemKey;
  }

  @Nullable
  public Throwable getThrowable()
  {
    return _throwable;
  }

  public String toString()
  {
    return "InvalidConnect[systemKey=" + _systemKey + ", Throwable=" + _throwable + "]";
  }
}
