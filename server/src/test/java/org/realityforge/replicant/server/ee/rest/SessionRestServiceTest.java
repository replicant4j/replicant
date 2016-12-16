package org.realityforge.replicant.server.ee.rest;

import javax.ws.rs.core.Response;
import org.realityforge.replicant.server.TestSession;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SessionRestServiceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final ReplicantSessionManager sessionManager = mock( ReplicantSessionManager.class );
    final AbstractSessionRestService resource = newResource( sessionManager );

    when( sessionManager.createSession() ).
      thenReturn( new TestSession( "2222" ) );

    final Response token = resource.createSession();

    assertEquals( token.getEntity(), "2222" );
  }

  protected AbstractSessionRestService newResource( final ReplicantSessionManager sessionManager )
    throws Exception
  {
    return new AbstractSessionRestService()
    {
      @Override
      protected ReplicantSessionManager getSessionManager()
      {
        return sessionManager;
      }
    };
  }
}
