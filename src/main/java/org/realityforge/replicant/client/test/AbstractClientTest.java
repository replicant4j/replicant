package org.realityforge.replicant.client.test;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Module;
import com.google.web.bindery.event.shared.Event;
import java.util.ArrayList;
import java.util.Collections;
import org.realityforge.guiceyloops.shared.AbstractSharedTest;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import static org.mockito.Mockito.*;

public abstract class AbstractClientTest
  extends AbstractSharedTest
{
  @Override
  protected Module[] getModules()
  {
    final ArrayList<Module> modules = new ArrayList<>();
    Collections.addAll( modules, super.getModules() );
    addModule( modules, new ReplicantClientTestModule() );
    return modules.toArray( new Module[ modules.size() ] );
  }

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

  protected void resumeBroker()
  {
    broker().resume( "TEST" );
  }

  protected void pauseBroker()
  {
    broker().pause( "TEST" );
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
