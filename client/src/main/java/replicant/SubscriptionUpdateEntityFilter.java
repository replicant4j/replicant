package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface that used during subscription updates to remove Entities from subscriptions
 * as a result of a filter change. Each channel with a {@link replicant.ChannelSchema.FilterType#DYNAMIC}
 * filter type must be associated with a filter of this type.
 */
public interface SubscriptionUpdateEntityFilter<T>
{
  /**
   * Return true if specified entity is matched by the channel designated filter.
   * This interfaces is invoked when the server updates a subscription and a client is responsible
   * for removing local Entities from that subscription that no longer match the filter.
   *
   * @param filter the filter.
   * @param entity the entity to match.
   */
  boolean doesEntityMatchFilter( @Nullable T filter, @Nonnull Entity entity );
}
