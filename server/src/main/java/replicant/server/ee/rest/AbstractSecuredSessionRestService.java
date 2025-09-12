package replicant.server.ee.rest;

import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import replicant.server.ChannelAddress;

@SuppressWarnings( "unused" )
public abstract class AbstractSecuredSessionRestService
  extends AbstractSessionRestService
{
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
    if ( disableSecurity() )
    {
      return action.get();
    }
    else
    {
      return createForbiddenResponse();
    }
  }

  private Response createForbiddenResponse()
  {
    return standardResponse( Response.Status.FORBIDDEN,
                             "No user authenticated or user does not have permission to perform action." );
  }
}
