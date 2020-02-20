package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.shared.SharedConstants;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.Replicant;
import replicant.SystemSchema;
import replicant.TestSpyEventHandler;
import replicant.spy.RequestCompletedEvent;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantRequestCallbackTest
  extends AbstractReplicantTest
{
  @Test
  public void onResponseReceived_OK_Complete()
  {
    final SystemSchema schema = newSchema();
    newConnection( createConnector( schema ) );
    Replicant.context().request( schema.getId(), ValueUtil.randomString(), () -> {

      final replicant.Request r = Replicant.context().currentRequest( schema.getId() );

      final RequestCallback chainedCallback = mock( RequestCallback.class );

      final Request request = mock( Request.class );
      final Response response = mock( Response.class );

      when( response.getStatusCode() ).thenReturn( 200 );
      when( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) ).thenReturn( "1" );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      new ReplicantRequestCallback( r, chainedCallback )
        .onResponseReceived( request, response );

      handler.assertEventCount( 1 );
      handler.assertNextEvent( RequestCompletedEvent.class, e -> {
        assertEquals( e.getRequestId(), r.getRequestId() );
        assertTrue( e.isNormalCompletion() );
        assertFalse( e.isExpectingResults() );
      } );

      verify( chainedCallback ).onResponseReceived( request, response );
    } );
  }

  @Test
  public void onResponseReceived_OK_InComplete()
  {
    final SystemSchema schema = newSchema();
    newConnection( createConnector( schema ) );
    Replicant.context().request( schema.getId(), ValueUtil.randomString(), () -> {

      final replicant.Request r = Replicant.context().currentRequest( schema.getId() );

      final RequestCallback chainedCallback = mock( RequestCallback.class );

      final Request request = mock( Request.class );
      final Response response = mock( Response.class );

      when( response.getStatusCode() ).thenReturn( 200 );
      when( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) ).thenReturn( "0" );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      new ReplicantRequestCallback( r, chainedCallback )
        .onResponseReceived( request, response );

      handler.assertEventCount( 1 );
      handler.assertNextEvent( RequestCompletedEvent.class, e -> {
        assertEquals( e.getRequestId(), r.getRequestId() );
        assertTrue( e.isNormalCompletion() );
        assertTrue( e.isExpectingResults() );
      } );

      verify( chainedCallback, never() ).onResponseReceived( request, response );
    } );
  }

  @Test
  public void onResponseReceived_AUTHError()
  {
    final SystemSchema schema = newSchema();
    newConnection( createConnector( schema ) );
    Replicant.context().request( schema.getId(), ValueUtil.randomString(), () -> {

      final replicant.Request r = Replicant.context().currentRequest( schema.getId() );

      final RequestCallback chainedCallback = mock( RequestCallback.class );

      final Request request = mock( Request.class );
      final Response response = mock( Response.class );

      when( response.getStatusCode() ).thenReturn( 400 );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      new ReplicantRequestCallback( r, chainedCallback )
        .onResponseReceived( request, response );

      handler.assertEventCount( 1 );
      handler.assertNextEvent( RequestCompletedEvent.class, e -> {
        assertEquals( e.getRequestId(), r.getRequestId() );
        assertFalse( e.isNormalCompletion() );
      } );

      verify( chainedCallback ).onError( eq( request ), any( InvalidHttpResponseException.class ) );
    } );
  }

  @Test
  public void onError()
  {
    final SystemSchema schema = newSchema();
    newConnection( createConnector( schema ) );
    Replicant.context().request( schema.getId(), ValueUtil.randomString(), () -> {

      final replicant.Request r = Replicant.context().currentRequest( schema.getId() );

      final RequestCallback chainedCallback = mock( RequestCallback.class );

      final Request request = mock( Request.class );
      final Response response = mock( Response.class );

      when( response.getStatusCode() ).thenReturn( 400 );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      final Throwable exception = new Throwable();
      new ReplicantRequestCallback( r, chainedCallback )
        .onError( request, exception );

      handler.assertEventCount( 1 );
      handler.assertNextEvent( RequestCompletedEvent.class, e -> {
        assertEquals( e.getRequestId(), r.getRequestId() );
        assertFalse( e.isNormalCompletion() );
      } );

      verify( chainedCallback ).onError( eq( request ), eq( exception ) );
    } );
  }
}
