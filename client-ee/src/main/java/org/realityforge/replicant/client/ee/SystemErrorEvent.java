package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SystemErrorEvent
{
  private final String _systemKey;
  private final String _message;
  private final Throwable _throwable;

  @SuppressWarnings( "ConstantConditions" )
  public SystemErrorEvent( @Nonnull final String systemKey,
                           @Nonnull final String message,
                           @Nullable final Throwable throwable )
  {
    if ( null == systemKey )
    {
      throw new IllegalArgumentException( "systemKey is null" );
    }
    if ( null == message )
    {
      throw new IllegalArgumentException( "message is null" );
    }
    _systemKey = systemKey;
    _message = message;
    _throwable = throwable;
  }

  @Nonnull
  public String getSystemKey()
  {
    return _systemKey;
  }

  @Nonnull
  public String getMessage()
  {
    return _message;
  }

  @Nullable
  public Throwable getThrowable()
  {
    return _throwable;
  }

  public String toString()
  {
    return "SystemError[systemKey=" + _systemKey + ",Message=" + _message + ", Throwable=" + _throwable + "]";
  }
}
