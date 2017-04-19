package org.realityforge.replicant.server.ee.rest;

import java.io.StringWriter;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.realityforge.keycloak.sks.SimpleAuthService;
import org.realityforge.replicant.server.transport.ReplicantSession;
import org.realityforge.rest.field_filter.FieldFilter;

public abstract class AbstractSecuredSessionRestService
  extends AbstractSessionRestService
{
  @Nonnull
  protected abstract SimpleAuthService getAuthService();

  /**
   * Override this and return true if security should be disabled. Typically used during local development.
   */
  protected boolean disableSecurity()
  {
    return false;
  }

  @Nonnull
  @Override
  protected Response doCreateSession()
  {
    final OidcKeycloakAccount account = getAuthService().findAccount();
    if ( !disableSecurity() && null == account )
    {
      return createForbiddenResponse();
    }
    else
    {
      return super.doCreateSession();
    }
  }

  @Nonnull
  @Override
  protected Response doDeleteSession( @Nonnull final String sessionID )
  {
    if ( disableSecurity() || doesCurrentUserMatchSession( sessionID ) )
    {
      return super.doDeleteSession( sessionID );
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  @Nonnull
  @Override
  protected Response doListSessions( @Nonnull final FieldFilter filter, @Nonnull final UriInfo uri )
  {
    if ( disableSecurity() )
    {
      return super.doListSessions( filter, uri );
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  @Nonnull
  @Override
  protected Response doGetSession( @Nonnull final String sessionID,
                                   @Nonnull final FieldFilter filter,
                                   @Nonnull final UriInfo uri )
  {
    if ( disableSecurity() || doesCurrentUserMatchSession( sessionID ) )
    {
      return super.doGetSession( sessionID, filter, uri );
    }
    else
    {
      return createForbiddenResponse();
    }
  }


  private boolean doesCurrentUserMatchSession( @Nonnull final String sessionID )
  {
    final OidcKeycloakAccount account = getAuthService().findAccount();
    return null != account && doesUserMatchSession( sessionID, account );
  }

  private boolean doesUserMatchSession( @Nonnull final String sessionID,
                                        @Nonnull final OidcKeycloakAccount account )
  {
    final String userID = account.getKeycloakSecurityContext().getToken().getId();
    final ReplicantSession session = getSessionManager().getSession( sessionID );
    return null != session && Objects.equals( session.getUserID(), userID );
  }

  private Response createForbiddenResponse()
  {
    final StringWriter writer = new StringWriter();
    final JsonGenerator g = factory().createGenerator( writer );
    g.writeStartObject();
    g.write( "code", Response.Status.FORBIDDEN.getStatusCode() );
    g.write( "description", "No user authenticated or user does not have permission to perform action." );
    g.writeEnd();
    g.close();

    final Response.ResponseBuilder builder = Response.status( Response.Status.FORBIDDEN );
    CacheUtil.configureNoCacheHeaders( builder );
    return builder.entity( writer.toString() ).build();
  }
}
