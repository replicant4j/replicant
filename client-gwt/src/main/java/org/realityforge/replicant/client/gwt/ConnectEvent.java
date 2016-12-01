package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;

public final class ConnectEvent
  extends GwtEvent<ConnectEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onConnect( @Nonnull ConnectEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onConnect( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "Disconnect[]";
  }
}
