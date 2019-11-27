package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import javax.annotation.Nonnull;

public final class InvalidDisconnectEvent
  extends AbstractDataLoaderErrorEvent<InvalidDisconnectEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onInvalidDisconnect( @Nonnull InvalidDisconnectEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();

  public InvalidDisconnectEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onInvalidDisconnect( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "InvalidDisconnect[SystemKey=" + getSystemKey() + ",Error=" + getThrowable() + "]";
  }
}
