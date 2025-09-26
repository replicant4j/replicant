package replicant.server.ee.rest;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.transaction.Transactional;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import replicant.server.ChannelAddress;
import replicant.shared.SharedConstants;

@Path( SharedConstants.CONNECTION_URL_FRAGMENT )
@Produces( MediaType.APPLICATION_JSON )
@ApplicationScoped
@Transactional
@SuppressWarnings( "unused" )
public class SecuredReplicantSessionRestService
  extends ReplicantSessionRestService
{
  private boolean _disableSessionServiceProtection;

  @SuppressWarnings( "BanJNDI" )
  @PostConstruct
  public void postConstruct()
  {
    try
    {
      _disableSessionServiceProtection = new InitialContext().lookup( "replicant/env/disable_session_service_protection" ).equals( Boolean.TRUE );
    }
    catch ( final Exception ignored )
    {
      //Ignored.
    }
  }

  /**
   * Override this and return true if security should be disabled. Typically used during local development.
   */
  @SuppressWarnings( "WeakerAccess" )
  protected boolean disableSecurity()
  {
    return _disableSessionServiceProtection;
  }

  @Nonnull
  @Override
  protected Response doListSessions( @Nonnull final UriInfo uri )
  {
    return disableSecurity() ? super.doListSessions( uri ) : createForbiddenResponse();
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
    return disableSecurity() ? action.get() : createForbiddenResponse();
  }

  @Nonnull
  private Response createForbiddenResponse()
  {
    return standardResponse( Response.Status.FORBIDDEN,
                             "No user authenticated or user does not have permission to perform action." );
  }
}
