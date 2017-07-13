package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Response;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ActionCallbackAdapterTest
{
  @Test
  public void basicOperationWithNoCallbacksAndNoSession()
  {
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( null, null, null, null );

    // Largely we expect no exceptions
    adapter.onSuccess( mock( Response.class ) );
    adapter.onFailure( new Throwable() );
  }

  @Test
  public void basicOperationWithNoSession()
  {
    final Object[] results = new Object[ 1 ];
    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, null, null );

    final Response response = mock( Response.class );
    adapter.onSuccess( response );
    assertEquals( results[ 0 ], response );
    results[ 0 ] = null;

    final Throwable throwable = new Throwable();
    adapter.onFailure( throwable );
    assertEquals( results[ 0 ], throwable );
  }

  @Test
  public void basicOperationWithSession_onSuccess()
  {
    final Object[] results = new Object[ 1 ];
    final ClientSession session = new ClientSession( mock( DataLoaderService.class ), ValueUtil.randomString() );
    final RequestEntry request = session.newRequest( ValueUtil.randomString(), null );
    request.setExpectingResults( true );

    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, request, session );

    final Response response = mock( Response.class );
    assertNull( request.getCompletionAction() );
    assertFalse( request.isCompletionDataPresent() );

    adapter.onSuccess( response );
    assertEquals( results[ 0 ], null );
    assertTrue( request.isCompletionDataPresent() );
    assertNotNull( request.getCompletionAction() );
    assertTrue( request.isNormalCompletion() );
  }

  @Test
  public void basicOperationWithSession_onFailure()
  {
    final Object[] results = new Object[ 1 ];
    final ClientSession session = new ClientSession( mock( DataLoaderService.class ), ValueUtil.randomString() );
    final RequestEntry request = session.newRequest( ValueUtil.randomString(), null );
    request.setExpectingResults( true );

    final ActionCallbackAdapter adapter =
      new ActionCallbackAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, request, session );

    assertNull( request.getCompletionAction() );
    assertFalse( request.isCompletionDataPresent() );

    adapter.onFailure( new Throwable(  ) );
    assertEquals( results[ 0 ], null );
    assertTrue( request.isCompletionDataPresent() );
    assertNotNull( request.getCompletionAction() );
    assertFalse( request.isNormalCompletion() );
  }
}
