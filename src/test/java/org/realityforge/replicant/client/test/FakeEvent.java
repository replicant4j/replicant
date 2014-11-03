package org.realityforge.replicant.client.test;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import javax.annotation.Nonnull;

public final class FakeEvent
  extends GwtEvent<FakeEvent.Handler>
{
  public static interface Handler
    extends EventHandler
  {
    void onClick( @Nonnull FakeEvent event );
  }

  public static final Type<Handler> TYPE = new Type<>();

  private final int _myField;

  public FakeEvent( final int myField )
  {
    _myField = myField;
  }

  public int getMyField()
  {
    return _myField;
  }

  @Override
  public Type<Handler> getAssociatedType()
  {
    return TYPE;
  }

  @Override
  protected void dispatch( final Handler handler )
  {
    handler.onClick( this );
  }
}
