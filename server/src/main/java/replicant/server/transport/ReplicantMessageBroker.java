package replicant.server.transport;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;

public interface ReplicantMessageBroker
{
  void processPendingSessions();

  void queueChangeMessage( @Nonnull ReplicantSession session,
                           boolean altersExplicitSubscriptions,
                           @Nullable Integer requestId,
                           @Nullable String response,
                           @Nullable String etag,
                           @Nonnull Collection<EntityMessage> messages,
                           @Nonnull ChangeSet changeSet );
}
