package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRpcRequestBuilderTest
{
  @Test
  public void noContext()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final SessionContext sessionContext = new SessionContext( "X" );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), anyString() );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void sessionIDSet()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setSession( new ClientSession( mock( DataLoaderService.class ), "1" ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( "1" ) );
    verify( rb, never() ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void requestIDSet_withSuccessAndComplete()
  {
    final ClientSession session = new ClientSession( mock( DataLoaderService.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = session.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setSession( session );
    sessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( ReplicantContext.REQUEST_COMPLETE_HEADER ) ).thenReturn( "1" );
    doAnswer( invocation ->
              {
                final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
                innerCallback.onResponseReceived( request, response );
                return null;
              } ).when( rb ).setCallback( any( RequestCallback.class ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.isExpectingResults(), false );
  }

  @Test
  public void requestIDSet_withSuccessAndIncomplete()
  {
    final ClientSession session = new ClientSession( mock( DataLoaderService.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = session.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setSession( session );
    sessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( ReplicantContext.REQUEST_COMPLETE_HEADER ) ).thenReturn( "0" );
    doAnswer( invocation ->
              {
                final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
                innerCallback.onResponseReceived( request, response );
                return null;
              } ).when( rb ).setCallback( any( RequestCallback.class ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.isCompletionDataPresent(), false );
    assertEquals( requestEntry.isExpectingResults(), true );
  }

  @Test
  public void requestIDSet_withFailure()
  {
    final ClientSession session = new ClientSession( mock( DataLoaderService.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = session.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setSession( session );
    sessionContext.setRequest( requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Throwable exception = new Throwable();
    doAnswer( invocation ->
              {
                final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
                innerCallback.onError( request, exception );
                return null;
              } ).when( rb ).setCallback( any( RequestCallback.class ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( callback ).onError( request, exception );
    verify( rb ).setHeader( refEq( ReplicantContext.SESSION_ID_HEADER ), refEq( session.getSessionID() ) );
    verify( rb ).setHeader( refEq( ReplicantContext.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestID() ) );

    assertEquals( requestEntry.isExpectingResults(), false );
  }
}
