package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAddress;

/**
 * Base class for session context implementations.
 */
public abstract class AbstractSessionContextImpl
{
  protected void addSubscription( @Nonnull final ReplicantSession session,
                                  @Nonnull final ChangeSet changeSet,
                                  @Nonnull final Object filter,
                                  final boolean explicitSubscribe,
                                  @Nonnull final ChannelAddress address,
                                  @Nullable final ChannelAddress sourceDescriptor )
  {
    SubscriptionEntry entry = session.findSubscriptionEntry( address );
    if ( null == entry )
    {
      changeSet.addAction( address, ChannelAction.Action.ADD, filter );
      entry = session.createSubscriptionEntry( address );
      entry.setFilter( filter );
    }
    else
    {
      changeSet.addAction( address, ChannelAction.Action.UPDATE, filter );
      entry.setFilter( filter );
    }
    if ( explicitSubscribe )
    {
      entry.setExplicitlySubscribed( true );
    }
    if ( null != sourceDescriptor )
    {
      final SubscriptionEntry subscriptionEntry = session.getSubscriptionEntry( sourceDescriptor );
      linkSubscriptionEntries( subscriptionEntry, entry );
    }
  }

  protected void linkSubscriptionEntries( @Nonnull final SubscriptionEntry sourceEntry,
                                          @Nonnull final SubscriptionEntry targetEntry )
  {
    sourceEntry.registerOutwardSubscriptions( targetEntry.getDescriptor() );
    targetEntry.registerInwardSubscriptions( sourceEntry.getDescriptor() );
  }
}
