package replicant.server.ee;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import javax.websocket.Session;
import replicant.server.transport.ReplicantSessionAuthorization;

public interface ReplicantHandshakeAuthenticator
{
  @Nullable
  ReplicantSessionAuthorization authenticate( @NonNull Session webSocketSession );
}
