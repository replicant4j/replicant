package org.realityforge.replicant.client.converger;

import arez.Disposable;
import arez.annotations.Action;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.aoi.AreaOfInterest;
import org.realityforge.replicant.client.aoi.AreaOfInterestListener;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.runtime.ReplicantSystemListener;
import org.realityforge.replicant.client.subscription.EntitySubscriptionManager;
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
      AreaOfInterest groupTemplate = null;
      AreaOfInterestAction groupAction = null;
      // Need to duplicate the list of AreasOfInterest. If an error occurs while processing AreaOfInterest
      // and the AreaOfInterest is removed, it will result in concurrent exception
      for ( final AreaOfInterest areaOfInterest : new ArrayList<>( getAreaOfInterestService().getAreasOfInterest() ) )
      {
        expectedChannels.add( areaOfInterest.getAddress() );
        final ConvergeAction convergeAction =
          convergeAreaOfInterest( areaOfInterest, groupTemplate, groupAction, true );
        switch ( convergeAction )
        {
          case TERMINATE:
            return;
          case SUBMITTED_ADD:
            groupAction = AreaOfInterestAction.ADD;
            groupTemplate = areaOfInterest;
            break;
          case SUBMITTED_UPDATE:
            groupAction = AreaOfInterestAction.UPDATE;
            groupTemplate = areaOfInterest;
            break;
          case IN_PROGRESS:
            if ( null == groupTemplate )
            {
              // First thing in the subscription queue is in flight, so terminate
              return;
            }
            break;
          case NO_ACTION:
            break;
        }
      }
      if ( null != groupTemplate )
      {
        return;
      }

      removeOrphanSubscriptions( expectedChannels );
      convergeComplete();
    }
  }

  final ConvergeAction convergeAreaOfInterest( @Nonnull final AreaOfInterest areaOfInterest,
                                               @Nullable final AreaOfInterest groupTemplate,
                                               @Nullable final AreaOfInterestAction groupAction,
                                               final boolean canGroup )
  {
    if ( !Disposable.isDisposed( areaOfInterest ) )
    {
      final ChannelAddress descriptor = areaOfInterest.getAddress();
      final DataLoaderService service = getReplicantClientSystem().getDataLoaderService( descriptor.getChannelType() );
      // service can be disconnected if it is not a required service and will converge later when it connects
      if ( DataLoaderService.State.CONNECTED == service.getState() )
      {
        final boolean subscribed = service.isSubscribed( descriptor );
        final Object filter = areaOfInterest.getChannel().getFilter();

        final int addIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, descriptor, filter );
        final int removeIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, descriptor, null );
        final int updateIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, descriptor, filter );

        if ( ( !subscribed && addIndex < 0 ) || removeIndex > addIndex )
        {
          if ( null != groupTemplate && !canGroup )
          {
            return ConvergeAction.TERMINATE;
          }
          if ( null == groupTemplate ||
               canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestAction.ADD ) )
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

          final Object existing =
            getSubscriptionManager().getChannelSubscription( descriptor ).getChannel().getFilter();
          final String newFilter = filterToString( filter );
          final String existingFilter = filterToString( existing );
          if ( !Objects.equals( newFilter, existingFilter ) )
          {
            if ( null != groupTemplate && !canGroup )
            {
              return ConvergeAction.TERMINATE;
            }

            if ( null == groupTemplate ||
                 canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestAction.UPDATE ) )
            {
              final String message =
                "Updating subscription " + descriptor + ". Changing filter to " + newFilter + " from " + existingFilter;
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

  boolean canGroup( @Nonnull final AreaOfInterest groupTemplate,
                    @Nullable final AreaOfInterestAction groupAction,
                    @Nonnull final AreaOfInterest areaOfInterest,
                    @Nullable final AreaOfInterestAction action )
  {
    if ( null != groupAction && null != action && !groupAction.equals( action ) )
    {
      return false;
    }
    else
    {
      final boolean sameChannel =
        groupTemplate.getAddress().getChannelType().equals( areaOfInterest.getAddress().getChannelType() );

      return sameChannel &&
             ( AreaOfInterestAction.REMOVE == action ||
               FilterUtil.filtersEqual( groupTemplate.getChannel().getFilter(),
                                        areaOfInterest.getChannel().getFilter() ) );
    }
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
    for ( final Enum channelType : getSubscriptionManager().getTypeChannelSubscriptions() )
    {
      removeSubscriptionIfOrphan( expectedChannels, new ChannelAddress( channelType ) );
    }
    for ( final Enum channelType : getSubscriptionManager().getInstanceChannelSubscriptionKeys() )
    {
      for ( final Object id : getSubscriptionManager().getInstanceChannelSubscriptions( channelType ) )
      {
        removeSubscriptionIfOrphan( expectedChannels, new ChannelAddress( channelType, id ) );
      }
    }
  }

  void removeSubscriptionIfOrphan( @Nonnull final Set<ChannelAddress> expected,
                                   @Nonnull final ChannelAddress descriptor )
  {
    if ( !expected.contains( descriptor ) &&
         getSubscriptionManager().getChannelSubscription( descriptor ).isExplicitSubscription() )
    {
      removeOrphanSubscription( descriptor );
    }
  }

  void removeOrphanSubscription( @Nonnull final ChannelAddress descriptor )
  {
    final DataLoaderService service = getReplicantClientSystem().getDataLoaderService( descriptor.getChannelType() );
    if ( DataLoaderService.State.CONNECTED == service.getState() &&
         !service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, descriptor, null ) )
    {
      LOG.info( "Removing orphan subscription: " + descriptor );
      service.requestUnsubscribe( descriptor );
    }
  }

  @Action
  protected void removeFailedSubscription( @Nonnull final ChannelAddress descriptor )
  {
    LOG.info( "Removing failed subscription " + descriptor );
    final AreaOfInterest subscription = getAreaOfInterestService().findAreaOfInterestByAddress( descriptor );
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
    public void areaOfInterestCreated( @Nonnull final AreaOfInterest areaOfInterest )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void areaOfInterestUpdated( @Nonnull final AreaOfInterest areaOfInterest )
    {
      markSubscriptionAsRequiringUpdate();
    }

    @Override
    public void areaOfInterestDeleted( @Nonnull final AreaOfInterest areaOfInterest )
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
