package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class SystemErrorEvent
  extends AbstractDataLoaderErrorEvent
{
  private final String _message;

  @SuppressWarnings( "ConstantConditions" )
  public SystemErrorEvent( @Nonnull final String systemKey,
                           @Nonnull final String message,
                           @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
    if ( null == message )
    {
      throw new IllegalArgumentException( "message is null" );
    }
    _message = message;
  }

  @Nonnull
  public String getMessage()
  {
    return _message;
  }

  public String toString()
  {
    return "SystemError[systemKey=" + getSystemKey() + ",Message=" + _message + ", Throwable=" + getThrowable() + "]";
  }
}
