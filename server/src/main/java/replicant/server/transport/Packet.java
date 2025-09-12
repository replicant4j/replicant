package replicant.server.transport;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;

/**
 * Packet contains the data generated from the transaction that needs to be sent to a specific client.
 * This packet has not been fully resolved and is just used to pass the data to another thread that will perform
 * "expandLinks" operation.
 *
 * @param altersExplicitSubscriptions The request that resulted in this change when packet is being sent back to the initiator.
 * @param requestId                   The request that resulted in this change when the packet is being sent back to the initiator.
 * @param response                    The response to the request that resulted in this change when the packet is being sent back to the initiator.
 * @param etag                        If the request was a subscription and this subscription is a single cacheable graph then this
 *                                    etag will contain the cache constant.
 * @param messages                    The change messages that were collected during the transaction.
 * @param changeSet                   Empty ChangeSet unless packet is directed at the request initiator in which case it was
 *                                    whatever was part of the session changes.
 */
record Packet(boolean altersExplicitSubscriptions, @Nullable Integer requestId, @Nullable String response,
              @Nullable String etag, @Nonnull Collection<EntityMessage> messages, @Nonnull ChangeSet changeSet)
{
}
