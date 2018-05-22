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
import replicant.MessageResponse;
import replicant.ReplicantContext;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import replicant.SystemSchema;

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

  /**
   * Action invoked after current action completes to reset connection state.
   */
  private Runnable _resetAction;

  protected AbstractDataLoaderService( @Nullable final ReplicantContext context,
                                       @Nonnull final SystemSchema schema,
                                       @Nonnull final SessionContext sessionContext )
  {
    super( context, schema );
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
    final Runnable runnable = () -> doSetConnection( connection, action );
    if ( null == connection || null == connection.getCurrentMessageResponse() )
    {
      runnable.run();
    }
    else
    {
      _resetAction = runnable;
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
        return progressAreaOfInterestAddRequests( requests );
      }
      else if ( AreaOfInterestRequest.Type.REMOVE == type )
      {
        return progressAreaOfInterestRemoveRequests( requests );
      }
      else
      {
        return progressAreaOfInterestUpdateRequests( requests );
      }
    }
  }

  private boolean progressAreaOfInterestUpdateRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededUpdateRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
      return true;
    }
    else if ( requests.size() > 1 )
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
      requestBulkUpdateSubscription( addresses, filter, onSuccess, onError );
    }
    else
    {
      final AreaOfInterestRequest request = requests.get( 0 );
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
      requestUpdateSubscription( address, filter, onSuccess, onError );
    }
    return true;
  }

  private boolean progressAreaOfInterestRemoveRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededRemoveRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
      return true;
    }

    final AreaOfInterestRequest request = requests.get( 0 );
    if ( requests.size() > 1 )
    {
      final List<ChannelAddress> addresses =
        requests.stream().map( AreaOfInterestRequest::getAddress ).collect( Collectors.toList() );
      addresses.forEach( this::onUnsubscribeStarted );

      final SafeProcedure onSuccess = () -> {
        removeExplicitSubscriptions( requests );
        completeAreaOfInterestRequest();
        addresses.forEach( this::onUnsubscribeCompleted );
      };

      final Consumer<Throwable> onError = error -> {
        removeExplicitSubscriptions( requests );
        completeAreaOfInterestRequest();
        addresses.forEach( a -> onUnsubscribeFailed( a, error ) );
      };

      requestBulkUnsubscribeFromChannel( addresses, onSuccess, onError );
    }
    else
    {
      final ChannelAddress address = request.getAddress();
      onUnsubscribeStarted( address );
      final SafeProcedure onSuccess = () -> {
        removeExplicitSubscriptions( requests );
        completeAreaOfInterestRequest();
        onUnsubscribeCompleted( address );
      };

      final Consumer<Throwable> onError = error ->
      {
        removeExplicitSubscriptions( requests );
        completeAreaOfInterestRequest();
        onUnsubscribeFailed( address, error );
      };

      requestUnsubscribeFromChannel( address, onSuccess, onError );
    }
    return true;
  }

  private boolean progressAreaOfInterestAddRequests( @Nonnull final List<AreaOfInterestRequest> requests )
  {
    removeUnneededAddRequests( requests );

    if ( requests.isEmpty() )
    {
      completeAreaOfInterestRequest();
      return true;
    }
    else if ( 1 == requests.size() )
    {
      final AreaOfInterestRequest request = requests.get( 0 );
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
      requestSubscribeToChannel( request.getAddress(),
                                 request.getFilter(),
                                 cacheKey,
                                 eTag,
                                 onCacheValid,
                                 onSuccess,
                                 onError );
    }
    else
    {
      // don't support bulk loading of anything that is already cached
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

      requestBulkSubscribeToChannel( addresses, requests.get( 0 ).getFilter(), onSuccess, onError );
    }
    return true;
  }

  protected abstract void requestSubscribeToChannel( @Nonnull ChannelAddress address,
                                                     @Nullable Object filter,
                                                     @Nullable String cacheKey,
                                                     @Nullable String eTag,
                                                     @Nullable SafeProcedure onCacheValid,
                                                     @Nonnull SafeProcedure onSuccess,
                                                     @Nonnull Consumer<Throwable> onError );

  protected abstract void requestUnsubscribeFromChannel( @Nonnull ChannelAddress address,
                                                         @Nonnull SafeProcedure onSuccess,
                                                         @Nonnull Consumer<Throwable> onError );

  protected abstract void requestUpdateSubscription( @Nonnull ChannelAddress address,
                                                     @Nonnull Object filter,
                                                     @Nonnull SafeProcedure onSuccess,
                                                     @Nonnull Consumer<Throwable> onError );

  protected abstract void requestBulkSubscribeToChannel( @Nonnull List<ChannelAddress> addresses,
                                                         @Nullable Object filter,
                                                         @Nonnull SafeProcedure onSuccess,
                                                         @Nonnull Consumer<Throwable> onError );

  protected abstract void requestBulkUnsubscribeFromChannel( @Nonnull List<ChannelAddress> addresses,
                                                             @Nonnull SafeProcedure onSuccess,
                                                             @Nonnull Consumer<Throwable> onError );

  protected abstract void requestBulkUpdateSubscription( @Nonnull List<ChannelAddress> addresses,
                                                         @Nonnull Object filter,
                                                         @Nonnull SafeProcedure onSuccess,
                                                         @Nonnull Consumer<Throwable> onError );

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean progressResponseProcessing()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.getCurrentMessageResponse();
    if ( null == response )
    {
      return connection.selectNextMessageResponse();
    }

    //Step: Parse the json
    if ( null != response.getRawJsonData() )
    {
      parseMessageResponse();
      return true;
    }

    if ( response.needsChannelChangesProcessed() )
    {
      processChannelChanges( response );
      return true;
    }

    //Step: Process a chunk of changes
    if ( response.areChangesPending() )
    {
      processEntityChanges( response );
      return true;
    }

    //Step: Process a chunk of links
    if ( response.areEntityLinksPending() )
    {
      processEntityLinks( response );
      return true;
    }

    //Step: Validate the world after the change set has been applied (if feature is enabled)
    if ( !response.hasWorldBeenValidated() )
    {
      validateWorld();
      return true;
    }
    else
    {
      completeMessageResponse();
      return true;
    }
  }

  private void completeMessageResponse()
  {
    final Connection connection = ensureConnection();
    final MessageResponse response = connection.ensureCurrentMessageResponse();

    // OOB messages are not sequenced
    if ( !response.isOob() )
    {
      connection.setLastRxSequence( response.getChangeSet().getSequence() );
    }

    //Step: Run the post actions
    final RequestEntry request = response.getRequest();
    if ( null != request )
    {
      request.markResultsAsArrived();
    }
    /*
     * An action will be returned if the message is an OOB message
     * or it is an answer to a response and the rpc invocation has
     * already returned.
     */
    final SafeProcedure action = response.getCompletionAction();
    if ( null != action )
    {
      action.call();
      // OOB messages are not in response to requests as such
      final String requestId = response.isOob() ? null : response.getChangeSet().getRequestId();
      if ( null != requestId )
      {
        // We can remove the request because this side ran second and the
        // RPC channel has already returned.

        final boolean removed = connection.removeRequest( requestId );
        if ( !removed )
        {
          LOG.severe( "ChangeSet " + response.getChangeSet().getSequence() + " expected to " +
                      "complete request '" + requestId + "' but no request was registered with connection." );
        }
      }
    }
    connection.setCurrentMessageResponse( null );
    onMessageProcessed( response.toStatus() );
    if ( null != _resetAction )
    {
      _resetAction.run();
      _resetAction = null;
    }
  }
}
