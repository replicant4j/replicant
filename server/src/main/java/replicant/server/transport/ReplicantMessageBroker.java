package replicant.server.transport;

import java.util.Collection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.json.JsonValue;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;

public interface ReplicantMessageBroker
{
  @NonNull
  Packet queueChangeMessage( @NonNull ReplicantSession session,
                           boolean altersExplicitSubscriptions,
                           @Nullable Integer requestId,
                           @Nullable JsonValue response,
                           @Nullable String etag,
                           @NonNull Collection<EntityMessage> messages,
                           @NonNull ChangeSet changeSet );
}
