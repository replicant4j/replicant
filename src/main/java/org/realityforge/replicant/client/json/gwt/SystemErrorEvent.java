package org.realityforge.replicant.client.json.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SystemErrorEvent
  extends GwtEvent<SystemErrorEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onSystemError( @Nonnull SystemErrorEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();
  private final String _message;
  private final Throwable _throwable;

  @SuppressWarnings( "ConstantConditions" )
  public SystemErrorEvent( @Nonnull final String message, @Nullable final Throwable throwable )
  {
    if ( null == message )
    {
      throw new IllegalArgumentException( "Message is null" );
    }

    _message = message;
    _throwable = throwable;
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

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onSystemError( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "SystemError[Message=" + _message + ", " + "Throwable=" + _throwable + "]";
  }
}
