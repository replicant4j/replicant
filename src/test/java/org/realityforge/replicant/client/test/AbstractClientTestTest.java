package org.realityforge.replicant.client.test;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Module;
import java.util.ArrayList;
import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public class AbstractClientTestTest
{
  public static class MyTest
    extends AbstractClientTest
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
  public void brokerInteraction()
    throws Exception
  {
    final MyTest t = new MyTest();
    t.preTest();

    final EntityChangeBroker broker = t.broker();

    verify( broker, never() ).pause( "TEST" );
    verify( broker, never() ).resume( "TEST" );
    t.pauseBroker();
    verify( broker, never() ).resume( "TEST" );
    verify( broker, times( 1 ) ).pause( "TEST" );

    verify( broker, times( 1 ) ).pause( "TEST" );
    verify( broker, never() ).resume( "TEST" );
    t.resumeBroker();
    verify( broker, times( 1 ) ).resume( "TEST" );
    verify( broker, times( 1 ) ).pause( "TEST" );
  }
}
