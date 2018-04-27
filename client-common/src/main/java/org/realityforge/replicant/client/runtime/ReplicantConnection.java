package org.realityforge.replicant.client.runtime;

import arez.Disposable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;
import org.realityforge.replicant.client.aoi.AreaOfInterest;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.subscription.EntitySubscriptionManager;

@Singleton
public class ReplicantConnection
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final ReplicantClientSystem _replicantClientSystem;
  private final AreaOfInterestService _areaOfInterestService;
  private final ContextConverger _converger;

  @Inject
  ReplicantConnection( @Nonnull final ContextConverger converger,
                       @Nonnull final EntitySubscriptionManager subscriptionManager,
                       @Nonnull final ReplicantClientSystem replicantClientSystem,
                       @Nonnull final AreaOfInterestService areaOfInterestService )
  {
    _areaOfInterestService = Objects.requireNonNull( areaOfInterestService );
    _subscriptionManager = Objects.requireNonNull( subscriptionManager );
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    _converger = Objects.requireNonNull( converger );
  }

  public void disconnect()
  {
    _converger.deactivate();
    _replicantClientSystem.deactivate();
  }

  public void connect()
  {
    _replicantClientSystem.activate();
    _converger.activate();
  }

  @Nonnull
  public AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  @Nonnull
  public ContextConverger getContextConverger()
  {
    return _converger;
  }

  @Nonnull
  public EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Nonnull
  public ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }

  /**
   * Convert object subscription to root object (specified object Type+ID) to target channels subscription
   * based on function. If the root object is not yet present then return an empty stream.
   * This is typically used by the function passed into the convergeCrossDataSourceSubscriptions() method.
   */
  @SuppressWarnings( "unchecked" )
  @Nonnull
  protected <T, O> Stream<O> instanceSubscriptionToValues( @Nonnull final Class<T> type,
                                                           @Nonnull final Object id,
                                                           @Nonnull final Function<T, Stream<O>> rootToStream )
  {
    final T root = (T) getSubscriptionManager().getEntitySubscription( type, id ).getEntity();
    return null != root ? rootToStream.apply( root ) : Stream.empty();
  }

  /**
   * Converge subscriptions across data sources.
   * All instances of the subscriptions to the source channelType within the scope are collected.
   * The supplied function is used to generate a stream of expected subscriptions to the target channelType
   * that are reachable from the source channelTypes. If an expected subscription is missing it is added,
   * if an additional subscription is present then it is released.
   */
  public void convergeCrossDataSourceSubscriptions( @Nonnull final Enum sourceChannelType,
                                                    @Nonnull final Enum targetChannelType,
                                                    @Nullable final Object filter,
                                                    @Nonnull final Function<Object, Stream<Object>> sourceIDToTargetIDs )
  {
    getContextConverger().pauseAndRun( () -> doConvergeCrossDataSourceSubscriptions( sourceChannelType,
                                                                                     targetChannelType,
                                                                                     filter,
                                                                                     sourceIDToTargetIDs ) );
  }

  /**
   * This is worker method for convergeCrossDataSourceSubscriptions. Do not use, do not override.
   */
  protected void doConvergeCrossDataSourceSubscriptions( @Nonnull final Enum sourceChannelType,
                                                         @Nonnull final Enum targetChannelType,
                                                         @Nullable final Object filter,
                                                         @Nonnull final Function<Object, Stream<Object>> sourceIDToTargetIDs )
  {
    // Need to check both subscription and filters are identical.
    // If they are not the next step will either update the filters or add subscriptions
    final AreaOfInterestService service = getAreaOfInterestService();
    final Map<Object, Channel> existing =
      service
        .getAreasOfInterest()
        .stream()
        .filter( s -> s.getAddress().getChannelType().equals( targetChannelType ) )
        .filter( subscription -> FilterUtil.filtersEqual( subscription.getChannel().getFilter(), filter ) )
        .map( AreaOfInterest::getChannel )
        .collect( Collectors.toMap( s -> s.getAddress().getId(), Function.identity() ) );

    //noinspection ConstantConditions
    service
      .getAreasOfInterest()
      .stream()
      .filter( s -> s.getAddress().getChannelType() == sourceChannelType )
      .map( s -> s.getAddress().getId() )
      .flatMap( sourceIDToTargetIDs )
      .filter( Objects::nonNull )
      .filter( id -> null == existing.remove( id ) )
      .forEach( id -> service.findOrCreateSubscription( asAddress( targetChannelType, id ), filter ) );

    getSubscriptionManager().getInstanceChannelSubscriptions( sourceChannelType ).stream().
      flatMap( sourceIDToTargetIDs ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> service.findOrCreateSubscription( asAddress( targetChannelType, id ), filter ) );

    existing.values().forEach( Disposable::dispose );
  }

  @Nonnull
  protected ChannelAddress asAddress( @Nonnull final Enum channel, @Nullable final Object id )
  {
    return new ChannelAddress( channel, id );
  }
}
