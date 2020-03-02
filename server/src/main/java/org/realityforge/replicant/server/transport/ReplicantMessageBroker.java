package org.realityforge.replicant.server.transport;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;

public interface ReplicantMessageBroker
{
  void processPendingSessions();

  void queueChangeMessage( @Nonnull ReplicantSession session,
                           @Nullable Integer requestId,
                           @Nullable String etag,
                           @Nonnull Collection<EntityMessage> messages,
                           @Nonnull ChangeSet changeSet );
}
