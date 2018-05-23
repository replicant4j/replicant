package org.realityforge.replicant.client.transport;

import java.util.List;
import java.util.Objects;
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
}
