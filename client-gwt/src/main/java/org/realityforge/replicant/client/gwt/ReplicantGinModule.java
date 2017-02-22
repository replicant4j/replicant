package org.realityforge.replicant.client.gwt;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Provides;
import javax.inject.Singleton;
import org.realityforge.replicant.client.EntityChangeBroker;
import org.realityforge.replicant.client.EntityRepository;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.EntitySystemImpl;

/**
 * A simple GIN module that defines the repository and change broker services.
 */
public class ReplicantGinModule
  extends AbstractGinModule
{
  @Override
  protected void configure()
  {
    bind( EntitySystem.class ).to( EntitySystemImpl.class ).asEagerSingleton();
  }

  @Provides
  @Singleton
  public final EntityRepository getEntityRepository( final EntitySystem system )
  {
    return system.getRepository();
  }

  @Provides
  @Singleton
  public final EntityChangeBroker getEntityChangeBroker( final EntitySystem system )
  {
    return system.getChangeBroker();
  }

  @Provides
  @Singleton
  public final EntitySubscriptionManager getEntitySubscriptionManager( final EntitySystem system )
  {
    return system.getSubscriptionManager();
  }
}
