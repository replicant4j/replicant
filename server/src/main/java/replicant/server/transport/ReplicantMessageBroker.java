package replicant.server.transport;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonValue;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;

public interface ReplicantMessageBroker
{
  void processPendingSessions();

  @Nonnull
  Packet queueChangeMessage( @Nonnull ReplicantSession session,
                           boolean altersExplicitSubscriptions,
                           @Nullable Integer requestId,
                           @Nullable JsonValue response,
                           @Nullable String etag,
                           @Nonnull Collection<EntityMessage> messages,
                           @Nonnull ChangeSet changeSet );
}
