package org.realityforge.replicant.client;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RequestAdapterTest
{
  static class TestRequestAdapter
    extends AbstractRequestAdapter
  {
    private final Consumer<Object> _callback;

    TestRequestAdapter( @Nullable final Consumer<Object> callback,
                        @Nullable final Consumer<Throwable> errorCallback,
                        @Nullable final RequestEntry request,
                        @Nullable final ClientSession session )
    {
      super( errorCallback, request, session );
      _callback = callback;
    }

    void onSuccess( @Nonnull final Object result )
    {
      final Runnable action = () ->
      {
        if ( null != _callback )
        {
          _callback.accept( result );
        }
      };
      completeNormalRequest( action );
    }
  }

  @Test
  public void basicOperationWithNoCallbacksAndNoSession()
  {
    final TestRequestAdapter adapter =
      new TestRequestAdapter( null, null, null, null );

    // Largely we expect no exceptions
    adapter.onSuccess( new Object() );
    adapter.onFailure( new Throwable() );
  }

  @Test
  public void basicOperationWithNoSession()
  {
    final Object[] results = new Object[ 1 ];
    final TestRequestAdapter adapter =
      new TestRequestAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, null, null );

    final Object response = new Object();
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

    final TestRequestAdapter adapter =
      new TestRequestAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, request, session );

    final Object response = new Object();
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

    final TestRequestAdapter adapter =
      new TestRequestAdapter( r -> results[ 0 ] = r, t -> results[ 0 ] = t, request, session );

    assertNull( request.getCompletionAction() );
    assertFalse( request.isCompletionDataPresent() );

    adapter.onFailure( new Throwable() );
    assertEquals( results[ 0 ], null );
    assertTrue( request.isCompletionDataPresent() );
    assertNotNull( request.getCompletionAction() );
    assertFalse( request.isNormalCompletion() );
  }
}
