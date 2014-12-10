package org.realityforge.replicant.client.transport;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;

public final class DataLoadCompleteEvent
  extends GwtEvent<DataLoadCompleteEvent.Handler>
{
  public interface Handler
    extends EventHandler
  {
    void onDataLoadComplete( @Nonnull DataLoadCompleteEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();
  private final DataLoadStatus _status;

  @SuppressWarnings( "ConstantConditions" )
  public DataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    if ( null == status )
    {
      throw new IllegalArgumentException( "status is null" );
    }

    _status = status;
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Nonnull
  public DataLoadStatus getStatus()
  {
    return _status;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onDataLoadComplete( this );
  }

  public String toDebugString()
  {
    return toString();
  }

  public String toString()
  {
    return "DataLoadComplete[Status=" + _status + "]";
  }
}
