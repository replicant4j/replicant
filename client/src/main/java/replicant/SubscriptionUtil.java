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
   * Converge subscriptions across data sources.
   * All instances of the subscriptions to the source channelType within the scope are collected.
   * The supplied function is used to generate a stream of expected subscriptions to the target channelType
   * that are reachable from the source channelTypes. If an expected subscription is missing it is added,
   * if an additional subscription is present then it is released.
   */
  public static void convergeCrossDataSourceSubscriptions( final int sourceSystemId,
                                                           final int sourceChannelId,
                                                           final int targetSystemId,
                                                           final int targetChannelId,
                                                           @Nullable final Object filter,
                                                           @Nonnull final Function<Integer, Stream<Integer>> sourceIdToTargetIds )
  {
    // Need to check both subscription and filters are identical.
    // If they are not the next step will either update the filters or add subscriptions
    final ReplicantContext context = Replicant.context();
    final Map<Integer, AreaOfInterest> existing =
      context
        .getAreasOfInterest()
        .stream()
        .filter( s -> s.getAddress().schemaId() == targetSystemId &&
                      s.getAddress().channelId() == targetChannelId )
        .filter( subscription -> FilterUtil.filtersEqual( subscription.getFilter(), filter ) )
        .collect( Collectors.toMap( s -> s.getAddress().rootId(), Function.identity() ) );

    context
      .getAreasOfInterest()
      .stream()
      .filter( s -> s.getAddress().schemaId() == sourceSystemId &&
                    s.getAddress().channelId() == sourceChannelId )
      .map( s -> s.getAddress().rootId() )
      .flatMap( sourceIdToTargetIds )
      .filter( Objects::nonNull )
      .filter( id -> null == existing.remove( id ) )
      .forEach( id -> context.createOrUpdateAreaOfInterest( new ChannelAddress( targetSystemId, targetChannelId, id ),
                                                            filter ) );

    context.getInstanceSubscriptionIds( sourceSystemId, sourceChannelId ).stream().
      flatMap( sourceIdToTargetIds ).
      filter( Objects::nonNull ).
      filter( id -> null == existing.remove( id ) ).
      forEach( id -> context.createOrUpdateAreaOfInterest( new ChannelAddress( targetSystemId, targetChannelId, id ),
                                                           filter ) );

    existing.values().forEach( Disposable::dispose );
  }
}
