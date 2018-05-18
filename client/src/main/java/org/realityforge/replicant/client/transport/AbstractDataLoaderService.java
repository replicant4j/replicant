package org.realityforge.replicant.client.transport;

import arez.Disposable;
import arez.annotations.Action;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Verifiable;
import replicant.AreaOfInterestRequest;
import replicant.CacheEntry;
import replicant.CacheService;
import replicant.ChangeSet;
import replicant.ChannelAddress;
import replicant.Connection;
import replicant.Connector;
import replicant.MessageResponse;
import replicant.Replicant;
import replicant.ReplicantContext;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import replicant.SystemSchema;
import static org.realityforge.braincheck.Guards.*;

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

  @SuppressWarnings( "SameParameterValue" )
  @Nonnull
  protected abstract ChangeSet parseChangeSet( @Nonnull String rawJsonData );

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
    // Step: Retrieve any out of band actions
    final LinkedList<MessageResponse> oobResponses = connection.getOutOfBandResponses();
    final MessageResponse response = connection.getCurrentMessageResponse();
    if ( null == response && !oobResponses.isEmpty() )
    {
      connection.setCurrentMessageResponse( oobResponses.removeFirst() );
      return true;
    }

    //Step: Retrieve the action from the parsed queue if it is the next in the sequence
    final LinkedList<MessageResponse> pendingResponses = connection.getPendingResponses();
    if ( null == response && !pendingResponses.isEmpty() )
    {
      final MessageResponse action = pendingResponses.get( 0 );
      if ( action.isOob() || connection.getLastRxSequence() + 1 == action.getChangeSet().getSequence() )
      {
        connection.setCurrentMessageResponse( pendingResponses.remove() );
        return true;
      }
    }

    // Abort if there is no pending data load actions to take
    final LinkedList<MessageResponse> unparsedResponses = connection.getUnparsedResponses();
    if ( null == response && unparsedResponses.isEmpty() )
    {
      return false;
    }

    //Step: Retrieve the action from the un-parsed queue
    if ( null == response )
    {
      final MessageResponse candidate = unparsedResponses.remove();
      connection.setCurrentMessageResponse( candidate );
      return true;
    }

    //Step: Parse the json
    if ( null != response.getRawJsonData() )
    {
      parseMessageResponse( response );
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
      validateWorld( response );
      return true;
    }
    completeMessageResponse( response );
    return true;
  }

  private void completeMessageResponse( @Nonnull final MessageResponse response )
  {
    final Connection connection = ensureConnection();

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
    final SafeProcedure runnable = response.getCompletionAction();
    if ( null != runnable )
    {
      runnable.call();
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

  private void validateWorld( @Nonnull final MessageResponse response )
  {
    response.markWorldAsValidated();
    if ( Replicant.shouldValidateRepositoryOnLoad() )
    {
      validateRepository();
    }
  }

  private void parseMessageResponse( @Nonnull final MessageResponse response )
  {
    final Connection connection = ensureConnection();
    final String rawJsonData = response.getRawJsonData();
    assert null != rawJsonData;
    final ChangeSet changeSet = parseChangeSet( rawJsonData );
    if ( Replicant.shouldValidateChangeSetOnRead() )
    {
      changeSet.validate();
    }

    final RequestEntry request;
    if ( response.isOob() )
    {
      request = null;
    }
    else
    {
      final String requestId = changeSet.getRequestId();
      final String eTag = changeSet.getETag();
      final int sequence = changeSet.getSequence();
      request = null != requestId ? connection.getRequest( requestId ) : null;
      if ( null == request && null != requestId )
      {
        final String message =
          "Unable to locate requestID '" + requestId + "' specified for ChangeSet: seq=" + sequence +
          " Existing Requests: " + connection.getRequests();
        if ( LOG.isLoggable( Level.WARNING ) )
        {
          LOG.warning( message );
        }
        throw new IllegalStateException( message );
      }
      else if ( null != request )
      {
        final String cacheKey = request.getCacheKey();
        if ( null != eTag && null != cacheKey )
        {
          final CacheService cacheService = getReplicantContext().getCacheService();
          if ( null != cacheService )
          {
            cacheService.store( cacheKey, eTag, rawJsonData );
          }
        }
      }
    }

    response.recordChangeSet( changeSet, request );
    final LinkedList<MessageResponse> pendingResponses = connection.getPendingResponses();
    pendingResponses.add( response );
    Collections.sort( pendingResponses );
    connection.setCurrentMessageResponse( null );
  }

  /**
   * Check all the entities in the repository and raise an exception if an entity fails to validateRepository.
   * This method should not need a transaction ... unless an entity is invalid and there is unlinked data so we
   * wrap in an @Action just in case.
   *
   * An entity can fail to validateRepository if it is {@link Disposable} and {@link Disposable#isDisposed()} returns
   * true. An entity can also fail to validateRepository if it is {@link Verifiable} and {@link Verifiable#verify()}
   * throws an exception.
   */
  @Action
  protected void validateRepository()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      for ( final Class<?> entityType : getReplicantContext().findAllEntityTypes() )
      {
        for ( final Object entity : getReplicantContext().findAllEntitiesByType( entityType ) )
        {
          invariant( () -> !Disposable.isDisposed( entity ),
                     () -> "Invalid disposed entity found during validation. Entity: " + entity );
          try
          {
            Verifiable.verify( entity );
          }
          catch ( final Exception e )
          {
            final String message = "Entity failed to verify. Entity = " + entity;
            LOG.log( Level.WARNING, message, e );
            throw new IllegalStateException( message, e );
          }
        }
      }
    }
  }
}
