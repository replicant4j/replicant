package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelDescriptor;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;

public abstract class ContextConvergerImpl
  implements ContextConverger
{
  private static final Logger LOG = Logger.getLogger( ContextConvergerImpl.class.getName() );

  protected static final int CONVERGE_DELAY_IN_MS = 100;

  private final ArrayList<Subscription> _subscriptions = new ArrayList<>();
  private final ConvergerAreaOfInterestListener _aoiListener = new ConvergerAreaOfInterestListener();
  private final ConvergerReplicantSystemListener _rsListener = new ConvergerReplicantSystemListener();
  private final ConvergerDataLoaderListener _dlListener = new ConvergerDataLoaderListener();
  private boolean _subscriptionsUpToDate;
  private boolean _convergeComplete;
  private boolean _paused;
  private Runnable _preConvergeAction;

  @Override
  public void setPreConvergeAction( @Nullable final Runnable preConvergeAction )
  {
    _preConvergeAction = preConvergeAction;
  }

  /**
   * Release resources associated with the system.
   */
  protected void release()
  {
    removeListeners();
  }

  protected void addListeners()
  {
    getAreaOfInterestService().addAreaOfInterestListener( _aoiListener );
    getReplicantClientSystem().addReplicantSystemListener( _rsListener );
    for ( final DataLoaderEntry entry : getReplicantClientSystem().getDataLoaders() )
    {
      entry.getService().addDataLoaderListener( _dlListener );
    }
  }

  protected void removeListeners()
  {
    getAreaOfInterestService().removeAreaOfInterestListener( _aoiListener );
    getReplicantClientSystem().removeReplicantSystemListener( _rsListener );
    for ( final DataLoaderEntry entry : getReplicantClientSystem().getDataLoaders() )
    {
      entry.getService().removeDataLoaderListener( _dlListener );
    }
  }

  protected void converge()
  {
    preConverge();
    convergeStep();
  }

  protected void preConverge()
  {
    if ( null != _preConvergeAction )
    {
      _preConvergeAction.run();
    }
  }

  protected void convergeStep()
  {
    if ( isActive() &&
         !_convergeComplete &&
         !isPaused() &&
         getReplicantClientSystem().getState() == ReplicantClientSystem.State.CONNECTED )
    {
      final HashSet<ChannelDescriptor> channels = new HashSet<>();
      final List<Subscription> subscriptions = getSubscriptions();
      for ( final Subscription subscription : subscriptions )
      {
        channels.add( subscription.getDescriptor() );
        if ( convergeSubscription( subscription ) )
        {
          return;
        }
      }

      removeOrphanSubscriptions( channels );
      _convergeComplete = true;
    }
  }

  @Override
  public void pause()
  {
    _paused = true;
  }

  @Override
  public void resume()
  {
    unpause();
    convergeStep();
  }

  /**
   * Turn of paused state.
   * Method does not schedule next converge step.
   */
  protected void unpause()
  {
    _paused = false;
  }

  @Override
  public boolean isPaused()
  {
    return _paused;
  }

  private void markSubscriptionAsRequiringUpdate()
  {
    _subscriptionsUpToDate = false;
    markConvergeAsIncomplete();
  }

  private void markConvergeAsIncomplete()
  {
    _convergeComplete = false;
    convergeStep();
  }

  boolean convergeSubscription( @Nonnull final Subscription subscription )
  {
    if ( subscription.isActive() )
    {
      for ( final Subscription child : subscription.getRequiredSubscriptions() )
      {
        if ( convergeSubscription( child ) )
        {
          return true;
        }
      }
      final ChannelDescriptor descriptor = subscription.getDescriptor();
      final DataLoaderService service = getReplicantClientSystem().getDataLoaderService( descriptor.getGraph() );
      // service can be disconnected if it is not a required service and will converge later when it connects
      if ( DataLoaderService.State.CONNECTED == service.getState() )
      {
        final boolean subscribed = service.isSubscribed( descriptor );
        final Object filter = subscription.getFilter();

        final int addIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, filter );
        final int removeIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null );
        final int updateIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, filter );

        if ( ( !subscribed && addIndex < 0 ) || removeIndex > addIndex )
        {
          LOG.info( "Adding subscription: " + descriptor + ". Setting filter to: " + filterToString( filter ) );
          service.requestSubscribe( descriptor, filter );
          return true;
        }
        else if ( addIndex >= 0 )
        {
          //Must have add in pipeline so pause until it completed
          return true;
        }
        else
        {
          //Must be subscribed...
          if ( updateIndex >= 0 )
          {
            //Update in progress so wait till it completes
            return true;
          }

          final Object existing = getSubscriptionManager().getSubscription( descriptor ).getFilter();
          final String newFilter = filterToString( filter );
          final String existingFilter = filterToString( existing );
          if ( !Objects.equals( newFilter, existingFilter ) )
          {
            final String message =
              "Updating subscription: " + descriptor + ". Changing filter to " + newFilter + " from " + existingFilter;
            LOG.info( message );
            service.requestSubscriptionUpdate( descriptor, filter );
            return true;
          }
        }
      }
    }
    return false;
  }

  @Nullable
  protected abstract String filterToString( @Nullable final Object filter );

  void removeOrphanSubscriptions( @Nonnull final Set<ChannelDescriptor> expectedChannels )
  {
    for ( final Enum graph : getSubscriptionManager().getTypeSubscriptions() )
    {
      removeSubscriptionIfOrphan( expectedChannels, new ChannelDescriptor( graph ) );
    }
    for ( final Enum graph : getSubscriptionManager().getInstanceSubscriptionKeys() )
    {
      for ( final Object id : getSubscriptionManager().getInstanceSubscriptions( graph ) )
      {
        removeSubscriptionIfOrphan( expectedChannels, new ChannelDescriptor( graph, id ) );
      }
    }
  }

  void removeSubscriptionIfOrphan( @Nonnull final Set<ChannelDescriptor> expected,
                                   @Nonnull final ChannelDescriptor descriptor )
  {
    if ( !expected.contains( descriptor ) &&
         getSubscriptionManager().getSubscription( descriptor ).isExplicitSubscription() )
    {
      removeOrphanSubscription( descriptor );
    }
  }

  void removeOrphanSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    final DataLoaderService service = getReplicantClientSystem().getDataLoaderService( descriptor.getGraph() );
    if ( DataLoaderService.State.CONNECTED == service.getState() &&
         !service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) )
    {
      LOG.info( "Removing orphan subscription: " + descriptor );
      service.requestUnsubscribe( descriptor );
    }
  }

  @Nonnull
  private synchronized List<Subscription> getSubscriptions()
  {
    if ( !_subscriptionsUpToDate )
    {
      _subscriptions.clear();
      final HashSet<ChannelDescriptor> processed = new HashSet<>();
      for ( final Subscription subscription : getAreaOfInterestService().getSubscriptionsMap().values() )
      {
        collectSubscription( subscription, processed );
      }
      _subscriptionsUpToDate = true;
    }
    return _subscriptions;
  }

  private void collectSubscription( @Nonnull final Subscription subscription,
                                    @Nonnull final HashSet<ChannelDescriptor> processed )
  {
    if ( !processed.contains( subscription.getDescriptor() ) )
    {
      for ( final Subscription requiredSubscription : subscription.getRequiredSubscriptions() )
      {
        collectSubscription( requiredSubscription, processed );
      }
      _subscriptions.add( subscription );
      processed.add( subscription.getDescriptor() );
    }
  }

  private void removeFailedSubscription( @Nonnull final ChannelDescriptor descriptor )
  {
    LOG.info( "Removing failed subscription " + descriptor );
    final Subscription subscription = getAreaOfInterestService().findSubscription( descriptor );
    if ( null != subscription )
    {
      subscription.release();
    }
    convergeStep();
  }

  @Nonnull
  protected abstract EntitySubscriptionManager getSubscriptionManager();

  @Nonnull
  protected abstract AreaOfInterestService getAreaOfInterestService();

  @Nonnull
  protected abstract ReplicantClientSystem getReplicantClientSystem();

  final class ConvergerReplicantSystemListener
    implements ReplicantSystemListener
  {
    @Override
    public void stateChanged( @Nonnull final ReplicantClientSystem system,
                              @Nonnull final ReplicantClientSystem.State newState,
                              @Nonnull final ReplicantClientSystem.State oldState )
    {
      markConvergeAsIncomplete();
    }
  }

  final class ConvergerAreaOfInterestListener
    implements AreaOfInterestListener
  {
    @Override
    public void scopeCreated( @Nonnull final Scope scope )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void scopeDeleted( @Nonnull final Scope scope )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void subscriptionCreated( @Nonnull final Subscription subscription )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void subscriptionUpdated( @Nonnull final Subscription subscription )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void subscriptionDeleted( @Nonnull final Subscription subscription )
    {
      markSubscriptionAsRequiringUpdate();
    }
  }

  final class ConvergerDataLoaderListener
    extends DataLoaderListenerAdapter
  {
    @Override
    public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelDescriptor descriptor )
    {
      convergeStep();
    }

    @Override
    public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelDescriptor descriptor,
                                   @Nonnull final Throwable throwable )
    {
      removeFailedSubscription( descriptor );
    }

    @Override
    public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                        @Nonnull final ChannelDescriptor descriptor )
    {
      convergeStep();
    }

    @Override
    public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                     @Nonnull final ChannelDescriptor descriptor,
                                     @Nonnull final Throwable throwable )
    {
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                               @Nonnull final ChannelDescriptor descriptor )
    {
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                            @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final Throwable throwable )
    {
      removeFailedSubscription( descriptor );
    }
  }
}
