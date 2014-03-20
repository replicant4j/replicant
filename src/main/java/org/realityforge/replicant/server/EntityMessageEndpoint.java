package org.realityforge.replicant.server;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EntityMessageEndpoint
{
  /**
   * Queue the specified messages to be saved as a change set.
   *
   * @param sessionID       the session that initiated the request that resulted in the changes, or null.
   * @param requestID       the request that resulted in the changes, or null.
   * @param messages        the messages.
   * @param sessionMessages the messages that are targeted for the session that initiated request.
   * @return true if any messages were routed to the initiating session.
   */
  boolean saveEntityMessages( @Nullable String sessionID,
                              @Nullable String requestID,
                              @Nonnull Collection<EntityMessage> messages,
                              @Nullable Collection<Change> sessionMessages );
}
