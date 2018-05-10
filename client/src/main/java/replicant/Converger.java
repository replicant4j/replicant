package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Observable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import replicant.spy.SubscriptionOrphanedEvent;
import static org.realityforge.braincheck.Guards.*;

@Singleton
@ArezComponent
public abstract class Converger
{
  /**
   * Reference to the context to which this service belongs.
   */
  @Nullable
  private final ReplicantContext _context;

  static Converger create( @Nullable final ReplicantContext context )
  {
    return new Arez_Converger( context );
  }

  Converger( @Nullable final ReplicantContext context )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      apiInvariant( () -> Replicant.areZonesEnabled() || null == context,
                    () -> "Replicant-0124: Converger passed a context but Replicant.areZonesEnabled() is false" );
    }
    _context = Replicant.areZonesEnabled() ? Objects.requireNonNull( context ) : null;
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
    if ( RuntimeState.CONNECTED == getReplicantRuntime().getState() )
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

  void convergeComplete()
  {
    final Runnable convergeCompleteAction = getConvergeCompleteAction();
    if ( null != convergeCompleteAction )
    {
      convergeCompleteAction.run();
    }
  }

  private void convergeStep()
  {
    AreaOfInterest groupTemplate = null;
    AreaOfInterestAction groupAction = null;
    for ( final AreaOfInterest areaOfInterest : getReplicantContext().getAreasOfInterest() )
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
    final Connector connector = getReplicantRuntime().getConnector( address.getSystem() );
    // service can be disconnected if it is not a required service and will converge later when it connects
    if ( ConnectorState.CONNECTED == connector.getState() )
    {
      final Subscription subscription = getReplicantContext().findSubscription( address );
      final boolean subscribed = null != subscription;
      final Object filter = areaOfInterest.getChannel().getFilter();

      final int addIndex =
        connector.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.ADD, address, filter );
      final int removeIndex =
        connector.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.REMOVE, address, null );
      final int updateIndex =
        connector.indexOfPendingAreaOfInterestAction( AreaOfInterestAction.UPDATE, address, filter );

      if ( ( !subscribed && addIndex < 0 ) || removeIndex > addIndex )
      {
        if ( null != groupTemplate && !canGroup )
        {
          return ConvergeAction.TERMINATE;
        }
        if ( null == groupTemplate ||
             canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestAction.ADD ) )
        {
          connector.requestSubscribe( address, filter );
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
            connector.requestSubscriptionUpdate( address, filter );
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

  void removeOrphanSubscriptions()
  {
    final HashSet<ChannelAddress> expected = new HashSet<>();
    getReplicantContext().getAreasOfInterest().forEach( aoi -> expected.add( aoi.getAddress() ) );

    for ( final Subscription subscription : getReplicantContext().getTypeSubscriptions() )
    {
      removeSubscriptionIfOrphan( expected, subscription );
    }
    for ( final Subscription subscription : getReplicantContext().getInstanceSubscriptions() )
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
    final ReplicantContext replicantContext = getReplicantContext();
    final Connector connector = getReplicantRuntime().getConnector( address.getSystem() );
    if ( ConnectorState.CONNECTED == connector.getState() &&
         !connector.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, address, null ) )
    {
      if ( Replicant.areSpiesEnabled() && replicantContext.getSpy().willPropagateSpyEvents() )
      {
        final Subscription subscription = replicantContext.findSubscription( address );
        assert null != subscription;
        replicantContext.getSpy().reportSpyEvent( new SubscriptionOrphanedEvent( subscription ) );
      }
      connector.requestUnsubscribe( address );
    }
  }

  @Nonnull
  private ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }

  @Nonnull
  final ReplicantContext getReplicantContext()
  {
    return Replicant.areZonesEnabled() ? Objects.requireNonNull( _context ) : Replicant.context();
  }
}
