package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;

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

  public DataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    _status = Objects.requireNonNull( status );
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
