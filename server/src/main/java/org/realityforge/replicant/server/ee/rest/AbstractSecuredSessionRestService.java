package org.realityforge.replicant.server.ee.rest;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.realityforge.keycloak.sks.SimpleAuthService;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.transport.ReplicantSession;

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
  protected Response doGetSession( @Nonnull final String sessionID,
                                   @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetSession( sessionID, uri ) );
  }

  @Nonnull
  @Override
  protected Response doSubscribeChannel( @Nonnull final String sessionID,
                                         @Nullable final String requestID,
                                         @Nullable final String eTag,
                                         @Nonnull final ChannelAddress descriptor,
                                         @Nonnull final String filterContent )
  {
    return guard( sessionID, () -> super.doSubscribeChannel( sessionID, requestID, eTag, descriptor, filterContent ) );
  }

  @Nonnull
  @Override
  protected Response doUnsubscribeChannel( @Nonnull final String sessionID,
                                           @Nullable final String requestID,
                                           @Nonnull final ChannelAddress descriptor )
  {
    return guard( sessionID, () -> super.doUnsubscribeChannel( sessionID, requestID, descriptor ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannel( @Nonnull final String sessionID,
                                   @Nonnull final ChannelAddress descriptor,
                                   @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetChannel( sessionID, descriptor, uri ) );
  }

  @Nonnull
  @Override
  protected Response doGetChannels( @Nonnull final String sessionID,
                                    @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetChannels( sessionID, uri ) );
  }

  @Nonnull
  @Override
  protected Response doBulkSubscribeChannel( @Nonnull final String sessionID,
                                             @Nullable final String requestID,
                                             final int channelId,
                                             @Nonnull final Collection<Integer> subChannelIds,
                                             @Nonnull final String filterContent )
  {
    return guard( sessionID,
                  () -> super.doBulkSubscribeChannel( sessionID, requestID, channelId, subChannelIds, filterContent ) );
  }

  @Nonnull
  @Override
  protected Response doBulkUnsubscribeChannel( @Nonnull final String sessionID,
                                               @Nullable final String requestID,
                                               final int channelId,
                                               @Nonnull final Collection<Integer> subChannelIds )
  {
    return guard( sessionID, () -> super.doBulkUnsubscribeChannel( sessionID, requestID, channelId, subChannelIds ) );
  }

  @Nonnull
  @Override
  protected Response doGetInstanceChannels( @Nonnull final String sessionID,
                                            final int channelId,
                                            @Nonnull final UriInfo uri )
  {
    return guard( sessionID, () -> super.doGetInstanceChannels( sessionID, channelId, uri ) );
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
    final String userID = account.getKeycloakSecurityContext().getToken().getPreferredUsername();
    final ReplicantSession session = getSessionManager().getSession( sessionID );
    return null != session && Objects.equals( session.getUserID(), userID );
  }

  private Response createForbiddenResponse()
  {
    return standardResponse( Response.Status.FORBIDDEN,
                             "No user authenticated or user does not have permission to perform action." );
  }
}
