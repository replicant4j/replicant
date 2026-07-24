package replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.Session;
import replicant.server.transport.ReplicantSessionAuthorization;

public interface ReplicantHandshakeAuthenticator
{
  @Nullable
  ReplicantSessionAuthorization authenticate( @Nonnull Session webSocketSession );
}
