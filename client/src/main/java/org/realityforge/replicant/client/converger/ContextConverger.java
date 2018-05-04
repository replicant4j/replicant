package org.realityforge.replicant.client.converger;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Observable;
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
  @Observable
  public abstract void setPreConvergeAction( @Nullable Runnable preConvergeAction );

  @Nullable
  public abstract Runnable getPreConvergeAction();

  /**
   * Set action that is runs after all the subscriptions have converged.
   */
  @Observable
  public abstract void setConvergeCompleteAction( @Nullable Runnable convergeCompleteAction );

  @Nullable
  public abstract Runnable getConvergeCompleteAction();

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

  @Autorun
  void converge()
  {
    preConverge();
    convergeStep();
  }

  void preConverge()
  {
    final Runnable preConvergeAction = getPreConvergeAction();
    if ( null != preConvergeAction )
    {
      preConvergeAction.run();
    }
  }

  protected void convergeStep()
  {
    if ( _replicantClientSystem.getState() == ReplicantClientSystem.State.CONNECTED )
    {
      final HashSet<ChannelAddress> expectedChannels = new HashSet<>();
      AreaOfInterest groupTemplate = null;
      AreaOfInterestAction groupAction = null;
      for ( final AreaOfInterest areaOfInterest : Replicant.context().getAreasOfInterest() )
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
    final Runnable convergeCompleteAction = getConvergeCompleteAction();
    if ( null != convergeCompleteAction )
    {
      convergeCompleteAction.run();
    }
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
    }

    @Override
    public void onSubscribeFailed( @Nonnull final DataLoaderService service,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final Throwable throwable )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.LOAD_FAILED, false, throwable );
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
    }

    @Override
    public void onUnsubscribeFailed( @Nonnull final DataLoaderService service,
                                     @Nonnull final ChannelAddress address,
                                     @Nonnull final Throwable throwable )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UNLOADED, false, throwable );
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
    }

    @Override
    public void onSubscriptionUpdateFailed( @Nonnull final DataLoaderService service,
                                            @Nonnull final ChannelAddress address,
                                            @Nonnull final Throwable throwable )
    {
      setAreaOfInterestState( address, AreaOfInterest.Status.UPDATE_FAILED, false, throwable );
    }
  }
}
