package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.lang.reflect.Field;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.transport.SessionContext;
import org.realityforge.replicant.shared.SharedConstants;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Connection;
import replicant.RequestEntry;
import replicant.TestConnector;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRpcRequestBuilderTest
  extends AbstractReplicantTest
{
  @Test
  public void noContext()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final SessionContext sessionContext = new SessionContext( "X" );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb, never() ).setHeader( refEq( SharedConstants.CONNECTION_ID_HEADER ), anyString() );
    verify( rb, never() ).setHeader( refEq( SharedConstants.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void connectionIdSet()
  {
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setConnection( new Connection( TestConnector.create( G.class ), "1" ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( rb ).setCallback( callback );
    verify( rb ).setHeader( refEq( SharedConstants.CONNECTION_ID_HEADER ), refEq( "1" ) );
    verify( rb, never() ).setHeader( refEq( SharedConstants.REQUEST_ID_HEADER ), anyString() );
  }

  @Test
  public void requestIDSet_withSuccessAndComplete()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = connection.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setConnection( connection );
    setRequest( sessionContext, requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) ).thenReturn( "1" );
    doAnswer( invocation ->
              {
                final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
                innerCallback.onResponseReceived( request, response );
                return null;
              } ).when( rb ).setCallback( any( RequestCallback.class ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( SharedConstants.CONNECTION_ID_HEADER ), refEq( connection.getConnectionId() ) );
    verify( rb ).setHeader( refEq( SharedConstants.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestId() ) );

    assertEquals( requestEntry.isExpectingResults(), false );
  }

  @Test
  public void requestIDSet_withSuccessAndIncomplete()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = connection.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setConnection( connection );
    setRequest( sessionContext, requestEntry );
    final RequestBuilder rb = mock( RequestBuilder.class );
    final RequestCallback callback = mock( RequestCallback.class );
    final Request request = mock( Request.class );
    final Response response = mock( Response.class );
    when( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) ).thenReturn( "0" );
    doAnswer( invocation ->
              {
                final RequestCallback innerCallback = (RequestCallback) invocation.getArguments()[ 0 ];
                innerCallback.onResponseReceived( request, response );
                return null;
              } ).when( rb ).setCallback( any( RequestCallback.class ) );
    new ReplicantRpcRequestBuilder( sessionContext ).doSetCallback( rb, callback );
    verify( callback ).onResponseReceived( request, response );
    verify( rb ).setHeader( refEq( SharedConstants.CONNECTION_ID_HEADER ), refEq( connection.getConnectionId() ) );
    verify( rb ).setHeader( refEq( SharedConstants.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestId() ) );

    assertEquals( requestEntry.isCompletionDataPresent(), false );
    assertEquals( requestEntry.isExpectingResults(), true );
  }

  @Test
  public void requestIDSet_withFailure()
    throws Exception
  {
    final Connection connection = new Connection( TestConnector.create( G.class ), ValueUtil.randomString() );
    final RequestEntry requestEntry = connection.newRequest( "", null );
    final SessionContext sessionContext = new SessionContext( "X" );
    sessionContext.setConnection( connection );
    setRequest( sessionContext, requestEntry );
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
    verify( rb ).setHeader( refEq( SharedConstants.CONNECTION_ID_HEADER ), refEq( connection.getConnectionId() ) );
    verify( rb ).setHeader( refEq( SharedConstants.REQUEST_ID_HEADER ), refEq( requestEntry.getRequestId() ) );

    assertEquals( requestEntry.isExpectingResults(), false );
  }

  private void setRequest( final SessionContext sessionContext, final RequestEntry requestEntry )
    throws Exception
  {
    final Field field = SessionContext.class.getDeclaredField( "_request" );
    field.setAccessible( true );
    field.set( sessionContext, requestEntry );
  }

  enum G
  {
    G1
  }
}
