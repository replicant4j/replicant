package org.realityforge.replicant.server.ee.rest;

import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.realityforge.keycloak.sks.SimpleAuthService;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.transport.ReplicantSession;

@SuppressWarnings( "unused" )
public abstract class AbstractSecuredSessionRestService
  extends AbstractSessionRestService
{
  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  protected abstract SimpleAuthService getAuthService();

  /**
   * Override this and return true if security should be disabled. Typically used during local development.
   */
  @SuppressWarnings( "WeakerAccess" )
  protected boolean disableSecurity()
  {
    return false;
  }

  @Nonnull
  @Override
  protected Response doListSessions( @Nonnull final UriInfo uri )
  {
    if ( disableSecurity() )
    {
      return super.doListSessions( uri );
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  @Nonnull
  @Override
  protected Response doGetSession( @Nonnull final String sessionId,
                                   @Nonnull final UriInfo uri )
  {
    return guard( sessionId, () -> super.doGetSession( sessionId, uri ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannel( @Nonnull final String sessionId,
                                   @Nonnull final ChannelAddress address,
                                   @Nonnull final UriInfo uri )
  {
    return guard( sessionId, () -> super.doGetChannel( sessionId, address, uri ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannels( @Nonnull final String sessionId,
                                    @Nonnull final UriInfo uri )
  {
    return guard( sessionId, () -> super.doGetChannels( sessionId, uri ) );
  }

  @Nonnull
  @Override
  protected Response doGetInstanceChannels( @Nonnull final String sessionId,
                                            final int channelId,
                                            @Nonnull final UriInfo uri )
  {
    return guard( sessionId, () -> super.doGetInstanceChannels( sessionId, channelId, uri ) );
  }

  @Nonnull
  private Response guard( @Nonnull final String sessionId, @Nonnull final Supplier<Response> action )
  {
    if ( disableSecurity() || doesCurrentUserMatchSession( sessionId ) )
    {
      return action.get();
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  private boolean doesCurrentUserMatchSession( @Nonnull final String sessionId )
  {
    final OidcKeycloakAccount account = getAuthService().findAccount();
    return null != account && doesUserMatchSession( sessionId, account );
  }

  private boolean doesUserMatchSession( @Nonnull final String sessionId,
                                        @Nonnull final OidcKeycloakAccount account )
  {
    final String userID = account.getKeycloakSecurityContext().getToken().getPreferredUsername();
    final ReplicantSession session = getSessionManager().getSession( sessionId );
    return null != session && Objects.equals( session.getUserId(), userID );
  }

  private Response createForbiddenResponse()
  {
    return standardResponse( Response.Status.FORBIDDEN,
                             "No user authenticated or user does not have permission to perform action." );
  }
}
