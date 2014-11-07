package org.realityforge.replicant.client.transport;

import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelSubscriptionEntry;
import org.realityforge.replicant.client.EntitySubscriptionManager;

public class EntitySubscriptionDebugger
{
  protected static final Logger LOG = Logger.getLogger( EntitySubscriptionDebugger.class.getName() );

  public void outputSubscriptionManager( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    outputTypeSubscriptions( subscriptionManager );
    outputInstanceSubscriptions( subscriptionManager );
  }

  protected void outputInstanceSubscriptions( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    for ( final Enum key : subscriptionManager.getInstanceSubscriptionKeys() )
    {
      final Set<Object> instanceSubscriptions = subscriptionManager.getInstanceSubscriptions( key );
      for ( final Object id : instanceSubscriptions )
      {
        outputSubscription( subscriptionManager.getSubscription( key, id ) );
      }
    }
  }

  protected void outputTypeSubscriptions( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    for ( final Enum type : subscriptionManager.getTypeSubscriptions() )
    {
      outputSubscription( subscriptionManager.getSubscription( type ) );
    }
  }

  protected void outputSubscription( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    final Object filter = subscription.getFilter();
    LOG.info( "Subscription: " + subscription.getDescriptor() +
              " Entities: " + subscription.getEntities().size() +
              ( null != filter ? " Filter: " + filter : "" ) );
  }
}
