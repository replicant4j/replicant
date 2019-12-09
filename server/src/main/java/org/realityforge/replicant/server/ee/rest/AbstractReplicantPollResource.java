package org.realityforge.replicant.server.ee.rest;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * Jax-RS resource that responds to poll requests for replicant data stream.
 *
 * It is expected that this endpoint has already had security applied. A
 * subclass of this class should be created with a form similar to:L
 *
 * <code>
 *
 * \@Path( ReplicantContext.REPLICANT_URL_FRAGMENT )
 * \@ApplicationScoped
 * \@Transactional( Transactional.TxType.NOT_SUPPORTED )
 * public class MyAppReplicantPollResource extends AbstractReplicantPollResource
 * {
 * protected String poll( @Nonnull String sessionID, int rxSequence )
 * throws Exception { ... }
 *
 * protected boolean isSessionConnected( @Nonnull String sessionID ) { ... }
 * \@Override
 * \@PostConstruct public void postConstruct()
 * {
 * super.postConstruct();
 * }
 * }
 * </code>
 */
public abstract class AbstractReplicantPollResource
{
  private final Map<AsyncResponse, SuspendedRequest> _requests = new ConcurrentHashMap<>();
  private ScheduledFuture<?> _future;

  public void postConstruct()
  {
    final PollSource pollSource = createContextualProxyPollSource();
    final PendingDataChecker dataChecker = new PendingDataChecker( _requests, pollSource );
    _future = schedule( dataChecker );
  }

  ScheduledFuture<?> schedule( final PendingDataChecker dataChecker )
  {
    return getScheduledExecutorService().scheduleWithFixedDelay( dataChecker, 0, 100, TimeUnit.MILLISECONDS );
  }

  @Nonnull
  PollSource createContextualProxyPollSource()
  {
    return getContextService().createContextualProxy( new PollSourceImpl(), PollSource.class );
  }

  @PreDestroy
  public void preDestroy()
  {
    _future.cancel( true );
  }

  @GET
  @Produces( "text/plain" )
  public void poll( @Suspended final AsyncResponse response,
                    @NotNull @HeaderParam( SharedConstants.CONNECTION_ID_HEADER ) final String sessionID,
                    @NotNull @QueryParam( SharedConstants.RECEIVE_SEQUENCE_PARAM ) final int rxSequence )
  {
    response.setTimeout( getPollTime(), TimeUnit.SECONDS );
    response.register( (ConnectionCallback) this::doDisconnect );
    response.setTimeoutHandler( this::doTimeout );

    try
    {
      final String data = poll( sessionID, rxSequence );
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
      handleException( sessionID, response, e );
    }
  }

  @Nonnull
  protected abstract ManagedScheduledExecutorService getScheduledExecutorService();

  @Nonnull
  protected abstract ContextService getContextService();

  /**
   * Appropriately handle exception raised during responding to request.
   * Some exception occur when the session is closed and thus should result
   * in an exception being passed back to the caller.
   */
  private void handleException( @Nonnull final String sessionID,
                                @Nonnull final AsyncResponse response,
                                @Nonnull final Exception e )
  {
    if ( !isSessionConnected( sessionID ) )
    {
      resume( response, "" );
    }
    else
    {
      resume( response, e );
    }
  }

  /**
   * Check if there is any data for specified session and return data if any immediately.
   * This method should not block.
   *
   * @param sessionID  the session identifier.
   * @param rxSequence the sequence of the last message received by the session.
   * @return data for session if any.
   * @throws java.lang.Exception if session is unknown, unavailable or the sequence makes no sense.
   */
  @Nullable
  protected abstract String poll( @Nonnull String sessionID, int rxSequence )
    throws Exception;

  /**
   * Return true if specified session is still connected.
   */
  protected abstract boolean isSessionConnected( @Nonnull String sessionID );

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
    return SharedConstants.MAX_POLL_TIME_IN_SECONDS;
  }

  /**
   * Perform the timeout for pending response.
   */
  private void doTimeout( @Nonnull final AsyncResponse response )
  {
    doDisconnect( response );
    resume( response, "" );
  }

  /**
   * Resume request processing with specified message.
   */
  private void resume( @Nonnull final AsyncResponse asyncResponse, @Nonnull final Object message )
  {
    asyncResponse.resume( toResponse( message ) );
  }

  @Nonnull
  private Response toResponse( @Nonnull final Object message )
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
  public class PendingDataChecker
    implements Runnable
  {
    private final Lock _lock = new ReentrantLock( true );
    private final Map<AsyncResponse, SuspendedRequest> _requests;
    private final PollSource _source;

    PendingDataChecker( @Nonnull final Map<AsyncResponse, SuspendedRequest> requests,
                        @Nonnull final PollSource source )
    {
      _requests = requests;
      _source = source;
    }

    @Override
    public void run()
    {
      try
      {
        _lock.lockInterruptibly();
        try
        {
          doPoll();
        }
        finally
        {
          _lock.unlock();
        }
      }
      catch ( final InterruptedException e )
      {
        //Ignore as indicates schedulre being terminated
      }
    }

    private void doPoll()
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
            handleException( request.getSessionID(), request.getResponse(), e );
            iterator.remove();
          }
        }
      }
    }
  }

  /**
   * Interface used to contextualize access to poll resource.
   */
  public interface PollSource
  {
    @Nullable
    String poll( @Nonnull String sessionID, int rxSequence )
      throws Exception;
  }

  public class PollSourceImpl
    implements PollSource
  {
    @Nullable
    @Override
    public String poll( @Nonnull final String sessionID, final int rxSequence )
      throws Exception
    {
      return AbstractReplicantPollResource.this.poll( sessionID, rxSequence );
    }
  }
}
