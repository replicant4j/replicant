package org.realityforge.replicant.server.ee.rest;

import java.lang.reflect.Field;
import javax.ws.rs.core.Response;
import org.realityforge.replicant.server.TestSession;
import org.realityforge.ssf.SessionManager;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class TokenRestServiceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final SessionManager sessionManager = mock( SessionManager.class );
    final TokenRestService resource = newResource( sessionManager );

    when( sessionManager.createSession() ).
      thenReturn( new TestSession( "2222" ) );

    final Response token = resource.generateToken();

    assertEquals( token.getEntity(), "2222" );
  }

  protected TokenRestService newResource( final SessionManager sessionManager )
    throws Exception
  {
    final TokenRestService resource = new TokenRestService();
    setField( resource, "_sessionManager", sessionManager );
    return resource;
  }

  private void setField( final TokenRestService resource,
                         final String fieldName,
                         final Object value )
    throws Exception
  {
    final Field field = TokenRestService.class.getDeclaredField( fieldName );
    field.setAccessible( true );
    field.set( resource, value );
  }
}
