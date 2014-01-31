package org.realityforge.replicant.client.transport.gwt;


import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import javax.annotation.Nonnull;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRpcRequestBuilderTest
{
  @BeforeMethod
  public void setup()
  {
    SessionContext.setSession( null );
    SessionContext.setRequest( null );
  }

  @Test
  public void noContext()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    ReplicantRpcRequestBuilder.INSTANCE.doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), anyString() );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void sessionIDSet()
  {
    SessionContext.setSession( new TestClientSession( "1" ) );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    ReplicantRpcRequestBuilder.INSTANCE.doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( "1" ) );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void requestIDSet_withSuccessAndComplete()
  {
    final TestClientSession session = new TestClientSession( "1" );
    final RequestEntry requestEntry = session.getRequestManager().newRequestRegistration( true );
    SessionContext.setSession( session );
    SessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( ReplicantContext.REQUEST_COMPLETE_HEADER ) ).thenReturn( "1" );
    doAnswer( new Answer()
    {
      @Override
      public Object answer( final InvocationOnMock invocation )
        throws Throwable
      {
        final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
        innerCallback.onResponseReceived( request, response );
        return null;
      }
    } ).when( rb ).setCallback( any( RequestCallback.class ) );
    ReplicantRpcRequestBuilder.INSTANCE.doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.isCompleted(), true );
  }

  @Test
  public void requestIDSet_withSuccessAndIncomplete()
  {
    final TestClientSession session = new TestClientSession( "1" );
    final RequestEntry requestEntry = session.getRequestManager().newRequestRegistration( true );
    SessionContext.setSession( session );
    SessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( ReplicantContext.REQUEST_COMPLETE_HEADER ) ).thenReturn( "0" );
    doAnswer( new Answer()
    {
      @Override
      public Object answer( final InvocationOnMock invocation )
        throws Throwable
      {
        final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
        innerCallback.onResponseReceived( request, response );
        return null;
      }
    } ).when( rb ).setCallback( any( RequestCallback.class ) );
    ReplicantRpcRequestBuilder.INSTANCE.doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.hasReturned(), true );
    assertEquals( requestEntry.isCompleted(), false );
  }

  @Test
  public void requestIDSet_withFailure()
  {
    final TestClientSession session = new TestClientSession( "1" );
    final RequestEntry requestEntry = session.getRequestManager().newRequestRegistration( true );
    SessionContext.setSession( session );
    SessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Throwable exception = new Throwable();
    doAnswer( new Answer()
    {
      @Override
      public Object answer( final InvocationOnMock invocation )
        throws Throwable
      {
        final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
        innerCallback.onError( request, exception );
        return null;
      }
    } ).when( rb ).setCallback( any( RequestCallback.class ) );
    ReplicantRpcRequestBuilder.INSTANCE.doSetCallback( rb, callback );
    verify( callback ).onError( request, exception );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.isCompleted(), true );
  }

  static class TestClientSession
    extends ClientSession
  {
    TestClientSession( @Nonnull final String sessionID )
    {
      super( sessionID );
    }
  }
}
