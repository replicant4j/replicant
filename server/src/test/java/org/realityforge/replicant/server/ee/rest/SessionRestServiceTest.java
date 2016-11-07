package org.realityforge.replicant.server.ee.rest;

import javax.ws.rs.core.Response;
import org.realityforge.replicant.server.TestSession;
import org.realityforge.ssf.SessionManager;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SessionRestServiceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final SessionManager sessionManager = mock( SessionManager.class );
    final AbstractSessionRestService resource = newResource( sessionManager );

    when( sessionManager.createSession() ).
      thenReturn( new TestSession( "2222" ) );

    final Response token = resource.createSession();

    assertEquals( token.getEntity(), "2222" );
  }

  protected AbstractSessionRestService newResource( final SessionManager sessionManager )
    throws Exception
  {
    return new AbstractSessionRestService()
    {
      @Override
      protected SessionManager getSessionManager()
      {
        return sessionManager;
      }
    };
  }
}
