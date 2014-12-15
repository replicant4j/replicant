package org.realityforge.replicant.client.test;

import com.google.gwt.event.shared.EventBus;
import com.google.web.bindery.event.shared.Event;
import org.realityforge.guiceyloops.shared.AbstractSharedTest;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import static org.mockito.Mockito.*;

public abstract class AbstractClientTest
  extends AbstractSharedTest
{
  protected final EntityRepository repository()
  {
    return s( EntityRepository.class );
  }

  protected final EntityChangeBroker broker()
  {
    return s( EntityChangeBroker.class );
  }

  protected final <H> H addHandler( final Event.Type<H> type, final H handler )
  {
    eventBus().addHandler( type, handler );
    return handler;
  }

  protected final void fireEvent( final Event<?> event )
  {
    eventBus().fireEvent( event );
  }

  protected final EventBus eventBus()
  {
    return s( EventBus.class );
  }

  protected final <T extends Event<?>> T event( final T value )
  {
    return refEq( value, "source" );
  }
}
