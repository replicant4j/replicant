package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Observable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.SubscriptionOrphanedEvent;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent
abstract class Converger
  extends ReplicantService
{
  static Converger create( @Nullable final ReplicantContext context )
  {
    return new Arez_Converger( context );
  }

  Converger( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  /**
   * Specify the action that invoked prior to converging the desired AreaOfInterest to actual Subscriptions.
   * This action is often used when subscriptions in one system trigger subscriptions in another system.
   * This property is an Arez observable.
   *
   * @param preConvergeAction the action.
   */
  @Observable
  abstract void setPreConvergeAction( @Nullable SafeProcedure preConvergeAction );

  /**
   * Return the pre-converge action. See {@link #setPreConvergeAction(SafeProcedure)} for further details.
   * This property is an Arez observable.
   *
   * @return the action.
   */
  @Nullable
  abstract SafeProcedure getPreConvergeAction();

  /**
   * Specify the action that is invoked after all the subscriptions converge.
   * This property is an Arez observable.
   *
   * @param convergeCompleteAction the action.
   */
  @Observable
  abstract void setConvergeCompleteAction( @Nullable SafeProcedure convergeCompleteAction );

  /**
   * Return the converge complete action. See {@link #setConvergeCompleteAction(SafeProcedure)} for further details.
   * This property is an Arez observable.
   *
   * @return the action.
   */
  @Nullable
  abstract SafeProcedure getConvergeCompleteAction();

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
    final SafeProcedure preConvergeAction = getPreConvergeAction();
    if ( null != preConvergeAction )
    {
      preConvergeAction.call();
    }
  }

  void convergeComplete()
  {
    final SafeProcedure convergeCompleteAction = getConvergeCompleteAction();
    if ( null != convergeCompleteAction )
    {
      convergeCompleteAction.call();
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

    removeOrphanSubscriptions( getReplicantContext().getTypeSubscriptions(), expected );
    removeOrphanSubscriptions( getReplicantContext().getInstanceSubscriptions(), expected );
  }

  private void removeOrphanSubscriptions( @Nonnull final Collection<Subscription> subscriptions,
                                          @Nonnull final HashSet<ChannelAddress> expected )
  {
    subscriptions
      .stream()
      // Subscription must be explicit
      .filter( Subscription::isExplicitSubscription )
      // Subscription should not be one of expected
      .map( s -> s.getChannel().getAddress() )
      .filter( address -> !expected.contains( address ) )
      // Subscription should not already have a remove pending
      .filter( address -> !isRemovePending( address ) )
      .forEachOrdered( this::removeOrphanSubscription );
  }

  private void removeOrphanSubscription( @Nonnull final ChannelAddress address )
  {
    if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
    {
      final Subscription subscription = getReplicantContext().findSubscription( address );
      assert null != subscription;
      getReplicantContext().getSpy().reportSpyEvent( new SubscriptionOrphanedEvent( subscription ) );
    }
    getReplicantRuntime().getConnector( address.getSystem() ).requestUnsubscribe( address );
  }

  /**
   * Return true if connector for address has a remove pending for address or the connector is not connected.
   *
   * @return true if connector for address has a remove pending for address or the connector is not connected.
   */
  private boolean isRemovePending( @Nonnull final ChannelAddress address )
  {
    final Connector connector = getReplicantRuntime().getConnector( address.getSystem() );
    return ConnectorState.CONNECTED != connector.getState() ||
           connector.isAreaOfInterestActionPending( AreaOfInterestAction.REMOVE, address, null );
  }

  @Nonnull
  private ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }
}
