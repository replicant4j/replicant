package org.realityforge.replicant.server.ee;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.shared.transport.ReplicantContext;

public abstract class AbstractReplicationRequestManager
  implements ReplicationRequestManager
{
  /**
   * The key used to access the registry to get the current call depth.
   */
  private static final String REPLICATION_TX_DEPTH = "ReplicationTxDepth";

  @Resource
  private TransactionSynchronizationRegistry _registry;

  public void startReplication( @Nullable final String sessionID, @Nullable final String requestID )
  {
    if ( null != sessionID )
    {
      _registry.putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
    }
    if ( null != requestID )
    {
      _registry.putResource( ReplicantContext.REQUEST_ID_KEY, requestID );
    }
  }

  public boolean completeReplication()
  {
    final String sessionID = (String) _registry.getResource( ReplicantContext.SESSION_ID_KEY );
    final String requestID = (String) _registry.getResource( ReplicantContext.REQUEST_ID_KEY );
    if ( getEntityManager().isOpen() && !_registry.getRollbackOnly() )
    {
      boolean requestComplete = true;
      getEntityManager().flush();
      final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( _registry );
      final ChangeSet changeSet = EntityMessageCacheUtil.removeSessionChanges( _registry );
      if ( null != messageSet || null != changeSet )
      {
        final Collection<EntityMessage> messages =
          null == messageSet ? Collections.<EntityMessage>emptySet() : messageSet.getEntityMessages();
        if ( null != changeSet || messages.size() > 0 )
        {
          requestComplete = !getEndpoint().saveEntityMessages( sessionID, requestID, messages, changeSet );
        }
      }
      final Boolean complete = (Boolean) _registry.getResource( ReplicantContext.REQUEST_COMPLETE_KEY );
      return !( null != complete && !complete ) && requestComplete;
    }
    else
    {
      return true;
    }
  }

  public void setReplicationCallDepth( final int depth )
  {
    if ( 0 == depth )
    {
      ReplicantContextHolder.remove( REPLICATION_TX_DEPTH );
    }
    else
    {
      ReplicantContextHolder.put( REPLICATION_TX_DEPTH, depth );
    }
  }

  public int getReplicationCallDepth()
  {
    final Integer depth = (Integer) ReplicantContextHolder.get( REPLICATION_TX_DEPTH );
    //final Integer depth = (Integer) _registry.getResource( REPLICATION_TX_DEPTH );
    if ( null == depth )
    {
      return 0;
    }
    else
    {
      return depth;
    }
  }

  protected abstract EntityManager getEntityManager();

  protected abstract EntityMessageEndpoint getEndpoint();
}
