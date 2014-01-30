package org.realityforge.replicant.server;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface EntityMessageEndpoint
{
  /**
   * Queue the specified messages to be saved as a change set.
   *
   * @param messages the messages.
   * @return true if any messages were routed to the initiating session.
   */
  boolean saveEntityMessages( @Nullable String sessionID,
                              @Nullable String requestID,
                              @Nonnull Collection<EntityMessage> messages );
}
