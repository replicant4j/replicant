package replicant.server.ee;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
import replicant.server.EntityMessageEndpoint;
import replicant.server.EntityMessageSet;
import replicant.server.ServerConstants;
import replicant.server.transport.ReplicantSession;

/**
 * Utility class for interacting with replication request infrastructure.
 */
public final class ReplicationRequestUtil
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicationRequestUtil.class.getName() );

  private ReplicationRequestUtil()
  {
  }

  public static <T> T runRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                  @Nonnull final EntityManager entityManager,
                                  @Nonnull final EntityMessageEndpoint endpoint,
                                  @Nonnull final String invocationKey,
                                  @Nullable final ReplicantSession session,
                                  @Nullable final Integer requestId,
                                  @Nonnull final Callable<T> action )
    throws Exception
  {
    startReplication( registry, invocationKey, session, requestId );
    try
    {
      return action.call();
    }
    finally
    {
      completeReplication( registry, entityManager, endpoint, invocationKey );
    }
  }

  static void sessionLockingRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                     @Nonnull final EntityManager entityManager,
                                     @Nonnull final EntityMessageEndpoint endpoint,
                                     @Nonnull final String invocationKey,
                                     @Nonnull final ReplicantSession session,
                                     @Nullable final Integer requestId,
                                     @Nonnull final Runnable action )
    throws InterruptedException
  {
    final ReentrantLock lock = session.getLock();
    try
    {
      lock.lockInterruptibly();
      startReplication( registry, invocationKey, session, requestId );
      try
      {
        action.run();
      }
      finally
      {
        completeReplication( registry, entityManager, endpoint, invocationKey );
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  static void sessionUpdateRequest( @Nonnull final TransactionSynchronizationRegistry registry,
                                    @Nonnull final EntityManager entityManager,
                                    @Nonnull final EntityMessageEndpoint endpoint,
                                    @Nonnull final String invocationKey,
                                    @Nonnull final ReplicantSession session,
                                    final int requestId,
                                    @Nonnull final Runnable action )
    throws InterruptedException
  {
    sessionLockingRequest( registry, entityManager, endpoint, invocationKey, session, requestId, () -> {
      registry.putResource( ServerConstants.SUBSCRIPTION_REQUEST_KEY, "1" );
      action.run();
    } );
  }

  /**
   * Start a replication context.
   *
   * @param invocationKey the identifier of the element that is initiating replication. (i.e. Method name).
   * @param session       the session that initiated change if any.
   * @param requestId     the id of the request in the session that initiated change..
   */
  private static void startReplication( @Nonnull final TransactionSynchronizationRegistry registry,
                                        @Nonnull final String invocationKey,
                                        @Nullable final ReplicantSession session,
                                        @Nullable final Integer requestId )
  {
    // Clear the context completely, in case the caller is not a GwtRpcServlet or does not reset the state.
    ReplicantContextHolder.clean();
    final Object existingKey = registry.getResource( ServerConstants.REPLICATION_INVOCATION_KEY );
    if ( null != existingKey )
    {
      final String message =
        "Attempted to invoke service method '" + invocationKey +
        "' while there is an active replication '" + existingKey + "'";
      throw new IllegalStateException( message );
    }

    registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, invocationKey );
    if ( null != session )
    {
      registry.putResource( ServerConstants.SESSION_ID_KEY, session.getId() );
    }
    else
    {
      registry.putResource( ServerConstants.SESSION_ID_KEY, null );
    }
    registry.putResource( ServerConstants.REQUEST_ID_KEY, requestId );
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
      final String sessionId = (String) registry.getResource( ServerConstants.SESSION_ID_KEY );
      final Integer requestId = (Integer) registry.getResource( ServerConstants.REQUEST_ID_KEY );
      final JsonValue response = (JsonValue) registry.getResource( ServerConstants.REQUEST_RESPONSE_KEY );
      boolean requestComplete = true;
      entityManager.flush();
      final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( registry );
      final ChangeSet changeSet = EntityMessageCacheUtil.removeSessionChanges( registry );
      if ( null != messageSet || null != changeSet || null != requestId )
      {
        final Collection<EntityMessage> messages =
          null == messageSet ? Collections.emptySet() : messageSet.getEntityMessages();
        if ( null != changeSet || !messages.isEmpty() || null != requestId )
        {
          requestComplete = !endpoint.saveEntityMessages( sessionId, requestId, response, messages, changeSet );
        }
      }
      final String complete = (String) registry.getResource( ServerConstants.REQUEST_COMPLETE_KEY );
      // Clear all state in case there is multiple replication contexts started in one transaction
      registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, null );
      registry.putResource( ServerConstants.SESSION_ID_KEY, null );
      registry.putResource( ServerConstants.REQUEST_ID_KEY, null );
      registry.putResource( ServerConstants.REQUEST_COMPLETE_KEY, null );
      registry.putResource( ServerConstants.REQUEST_RESPONSE_KEY, null );
      registry.putResource( ServerConstants.CACHED_RESULT_HANDLED_KEY, null );
      registry.putResource( ServerConstants.SUBSCRIPTION_REQUEST_KEY, null );

      final boolean isComplete = !( null != complete && !"1".equals( complete ) ) && requestComplete;
      ReplicantContextHolder.put( ServerConstants.REQUEST_COMPLETE_KEY, isComplete ? "1" : "0" );
      ReplicantContextHolder.put( ServerConstants.REQUEST_RESPONSE_KEY, response );
    }
    else
    {
      ReplicantContextHolder.put( ServerConstants.REQUEST_COMPLETE_KEY, "1" );
      ReplicantContextHolder.put( ServerConstants.REQUEST_RESPONSE_KEY, null );
    }
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Completed invocation of " + invocationKey + " Thread: " + Thread.currentThread().getId() );
    }
  }
}
