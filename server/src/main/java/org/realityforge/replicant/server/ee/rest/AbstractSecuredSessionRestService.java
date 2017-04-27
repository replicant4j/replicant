package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.realityforge.keycloak.sks.SimpleAuthService;
import org.realityforge.replicant.server.ChannelDescriptor;
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
    if ( disableSecurity() || null != account )
    {
      return super.doCreateSession();
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  @Nonnull
  @Override
  protected Response doDeleteSession( @Nonnull final String sessionID )
  {
    return guard( sessionID, () -> super.doDeleteSession( sessionID ) );
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
    return guard( sessionID, () -> super.doGetSession( sessionID, filter, uri ) );
  }

  @Nonnull
  @Override
  protected Response doUnsubscribeChannel( @Nonnull final String sessionID,
                                           @Nonnull final ChannelDescriptor descriptor )
  {
    return guard( sessionID, () -> super.doUnsubscribeChannel( sessionID, descriptor ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannel( @Nonnull final String sessionID,
                                   @Nonnull final ChannelDescriptor descriptor,
                                   @Nonnull final FieldFilter filter,
                                   @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetChannel( sessionID, descriptor, filter, uri ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannels( @Nonnull final String sessionID,
                                    @Nonnull final FieldFilter filter,
                                    @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetChannels( sessionID, filter, uri ) );
  }

  @Nonnull
  @Override
  protected Response doBulkSubscribeChannel( @Nonnull final String sessionID,
                                             final int channelID,
                                             @Nonnull final Collection<Serializable> subChannelIDs,
                                             @Nonnull final String filterContent )
  {
    return guard( sessionID, () -> super.doBulkSubscribeChannel( sessionID, channelID, subChannelIDs, filterContent ) );
  }

  @Nonnull
  @Override
  protected Response doBulkUnsubscribeChannel( @Nonnull final String sessionID,
                                               final int channelID,
                                               @Nonnull final Collection<Serializable> subChannelIDs )
  {
    return guard( sessionID, () -> super.doBulkUnsubscribeChannel( sessionID, channelID, subChannelIDs ) );
  }

  @Nonnull
  @Override
  protected Response doGetInstanceChannels( @Nonnull final String sessionID,
                                            final int channelID,
                                            @Nonnull final FieldFilter filter,
                                            @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetInstanceChannels( sessionID, channelID, filter, uri ) );
  }

  @Nonnull
  private Response guard( @Nonnull final String sessionID, @Nonnull final Supplier<Response> action )
  {
    if ( disableSecurity() || doesCurrentUserMatchSession( sessionID ) )
    {
      return action.get();
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
    return standardResponse( Response.Status.FORBIDDEN,
                             "No user authenticated or user does not have permission to perform action." );
  }
}
