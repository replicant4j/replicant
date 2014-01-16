package org.realityforge.replicant.server;

import java.util.Collection;
import javax.annotation.Nonnull;

public interface EntityMessageEndpoint
{
  /**
   * Queue the specified messages to be saved as a change set.
   *
   * @param messages the messages.
   */
  void saveEntityMessages( @Nonnull Collection<EntityMessage> messages );
}
