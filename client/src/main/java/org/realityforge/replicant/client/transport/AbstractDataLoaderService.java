package org.realityforge.replicant.client.transport;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.AreaOfInterestRequest;
import replicant.CacheEntry;
import replicant.CacheService;
import replicant.ChannelAddress;
import replicant.Connection;
import replicant.Connector;
import replicant.ReplicantContext;
import replicant.SafeProcedure;
import replicant.SystemSchema;
import replicant.Transport;

/**
 * Class from which to extend to implement a service that loads data from a change set.
 * Data is loaded incrementally and the load can be broken up into several
 * steps to avoid locking a thread such as in GWT.
 */
@SuppressWarnings( { "WeakerAccess", "unused" } )
public abstract class AbstractDataLoaderService
  extends Connector
{
  protected static final Logger LOG = Logger.getLogger( AbstractDataLoaderService.class.getName() );

  private final SessionContext _sessionContext;

  protected AbstractDataLoaderService( @Nullable final ReplicantContext context,
                                       @Nonnull final SystemSchema schema,
                                       @Nonnull final Transport transport,
                                       @Nonnull final SessionContext sessionContext )
  {
    super( context, schema, transport );
    _sessionContext = Objects.requireNonNull( sessionContext );
  }

  @Nonnull
  protected final SessionContext getSessionContext()
  {
    return _sessionContext;
  }

  protected void onConnection( @Nonnull final String connectionId, @Nonnull final SafeProcedure action )
  {
    setConnection( new Connection( this, connectionId ), action );
    triggerScheduler();
  }

  protected void onDisconnection( @Nonnull final SafeProcedure action )
  {
    setConnection( null, action );
  }

  private void setConnection( @Nullable final Connection connection, @Nonnull final SafeProcedure action )
  {
    if ( null == connection || null == connection.getCurrentMessageResponse() )
    {
      doSetConnection( connection, action );
    }
    else
    {
      setPostMessageResponseAction( () -> doSetConnection( connection, action ) );
    }
  }

  protected void doSetConnection( @Nullable final Connection connection, @Nonnull final SafeProcedure action )
  {
    if ( connection != getConnection() )
    {
      setConnection( connection );
      // This should probably be moved elsewhere ... but where?
      getSessionContext().setConnection( connection );
    }
    action.call();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean progressAreaOfInterestRequestProcessing()
  {
    final List<AreaOfInterestRequest> requests = ensureConnection().getCurrentAreaOfInterestRequests();
    if ( requests.isEmpty() )
    {
      return false;
    }
    else if ( requests.get( 0 ).isInProgress() )
    {
      return false;
    }
    else
    {
      requests.forEach( AreaOfInterestRequest::markAsInProgress );
      final AreaOfInterestRequest.Type type = requests.get( 0 ).getType();
      if ( AreaOfInterestRequest.Type.ADD == type )
      {
        progressAreaOfInterestAddRequests( requests );
      }
      else if ( AreaOfInterestRequest.Type.REMOVE == type )
      {
        progressAreaOfInterestRemoveRequests( requests );
      }
      else
      {
        progressAreaOfInterestUpdateRequests( requests );
      }
      return true;
    }
  }

  private void progressAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededUpdateRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
    }
    else if ( requests.size() > 1 )
    {
      progressBulkAreaOfInterestUpdateRequests( requests );
    }
    else
    {
      progressAreaOfInterestUpdateRequest( requests.get( 0 ) );
    }
  }

  private void progressAreaOfInterestUpdateRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscriptionUpdateStarted( address );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      onSubscriptionUpdateCompleted( address );
    };

    final Consumer<Throwable> onError = error ->
    {
      completeAreaOfInterestRequest();
      onSubscriptionUpdateFailed( address, error );
    };

    final Object filter = request.getFilter();
    assert null != filter;
    getTransport().requestSubscriptionUpdate( address, filter, onSuccess, onError );
  }

  private void progressBulkAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscriptionUpdateStarted );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      addresses.forEach( this::onSubscriptionUpdateCompleted );
    };

    final Consumer<Throwable> onError = error -> {
      completeAreaOfInterestRequest();
      addresses.forEach( a -> onSubscriptionUpdateFailed( a, error ) );
    };
    // All filters will be the same if they are grouped
    final Object filter = requests.get( 0 ).getFilter();
    assert null != filter;
    getTransport().requestBulkSubscriptionUpdate( addresses, filter, onSuccess, onError );
  }

  private void progressAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededAddRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
    }
    else if ( 1 == requests.size() )
    {
      progressAreaOfInterestAddRequest( requests.get( 0 ) );
    }
    else
    {
      progressBulkAreaOfInterestAddRequests( requests );
    }
  }

  private void progressAreaOfInterestAddRequest( @Nonnull final AreaOfInterestRequest request )
  {
    final ChannelAddress address = request.getAddress();
    onSubscribeStarted( address );
    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      onSubscribeCompleted( address );
    };

    final Consumer<Throwable> onError = error ->
    {
      completeAreaOfInterestRequest();
      onSubscribeFailed( address, error );
    };

    final String cacheKey = request.getCacheKey();
    final CacheService cacheService = getReplicantContext().getCacheService();
    final CacheEntry cacheEntry = null == cacheService ? null : cacheService.lookup( cacheKey );
    final String eTag;
    final SafeProcedure onCacheValid;
    if ( null != cacheEntry )
    {
      eTag = cacheEntry.getETag();
      LOG.info( () -> "Found locally cached data for channel " + request + " with etag " + eTag + "." );
      onCacheValid = () ->
      {
        LOG.info( () -> "Loading cached data for channel " + request + " with etag " + eTag );
        final SafeProcedure completeCachedAction = () ->
        {
          LOG.info( () -> "Completed load of cached data for channel " + request + " with etag " + eTag + "." );
          onSuccess.call();
        };
        ensureConnection().enqueueOutOfBandResponse( cacheEntry.getContent(), completeCachedAction );
        triggerScheduler();
      };
    }
    else
    {
      eTag = null;
      onCacheValid = null;
    }
    getTransport().requestSubscribe( request.getAddress(),
                                     request.getFilter(),
                                     cacheKey,
                                     eTag,
                                     onCacheValid,
                                     onSuccess,
                                     onError );
  }

  private void progressBulkAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    final List<ChannelAddress> addresses =
      requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
    addresses.forEach( this::onSubscribeStarted );

    final SafeProcedure onSuccess = () -> {
      completeAreaOfInterestRequest();
      addresses.forEach( this::onSubscribeCompleted );
    };

    final Consumer<Throwable> onError = error -> {
      completeAreaOfInterestRequest();
      addresses.forEach( a -> onSubscribeFailed( a, error ) );
    };

    getTransport().requestBulkSubscribe( addresses, requests.get( 0 ).getFilter(), onSuccess, onError );
  }
}
