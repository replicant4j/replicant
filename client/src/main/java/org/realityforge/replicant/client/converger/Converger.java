package org.realityforge.replicant.client.converger;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Observable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.anodoc.VisibleForTesting;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.transport.AreaOfInterestAction;
import org.realityforge.replicant.client.transport.DataLoaderService;
import replicant.AreaOfInterest;
import replicant.ChannelAddress;
import replicant.FilterUtil;
import replicant.Replicant;
import replicant.Subscription;
import replicant.spy.SubscriptionOrphanedEvent;
import static org.realityforge.braincheck.Guards.*;

@Singleton
@ArezComponent
public abstract class Converger
{
  @Nonnull
  private final ReplicantClientSystem _replicantClientSystem;

  public static Converger create( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    return new Arez_Converger( replicantClientSystem );
  }

  Converger( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
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

  @Autorun
  void converge()
  {
    preConverge();
    removeOrphanSubscriptions();
    if ( _replicantClientSystem.getState() == ReplicantClientSystem.State.CONNECTED )
    {
      convergeStep();
    }
  }

  void preConverge()
  {
    final Runnable preConvergeAction = getPreConvergeAction();
    if ( null != preConvergeAction )
    {
      preConvergeAction.run();
    }
  }

  private void convergeStep()
  {
    AreaOfInterest groupTemplate = null;
    AreaOfInterestAction groupAction = null;
    for ( final AreaOfInterest areaOfInterest : Replicant.context().getAreasOfInterest() )
    {
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

    convergeComplete();
  }

  @VisibleForTesting
  final ConvergeAction convergeAreaOfInterest( @Nonnull final AreaOfInterest areaOfInterest,
                                               @Nullable final AreaOfInterest groupTemplate,
                                               @Nullable final AreaOfInterestAction groupAction,
                                               final boolean canGroup )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> !Disposable.isDisposed( areaOfInterest ),
                 () -> "Replicant-0020: Invoked convergeAreaOfInterest() with disposed AreaOfInterest." );
    }
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

  void removeOrphanSubscriptions()
  {
    final HashSet<ChannelAddress> expected = new HashSet<>();
    Replicant.context().getAreasOfInterest().forEach( aoi -> expected.add( aoi.getAddress() ) );

    for ( final Subscription subscription : Replicant.context().getTypeSubscriptions() )
    {
      removeSubscriptionIfOrphan( expected, subscription );
    }
    for ( final Subscription subscription : Replicant.context().getInstanceSubscriptions() )
    {
      removeSubscriptionIfOrphan( expected, subscription );
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
      if ( Replicant.areSpiesEnabled() && Replicant.context().getSpy().willPropagateSpyEvents() )
      {
        final Subscription subscription = Replicant.context().findSubscription( address );
        assert null != subscription;
        Replicant.context().getSpy().reportSpyEvent( new SubscriptionOrphanedEvent( subscription ) );
      }
      service.requestUnsubscribe( address );
    }
  }

  @Action
  protected void setAreaOfInterestState( @Nonnull final ChannelAddress address,
                                         @Nonnull final AreaOfInterest.Status status,
                                         @Nullable final Throwable throwable )
  {
    final AreaOfInterest areaOfInterest = Replicant.context().findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      areaOfInterest.updateAreaOfInterest( status, throwable );
    }
  }
}
