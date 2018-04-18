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
import org.realityforge.replicant.client.FilterUtil;
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
  private Runnable _convergeCompleteAction;

  @Override
  public void setPreConvergeAction( @Nullable final Runnable preConvergeAction )
  {
    _preConvergeAction = preConvergeAction;
  }

  @Override
  public void setConvergeCompleteAction( @Nullable final Runnable convergeCompleteAction )
  {
    _convergeCompleteAction = convergeCompleteAction;
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

  public void converge()
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

  @Override
  public boolean isConvergeComplete()
  {
    return _convergeComplete;
  }

  @Override
  public boolean isIdle()
  {
    return isConvergeComplete() &&
           getReplicantClientSystem().getDataLoaders().stream().allMatch( dl -> dl.getService().isIdle() );
  }

  enum ConvergeAction
  {
    SUBMITTED_ADD,    // The submission has been added to the AOI queue
    SUBMITTED_UPDATE, // The submission has been added to the AOI queue
    TERMINATE, // The submission has been added to the AOI queue, and can't be grouped
    IN_PROGRESS,  // The submission is already in progress, still waiting for a response
    NO_ACTION     // Nothing was done, fully converged
  }

  protected void convergeStep()
  {
    if ( isActive() &&
         !_convergeComplete &&
         !isPaused() &&
         getReplicantClientSystem().getState() == ReplicantClientSystem.State.CONNECTED )
    {
      final HashSet<ChannelDescriptor> expectedChannels = new HashSet<>();
      // Need to duplicate the list of subscriptions. If an error occurs while processing subscription
      // and the subscription is removed, it will result in concurrent exception
      final List<Subscription> subscriptions = new ArrayList<>( getSubscriptions() );
      Subscription template = null;
      AreaOfInterestAction aoiGroupAction = null;
      for ( final Subscription subscription : subscriptions )
      {
        expectedChannels.add( subscription.getDescriptor() );
        final ConvergeAction convergeAction =
          convergeSubscription( expectedChannels, subscription, template, aoiGroupAction, true );
        switch ( convergeAction )
        {
          case TERMINATE:
            return;
          case SUBMITTED_ADD:
            aoiGroupAction = AreaOfInterestAction.ADD;
            template = subscription;
            break;
          case SUBMITTED_UPDATE:
            aoiGroupAction = AreaOfInterestAction.UPDATE;
            template = subscription;
            break;
          case IN_PROGRESS:
            if ( null == template )
            {
              // First thing in the subscription queue is in flight, so terminate
              return;
            }
            break;
          case NO_ACTION:
            break;
        }
      }
      if ( null != template )
      {
        return;
      }

      removeOrphanSubscriptions( expectedChannels );
      convergeComplete();
    }
  }

  ConvergeAction convergeSubscription( @Nonnull final Set<ChannelDescriptor> expectedChannels,
                                       @Nonnull final Subscription subscription,
                                       final Subscription templateForGrouping,
                                       final AreaOfInterestAction aoiGroupAction,
                                       final boolean canGroup )
  {
    if ( subscription.isActive() )
    {
      for ( final Subscription child : subscription.getRequiredSubscriptions() )
      {
        expectedChannels.add( child.getDescriptor() );
        switch ( convergeSubscription( expectedChannels, child, templateForGrouping, aoiGroupAction, false ) )
        {
          case NO_ACTION:
            break;
          default:
            return ConvergeAction.TERMINATE;
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
          if ( null != templateForGrouping && !canGroup )
          {
            return ConvergeAction.TERMINATE;
          }
          if ( null == templateForGrouping ||
               canGroup( templateForGrouping, aoiGroupAction, subscription, AreaOfInterestAction.ADD ) )
          {
            LOG.info( "Adding subscription: " + descriptor + ". Setting filter to: " + filterToString( filter ) );
            service.requestSubscribe( descriptor, filter );
            return ConvergeAction.SUBMITTED_ADD;
          }
          else
          {
            return ConvergeAction.NO_ACTION;
          }
        }
        else if ( addIndex >= 0 )
        {
          //Must have add in pipeline so pause until it completed
          return ConvergeAction.IN_PROGRESS;
        }
        else
        {
          // Must be subscribed...
          if ( updateIndex >= 0 )
          {
            //Update in progress so wait till it completes
            return ConvergeAction.IN_PROGRESS;
          }

          final Object existing = getSubscriptionManager().getSubscription( descriptor ).getFilter();
          final String newFilter = filterToString( filter );
          final String existingFilter = filterToString( existing );
          if ( !Objects.equals( newFilter, existingFilter ) )
          {
            if ( null != templateForGrouping && !canGroup )
            {
              return ConvergeAction.TERMINATE;
            }

            if ( null == templateForGrouping ||
                 canGroup( templateForGrouping, aoiGroupAction, subscription, AreaOfInterestAction.UPDATE ) )
            {
              final String message = "Updating subscription: " + descriptor + ". Changing filter to " + newFilter +
                                     " from " + existingFilter;
              LOG.info( message );
              service.requestSubscriptionUpdate( descriptor, filter );
              return ConvergeAction.SUBMITTED_UPDATE;
            }
            else
            {
              return ConvergeAction.NO_ACTION;
            }
          }
        }
      }
    }
    return ConvergeAction.NO_ACTION;
  }

  boolean canGroup( @Nonnull final Subscription templateForGrouping,
                    final AreaOfInterestAction aoiGroupAction,
                    @Nonnull final Subscription subscription,
                    final AreaOfInterestAction subscriptionAction )
  {
    if ( null != aoiGroupAction && subscriptionAction != null && !aoiGroupAction.equals( subscriptionAction ) )
    {
      return false;
    }

    final boolean sameGraph =
      templateForGrouping.getDescriptor().getGraph().equals( subscription.getDescriptor().getGraph() );

    return sameGraph &&
           ( AreaOfInterestAction.REMOVE == subscriptionAction ||
             FilterUtil.filtersEqual( templateForGrouping.getFilter(), subscription.getFilter() ) );
  }

  private void convergeComplete()
  {
    _convergeComplete = true;
    if ( null != _convergeCompleteAction )
    {
      _convergeCompleteAction.run();
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

  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return FilterUtil.filterToString( filter );
  }

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
