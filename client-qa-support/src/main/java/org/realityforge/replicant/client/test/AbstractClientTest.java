package org.realityforge.replicant.client.test;

import com.google.inject.Module;
import java.util.ArrayList;
import java.util.Collections;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.AbstractSharedTest;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;

public abstract class AbstractClientTest
  extends AbstractSharedTest
{
  @Override
  protected Module[] getModules()
  {
    final ArrayList<Module> modules = new ArrayList<>();
    Collections.addAll( modules, super.getModules() );
    addModule( modules, newReplicantClientTestModule() );
    return modules.toArray( new Module[ modules.size() ] );
  }

  @Nonnull
  private Module newReplicantClientTestModule()
  {
    return new ReplicantClientTestModule();
  }

  protected final EntityRepository repository()
  {
    return s( EntityRepository.class );
  }

  protected final EntityChangeBroker broker()
  {
    return s( EntityChangeBroker.class );
  }

  protected void resumeBroker()
  {
    broker().resume( "TEST" );
  }

  protected void pauseBroker()
  {
    broker().pause( "TEST" );
  }
}
