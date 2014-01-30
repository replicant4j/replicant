package org.realityforge.replicant.server.ee;

import java.util.Collection;
import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
public abstract class AbstractReplicationInterceptor
{
  /**
   * The key used to access the registry to get the current call depth.
   */
  private static final String REPLICATION_TX_DEPTH = "ReplicationTxDepth";

  @Resource
  private TransactionSynchronizationRegistry _registry;

  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final Integer depth = (Integer) _registry.getResource( REPLICATION_TX_DEPTH );
    if ( null == depth )
    {
      final String sessionID = (String) ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY );
      if ( null != sessionID )
      {
        _registry.putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
      }
      final String jobID = (String) ReplicantContextHolder.get( ReplicantContext.JOB_ID_KEY );
      if ( null != jobID )
      {
        _registry.putResource( ReplicantContext.JOB_ID_KEY, jobID );
      }
    }
    final String sessionID = (String) _registry.getResource( ReplicantContext.SESSION_ID_KEY );
    final String jobID = (String) _registry.getResource( ReplicantContext.JOB_ID_KEY );
    _registry.putResource( REPLICATION_TX_DEPTH, ( null == depth ? 1 : depth + 1 ) );
    try
    {
      return context.proceed();
    }
    finally
    {
      _registry.putResource( REPLICATION_TX_DEPTH, depth );
      if( null == depth )
      {
        if( getEntityManager().isOpen() )
        {
          getEntityManager().flush();
          final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( _registry );
          if( null != messageSet && !_registry.getRollbackOnly() )
          {
            final Collection<EntityMessage> messages = messageSet.getEntityMessages();
            if( messages.size() > 0 )
            {
              getEndpoint().saveEntityMessages( sessionID, jobID, messages );
            }
          }
        }
        ReplicantContextHolder.clean();
      }
    }
  }

  protected abstract EntityManager getEntityManager();

  protected abstract EntityMessageEndpoint getEndpoint();
}

