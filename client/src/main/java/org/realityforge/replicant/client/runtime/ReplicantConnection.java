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
import replicant.AreaOfInterest;
import replicant.Channel;
import replicant.ChannelAddress;
import replicant.Entity;
import replicant.FilterUtil;
import replicant.Replicant;
import replicant.ReplicantContext;

@Singleton
public class ReplicantConnection
{
  private final ReplicantClientSystem _replicantClientSystem;

  @Inject
  ReplicantConnection( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
  }

  public void disconnect()
  {
    _replicantClientSystem.deactivate();
  }

  public void connect()
  {
    _replicantClientSystem.activate();
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
    final Entity entity = Replicant.context().findEntityByTypeAndId( type, id );
    assert null != entity;
    final T root = (T) entity.getUserObject();
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
    // Need to check both subscription and filters are identical.
    // If they are not the next step will either update the filters or add subscriptions
    final ReplicantContext context = Replicant.context();
    final Map<Object, Channel> existing =
      context
        .getAreasOfInterest()
        .stream()
        .filter( s -> s.getAddress().getChannelType().equals( targetChannelType ) )
        .filter( subscription -> FilterUtil.filtersEqual( subscription.getChannel().getFilter(), filter ) )
        .map( AreaOfInterest::getChannel )
        .collect( Collectors.toMap( s -> s.getAddress().getId(), Function.identity() ) );

    context
      .getAreasOfInterest()
      .stream()
      .filter( s -> s.getAddress().getChannelType() == sourceChannelType )
      .map( s -> s.getAddress().getId() )
      .flatMap( sourceIDToTargetIDs )
      .filter( Objects::nonNull )
      .filter( id -> null == existing.remove( id ) )
      .forEach( id -> context.findOrCreateAreaOfInterest( asAddress( targetChannelType, id ), filter ) );

    context.getInstanceSubscriptionIds( sourceChannelType ).stream().
      flatMap( sourceIDToTargetIDs ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> context.findOrCreateAreaOfInterest( asAddress( targetChannelType, id ), filter ) );

    existing.values().forEach( Disposable::dispose );
  }

  @Nonnull
  private ChannelAddress asAddress( @Nonnull final Enum channel, @Nullable final Object id )
  {
    return new ChannelAddress( channel, id );
  }
}
