package replicant.server;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonValue;

public interface EntityMessageEndpoint
{
  /**
   * Queue the specified messages to be saved as a change set.
   *
   * @param sessionId the session that initiated the request that resulted in the changes, or null.
   * @param requestId the request that resulted in the changes, or null if no such request.
   * @param response the response to the request that resulted in the changes, or null if no request.
   * @param messages  the messages.
   * @param sessionChanges the changes that are targeted for the session that initiated request.
   * @return true if any messages were routed to the initiating session.
   */
  boolean saveEntityMessages( @Nullable String sessionId,
                              @Nullable Integer requestId,
                              @Nullable JsonValue response,
                              @Nonnull Collection<EntityMessage> messages,
                              @Nullable ChangeSet sessionChanges );
}
