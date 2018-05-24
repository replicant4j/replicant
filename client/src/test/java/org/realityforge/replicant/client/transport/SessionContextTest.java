package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Connection;
import replicant.RequestEntry;
import replicant.TestConnector;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SessionContextTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final SessionContext sessionContext = new SessionContext();

    //authToken
    {
      final String authToken = ValueUtil.randomString();
      assertEquals( sessionContext.getAuthenticationToken(), null );
      sessionContext.setAuthenticationToken( authToken );
      assertEquals( sessionContext.getAuthenticationToken(), authToken );
      sessionContext.setAuthenticationToken( null );
      assertEquals( sessionContext.getAuthenticationToken(), null );
    }
  }

  @Test
  public void request_noSession()
  {
    final String requestKey = ValueUtil.randomString();
    final String cacheKey = ValueUtil.randomString();
    final RequestAction action = mock( RequestAction.class );

    final SessionContext sessionContext = new SessionContext();

    sessionContext.request( requestKey, cacheKey, action );

    verify( action ).invokeRequest( null, null );
  }

  @Test
  public void request_sessionPresent()
  {
    final String name = ValueUtil.randomString();
    final String cacheKey = ValueUtil.randomString();
    final TestRequestAction action = new TestRequestAction();
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );

    final SessionContext sessionContext = new SessionContext();
    sessionContext.setConnector( connector );

    sessionContext.request( name, cacheKey, action );

    assertEquals( action._session, connection );
    assertNotNull( action._request );
    assertEquals( action._request.getName(), name );
    assertEquals( action._request.getCacheKey(), cacheKey );
  }

  static class TestRequestAction
    implements RequestAction
  {
    private Connection _session;
    private RequestEntry _request;

    @Override
    public void invokeRequest( @Nullable final Connection connection, @Nullable final RequestEntry request )
    {
      _session = connection;
      _request = request;
    }
  }
}
