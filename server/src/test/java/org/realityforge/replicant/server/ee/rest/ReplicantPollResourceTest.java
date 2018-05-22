package org.realityforge.replicant.server.ee.rest;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import org.mockito.ArgumentCaptor;
import org.realityforge.replicant.server.ee.rest.AbstractReplicantPollResource.SuspendedRequest;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantPollResourceTest
{
  @Test
  public void poll_dataAvailable()
    throws Exception
  {
    final TestReplicantPollResource resource = newResource();

    final AsyncResponse response = mock( AsyncResponse.class );

    when( resource._pollSource.poll( "X", 22 ) ).thenReturn( "DATA!" );

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
    assertEquals( value.getHeaderString( "Cache-control" ),
                  "private, no-store, no-cache, must-revalidate, max-age=0, pre-check=0, post-check=0" );
    assertEquals( value.getHeaderString( "Expires" ), "0" );
    assertNotNull( value.getHeaderString( "Date" ) );
    assertNotNull( value.getHeaderString( "Last-Modified" ) );
  }

  @Test
  public void poll_suspendRequest_thenResume()
    throws Exception
  {
    final TestReplicantPollResource resource = newResource();

    final AsyncResponse response = mock( AsyncResponse.class );

    when( resource._pollSource.poll( "X", 22 ) ).thenReturn( null );

    resource.poll( response, "X", 22 );

    verify( response, never() ).resume( any() );

    final Map<AsyncResponse, SuspendedRequest> requests = resource.getRequests();

    assertEquals( requests.size(), 1 );
    final SuspendedRequest request = requests.get( response );
    assertNotNull( request );
    assertEquals( request.getResponse(), response );
    assertEquals( request.getSessionID(), "X" );
    assertEquals( request.getRxSequence(), 22 );

    when( resource._pollSource.poll( "X", 22 ) ).thenReturn( "DATA" );

    when( response.isSuspended() ).thenReturn( true );
    when( response.isCancelled() ).thenReturn( false );

    resource._dataChecker.run();

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( "DATA", captor.getValue() );

    assertEquals( requests.size(), 0 );

    resource._dataChecker.run();
    assertEquals( requests.size(), 0 );
  }

  @Test
  public void poll_suspendRequest_thenException()
    throws Exception
  {
    final TestReplicantPollResource resource = newResource();

    final AsyncResponse response = mock( AsyncResponse.class );

    when( resource._pollSource.poll( "X", 22 ) ).thenReturn( null );

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
    when( resource._pollSource.poll( "X", 22 ) ).thenThrow( exception );

    when( response.isSuspended() ).thenReturn( true );
    when( response.isCancelled() ).thenReturn( false );

    resource._dataChecker.run();

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response, times( 1 ) ).resume( captor.capture() );
    assertResponse( exception, captor.getValue() );
    assertEquals( requests.size(), 0 );

    resource._dataChecker.run();
    assertEquals( requests.size(), 0 );
  }

  @Test
  public void poll_exception()
    throws Exception
  {
    final TestReplicantPollResource resource = newResource();

    final AsyncResponse response = mock( AsyncResponse.class );

    final Exception throwable = new Exception();
    when( resource._pollSource.poll( "X", 22 ) ).thenThrow( throwable );

    resource.poll( response, "X", 22 );

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( throwable, captor.getValue() );
  }

  @Test
  public void poll_exceptionWhenSessionComplete()
    throws Exception
  {
    final TestReplicantPollResource resource = newResource();

    final AsyncResponse response = mock( AsyncResponse.class );

    final Exception throwable = new Exception();
    when( resource._pollSource.poll( "X", 22 ) ).thenThrow( throwable );

    resource._sessionConnected = false;
    resource.poll( response, "X", 22 );

    final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass( Response.class );
    verify( response ).resume( captor.capture() );
    assertResponse( "", captor.getValue() );
  }

  protected TestReplicantPollResource newResource()
    throws Exception
  {
    final TestReplicantPollResource resource = new TestReplicantPollResource();
    resource.postConstruct();
    return resource;
  }

  static class TestReplicantPollResource
    extends AbstractReplicantPollResource
  {
    private final PollSource _pollSource = mock( PollSource.class );
    PendingDataChecker _dataChecker;
    boolean _sessionConnected = true;

    @Override
    ScheduledFuture<?> schedule( final PendingDataChecker dataChecker )
    {
      _dataChecker = dataChecker;
      return mock( ScheduledFuture.class );
    }

    @Nonnull
    @Override
    PollSource createContextualProxyPollSource()
    {
      return _pollSource;
    }

    @Nonnull
    @Override
    protected ManagedScheduledExecutorService getScheduledExecutorService()
    {
      return mock( ManagedScheduledExecutorService.class );
    }

    @Nonnull
    @Override
    protected ContextService getContextService()
    {
      return mock( ContextService.class );
    }

    @Nullable
    @Override
    protected String poll( @Nonnull final String sessionId, final int rxSequence )
      throws Exception
    {
      return _pollSource.poll( sessionId, rxSequence );
    }

    @Override
    protected boolean isSessionConnected( @Nonnull final String sessionId )
    {
      return _sessionConnected;
    }
  }
}
