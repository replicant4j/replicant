package org.realityforge.replicant.client.subscription;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class EntitySubscriptionDebugger
{
  protected static final Logger LOG = Logger.getLogger( EntitySubscriptionDebugger.class.getName() );

  public void outputSubscriptionManager( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    subscriptionManager.getTypeSubscriptions().forEach( this::outputSubscription );
    subscriptionManager.getInstanceSubscriptions().forEach( this::outputSubscription );
  }

  private void outputSubscription( @Nonnull final Subscription subscription )
  {
    LOG.info( subscription.getChannel().toString() );
  }
}
