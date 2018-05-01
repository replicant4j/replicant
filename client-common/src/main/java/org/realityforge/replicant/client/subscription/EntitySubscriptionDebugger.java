package org.realityforge.replicant.client.subscription;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.ChannelAddress;

public class EntitySubscriptionDebugger
{
  protected static final Logger LOG = Logger.getLogger( EntitySubscriptionDebugger.class.getName() );

  public void outputSubscriptionManager( @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    subscriptionManager.getTypeChannelSubscriptions().forEach( this::outputSubscription );
    for ( final Enum key : subscriptionManager.getInstanceChannelSubscriptionKeys() )
    {
      for ( final Object id : subscriptionManager.getInstanceChannelSubscriptions( key ) )
      {
        outputSubscription( subscriptionManager.getChannelSubscription( new ChannelAddress( key, id ) ) );
      }
    }
  }

  private void outputSubscription( @Nonnull final ChannelSubscriptionEntry subscription )
  {
    final Object filter = subscription.getChannel().getFilter();
    LOG.info( "Subscription: " + subscription.getChannel().getAddress() +
              " Entities: " + subscription.getEntitySubscriptionEntries().size() +
              ( null != filter ? " Filter: " + filter : "" ) );
  }
}
