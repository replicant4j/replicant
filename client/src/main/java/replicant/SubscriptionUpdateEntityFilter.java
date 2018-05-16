package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface that used during subscription updates to remove Entities from subscriptions
 * as a result of a filter change.
 */
public interface SubscriptionUpdateEntityFilter
{
  /**
   * Return true if specified entity is matched by the channel designated by address and filter.
   * This interfaces is invoked when the server updates a subscription and a client is responsible
   * for removing local Entities from that subscription that no longer match the filter.
   *
   * @param address the channel address.
   * @param filter  the filter.
   * @param entity  the entity to match.
   */
  boolean doesEntityMatchFilter( @Nonnull ChannelAddress address, @Nullable Object filter, @Nonnull Entity entity );
}
