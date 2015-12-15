package org.realityforge.replicant.server.ee.rest;

import java.lang.reflect.Field;
import java.util.Map;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
import org.realityforge.replicant.server.ee.rest.ReplicantPollResource.SuspendedRequest;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantPollResourceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final ReplicantPollSource source = mock( ReplicantPollSource.class );
    final ReplicantPollResource resource = newResource( source );

    final AsyncResponse response = mock( AsyncResponse.class );

    when( source.poll( "X", 22 ) ).thenReturn( "DATA!" );

    resource.poll( response, "X", 22 );

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( "DATA!", captor.getValue() );
  }

  private void assertResponse( final Object content, final Response value )
  {
    assertEquals( value.getStatus(), Response.Status.OK.getStatusCode() );
    assertEquals( value.getEntity(), content );
    assertEquals( value.getHeaderString( "Pragma" ), "no-cache" );
    assertEquals( value.getHeaderString( "Cache-control" ), "private, no-store, no-cache, must-revalidate, max-age=0, pre-check=0, post-check=0" );
    assertEquals( value.getHeaderString( "Expires" ), "0" );
    assertNotNull( value.getHeaderString( "Date" ) );
    assertNotNull( value.getHeaderString( "Last-Modified" ) );
  }

  @Test
  public void poll_suspendRequest_thenResume()
    throws Exception
  {
    final ReplicantPollSource source = mock( ReplicantPollSource.class );
    final ReplicantPollResource resource = newResource( source );

    final AsyncResponse response = mock( AsyncResponse.class );

    when( source.poll( "X", 22 ) ).thenReturn( null );

    resource.poll( response, "X", 22 );

    verify( response, never() ).resume( any() );

    final Map<AsyncResponse, SuspendedRequest> requests = resource.getRequests();

    assertEquals( requests.size(), 1 );
    final SuspendedRequest request = requests.get( response );
    assertNotNull( request );
    assertEquals( request.getResponse(), response );
    assertEquals( request.getSessionID(), "X" );
    assertEquals( request.getRxSequence(), 22 );

    when( source.poll( "X", 22 ) ).thenReturn( "DATA" );

    when( response.isSuspended() ).thenReturn( true );
    when( response.isCancelled() ).thenReturn( false );

    getChecker( resource ).run();

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( "DATA", captor.getValue() );

    assertEquals( requests.size(), 0 );

    getChecker( resource ).run();
    assertEquals( requests.size(), 0 );
  }

  @Test
  public void poll_suspendRequest_thenException()
    throws Exception
  {
    final ReplicantPollSource source = mock( ReplicantPollSource.class );
    final ReplicantPollResource resource = newResource( source );

    final AsyncResponse response = mock( AsyncResponse.class );

    when( source.poll( "X", 22 ) ).thenReturn( null );

    resource.poll( response, "X", 22 );

    verify( response, never() ).resume( any() );

    final Map<AsyncResponse, SuspendedRequest> requests = resource.getRequests();

    assertEquals( requests.size(), 1 );
    final SuspendedRequest request = requests.get( response );
    assertNotNull( request );
    assertEquals( request.getResponse(), response );
    assertEquals( request.getSessionID(), "X" );
    assertEquals( request.getRxSequence(), 22 );

    final Exception exception = new Exception();
    when( source.poll( "X", 22 ) ).thenThrow( exception );

    when( response.isSuspended() ).thenReturn( true );
    when( response.isCancelled() ).thenReturn( false );

    getChecker( resource ).run();

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response, times( 1 ) ).resume( captor.capture() );
    assertResponse( exception, captor.getValue() );
    assertEquals( requests.size(), 0 );

    getChecker( resource ).run();
    assertEquals( requests.size(), 0 );
  }

  @Test
  public void poll_exception()
    throws Exception
  {
    final ReplicantPollSource source = mock( ReplicantPollSource.class );
    final ReplicantPollResource resource = newResource( source );

    final AsyncResponse response = mock( AsyncResponse.class );

    final Exception throwable = new Exception();
    when( source.poll( "X", 22 ) ).thenThrow( throwable );

    resource.poll( response, "X", 22 );

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( throwable, captor.getValue() );
  }

  protected ReplicantPollResource newResource( final ReplicantPollSource source )
    throws Exception
  {
    final ReplicantPollResource resource = new ReplicantPollResource();
    setField( resource, "_source", source );
    resource.setupDataChecker();
    return resource;
  }

  private ReplicantPollResource.PendingDataChecker getChecker( final ReplicantPollResource resource )
    throws Exception
  {
    return getField( resource, "_dataChecker" );
  }

  private <T> T getField( final ReplicantPollResource resource,
                          final String fieldName )
    throws Exception
  {
    final Field field = ReplicantPollResource.class.getDeclaredField( fieldName );
    field.setAccessible( true );
    return (T) field.get( resource );
  }

  private void setField( final ReplicantPollResource resource,
                         final String fieldName,
                         final Object value )
    throws Exception
  {
    final Field field = ReplicantPollResource.class.getDeclaredField( fieldName );
    field.setAccessible( true );
    field.set( resource, value );
  }
}
