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
   */
  void saveEntityMessages( @Nullable String sessionID,
                           @Nullable String jobID,
                           @Nonnull Collection<EntityMessage> messages );
}
