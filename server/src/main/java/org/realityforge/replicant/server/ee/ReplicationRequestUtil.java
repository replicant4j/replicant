package org.realityforge.replicant.server.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.server.ServerConstants;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * Utility class for interacting with replication request infrastructure.
 */
public final class ReplicationRequestUtil
{
  private static final Logger LOG = Logger.getLogger( ReplicationRequestUtil.class.getName() );

  private ReplicationRequestUtil()
  {
  }

  public static <T> T runRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                  @Nonnull final EntityManager entityManager,
                                  @Nonnull final EntityMessageEndpoint endpoint,
                                  @Nonnull final String invocationKey,
                                  @Nullable final String sessionID,
                                  @Nullable final String requestID,
                                  @Nonnull final Callable<T> action )
    throws Exception
  {
    startReplication( registry, invocationKey, sessionID, requestID );
    try
    {
      return action.call();
    }
    finally
    {
      completeReplication( registry, entityManager, endpoint, invocationKey );
    }
  }

  @Nullable
  public static ReplicantSessionManager.CacheStatus runRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                                                @Nonnull final EntityManager entityManager,
                                                                @Nonnull final EntityMessageEndpoint endpoint,
                                                                @Nonnull final String invocationKey,
                                                                @Nullable final String sessionID,
                                                                @Nullable final String requestID,
                                                                @Nonnull final Supplier<ReplicantSessionManager.CacheStatus> action )
  {
    startReplication( registry, invocationKey, sessionID, requestID );
    try
    {
      return action.get();
    }
    finally
    {
      completeReplication( registry, entityManager, endpoint, invocationKey );
    }
  }

  public static void runRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                 @Nonnull final EntityManager entityManager,
                                 @Nonnull final EntityMessageEndpoint endpoint,
                                 @Nonnull final String invocationKey,
                                 @Nullable final String sessionID,
                                 @Nullable final String requestID,
                                 @Nonnull final Runnable action )
  {
    startReplication( registry, invocationKey, sessionID, requestID );
    try
    {
      action.run();
    }
    finally
    {
      completeReplication( registry, entityManager, endpoint, invocationKey );
    }
  }

  /**
   * Start a replication context.
   *
   * @param invocationKey the identifier of the element that is initiating replication. (i.e. Method name).
   * @param sessionID     the id of the session that initiated change if any.
   * @param requestID     the id of the request in the session that initiated change..
   */
  private static void startReplication( @Nonnull final TransactionSynchronizationRegistry registry,
                                        @Nonnull final String invocationKey,
                                        @Nullable final String sessionID,
                                        @Nullable final String requestID )
  {
    // Clear the context completely, in case the caller is not a GwtRpcServlet or does not reset the state.
    ReplicantContextHolder.clean();
    final Object existingKey = registry.getResource( SharedConstants.REPLICATION_INVOCATION_KEY );
    if ( null != existingKey )
    {
      final String message =
        "Attempted to invoke service method '" + invocationKey +
        "' while there is an active replication '" + existingKey + "'";
      throw new IllegalStateException( message );
    }

    registry.putResource( SharedConstants.REPLICATION_INVOCATION_KEY, invocationKey );
    if ( null != sessionID )
    {
      registry.putResource( ServerConstants.SESSION_ID_KEY, sessionID );
    }
    else
    {
      registry.putResource( ServerConstants.SESSION_ID_KEY, null );
    }
    if ( null != requestID )
    {
      registry.putResource( ServerConstants.REQUEST_ID_KEY, requestID );
    }
    else
    {
      registry.putResource( ServerConstants.REQUEST_ID_KEY, null );
    }
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Starting invocation of " + invocationKey + " Thread: " + Thread.currentThread().getId() );
    }
  }

  /**
   * Complete a replication context and submit changes for replication.
   */
  private static void completeReplication( @Nonnull final TransactionSynchronizationRegistry registry,
                                           @Nonnull final EntityManager entityManager,
                                           @Nonnull final EntityMessageEndpoint endpoint,
                                           @Nonnull final String invocationKey )
  {
    // Clear the context completely to ensure it contains only the completion key.
    ReplicantContextHolder.clean();

    if ( Status.STATUS_ACTIVE == registry.getTransactionStatus() &&
         entityManager.isOpen() &&
         !registry.getRollbackOnly() )
    {
      final String sessionID = (String) registry.getResource( ServerConstants.SESSION_ID_KEY );
      final String requestID = (String) registry.getResource( ServerConstants.REQUEST_ID_KEY );
      boolean requestComplete = true;
      entityManager.flush();
      final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( registry );
      final ChangeSet changeSet = EntityMessageCacheUtil.removeSessionChanges( registry );
      if ( null != messageSet || null != changeSet )
      {
        final Collection<EntityMessage> messages =
          null == messageSet ? Collections.<EntityMessage>emptySet() : messageSet.getEntityMessages();
        if ( null != changeSet || messages.size() > 0 )
        {
          requestComplete = !endpoint.saveEntityMessages( sessionID, requestID, messages, changeSet );
        }
      }
      final Boolean complete = (Boolean) registry.getResource( ServerConstants.REQUEST_COMPLETE_KEY );
      // Clear all state in case there is multiple replication contexts started in one transaction
      registry.putResource( SharedConstants.REPLICATION_INVOCATION_KEY, null );
      registry.putResource( ServerConstants.SESSION_ID_KEY, null );
      registry.putResource( ServerConstants.REQUEST_ID_KEY, null );
      registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, null );

      final boolean isComplete = !( null != complete && !complete ) && requestComplete;
      ReplicantContextHolder.put( ServerConstants.REQUEST_COMPLETE_KEY, isComplete ? "1" : "0" );
    }
    else
    {
      ReplicantContextHolder.put( ServerConstants.REQUEST_COMPLETE_KEY, "1" );
    }
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Completed invocation of " + invocationKey + " Thread: " + Thread.currentThread().getId() );
    }
  }
}
