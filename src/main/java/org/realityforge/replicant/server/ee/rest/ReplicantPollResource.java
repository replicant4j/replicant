package org.realityforge.replicant.server.ee.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * Jax-RS resource that responds to poll requests for replicant data stream.
 *
 * It is expected that this endpoint has already had security applied.
 */
@Path( ReplicantContext.REPLICANT_URL_FRAGMENT )
@ApplicationScoped
@Transactional( Transactional.TxType.NOT_SUPPORTED )
public class ReplicantPollResource
{
  private final Map<AsyncResponse, SuspendedRequest> _requests = new ConcurrentHashMap<>();
  private final ScheduledExecutorService _scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
  @Inject
  private ReplicantPollSource _source;
  private PendingDataChecker _dataChecker;

  @PostConstruct
  public void postConstruct()
  {
    setupDataChecker();
    _scheduledExecutor.scheduleWithFixedDelay( _dataChecker, 0, 100, TimeUnit.MILLISECONDS );
  }

  protected void setupDataChecker()
  {
    _dataChecker = new PendingDataChecker( _requests, _source );
  }

  @PreDestroy
  public void preDestroy()
  {
    _scheduledExecutor.shutdown();
  }

  @GET
  @Produces( "text/plain" )
  public void poll( @Suspended AsyncResponse response,
                    @Nonnull @HeaderParam( ReplicantContext.SESSION_ID_HEADER ) final String sessionID,
                    @QueryParam( ReplicantContext.RECEIVE_SEQUENCE_PARAM ) final int rxSequence )
  {
    response.setTimeout( getPollTime(), TimeUnit.SECONDS );
    response.register( new ConnectionCallback()
    {
      @Override
      public void onDisconnect( final AsyncResponse disconnected )
      {
        doDisconnect( disconnected );
      }
    } );
    response.setTimeoutHandler( new TimeoutHandler()
    {
      @Override
      public void handleTimeout( final AsyncResponse asyncResponse )
      {
        doTimeout( asyncResponse );
      }
    } );

    try
    {
      final String data = _source.poll( sessionID, rxSequence );
      if ( null != data )
      {
        resume( response, data );
      }
      else
      {
        _requests.put( response, new SuspendedRequest( sessionID, rxSequence, response ) );
      }
    }
    catch ( final Exception e )
    {
      resume( response, e );
    }
  }

  /**
   * Return a map of pending requests.
   */
  @Nonnull
  protected Map<AsyncResponse, SuspendedRequest> getRequests()
  {
    return _requests;
  }

  /**
   * Return the number of seconds per polling cycle.
   */
  protected int getPollTime()
  {
    return 30;
  }

  /**
   * Perform the timeout for pending response.
   */
  private void doTimeout( @Nonnull final AsyncResponse response )
  {
    doDisconnect( response );
    resume( response, "" );
  }

  private static void resume( @Nonnull final AsyncResponse asyncResponse, @Nonnull final Object message )
  {
    asyncResponse.resume( toResponse( message ) );
  }

  @Nonnull
  private static Response toResponse( @Nonnull final Object message )
  {
    final Response.ResponseBuilder builder = Response.ok();
    CacheUtil.configureNoCacheHeaders( builder );
    return builder.entity( message ).build();
  }

  /**
   * Perform the disconnection for pending response.
   */
  private void doDisconnect( @Nonnull final AsyncResponse response )
  {
    _requests.remove( response );
  }

  /**
   * A container for suspended requests.
   */
  static class SuspendedRequest
  {
    private final String _sessionID;
    private final int _rxSequence;
    private final AsyncResponse _response;

    SuspendedRequest( @Nonnull final String sessionID,
                      final int rxSequence,
                      @Nonnull final AsyncResponse response )
    {
      _sessionID = sessionID;
      _rxSequence = rxSequence;
      _response = response;
    }

    String getSessionID()
    {
      return _sessionID;
    }

    int getRxSequence()
    {
      return _rxSequence;
    }

    AsyncResponse getResponse()
    {
      return _response;
    }
  }

  /**
   * This class is used to deliver pending data for any outstanding requests.
   */
  static final class PendingDataChecker
    implements Runnable
  {
    private final Map<AsyncResponse, SuspendedRequest> _requests;
    private final ReplicantPollSource _source;

    PendingDataChecker( @Nonnull final Map<AsyncResponse, SuspendedRequest> requests,
                        @Nonnull final ReplicantPollSource source )
    {
      _requests = requests;
      _source = source;
    }

    @Override
    public void run()
    {
      final Iterator<SuspendedRequest> iterator = _requests.values().iterator();
      while ( iterator.hasNext() )
      {
        final SuspendedRequest request = iterator.next();

        if ( !request.getResponse().isSuspended() || request.getResponse().isCancelled() )
        {
          iterator.remove();
        }
        else
        {
          try
          {
            final String data = _source.poll( request.getSessionID(), request.getRxSequence() );
            if ( null != data )
            {
              resume( request.getResponse(), data );
              iterator.remove();
            }
          }
          catch ( final Exception e )
          {
            resume( request.getResponse(), e );
            iterator.remove();
          }
        }
      }
    }
  }
}
