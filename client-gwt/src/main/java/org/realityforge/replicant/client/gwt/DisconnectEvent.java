package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;

public final class DisconnectEvent
  extends AbstractDataLoaderEvent<DisconnectEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onDisconnect( @Nonnull DisconnectEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();

  public DisconnectEvent( @Nonnull final String systemKey )
  {
    super( systemKey );
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onDisconnect( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "Disconnect[SystemKey=" + getSystemKey() + "]";
  }
}
