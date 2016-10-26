package org.realityforge.replicant.client.test.gwt;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Module;
import com.google.web.bindery.event.shared.Event;
import java.util.ArrayList;
import java.util.Collections;
import org.realityforge.replicant.client.test.AbstractClientTest;
import static org.mockito.Mockito.*;

public abstract class AbstractGwtClientTest
  extends AbstractClientTest
{
  @Override
  protected Module[] getModules()
  {
    final ArrayList<Module> modules = new ArrayList<>();
    Collections.addAll( modules, super.getModules() );
    addModule( modules, new ReplicantGwtClientTestModule() );
    return modules.toArray( new Module[ modules.size() ] );
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
