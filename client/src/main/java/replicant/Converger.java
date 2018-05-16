package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Autorun;
import arez.annotations.Observable;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.SubscriptionOrphanedEvent;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent
abstract class Converger
  extends ReplicantService
{
  /**
   * Enum describing action during converge step.
   */
  enum Action
  {
    /// The submission has been added to the AOI queue
    SUBMITTED_ADD,
    /// The submission has been added to the AOI queue
    SUBMITTED_UPDATE,
    /// The submission has been added to the AOI queue, and can't be grouped
    TERMINATE,
    /// The submission is already in progress, still waiting for a response
    IN_PROGRESS,
    /// Nothing was done, fully converged
    NO_ACTION
  }

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
    AreaOfInterestRequest.Type groupAction = null;
    for ( final AreaOfInterest areaOfInterest : getReplicantContext().getAreasOfInterest() )
    {
      final Action action = convergeAreaOfInterest( areaOfInterest, groupTemplate, groupAction, true );
      switch ( action )
      {
        case TERMINATE:
          return;
        case SUBMITTED_ADD:
          groupAction = AreaOfInterestRequest.Type.ADD;
          groupTemplate = areaOfInterest;
          break;
        case SUBMITTED_UPDATE:
          groupAction = AreaOfInterestRequest.Type.UPDATE;
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

  @Nonnull
  final Action convergeAreaOfInterest( @Nonnull final AreaOfInterest areaOfInterest,
                                       @Nullable final AreaOfInterest groupTemplate,
                                       @Nullable final AreaOfInterestRequest.Type groupAction,
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
      final Object filter = areaOfInterest.getFilter();

      final int addIndex =
        connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.ADD, address, filter );
      final int removeIndex =
        connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.REMOVE, address, null );
      final int updateIndex =
        connector.lastIndexOfPendingAreaOfInterestRequest( AreaOfInterestRequest.Type.UPDATE, address, filter );

      if ( ( !subscribed && addIndex < 0 ) || removeIndex > addIndex )
      {
        if ( null != groupTemplate && !canGroup )
        {
          return Action.TERMINATE;
        }
        if ( null == groupTemplate ||
             canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestRequest.Type.ADD ) )
        {
          connector.requestSubscribe( address, filter );
          return Action.SUBMITTED_ADD;
        }
        else
        {
          return Action.NO_ACTION;
        }
      }
      else if ( addIndex >= 0 )
      {
        //Must have add in pipeline so pause until it completed
        return Action.IN_PROGRESS;
      }
      else
      {
        // Must be subscribed...
        if ( updateIndex >= 0 )
        {
          //Update in progress so wait till it completes
          return Action.IN_PROGRESS;
        }

        if ( !FilterUtil.filtersEqual( filter, subscription.getFilter() ) )
        {
          if ( null != groupTemplate && !canGroup )
          {
            return Action.TERMINATE;
          }

          if ( null == groupTemplate ||
               canGroup( groupTemplate, groupAction, areaOfInterest, AreaOfInterestRequest.Type.UPDATE ) )
          {
            connector.requestSubscriptionUpdate( address, filter );
            return Action.SUBMITTED_UPDATE;
          }
          else
          {
            return Action.NO_ACTION;
          }
        }
      }
    }
    return Action.NO_ACTION;
  }

  boolean canGroup( @Nonnull final AreaOfInterest groupTemplate,
                    @Nullable final AreaOfInterestRequest.Type groupAction,
                    @Nonnull final AreaOfInterest areaOfInterest,
                    @Nullable final AreaOfInterestRequest.Type action )
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
             ( AreaOfInterestRequest.Type.REMOVE == action ||
               FilterUtil.filtersEqual( groupTemplate.getFilter(), areaOfInterest.getFilter() ) );
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
      .map( Subscription::getAddress )
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
           connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null );
  }

  @Nonnull
  private ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }
}
