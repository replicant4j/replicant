package org.realityforge.replicant.server.transport;

import java.util.UUID;
import javax.annotation.Nonnull;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.realityforge.keycloak.sks.SimpleAuthService;

/**
 * Base class for keycloak secured session managers.
 */
public abstract class ReplicantSecuredSessionManagerImpl
  extends ReplicantSessionManagerImpl
{
  @Nonnull
  protected abstract SimpleAuthService getAuthService();

  @Nonnull
  @Override
  protected ReplicantSession newSessionInfo()
  {
    final OidcKeycloakAccount account = getAuthService().getAccount();
    final String userID = account.getKeycloakSecurityContext().getToken().getId();
    final String sessionID = UUID.randomUUID().toString();
    return new ReplicantSession( userID, sessionID );
  }
}
