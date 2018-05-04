package org.realityforge.replicant.client.converger;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;
import org.realityforge.replicant.client.transport.DataLoaderService;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.FilterUtil;
import replicant.Replicant;
import replicant.Subscription;

@Singleton
@ArezComponent
public abstract class ContextConverger
{
  private static final Logger LOG = Logger.getLogger( ContextConverger.class.getName() );
  @Nonnull
  private final ConvergerDataLoaderListener _dlListener = new ConvergerDataLoaderListener();
  @Nonnull
  private final ReplicantClientSystem _replicantClientSystem;
  private boolean _paused;
  @Nullable
  private Runnable _preConvergeAction;
  @Nullable
  private Runnable _convergeCompleteAction;

  public static ContextConverger create( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    return new Arez_ContextConverger( replicantClientSystem );
  }

  ContextConverger( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    addListeners();
  }

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

  private void addListeners()
  {
    for ( final DataLoaderEntry entry : _replicantClientSystem.getDataLoaders() )
    {
      entry.getService().addDataLoaderListener( _dlListener );
    }
  }

  private void removeListeners()
  {
    for ( final DataLoaderEntry entry : _replicantClientSystem.getDataLoaders() )
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

  void preConverge()
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
           _replicantClientSystem.getDataLoaders().stream().allMatch( dl -> dl.getService().isIdle() );
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
    if ( !_convergeComplete &&
         !isPaused() &&
         _replicantClientSystem.getState() == ReplicantClientSystem.State.CONNECTED )
    {
      final HashSet<ChannelAddress> expectedChannels = new HashSet<>();
      AreaOfInterest groupTemplate = null;
      AreaOfInterestAction groupAction = null;
      // Need to duplicate the list of AreasOfInterest. If an error occurs while processing AreaOfInterest
      // and the AreaOfInterest is removed, it will result in concurrent exception
      for ( final AreaOfInterest areaOfInterest : new ArrayList<>( Replicant.context().getAreasOfInterest() ) )
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
      final ChannelAddress address = areaOfInterest.getAddress();
      final DataLoaderService service = _replicantClientSystem.getDataLoaderService( address.getChannelType() );
      // service can be disconnected if it is not a required service and will converge later when it connects
      if ( DataLoaderService.State.CONNECTED == service.getState() )
      {
        final Subscription subscription = Replicant.context().findSubscription( address );
        final boolean subscribed = null != subscription;
        final Object filter = areaOfInterest.getChannel().getFilter();

        final int addIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, filter );
        final int removeIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null );
        final int updateIndex =
          service.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address, filter );

        if ( ( !subscribed && addIndex < 0 ) || removeIndex > addIndex )
        {
          if ( null != groupTemplate && !canGroup )
          {
            return ConvergeAction.TERMINATE;
          }
          if ( null == groupTemplate ||
               canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestAction.ADD ) )
          {
            LOG.info( "Adding subscription: " + address + ". " +
                      "Setting filter to: " + FilterUtil.filterToString( filter ) );
            service.requestSubscribe( address, filter );
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

          final Object existing = subscription.getChannel().getFilter();
          final String newFilter = FilterUtil.filterToString( filter );
          final String existingFilter = FilterUtil.filterToString( existing );
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
                "Updating subscription " + address + ". Changing filter to " + newFilter + " from " + existingFilter;
              LOG.info( message );
              service.requestSubscriptionUpdate( address, filter );
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
   * Turn off paused state.
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
  protected void markConvergeAsIncomplete()
  {
    _convergeComplete = false;
    convergeStep();
  }

  void removeOrphanSubscriptions( @Nonnull final Set<ChannelAddress> expectedChannels )
  {
    for ( final Subscription subscription : Replicant.context().getTypeSubscriptions() )
    {
      removeSubscriptionIfOrphan( expectedChannels, subscription );
    }
    for ( final Subscription subscription : Replicant.context().getInstanceSubscriptions() )
    {
      removeSubscriptionIfOrphan( expectedChannels, subscription );
    }
  }

  void removeSubscriptionIfOrphan( @Nonnull final Set<ChannelAddress> expected,
                                   @Nonnull final Subscription subscription )
  {
    final ChannelAddress address = subscription.getChannel().getAddress();
    if ( !expected.contains( address ) && subscription.isExplicitSubscription() )
    {
      removeOrphanSubscription( address );
    }
  }

  void removeOrphanSubscription( @Nonnull final ChannelAddress address )
  {
    final DataLoaderService service = _replicantClientSystem.getDataLoaderService( address.getChannelType() );
    if ( DataLoaderService.State.CONNECTED == service.getState() &&
         !service.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, address, null ) )
    {
      LOG.info( "Removing orphan subscription: " + address );
      service.requestUnsubscribe( address );
    }
  }

  @Action
  protected void removeFailedSubscription( @Nonnull final ChannelAddress address,
                                           @Nonnull final Throwable error )
  {
    LOG.info( "Removing failed subscription " + address );
    final AreaOfInterest areaOfInterest = Replicant.context().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      Disposable.dispose( areaOfInterest );
    }
    convergeStep();
  }

  @Action
  protected void setAreaOfInterestState( @Nonnull final ChannelAddress address,
                                         @Nonnull final AreaOfInterest.Status status,
                                         final boolean attemptEntryLoad,
                                         @Nullable final Throwable throwable )
  {
    LOG.info( "Update AreaOfInterest " + address + " to status " + status );
    final AreaOfInterest areaOfInterest = Replicant.context().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.setStatus( status );
      areaOfInterest.setSubscription( attemptEntryLoad ? Replicant.context().findSubscription( address ) : null );
      areaOfInterest.setError( throwable );
    }
    markConvergeAsIncomplete();
  }

  final class ConvergerDataLoaderListener
    extends DataLoaderListenerAdapter
  {
    @Override
    public void onSubscribeStarted( @Nonnull final DataLoaderService service, @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.LOADING, false, null );
    }

    @Override
    public void onSubscribeCompleted( @Nonnull final DataLoaderService service,
                                      @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.LOADED, true, null );
      convergeStep();
    }

    @Override
    public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final Throwable throwable )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.LOAD_FAILED, false, throwable );
      removeFailedSubscription( address, throwable );
    }

    @Override
    public void onUnsubscribeStarted( @Nonnull final DataLoaderService service, @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UNLOADING, false, null );
    }

    @Override
    public void onUnsubscribeCompleted( @Nonnull final DataLoaderService service,
                                        @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UNLOADED, false, null );
      convergeStep();
    }

    @Override
    public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                     @Nonnull final ChannelAddress address,
                                     @Nonnull final Throwable throwable )
    {
      // Note can not call removeFailedSubscription( address, throwable ) as the unsubscribe failure
      // may be due to the server going away and thus unsubscribe expected to fail. But we should not
      // then remove subscription
      setAreaOfInterestState( address, AreaOfInterest.Status.UNLOADED, false, throwable );
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateStarted( @Nonnull final DataLoaderService service,
                                             @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UPDATING, true, null );
    }

    @Override
    public void onSubscriptionUpdateCompleted( @Nonnull final DataLoaderService service,
                                               @Nonnull final ChannelAddress address )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UPDATED, true, null );
      convergeStep();
    }

    @Override
    public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                            @Nonnull final ChannelAddress address,
                                            @Nonnull final Throwable throwable )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UPDATE_FAILED, false, throwable );
      removeFailedSubscription( address, throwable );
    }
  }
}
