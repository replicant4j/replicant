package org.realityforge.replicant.client.test.gwt;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Module;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import java.util.ArrayList;
import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.test.gwt.FakeEvent.Handler;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public class AbstractGwtClientTestTest
{
  public static class MyTest
    extends AbstractGwtClientTest
  {
    @Override
    protected Module[] getModules()
    {
      final ArrayList<Module> modules = new ArrayList<>();
      addModule( modules, getTestModule() );
      return modules.toArray( new Module[ modules.size() ] );
    }

    @Override
    protected Module getDefaultTestModule()
    {
      return new AbstractModule()
      {
        @Override
        protected void configure()
        {
          bindMock( EventBus.class );
          bindMock( EntityRepository.class );
          bindMock( EntityChangeBroker.class );
        }
      };
    }
  }

  @Test
  public void eventBusInteraction()
    throws Exception
  {
    final MyTest t = new MyTest();
    t.preTest();

    final EventBus eventBus = t.eventBus();

    verify( eventBus, never() ).fireEvent( any( GwtEvent.class ) );
    t.fireEvent( new FakeEvent( 22 ) );
    verify( eventBus, times( 1 ) ).fireEvent( (Event<?>) t.event( new FakeEvent( 22 ) ) );

    verify( eventBus, never() ).addHandler( any( Type.class ), any() );
    t.addHandler( FakeEvent.TYPE, mock( Handler.class ) );
    verify( eventBus, times( 1 ) ).addHandler( (Type) refEq( FakeEvent.TYPE ), any( Handler.class ) );
  }
}
