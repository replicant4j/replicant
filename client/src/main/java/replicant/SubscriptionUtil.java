package replicant;

import arez.Disposable;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utilities for integration across different datasources.
 */
public class SubscriptionUtil
{
  private SubscriptionUtil()
  {
  }

  /**
   * Convert object subscription to root object (specified object Type+ID) to target channels subscription
   * based on function. If the root object is not yet present then return an empty stream.
   * This is typically used by the function passed into the convergeCrossDataSourceSubscriptions() method.
   */
  @SuppressWarnings( "unchecked" )
  @Nonnull
  public static <T, O> Stream<O> instanceSubscriptionToValues( @Nonnull final Class<T> type,
                                                               @Nonnull final Integer id,
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
  public static void convergeCrossDataSourceSubscriptions( @Nonnull final Enum sourceChannelType,
                                                           @Nonnull final Enum targetChannelType,
                                                           @Nullable final Object filter,
                                                           @Nonnull final Function<Object, Stream<Integer>> sourceIDToTargetIDs )
  {
    // Need to check both subscription and filters are identical.
    // If they are not the next step will either update the filters or add subscriptions
    final ReplicantContext context = Replicant.context();
    final Map<Integer, Channel> existing =
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
      .forEach( id -> context.findOrCreateAreaOfInterest( new ChannelAddress( targetChannelType, id ), filter ) );

    context.getInstanceSubscriptionIds( sourceChannelType ).stream().
      flatMap( sourceIDToTargetIDs ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> context.findOrCreateAreaOfInterest( new ChannelAddress( targetChannelType, id ), filter ) );

    existing.values().forEach( Disposable::dispose );
  }
}
