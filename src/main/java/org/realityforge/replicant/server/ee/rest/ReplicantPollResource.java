package org.realityforge.replicant.server.ee.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * Jax-RS resource that responds to poll requests for replicant data stream.
 */
@Path(ReplicantContext.REPLICANT_URL_FRAGMENT)
@Singleton
public class ReplicantPollResource
{
  private final Map<AsyncResponse, SuspendedRequest> _requests =
    Collections.synchronizedMap( new HashMap<AsyncResponse, SuspendedRequest>() );
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
  @Produces("text/plain")
  public void poll( @Suspended AsyncResponse response,
                    @Nonnull @HeaderParam(ReplicantContext.SESSION_ID_HEADER) final String sessionID,
                    @QueryParam("rx") final int rxSequence )
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
        response.resume( data );
      }
      else
      {
        _requests.put( response, new SuspendedRequest( sessionID, rxSequence, response ) );
      }
    }
    catch ( final Exception e )
    {
      response.resume( e );
    }
  }

  /**
   * Return a map of pending requests.
   */
  protected Map<AsyncResponse, SuspendedRequest> getRequests()
  {
    return _requests;
  }

  /**
   * Return the number of seconds per polling cycle.
   */
  protected int getPollTime()
  {
    return 2;
  }

  /**
   * Perform the timeout for pending response.
   */
  private void doTimeout( final AsyncResponse response )
  {
    doDisconnect( response );
    response.resume( "" );
  }

  /**
   * Perform the disconnection for pending response.
   */
  private void doDisconnect( final AsyncResponse response )
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

    SuspendedRequest( final String sessionID, final int rxSequence, final AsyncResponse response )
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

    PendingDataChecker( final Map<AsyncResponse, SuspendedRequest> requests, final ReplicantPollSource source )
    {
      _requests = requests;
      _source = source;
    }

    @Override
    public void run()
    {
      synchronized ( _requests )
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
                request.getResponse().resume( data );
                iterator.remove();
              }
            }
            catch ( final Exception e )
            {
              request.getResponse().resume( e );
              iterator.remove();
            }
          }
        }
      }
    }
  }
}
