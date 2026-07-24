package replicant.server.ee;

import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.transport.ReplicantSessionAuthorization;

public interface ReplicantHandshakeAuthenticator {
    @Nullable
    ReplicantSessionAuthorization authenticate(@NonNull Session webSocketSession);
}
