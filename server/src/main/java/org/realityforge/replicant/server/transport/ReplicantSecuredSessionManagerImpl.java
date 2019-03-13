package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  @Nullable
  protected String getUserID()
  {
    final OidcKeycloakAccount account = getAuthService().findAccount();
    return null == account ? null : account.getKeycloakSecurityContext().getToken().getPreferredUsername();
  }
}
