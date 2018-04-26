package org.realityforge.replicant.client.runtime;

import arez.Disposable;
import arez.annotations.Action;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.aoi.AreaOfInterest;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.aoi.AreaOfInterestListener;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;

public abstract class ContextConverger
{
  private static final Logger LOG = Logger.getLogger( ContextConverger.class.getName() );
  protected static final int CONVERGE_DELAY_IN_MS = 100;
  private final ConvergerAreaOfInterestListener _aoiListener = new ConvergerAreaOfInterestListener();
  private final ConvergerReplicantSystemListener _rsListener = new ConvergerReplicantSystemListener();
  private final ConvergerDataLoaderListener _dlListener = new ConvergerDataLoaderListener();
  private boolean _convergeComplete;
  private boolean _paused;
  private Runnable _preConvergeAction;
  private Runnable _convergeCompleteAction;

  public abstract void activate();

  public abstract void deactivate();

  public abstract boolean isActive();

  /**
   * Set action that is run prior to converging.
   * This is typically used to ensure the subscriptions are uptodate prior to attempting to convergeStep.
   */
  public void setPreConvergeAction( @Nullable final Runnable preConvergeAction )
  {
    _preConvergeAction = preConvergeAction;
  }

  /**
   * Set action that is runs after all the subscriptions have converged.
   */
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

  @Action
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

  public boolean isConvergeComplete()
  {
    return _convergeComplete;
  }

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

  @Action
  protected void convergeStep()
  {
    if ( isActive() &&
         !_convergeComplete &&
         !isPaused() &&
         getReplicantClientSystem().getState() == ReplicantClientSystem.State.CONNECTED )
    {
      final HashSet<ChannelAddress> expectedChannels = new HashSet<>();
      // Need to duplicate the list of subscriptions. If an error occurs while processing subscription
      // and the subscription is removed, it will result in concurrent exception
      final List<Channel> subscriptions = new ArrayList<>( getSubscriptions() );
      Channel template = null;
      AreaOfInterestAction aoiGroupAction = null;
      for ( final Channel subscription : subscriptions )
      {
        expectedChannels.add( subscription.getAddress() );
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

  ConvergeAction convergeSubscription( @Nonnull final Set<ChannelAddress> expectedChannels,
                                       @Nonnull final Channel subscription,
                                       final Channel templateForGrouping,
                                       final AreaOfInterestAction aoiGroupAction,
                                       final boolean canGroup )
  {
    if ( !Disposable.isDisposed( subscription ) )
    {
      final ChannelAddress descriptor = subscription.getAddress();
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

          final Object existing = getSubscriptionManager().getSubscription( descriptor ).getChannel().getFilter();
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

  boolean canGroup( @Nonnull final Channel templateForGrouping,
                    final AreaOfInterestAction aoiGroupAction,
                    @Nonnull final Channel subscription,
                    final AreaOfInterestAction subscriptionAction )
  {
    if ( null != aoiGroupAction && subscriptionAction != null && !aoiGroupAction.equals( subscriptionAction ) )
    {
      return false;
    }

    final boolean sameGraph =
      templateForGrouping.getAddress().getGraph().equals( subscription.getAddress().getGraph() );

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

  /**
   * Pause the converger for the duration of the action.
   */
  public void pauseAndRun( @Nonnull final Runnable action )
  {
    pause();
    try
    {
      Objects.requireNonNull( action ).run();
    }
    finally
    {
      resume();
    }
  }

  public void pause()
  {
    _paused = true;
  }

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

  public boolean isPaused()
  {
    return _paused;
  }

  @Action
  protected void markSubscriptionAsRequiringUpdate()
  {
    //setSubscriptionsUpToDate( false );
    markConvergeAsIncomplete();
  }

  @Action
  protected void markConvergeAsIncomplete()
  {
    _convergeComplete = false;
    convergeStep();
  }

  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return FilterUtil.filterToString( filter );
  }

  void removeOrphanSubscriptions( @Nonnull final Set<ChannelAddress> expectedChannels )
  {
    for ( final Enum graph : getSubscriptionManager().getTypeSubscriptions() )
    {
      removeSubscriptionIfOrphan( expectedChannels, new ChannelAddress( graph ) );
    }
    for ( final Enum graph : getSubscriptionManager().getInstanceSubscriptionKeys() )
    {
      for ( final Object id : getSubscriptionManager().getInstanceSubscriptions( graph ) )
      {
        removeSubscriptionIfOrphan( expectedChannels, new ChannelAddress( graph, id ) );
      }
    }
  }

  void removeSubscriptionIfOrphan( @Nonnull final Set<ChannelAddress> expected,
                                   @Nonnull final ChannelAddress descriptor )
  {
    if ( !expected.contains( descriptor ) &&
         getSubscriptionManager().getSubscription( descriptor ).isExplicitSubscription() )
    {
      removeOrphanSubscription( descriptor );
    }
  }

  void removeOrphanSubscription( @Nonnull final ChannelAddress descriptor )
  {
    final DataLoaderService service = getReplicantClientSystem().getDataLoaderService( descriptor.getGraph() );
    if ( DataLoaderService.State.CONNECTED == service.getState() &&
         !service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) )
    {
      LOG.info( "Removing orphan subscription: " + descriptor );
      service.requestUnsubscribe( descriptor );
    }
  }

  //TODO: @Computed
  @Nonnull
  protected List<Channel> getSubscriptions()
  {
    final ArrayList<Channel> subscriptions = new ArrayList<>();
    final HashSet<ChannelAddress> processed = new HashSet<>();
    for ( final AreaOfInterest subscription : getAreaOfInterestService().getAreasOfInterest() )
    {
      collectSubscription( subscription.getChannel(), processed, subscriptions );
    }
    return subscriptions;
  }

  private void collectSubscription( @Nonnull final Channel subscription,
                                    @Nonnull final HashSet<ChannelAddress> processed,
                                    @Nonnull final ArrayList<Channel> subscriptions )
  {
    if ( !processed.contains( subscription.getAddress() ) )
    {
      subscriptions.add( subscription );
      processed.add( subscription.getAddress() );
    }
  }

  @Action
  protected void removeFailedSubscription( @Nonnull final ChannelAddress descriptor )
  {
    LOG.info( "Removing failed subscription " + descriptor );
    final AreaOfInterest subscription = getAreaOfInterestService().findAreaOfInterest( descriptor );
    if ( null != subscription )
    {
      Disposable.dispose( subscription );
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
    public void channelCreated( @Nonnull final Channel channel )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void channelUpdated( @Nonnull final Channel channel )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void channelDeleted( @Nonnull final Channel channel )
    {
      markSubscriptionAsRequiringUpdate();
    }
  }

  final class ConvergerDataLoaderListener
    extends DataLoaderListenerAdapter
  {
    @Override
    public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelAddress descriptor )
    {
      convergeStep();
    }

    @Override
    public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nonnull final Throwable throwable )
    {
      removeFailedSubscription( descriptor );
    }

    @Override
    public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                        @Nonnull final ChannelAddress descriptor )
    {
      convergeStep();
    }

    @Override
    public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                     @Nonnull final ChannelAddress descriptor,
                                     @Nonnull final Throwable throwable )
    {
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                               @Nonnull final ChannelAddress address )
    {
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                            @Nonnull final ChannelAddress address,
                                            @Nonnull final Throwable throwable )
    {
      removeFailedSubscription( address );
    }
  }
}
