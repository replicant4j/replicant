package org.realityforge.replicant.server.ee;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * Utility class for interacting with replication request infrastructure.
 */
public final class ReplicationRequestUtil
{
  private ReplicationRequestUtil()
  {
  }

  /**
   * Start a replication context.
   *
   * @param invocationKey the identifier of the element that is initiating replication. (i.e. Method name).
   * @param sessionID the id of the session that initiated change if any.
   * @param requestID the id of the request in the session that initiated change..
   */
  public static void startReplication( @Nonnull final TransactionSynchronizationRegistry registry,
                                       @Nonnull final String invocationKey,
                                       @Nullable final String sessionID,
                                       @Nullable final String requestID )
  {
    final Object existingKey = registry.getResource( ReplicantContext.REPLICATION_INVOCATION_KEY );
    if ( null != existingKey )
    {
      final String message =
        "Attempted to invoke service method '" + invocationKey +
        "' while there is an active replication '" + existingKey + "'";
      throw new IllegalStateException( message );
    }

    registry.putResource( ReplicantContext.REPLICATION_INVOCATION_KEY, invocationKey );
    if ( null != sessionID )
    {
      registry.putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
    }
    else
    {
      registry.putResource( ReplicantContext.SESSION_ID_KEY, null );
    }
    if ( null != requestID )
    {
      registry.putResource( ReplicantContext.REQUEST_ID_KEY, requestID );
    }
    else
    {
      registry.putResource( ReplicantContext.REQUEST_ID_KEY, null );
    }
  }

  /**
   * Complete a replication context and submit changes for replication.
   *
   * @return true if the request is complete and did not generate any change messages, false otherwise.
   */
  public static boolean completeReplication( @Nonnull final TransactionSynchronizationRegistry registry,
                                             @Nonnull final EntityManager entityManager,
                                             @Nonnull final EntityMessageEndpoint endpoint )
  {
    if ( Status.STATUS_ACTIVE == registry.getTransactionStatus() &&
         entityManager.isOpen() &&
         !registry.getRollbackOnly() )
    {
      // Remove invocation key to finalize replication context
      registry.putResource( ReplicantContext.REPLICATION_INVOCATION_KEY, null );

      final String sessionID = (String) registry.getResource( ReplicantContext.SESSION_ID_KEY );
      final String requestID = (String) registry.getResource( ReplicantContext.REQUEST_ID_KEY );
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
      final Boolean complete = (Boolean) registry.getResource( ReplicantContext.REQUEST_COMPLETE_KEY );

      // Clear all state in case there is multiple replication contexts started in one transaction
      registry.putResource( ReplicantContext.SESSION_ID_KEY, null );
      registry.putResource( ReplicantContext.REQUEST_ID_KEY, null );
      registry.putResource( ReplicantContext.REQUEST_COMPLETE_KEY, null );

      return !( null != complete && !complete ) && requestComplete;
    }
    else
    {
      return true;
    }
  }
}
