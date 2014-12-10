package org.realityforge.replicant.server.ee.rest;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.realityforge.ssf.SessionManager;

/**
 * The token source is for generating the initial token.
 *
 * It is expected that this endpoint has already had security applied.
 */
@Path( ReplicantContext.TOKEN_URL_FRAGMENT )
@Produces( MediaType.TEXT_PLAIN )
public class TokenRestService
{
  @EJB
  private SessionManager _sessionManager;

  @GET
  public String generateToken()
  {
    return _sessionManager.createSession().getSessionID();
  }
}
