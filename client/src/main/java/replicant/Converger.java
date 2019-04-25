package replicant;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.Observe;
import java.util.Collection;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.SubscriptionOrphanedEvent;
import static org.realityforge.braincheck.Guards.*;

@ArezComponent( deferSchedule = true, disposeNotifier = Feature.DISABLE )
abstract class Converger
  extends ReplicantService
{
  /**
   * Enum describing action during converge step.
   */
  enum Action
  {
    /**
     * The request has resulted in a subscribe request added to the AOI queue.
     */
    SUBMITTED_ADD,
    /**
     * The request has resulted in a subscription update request added to the AOI queue.
     */
    SUBMITTED_UPDATE,
    /**
     * The request to update subscription with static filter has resulted in a remove request added to the AOI queue.
     */
    SUBMITTED_REMOVE,
    /**
     * The request is already in progress, still waiting for a response.
     */
    IN_PROGRESS,
    /**
     * Nothing was done, fully converged.
     */
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

  @Observe( mutation = true, nestedActionsAllowed = true )
  void converge()
  {
    preConverge();
    if ( RuntimeState.CONNECTED == getReplicantRuntime().getState() )
    {
      convergeStep();
    }
  }

  @arez.annotations.Action( requireNewTransaction = true, verifyRequired = false )
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

  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  private void convergeStep()
  {
    AreaOfInterest groupTemplate = null;
    AreaOfInterestRequest.Type groupAction = null;
    for ( final AreaOfInterest areaOfInterest : getReplicantContext().getAreasOfInterest() )
    {
      // Make sure we observe the filter so that if it is changed, a re-converge will happen
      areaOfInterest.getFilter();

      // Make sure we observe the status so that converger will re-run when status updates. Usually not needed
      // except when multiple areaOfInterest are queued up simultaneously and the the later can not be grouped
      // into first AreaOfInterest. If this is not here then the converger will not re-run.
      areaOfInterest.getStatus();

      if ( AreaOfInterest.Status.DELETED != areaOfInterest.getStatus() )
      {
        final Action action = convergeAreaOfInterest( areaOfInterest, groupTemplate, groupAction );
        switch ( action )
        {
          case SUBMITTED_ADD:
            groupAction = AreaOfInterestRequest.Type.ADD;
            groupTemplate = areaOfInterest;
            break;
          case SUBMITTED_UPDATE:
            groupAction = AreaOfInterestRequest.Type.UPDATE;
            groupTemplate = areaOfInterest;
            break;
          case SUBMITTED_REMOVE:
            // A request to update a subscription that has static filter has resulted in remove being submitted.
            return;
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
    }
    if ( null != groupTemplate )
    {
      return;
    }

    convergeComplete();
  }

  @arez.annotations.Action( requireNewTransaction = true, verifyRequired = false )
  @Nonnull
  Action convergeAreaOfInterest( @Nonnull final AreaOfInterest areaOfInterest,
                                 @Nullable final AreaOfInterest groupTemplate,
                                 @Nullable final AreaOfInterestRequest.Type groupAction )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> Disposable.isNotDisposed( areaOfInterest ),
                 () -> "Replicant-0020: Invoked convergeAreaOfInterest() with disposed AreaOfInterest." );
    }
    final ChannelAddress address = areaOfInterest.getAddress();
    final Connector connector = getReplicantRuntime().getConnector( address.getSystemId() );
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
          final SystemSchema schema = getReplicantContext().getSchemaService().getById( address.getSystemId() );
          final ChannelSchema.FilterType filterType = schema.getChannel( address.getChannelId() ).getFilterType();
          if ( null == groupTemplate && ChannelSchema.FilterType.DYNAMIC != filterType )
          {
            /*
            If the subscription needs an update but the backend does not support updates
            and subscription is explicitly subscribed then need to do a remove. Eventually it will
            fall through the add path once remove goes through. If the subscription is NOT explicitly
            subscribed then generate an error and fail.
            */
            if ( Replicant.shouldCheckInvariants() )
            {
              invariant( subscription::isExplicitSubscription,
                         () -> "Replicant-0083: Attempting to update channel " + address + " but channel does not " +
                               "allow dynamic updates of filter and channel has not been explicitly subscribed." );
            }
            connector.requestUnsubscribe( address );
            return Action.SUBMITTED_REMOVE;
          }
          else if ( null == groupTemplate ||
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
        else
        {
          /*
           * The AreaOfInterest was added but an existing subscription matched it exactly.
           * If the subscription is explicitly subscribed then just update the status of
           * the AreaOfInterest, otherwise request subscription so that the server is aware
           * of the explicit subscription.
           */
          if ( AreaOfInterest.Status.NOT_ASKED == areaOfInterest.getStatus() )
          {
            if ( subscription.isExplicitSubscription() )
            {
              areaOfInterest.updateAreaOfInterest( AreaOfInterest.Status.LOADED, null );
            }
            else
            {
              connector.requestSubscribe( address, filter );
              return Action.SUBMITTED_ADD;
            }
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
        groupTemplate.getAddress().getSystemId() == areaOfInterest.getAddress().getSystemId() &&
        groupTemplate.getAddress().getChannelId() == areaOfInterest.getAddress().getChannelId();

      return sameChannel &&
             ( AreaOfInterestRequest.Type.REMOVE == action ||
               FilterUtil.filtersEqual( groupTemplate.getFilter(), areaOfInterest.getFilter() ) );
    }
  }

  @arez.annotations.Action
  void removeOrphanSubscriptions()
  {
    final HashSet<ChannelAddress> expected = new HashSet<>();
    getReplicantContext().getAreasOfInterest().forEach( aoi -> expected.add( aoi.getAddress() ) );

    final SubscriptionService subscriptionService = getReplicantContext().getSubscriptionService();
    removeOrphanSubscriptions( subscriptionService.getTypeSubscriptions(), expected );
    removeOrphanSubscriptions( subscriptionService.getInstanceSubscriptions(), expected );
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
    getReplicantRuntime().getConnector( address.getSystemId() ).requestUnsubscribe( address );
  }

  /**
   * Return true if connector for address has a remove pending for address or the connector is not connected.
   *
   * @return true if connector for address has a remove pending for address or the connector is not connected.
   */
  private boolean isRemovePending( @Nonnull final ChannelAddress address )
  {
    final Connector connector = getReplicantRuntime().getConnector( address.getSystemId() );
    return ConnectorState.CONNECTED != connector.getState() ||
           connector.isAreaOfInterestRequestPending( AreaOfInterestRequest.Type.REMOVE, address, null );
  }

  @Nonnull
  private ReplicantRuntime getReplicantRuntime()
  {
    return getReplicantContext().getRuntime();
  }
}
