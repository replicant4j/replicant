package org.realityforge.replicant.server.ee.rest;

import java.lang.reflect.Field;
import java.util.Map;
import javax.ws.rs.container.AsyncResponse;
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

    verify( response ).resume( "DATA!" );
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

    verify( response, times( 1 ) ).resume( "DATA" );
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

    verify( response, times( 1 ) ).resume( exception );
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

    verify( response ).resume( throwable );
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
