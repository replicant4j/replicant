package replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;

/**
 * A record of a change in channel subscriptions.
 */
public record ChannelAction(@Nonnull ChannelAddress address, @Nonnull Action action, @Nullable JsonObject filter)
{
  public enum Action
  {
    // The channel has been successfully added.
    ADD,
    // The channel has been removed. This could be as a result of client request or as a result of the
    // filter excluding the graph, as the root instance being deleted
    REMOVE,
    // The filter associated with the channel has been updated
    UPDATE,
    // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
    DELETE
  }
}
