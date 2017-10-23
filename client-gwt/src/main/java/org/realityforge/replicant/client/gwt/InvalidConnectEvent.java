package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import javax.annotation.Nonnull;

public final class InvalidConnectEvent
  extends AbstractDataLoaderErrorEvent<InvalidConnectEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onInvalidConnect( @Nonnull InvalidConnectEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();

  public InvalidConnectEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
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
    handler.onInvalidConnect( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "InvalidConnect[SystemKey=" + getSystemKey() + ",Error=" + getThrowable() + "]";
  }
}
