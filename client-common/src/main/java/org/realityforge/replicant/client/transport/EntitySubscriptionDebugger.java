package org.realityforge.replicant.client.transport;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelAddress;
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
      for ( final Object id : subscriptionManager.getInstanceSubscriptions( key ) )
      {
        outputSubscription( subscriptionManager.getSubscription( new ChannelAddress( key, id ) ) );
      }
    }
  }

  protected void outputTypeSubscriptions( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    for ( final Enum type : subscriptionManager.getTypeSubscriptions() )
    {
      outputSubscription( subscriptionManager.getSubscription( new ChannelAddress( type ) ) );
    }
  }

  protected void outputSubscription( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    final Object filter = subscription.getChannel().getFilter();
    LOG.info( "Subscription: " + subscription.getChannel().getAddress() +
              " Entities: " + subscription.getEntitySubscriptionEntries().size() +
              ( null != filter ? " Filter: " + filter : "" ) );
  }
}
